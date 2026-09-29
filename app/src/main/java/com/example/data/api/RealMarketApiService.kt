package com.example.data.api

import android.util.Log
import com.example.model.AssetClass
import com.example.model.Candle
import com.example.model.MarketAsset
import com.example.model.Timeframe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class RealTickerUpdate(
    val symbol: String,
    val price: Double,
    val changeNominal: Double,
    val changePercent: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24h: String
)

object RealMarketApiService {
    private const val TAG = "RealMarketApi"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    /**
     * Map app Timeframe enum to Binance Kline Interval
     */
    private fun timeframeToBinanceInterval(tf: Timeframe): String = when (tf) {
        Timeframe.SEC_1 -> "1s"
        Timeframe.MIN_1 -> "1m"
        Timeframe.MIN_5 -> "5m"
        Timeframe.MIN_15 -> "15m"
        Timeframe.HOUR_1 -> "1h"
        Timeframe.HOUR_4 -> "4h"
        Timeframe.DAY_1 -> "1d"
        Timeframe.WEEK_1 -> "1w"
    }

    /**
     * Map app Timeframe enum to Yahoo Finance interval and range
     */
    private fun timeframeToYahooParams(tf: Timeframe): Pair<String, String> = when (tf) {
        Timeframe.SEC_1 -> "1m" to "1d"
        Timeframe.MIN_1 -> "1m" to "1d"
        Timeframe.MIN_5 -> "5m" to "1d"
        Timeframe.MIN_15 -> "15m" to "5d"
        Timeframe.HOUR_1 -> "60m" to "1mo"
        Timeframe.HOUR_4 -> "60m" to "3mo"
        Timeframe.DAY_1 -> "1d" to "1y"
        Timeframe.WEEK_1 -> "1wk" to "5y"
    }

    /**
     * Fetch real 24hr tickers for a list of crypto symbols from Binance
     */
    suspend fun fetchBinance24hrTickers(symbols: List<String>): Map<String, RealTickerUpdate> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, RealTickerUpdate>()
        if (symbols.isEmpty()) return@withContext result

        try {
            val formattedSymbols = symbols.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]")
            val url = "https://api.binance.com/api/v3/ticker/24hr?symbols=$formattedSymbols"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TradeViewPro/2.0 Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext result
                val bodyStr = response.body?.string() ?: return@withContext result
                val jsonArray = JSONArray(bodyStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val symbol = obj.getString("symbol")
                    val price = obj.getString("lastPrice").toDoubleOrNull() ?: continue
                    val priceChange = obj.getString("priceChange").toDoubleOrNull() ?: 0.0
                    val priceChangePercent = obj.getString("priceChangePercent").toDoubleOrNull() ?: 0.0
                    val high = obj.getString("highPrice").toDoubleOrNull() ?: price
                    val low = obj.getString("lowPrice").toDoubleOrNull() ?: price
                    val volume = obj.getString("volume").toDoubleOrNull() ?: 0.0

                    val volStr = when {
                        volume >= 1_000_000 -> String.format(Locale.US, "%.1fM", volume / 1_000_000)
                        volume >= 1_000 -> String.format(Locale.US, "%.1fK", volume / 1_000)
                        else -> String.format(Locale.US, "%.1f", volume)
                    }

                    result[symbol] = RealTickerUpdate(
                        symbol = symbol,
                        price = price,
                        changeNominal = priceChange,
                        changePercent = priceChangePercent,
                        high24h = high,
                        low24h = low,
                        volume24h = volStr
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching Binance tickers: ${e.message}")
        }
        result
    }

    /**
     * Fetch real instantaneous price for single symbol
     */
    suspend fun fetchBinanceInstantPrice(symbol: String): Double? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.binance.com/api/v3/ticker/price?symbol=$symbol"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: return@withContext null
                    val json = JSONObject(body)
                    return@withContext json.getString("price").toDoubleOrNull()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Instant price failed for $symbol: ${e.message}")
        }
        null
    }

    /**
     * Fetch real historical candlesticks directly from Binance Kline API
     */
    suspend fun fetchBinanceKlines(symbol: String, timeframe: Timeframe, limit: Int = 120): List<Candle> = withContext(Dispatchers.IO) {
        val rawCandles = mutableListOf<Candle>()
        try {
            val interval = timeframeToBinanceInterval(timeframe)
            val url = "https://api.binance.com/api/v3/klines?symbol=$symbol&interval=$interval&limit=$limit"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TradeViewPro/2.0 Android")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val array = JSONArray(bodyStr)

                for (i in 0 until array.length()) {
                    val kline = array.getJSONArray(i)
                    val openTime = kline.getLong(0)
                    val open = kline.getString(1).toDoubleOrNull() ?: 0.0
                    val high = kline.getString(2).toDoubleOrNull() ?: 0.0
                    val low = kline.getString(3).toDoubleOrNull() ?: 0.0
                    val close = kline.getString(4).toDoubleOrNull() ?: 0.0
                    val volume = kline.getString(5).toDoubleOrNull() ?: 0.0

                    val label = if (timeframe == Timeframe.DAY_1 || timeframe == Timeframe.WEEK_1) {
                        dateFormat.format(Date(openTime))
                    } else {
                        timeFormat.format(Date(openTime))
                    }

                    rawCandles.add(
                        Candle(
                            timestamp = openTime,
                            timeLabel = label,
                            open = open,
                            high = high,
                            low = low,
                            close = close,
                            volume = volume
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Binance klines error: ${e.message}")
        }

        if (rawCandles.isNotEmpty()) {
            calculateTechnicalIndicators(rawCandles)
        } else {
            emptyList()
        }
    }

    /**
     * Fetch real stock, index, forex, commodity quotes and chart from Yahoo Finance v8 API
     */
    suspend fun fetchYahooChart(symbol: String, timeframe: Timeframe): Pair<RealTickerUpdate?, List<Candle>> = withContext(Dispatchers.IO) {
        try {
            val (interval, range) = timeframeToYahooParams(timeframe)
            val encodedSymbol = symbol.replace("^", "%5E").replace("=", "%3D")
            val url = "https://query1.finance.yahoo.com/v8/finance/chart/$encodedSymbol?interval=$interval&range=$range"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Pair(null, emptyList())
                val bodyStr = response.body?.string() ?: return@withContext Pair(null, emptyList())
                val root = JSONObject(bodyStr)
                val chart = root.getJSONObject("chart")
                val results = chart.optJSONArray("result") ?: return@withContext Pair(null, emptyList())
                if (results.length() == 0) return@withContext Pair(null, emptyList())

                val resultObj = results.getJSONObject(0)
                val meta = resultObj.getJSONObject("meta")
                val regularPrice = meta.optDouble("regularMarketPrice", 0.0)
                val prevClose = meta.optDouble("chartPreviousClose", regularPrice)
                val high24h = meta.optDouble("regularMarketDayHigh", regularPrice)
                val low24h = meta.optDouble("regularMarketDayLow", regularPrice)
                val volume24hRaw = meta.optDouble("regularMarketVolume", 0.0)

                val diff = regularPrice - prevClose
                val pct = if (prevClose > 0) (diff / prevClose) * 100 else 0.0

                val volStr = when {
                    volume24hRaw >= 1_000_000 -> String.format(Locale.US, "%.1fM", volume24hRaw / 1_000_000)
                    volume24hRaw >= 1_000 -> String.format(Locale.US, "%.1fK", volume24hRaw / 1_000)
                    else -> String.format(Locale.US, "%.0f", volume24hRaw)
                }

                val ticker = RealTickerUpdate(
                    symbol = symbol,
                    price = regularPrice,
                    changeNominal = diff,
                    changePercent = pct,
                    high24h = high24h,
                    low24h = low24h,
                    volume24h = volStr
                )

                val rawCandles = mutableListOf<Candle>()
                val timestamps = resultObj.optJSONArray("timestamp")
                val indicators = resultObj.optJSONObject("indicators")
                val quotes = indicators?.optJSONArray("quote")
                if (timestamps != null && quotes != null && quotes.length() > 0) {
                    val quote = quotes.getJSONObject(0)
                    val opens = quote.optJSONArray("open")
                    val highs = quote.optJSONArray("high")
                    val lows = quote.optJSONArray("low")
                    val closes = quote.optJSONArray("close")
                    val volumes = quote.optJSONArray("volume")

                    for (i in 0 until timestamps.length()) {
                        val ts = timestamps.optLong(i) * 1000L
                        val o = opens?.optDouble(i) ?: Double.NaN
                        val h = highs?.optDouble(i) ?: Double.NaN
                        val l = lows?.optDouble(i) ?: Double.NaN
                        val c = closes?.optDouble(i) ?: Double.NaN
                        val v = volumes?.optDouble(i) ?: 0.0

                        if (o.isNaN() || h.isNaN() || l.isNaN() || c.isNaN()) continue

                        val label = if (timeframe == Timeframe.DAY_1 || timeframe == Timeframe.WEEK_1) {
                            dateFormat.format(Date(ts))
                        } else {
                            timeFormat.format(Date(ts))
                        }

                        rawCandles.add(
                            Candle(
                                timestamp = ts,
                                timeLabel = label,
                                open = o,
                                high = h,
                                low = l,
                                close = c,
                                volume = v
                            )
                        )
                    }
                }

                val candlesToProcess = if (timeframe == Timeframe.HOUR_4 && rawCandles.size >= 4) {
                    aggregateTo4HourCandles(rawCandles)
                } else {
                    rawCandles
                }

                val finalCandles = if (candlesToProcess.isNotEmpty()) {
                    calculateTechnicalIndicators(candlesToProcess.takeLast(120))
                } else emptyList()

                return@withContext Pair(ticker, finalCandles)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Yahoo chart error for $symbol: ${e.message}")
        }
        Pair(null, emptyList())
    }

    private fun aggregateTo4HourCandles(hourlyCandles: List<Candle>): List<Candle> {
        if (hourlyCandles.isEmpty()) return emptyList()
        val result = mutableListOf<Candle>()
        val chunks = hourlyCandles.chunked(4)
        for (chunk in chunks) {
            val first = chunk.first()
            val last = chunk.last()
            val high = chunk.maxOf { it.high }
            val low = chunk.minOf { it.low }
            val vol = chunk.sumOf { it.volume }
            result.add(
                Candle(
                    timestamp = first.timestamp,
                    timeLabel = first.timeLabel,
                    open = first.open,
                    high = high,
                    low = low,
                    close = last.close,
                    volume = vol
                )
            )
        }
        return result
    }

    /**
     * Fast quote fetcher for non-crypto symbols (India stocks, Indices, Gold, Forex)
     */
    suspend fun fetchYahooFastQuote(symbol: String): RealTickerUpdate? = withContext(Dispatchers.IO) {
        val (ticker, _) = fetchYahooChart(symbol, Timeframe.MIN_15)
        ticker
    }

    /**
     * Calculate professional technical indicators on actual real candles:
     * - Exponential Moving Averages (EMA 20, 50, 200)
     * - Bollinger Bands (20 periods, 2 std dev)
     * - Relative Strength Index (RSI 14)
     * - MACD (12, 26, 9)
     * - Volume Weighted Average Price (VWAP)
     */
    fun calculateTechnicalIndicators(candles: List<Candle>): List<Candle> {
        if (candles.isEmpty()) return emptyList()

        val n = candles.size
        val closes = candles.map { it.close }
        val volumes = candles.map { it.volume }

        // EMA calculations
        val ema20 = calculateEMA(closes, 20)
        val ema50 = calculateEMA(closes, 50)
        val ema200 = calculateEMA(closes, 200)

        // Bollinger Bands (20, 2)
        val bbUpper = DoubleArray(n) { 0.0 }
        val bbMiddle = DoubleArray(n) { 0.0 }
        val bbLower = DoubleArray(n) { 0.0 }
        for (i in 0 until n) {
            val windowSize = min(20, i + 1)
            val sub = closes.subList(max(0, i - windowSize + 1), i + 1)
            val mean = sub.average()
            val variance = sub.map { (it - mean) * (it - mean) }.average()
            val std = sqrt(variance)
            bbMiddle[i] = mean
            bbUpper[i] = mean + (2 * std)
            bbLower[i] = mean - (2 * std)
        }

        // RSI (14)
        val rsi = calculateRSI(closes, 14)

        // MACD (12, 26, 9)
        val ema12 = calculateEMA(closes, 12)
        val ema26 = calculateEMA(closes, 26)
        val macdLine = DoubleArray(n) { i -> ema12[i] - ema26[i] }
        val macdSignal = calculateEMA(macdLine.toList(), 9)

        // VWAP
        var cumVol = 0.0
        var cumVolPrice = 0.0
        val vwap = DoubleArray(n) { i ->
            val c = candles[i]
            val typicalPrice = (c.high + c.low + c.close) / 3.0
            cumVol += max(1.0, c.volume)
            cumVolPrice += typicalPrice * max(1.0, c.volume)
            cumVolPrice / cumVol
        }

        return candles.mapIndexed { i, c ->
            val hist = macdLine[i] - macdSignal[i]
            c.copy(
                ema20 = ema20[i],
                ema50 = ema50[i],
                ema200 = ema200[i],
                bbUpper = bbUpper[i],
                bbMiddle = bbMiddle[i],
                bbLower = bbLower[i],
                rsi = rsi[i],
                macd = macdLine[i],
                macdSignal = macdSignal[i],
                macdHist = hist,
                vwap = vwap[i]
            )
        }
    }

    private fun calculateEMA(data: List<Double>, period: Int): DoubleArray {
        val n = data.size
        val ema = DoubleArray(n)
        if (n == 0) return ema
        val k = 2.0 / (period + 1.0)
        ema[0] = data[0]
        for (i in 1 until n) {
            ema[i] = (data[i] * k) + (ema[i - 1] * (1.0 - k))
        }
        return ema
    }

    private fun calculateRSI(data: List<Double>, period: Int): DoubleArray {
        val n = data.size
        val rsi = DoubleArray(n) { 50.0 }
        if (n <= period) return rsi

        var avgGain = 0.0
        var avgLoss = 0.0

        for (i in 1..period) {
            val change = data[i] - data[i - 1]
            if (change > 0) avgGain += change else avgLoss -= change
        }
        avgGain /= period
        avgLoss /= period

        if (avgLoss == 0.0) rsi[period] = 100.0 else {
            val rs = avgGain / avgLoss
            rsi[period] = 100.0 - (100.0 / (1.0 + rs))
        }

        for (i in (period + 1) until n) {
            val change = data[i] - data[i - 1]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) -change else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period

            if (avgLoss == 0.0) {
                rsi[i] = 100.0
            } else {
                val rs = avgGain / avgLoss
                rsi[i] = 100.0 - (100.0 / (1.0 + rs))
            }
        }
        return rsi
    }
}
