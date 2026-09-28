package com.example.data

import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class MarketRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    // Initial asset catalogue across Crypto, Stocks, Forex, Indices, Commodities
    private val _assets = MutableStateFlow<List<MarketAsset>>(initialAssets())
    val assets: StateFlow<List<MarketAsset>> = _assets.asStateFlow()

    // Selected asset for charting
    private val _selectedAsset = MutableStateFlow<MarketAsset>(_assets.value.first())
    val selectedAsset: StateFlow<MarketAsset> = _selectedAsset.asStateFlow()

    // Timeframe
    private val _selectedTimeframe = MutableStateFlow<Timeframe>(Timeframe.MIN_15)
    val selectedTimeframe: StateFlow<Timeframe> = _selectedTimeframe.asStateFlow()

    // Candles cache per (symbol, timeframe)
    private val candleCache = mutableMapOf<String, List<Candle>>()

    private val _currentCandles = MutableStateFlow<List<Candle>>(emptyList())
    val currentCandles: StateFlow<List<Candle>> = _currentCandles.asStateFlow()

    // Bar Replay & Forward Backtest Simulation Engine
    private var fullCandlesList = listOf<Candle>()
    private val _isReplayMode = MutableStateFlow(false)
    val isReplayMode: StateFlow<Boolean> = _isReplayMode.asStateFlow()

    private val _isReplayPlaying = MutableStateFlow(false)
    val isReplayPlaying: StateFlow<Boolean> = _isReplayPlaying.asStateFlow()

    private val _replayIndex = MutableStateFlow(0)
    val replayIndex: StateFlow<Int> = _replayIndex.asStateFlow()

    private val _replayTotalBars = MutableStateFlow(0)
    val replayTotalBars: StateFlow<Int> = _replayTotalBars.asStateFlow()

    private val _replaySpeed = MutableStateFlow(1f) // 1x, 2x, 5x
    val replaySpeed: StateFlow<Float> = _replaySpeed.asStateFlow()

    private var replayJob: Job? = null

    // Active Paper Trading
    private val _paperBalance = MutableStateFlow(100000.0) // $100,000 USD virtual balance
    val paperBalance: StateFlow<Double> = _paperBalance.asStateFlow()

    private val _openPositions = MutableStateFlow<List<PaperTrade>>(emptyList())
    val openPositions: StateFlow<List<PaperTrade>> = _openPositions.asStateFlow()

    private val _tradeHistory = MutableStateFlow<List<PaperTrade>>(emptyList())
    val tradeHistory: StateFlow<List<PaperTrade>> = _tradeHistory.asStateFlow()

    // Alerts
    private val _alerts = MutableStateFlow<List<PriceAlert>>(initialAlerts())
    val alerts: StateFlow<List<PriceAlert>> = _alerts.asStateFlow()

    private val _triggeredAlertEvent = MutableStateFlow<PriceAlert?>(null)
    val triggeredAlertEvent: StateFlow<PriceAlert?> = _triggeredAlertEvent.asStateFlow()

    // Community Ideas & News
    private val _ideas = MutableStateFlow<List<TradingIdea>>(initialIdeas())
    val ideas: StateFlow<List<TradingIdea>> = _ideas.asStateFlow()

    private val _news = MutableStateFlow<List<MarketNewsItem>>(initialNews())
    val news: StateFlow<List<MarketNewsItem>> = _news.asStateFlow()

    init {
        // Load initial candles for first asset
        loadCandlesForSelected()
        // Start live ticker engine loop
        startLiveMarketFeed()
    }

    fun selectAsset(symbol: String) {
        val asset = _assets.value.find { it.symbol == symbol } ?: return
        _selectedAsset.value = asset
        loadCandlesForSelected()
    }

    fun selectTimeframe(tf: Timeframe) {
        _selectedTimeframe.value = tf
        loadCandlesForSelected()
    }

    fun toggleFavorite(symbol: String) {
        _assets.value = _assets.value.map {
            if (it.symbol == symbol) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    private fun loadCandlesForSelected() {
        val asset = _selectedAsset.value
        val tf = _selectedTimeframe.value
        val cacheKey = "${asset.symbol}_${tf.name}"

        val cached = candleCache[cacheKey]
        val candles = if (cached != null && cached.isNotEmpty()) {
            cached
        } else {
            val generated = generateCandleHistory(asset.price, tf, count = 120)
            candleCache[cacheKey] = generated
            generated
        }
        fullCandlesList = candles
        _replayTotalBars.value = candles.size
        if (_isReplayMode.value) {
            val safeIdx = _replayIndex.value.coerceIn(10, candles.size)
            _currentCandles.value = fullCandlesList.take(safeIdx)
        } else {
            _currentCandles.value = candles
        }
    }

    // --- Bar Replay & Forward Testing Methods ---

    fun startReplay(cutoffRatio: Float = 0.5f) {
        val total = fullCandlesList.size
        if (total < 10) return
        val startIndex = (total * cutoffRatio).toInt().coerceIn(10, total - 1)
        _isReplayMode.value = true
        _isReplayPlaying.value = false
        _replayIndex.value = startIndex
        _replayTotalBars.value = total
        _currentCandles.value = fullCandlesList.take(startIndex)
        val cutoffCandle = fullCandlesList[startIndex - 1]
        _selectedAsset.value = _selectedAsset.value.copy(price = cutoffCandle.close)
    }

    fun exitReplay() {
        replayJob?.cancel()
        _isReplayMode.value = false
        _isReplayPlaying.value = false
        _currentCandles.value = fullCandlesList
        val last = fullCandlesList.lastOrNull()
        if (last != null) {
            _selectedAsset.value = _selectedAsset.value.copy(price = last.close)
        }
    }

    fun stepReplayForward() {
        val currentIdx = _replayIndex.value
        val total = fullCandlesList.size
        if (currentIdx < total) {
            val nextIdx = currentIdx + 1
            _replayIndex.value = nextIdx
            _currentCandles.value = fullCandlesList.take(nextIdx)
            val revealed = fullCandlesList[nextIdx - 1]
            _selectedAsset.value = _selectedAsset.value.copy(price = revealed.close)
            // Update paper trades with backtested price
            updateOpenPositions(_assets.value.map { if (it.symbol == _selectedAsset.value.symbol) it.copy(price = revealed.close) else it })
        } else {
            _isReplayPlaying.value = false
            replayJob?.cancel()
        }
    }

    fun stepReplayBackward() {
        val currentIdx = _replayIndex.value
        if (currentIdx > 10) {
            val prevIdx = currentIdx - 1
            _replayIndex.value = prevIdx
            _currentCandles.value = fullCandlesList.take(prevIdx)
            val revealed = fullCandlesList[prevIdx - 1]
            _selectedAsset.value = _selectedAsset.value.copy(price = revealed.close)
        }
    }

    fun toggleReplayPlay() {
        if (_isReplayPlaying.value) {
            _isReplayPlaying.value = false
            replayJob?.cancel()
        } else {
            _isReplayPlaying.value = true
            replayJob = scope.launch {
                while (isActive && _isReplayPlaying.value) {
                    val delayMs = (1000L / _replaySpeed.value).toLong().coerceAtLeast(200L)
                    delay(delayMs)
                    val currentIdx = _replayIndex.value
                    if (currentIdx < fullCandlesList.size) {
                        stepReplayForward()
                    } else {
                        _isReplayPlaying.value = false
                        break
                    }
                }
            }
        }
    }

    fun setReplaySpeed(speed: Float) {
        _replaySpeed.value = speed
    }

    fun jumpReplayTo(index: Int) {
        val safe = index.coerceIn(10, fullCandlesList.size)
        _replayIndex.value = safe
        _currentCandles.value = fullCandlesList.take(safe)
        val revealed = fullCandlesList[safe - 1]
        _selectedAsset.value = _selectedAsset.value.copy(price = revealed.close)
    }

    private fun startLiveMarketFeed() {
        scope.launch {
            val random = Random()
            while (isActive) {
                delay(1200L) // Real-time pulse every 1.2s

                val currentAssets = _assets.value
                val updatedAssets = currentAssets.map { asset ->
                    // Micro fluctuation between -0.3% to +0.3%
                    val deltaPercent = (random.nextGaussian() * 0.0015)
                    val newPrice = (asset.price * (1.0 + deltaPercent)).coerceAtLeast(0.0001)
                    val formattedPrice = roundToDecimals(newPrice, if (asset.price < 2.0) 4 else 2)
                    val nominalDiff = formattedPrice - asset.price
                    val updatedChangeNominal = asset.changeNominal + nominalDiff
                    val updatedChangePercent = (updatedChangeNominal / (formattedPrice - updatedChangeNominal)) * 100.0

                    val dir = when {
                        nominalDiff > 0 -> 1
                        nominalDiff < 0 -> -1
                        else -> 0
                    }

                    val updatedHigh = max(asset.high24h, formattedPrice)
                    val updatedLow = min(asset.low24h, formattedPrice)
                    val updatedSparkline = (asset.sparkline.drop(1) + formattedPrice)

                    asset.copy(
                        price = formattedPrice,
                        changeNominal = updatedChangeNominal,
                        changePercent = updatedChangePercent,
                        high24h = updatedHigh,
                        low24h = updatedLow,
                        sparkline = updatedSparkline,
                        lastUpdateDirection = dir
                    )
                }

                _assets.value = updatedAssets

                // Update selected asset price and current candle
                val sel = updatedAssets.find { it.symbol == _selectedAsset.value.symbol }
                if (sel != null) {
                    _selectedAsset.value = sel
                    updateActiveCandle(sel.price)
                }

                // Update open positions real-time PnL
                updateOpenPositions(updatedAssets)

                // Check alerts
                checkAlerts(updatedAssets)
            }
        }
    }

    private fun updateActiveCandle(latestPrice: Double) {
        if (_isReplayMode.value) return // Preserve historical cutoff in backtest replay mode
        val candles = _currentCandles.value
        if (candles.isEmpty()) return

        val lastIndex = candles.size - 1
        val last = candles[lastIndex]
        val updatedHigh = max(last.high, latestPrice)
        val updatedLow = min(last.low, latestPrice)
        val updatedClose = latestPrice
        val updatedVol = last.volume + (abs(latestPrice - last.close) * 100.0).coerceAtLeast(0.5)

        val updatedLast = last.copy(
            high = updatedHigh,
            low = updatedLow,
            close = updatedClose,
            volume = updatedVol
        )

        val mutableList = candles.toMutableList()
        mutableList[lastIndex] = updatedLast

        // Recompute latest indicators for this candle
        _currentCandles.value = recalculateIndicators(mutableList)
    }

    private fun updateOpenPositions(assets: List<MarketAsset>) {
        val open = _openPositions.value
        if (open.isEmpty()) return

        val assetMap = assets.associateBy { it.symbol }
        val updated = open.map { pos ->
            val asset = assetMap[pos.symbol]
            if (asset != null) {
                pos.copy(currentPrice = asset.price)
            } else {
                pos
            }
        }
        _openPositions.value = updated
    }

    private fun checkAlerts(assets: List<MarketAsset>) {
        val assetMap = assets.associateBy { it.symbol }
        val alertsList = _alerts.value
        var triggered: PriceAlert? = null

        val updatedAlerts = alertsList.map { alert ->
            if (!alert.isActive || alert.isTriggered) return@map alert

            val asset = assetMap[alert.symbol] ?: return@map alert
            val isHit = when (alert.condition) {
                AlertCondition.CROSS_ABOVE, AlertCondition.GREATER_THAN -> asset.price >= alert.targetPrice
                AlertCondition.CROSS_BELOW, AlertCondition.LESS_THAN -> asset.price <= alert.targetPrice
            }

            if (isHit) {
                triggered = alert
                alert.copy(isTriggered = true, isActive = false)
            } else {
                alert
            }
        }

        if (triggered != null) {
            _alerts.value = updatedAlerts
            _triggeredAlertEvent.value = triggered
        }
    }

    fun dismissTriggeredAlert() {
        _triggeredAlertEvent.value = null
    }

    // --- Paper Trading Operations ---

    fun placePaperOrder(
        symbol: String,
        type: PositionType,
        quantity: Double,
        leverage: Int = 1,
        stopLoss: Double? = null,
        takeProfit: Double? = null
    ): Boolean {
        val asset = _assets.value.find { it.symbol == symbol } ?: return false
        val marginRequired = (asset.price * quantity) / leverage
        if (_paperBalance.value < marginRequired) {
            return false // Insufficient paper funds
        }

        _paperBalance.value -= marginRequired

        val newPosition = PaperTrade(
            id = UUID.randomUUID().toString().take(8),
            symbol = symbol,
            type = type,
            entryPrice = asset.price,
            currentPrice = asset.price,
            quantity = quantity,
            leverage = leverage,
            stopLoss = stopLoss,
            takeProfit = takeProfit
        )

        _openPositions.value = _openPositions.value + newPosition
        return true
    }

    fun closePaperPosition(tradeId: String) {
        val position = _openPositions.value.find { it.id == tradeId } ?: return
        val realizedPnl = position.pnl
        val returnedMargin = position.margin

        _paperBalance.value += (returnedMargin + realizedPnl)
        _openPositions.value = _openPositions.value.filter { it.id != tradeId }

        val closed = position.copy(
            isClosed = true,
            closePrice = position.currentPrice
        )
        _tradeHistory.value = listOf(closed) + _tradeHistory.value
    }

    fun resetPaperAccount() {
        _paperBalance.value = 100000.0
        _openPositions.value = emptyList()
        _tradeHistory.value = emptyList()
    }

    // --- Alert Operations ---

    fun addAlert(symbol: String, targetPrice: Double, condition: AlertCondition, note: String) {
        val newAlert = PriceAlert(
            id = UUID.randomUUID().toString().take(8),
            symbol = symbol,
            targetPrice = targetPrice,
            condition = condition,
            note = note.ifBlank { "$symbol target at $targetPrice" }
        )
        _alerts.value = listOf(newAlert) + _alerts.value
    }

    fun toggleAlert(id: String) {
        _alerts.value = _alerts.value.map {
            if (it.id == id) it.copy(isActive = !it.isActive, isTriggered = false) else it
        }
    }

    fun removeAlert(id: String) {
        _alerts.value = _alerts.value.filter { it.id != id }
    }

    // --- Technical Analysis Calculator ---

    fun getTechnicalAnalysis(symbol: String): TechnicalAnalysis {
        val asset = _assets.value.find { it.symbol == symbol } ?: _assets.value.first()
        val candles = _currentCandles.value
        val rsiVal = candles.lastOrNull()?.rsi ?: 56.4
        val macdVal = candles.lastOrNull()?.macd ?: 14.2
        val stochVal = 62.0
        val adxVal = 28.5

        val buyCount = when {
            rsiVal in 45.0..65.0 && macdVal > 0 -> 14
            rsiVal < 35.0 -> 18 // oversold rebound
            else -> 8
        }
        val sellCount = when {
            rsiVal > 70.0 -> 14
            macdVal < 0 -> 10
            else -> 4
        }
        val neutralCount = 26 - buyCount - sellCount

        val overall = when {
            buyCount >= 14 -> TechnicalRating.STRONG_BUY
            buyCount > sellCount -> TechnicalRating.BUY
            sellCount >= 14 -> TechnicalRating.STRONG_SELL
            sellCount > buyCount -> TechnicalRating.SELL
            else -> TechnicalRating.NEUTRAL
        }

        return TechnicalAnalysis(
            overall = overall,
            oscillators = if (rsiVal > 70) TechnicalRating.SELL else if (rsiVal < 30) TechnicalRating.BUY else TechnicalRating.NEUTRAL,
            movingAverages = if (asset.changePercent > 0) TechnicalRating.STRONG_BUY else TechnicalRating.SELL,
            buySignals = buyCount,
            neutralSignals = neutralCount.coerceAtLeast(2),
            sellSignals = sellCount.coerceAtLeast(1),
            rsi = rsiVal,
            macd = macdVal,
            stochK = stochVal,
            adx = adxVal
        )
    }

    // --- Candle Generation & Indicator Math ---

    private fun generateCandleHistory(basePrice: Double, timeframe: Timeframe, count: Int): List<Candle> {
        val list = mutableListOf<Candle>()
        val cal = Calendar.getInstance()
        val timeStepMs = timeframe.seconds * 1000L
        cal.timeInMillis = System.currentTimeMillis() - (count * timeStepMs)

        var currentPrice = basePrice * 0.92 // Start slight trend
        val sdf = SimpleDateFormat(if (timeframe.seconds < 3600) "HH:mm" else "dd MMM", Locale.US)

        val random = Random(42) // Seeded for consistency

        for (i in 0 until count) {
            val walk = (random.nextGaussian() * (basePrice * 0.005))
            val open = currentPrice
            val close = (open + walk).coerceAtLeast(0.001)
            val wickUp = abs(random.nextGaussian() * (basePrice * 0.004))
            val wickDown = abs(random.nextGaussian() * (basePrice * 0.004))
            val high = max(open, close) + wickUp
            val low = (min(open, close) - wickDown).coerceAtLeast(0.0001)
            val volume = abs(random.nextGaussian() * 500.0 + 800.0)

            val timeLabel = sdf.format(cal.time)
            cal.timeInMillis += timeStepMs
            currentPrice = close

            list.add(
                Candle(
                    timestamp = cal.timeInMillis,
                    timeLabel = timeLabel,
                    open = roundToDecimals(open, 2),
                    high = roundToDecimals(high, 2),
                    low = roundToDecimals(low, 2),
                    close = roundToDecimals(close, 2),
                    volume = roundToDecimals(volume, 1)
                )
            )
        }

        // Anchor the very last candle's close to the current actual asset basePrice
        if (list.isNotEmpty()) {
            val last = list.last()
            list[list.size - 1] = last.copy(
                close = basePrice,
                high = max(last.high, basePrice),
                low = min(last.low, basePrice)
            )
        }

        return recalculateIndicators(list)
    }

    private fun recalculateIndicators(candles: List<Candle>): List<Candle> {
        if (candles.isEmpty()) return candles

        val closes = candles.map { it.close }
        val n = candles.size

        // Calculate EMA 20, 50, 200
        val ema20List = calculateEMA(closes, 20)
        val ema50List = calculateEMA(closes, 50)
        val ema200List = calculateEMA(closes, min(200, n))

        // Calculate Bollinger Bands (period 20, k=2.0)
        val (bbUpperList, bbMiddleList, bbLowerList) = calculateBollingerBands(closes, 20, 2.0)

        // Calculate RSI (period 14)
        val rsiList = calculateRSI(closes, 14)

        // Calculate MACD (fast=12, slow=26, signal=9)
        val (macdList, macdSignalList, macdHistList) = calculateMACD(closes, 12, 26, 9)

        // Calculate cumulative VWAP
        val vwapList = calculateVWAP(candles)

        return candles.mapIndexed { i, candle ->
            candle.copy(
                ema20 = ema20List.getOrNull(i),
                ema50 = ema50List.getOrNull(i),
                ema200 = ema200List.getOrNull(i),
                bbUpper = bbUpperList.getOrNull(i),
                bbMiddle = bbMiddleList.getOrNull(i),
                bbLower = bbLowerList.getOrNull(i),
                rsi = rsiList.getOrNull(i),
                macd = macdList.getOrNull(i),
                macdSignal = macdSignalList.getOrNull(i),
                macdHist = macdHistList.getOrNull(i),
                vwap = vwapList.getOrNull(i)
            )
        }
    }

    private fun calculateEMA(values: List<Double>, period: Int): List<Double?> {
        val result = arrayOfNulls<Double>(values.size)
        if (values.size < period) return result.toList()

        val k = 2.0 / (period + 1)
        var sum = 0.0
        for (i in 0 until period) {
            sum += values[i]
        }
        var ema = sum / period
        result[period - 1] = ema

        for (i in period until values.size) {
            ema = (values[i] * k) + (ema * (1.0 - k))
            result[i] = ema
        }
        return result.toList()
    }

    private fun calculateBollingerBands(
        values: List<Double>,
        period: Int,
        numStdDev: Double
    ): Triple<List<Double?>, List<Double?>, List<Double?>> {
        val uppers = arrayOfNulls<Double>(values.size)
        val middles = arrayOfNulls<Double>(values.size)
        val lowers = arrayOfNulls<Double>(values.size)

        if (values.size < period) return Triple(uppers.toList(), middles.toList(), lowers.toList())

        for (i in period - 1 until values.size) {
            val window = values.subList(i - period + 1, i + 1)
            val mean = window.average()
            val variance = window.map { (it - mean) * (it - mean) }.average()
            val stdDev = sqrt(variance)

            middles[i] = mean
            uppers[i] = mean + (numStdDev * stdDev)
            lowers[i] = mean - (numStdDev * stdDev)
        }
        return Triple(uppers.toList(), middles.toList(), lowers.toList())
    }

    private fun calculateRSI(values: List<Double>, period: Int): List<Double?> {
        val result = arrayOfNulls<Double>(values.size)
        if (values.size <= period) return result.toList()

        var gainSum = 0.0
        var lossSum = 0.0

        for (i in 1..period) {
            val change = values[i] - values[i - 1]
            if (change > 0) gainSum += change else lossSum += abs(change)
        }

        var avgGain = gainSum / period
        var avgLoss = lossSum / period
        var rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
        result[period] = 100.0 - (100.0 / (1.0 + rs))

        for (i in period + 1 until values.size) {
            val change = values[i] - values[i - 1]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period

            rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            result[i] = 100.0 - (100.0 / (1.0 + rs))
        }

        return result.toList()
    }

    private fun calculateMACD(
        values: List<Double>,
        fast: Int,
        slow: Int,
        signalPeriod: Int
    ): Triple<List<Double?>, List<Double?>, List<Double?>> {
        val fastEma = calculateEMA(values, fast)
        val slowEma = calculateEMA(values, slow)

        val macdLine = arrayOfNulls<Double>(values.size)
        for (i in values.indices) {
            val f = fastEma[i]
            val s = slowEma[i]
            if (f != null && s != null) {
                macdLine[i] = f - s
            }
        }

        // Signal is EMA of MACD Line
        val nonNullIndices = macdLine.indices.filter { macdLine[it] != null }
        val signalLine = arrayOfNulls<Double>(values.size)
        val hist = arrayOfNulls<Double>(values.size)

        if (nonNullIndices.size >= signalPeriod) {
            val validMacd = nonNullIndices.map { macdLine[it]!! }
            val signalCalc = calculateEMA(validMacd, signalPeriod)

            nonNullIndices.forEachIndexed { idx, origIdx ->
                signalLine[origIdx] = signalCalc[idx]
                val macd = macdLine[origIdx]
                val sig = signalLine[origIdx]
                if (macd != null && sig != null) {
                    hist[origIdx] = macd - sig
                }
            }
        }

        return Triple(macdLine.toList(), signalLine.toList(), hist.toList())
    }

    private fun calculateVWAP(candles: List<Candle>): List<Double> {
        var cumTypicalVol = 0.0
        var cumVol = 0.0
        return candles.map { c ->
            val typicalPrice = (c.high + c.low + c.close) / 3.0
            cumTypicalVol += typicalPrice * c.volume
            cumVol += c.volume
            if (cumVol > 0) cumTypicalVol / cumVol else typicalPrice
        }
    }

    private fun roundToDecimals(value: Double, decimals: Int): Double {
        val factor = 10.0.pow(decimals)
        return kotlin.math.round(value * factor) / factor
    }

    // --- Seed Data ---

    private fun initialAssets(): List<MarketAsset> = listOf(
        MarketAsset(
            id = "btc",
            symbol = "BTC/USDT",
            name = "Bitcoin",
            exchange = "BINANCE",
            assetClass = AssetClass.CRYPTO,
            price = 96420.50,
            changeNominal = 2410.20,
            changePercent = 2.56,
            high24h = 97100.0,
            low24h = 93850.0,
            volume24h = "38.4B",
            marketCap = "$1.89T",
            sparkline = listOf(93900.0, 94200.0, 95100.0, 94800.0, 95600.0, 96100.0, 96420.5),
            technicalRating = TechnicalRating.STRONG_BUY,
            isFavorite = true
        ),
        MarketAsset(
            id = "eth",
            symbol = "ETH/USDT",
            name = "Ethereum",
            exchange = "BINANCE",
            assetClass = AssetClass.CRYPTO,
            price = 3385.10,
            changeNominal = 112.40,
            changePercent = 3.43,
            high24h = 3420.0,
            low24h = 3240.0,
            volume24h = "19.2B",
            marketCap = "$407B",
            sparkline = listOf(3250.0, 3290.0, 3310.0, 3280.0, 3350.0, 3385.1),
            technicalRating = TechnicalRating.BUY,
            isFavorite = true
        ),
        MarketAsset(
            id = "sol",
            symbol = "SOL/USDT",
            name = "Solana",
            exchange = "BINANCE",
            assetClass = AssetClass.CRYPTO,
            price = 214.80,
            changeNominal = 12.30,
            changePercent = 6.07,
            high24h = 218.0,
            low24h = 199.5,
            volume24h = "7.8B",
            marketCap = "$101B",
            sparkline = listOf(200.0, 203.0, 206.0, 209.0, 212.0, 214.8),
            technicalRating = TechnicalRating.STRONG_BUY,
            isFavorite = true
        ),
        MarketAsset(
            id = "nvda",
            symbol = "NVDA",
            name = "NVIDIA Corporation",
            exchange = "NASDAQ",
            assetClass = AssetClass.STOCKS,
            price = 142.60,
            changeNominal = 3.75,
            changePercent = 2.70,
            high24h = 144.10,
            low24h = 138.20,
            volume24h = "45.1M",
            marketCap = "$3.51T",
            sparkline = listOf(138.5, 139.2, 140.8, 141.2, 142.6),
            technicalRating = TechnicalRating.STRONG_BUY,
            isFavorite = true
        ),
        MarketAsset(
            id = "aapl",
            symbol = "AAPL",
            name = "Apple Inc.",
            exchange = "NASDAQ",
            assetClass = AssetClass.STOCKS,
            price = 238.45,
            changeNominal = -1.25,
            changePercent = -0.52,
            high24h = 241.0,
            low24h = 237.1,
            volume24h = "32.8M",
            marketCap = "$3.62T",
            sparkline = listOf(240.2, 239.5, 238.9, 239.1, 238.45),
            technicalRating = TechnicalRating.NEUTRAL
        ),
        MarketAsset(
            id = "tsla",
            symbol = "TSLA",
            name = "Tesla, Inc.",
            exchange = "NASDAQ",
            assetClass = AssetClass.STOCKS,
            price = 345.20,
            changeNominal = 18.40,
            changePercent = 5.63,
            high24h = 349.8,
            low24h = 322.0,
            volume24h = "68.2M",
            marketCap = "$1.11T",
            sparkline = listOf(324.0, 330.0, 335.0, 342.0, 345.2),
            technicalRating = TechnicalRating.STRONG_BUY
        ),
        MarketAsset(
            id = "gold",
            symbol = "XAU/USD",
            name = "Gold Ounce Spot",
            exchange = "COMEX",
            assetClass = AssetClass.COMMODITIES,
            price = 2742.60,
            changeNominal = 14.80,
            changePercent = 0.54,
            high24h = 2755.0,
            low24h = 2724.0,
            volume24h = "14.2B",
            marketCap = "$17.4T",
            sparkline = listOf(2725.0, 2730.0, 2738.0, 2735.0, 2742.6),
            technicalRating = TechnicalRating.BUY
        ),
        MarketAsset(
            id = "spx",
            symbol = "SPX 500",
            name = "S&P 500 Index",
            exchange = "CBOE",
            assetClass = AssetClass.INDICES,
            price = 6012.40,
            changeNominal = 34.60,
            changePercent = 0.58,
            high24h = 6025.0,
            low24h = 5970.0,
            volume24h = "4.2B",
            marketCap = "$45T",
            sparkline = listOf(5980.0, 5992.0, 6005.0, 6002.0, 6012.4),
            technicalRating = TechnicalRating.BUY
        ),
        MarketAsset(
            id = "eurusd",
            symbol = "EUR/USD",
            name = "Euro / US Dollar",
            exchange = "FXCM",
            assetClass = AssetClass.FOREX,
            price = 1.0542,
            changeNominal = -0.0032,
            changePercent = -0.30,
            high24h = 1.0585,
            low24h = 1.0520,
            volume24h = "120B",
            marketCap = "-",
            sparkline = listOf(1.058, 1.0565, 1.055, 1.0545, 1.0542),
            technicalRating = TechnicalRating.SELL
        ),
        MarketAsset(
            id = "gbpusd",
            symbol = "GBP/USD",
            name = "British Pound / USD",
            exchange = "FXCM",
            assetClass = AssetClass.FOREX,
            price = 1.2680,
            changeNominal = 0.0045,
            changePercent = 0.36,
            high24h = 1.2710,
            low24h = 1.2625,
            volume24h = "85B",
            marketCap = "-",
            sparkline = listOf(1.263, 1.2645, 1.266, 1.267, 1.268),
            technicalRating = TechnicalRating.BUY
        )
    )

    private fun initialAlerts(): List<PriceAlert> = listOf(
        PriceAlert(
            id = "alt-1",
            symbol = "BTC/USDT",
            targetPrice = 100000.0,
            condition = AlertCondition.CROSS_ABOVE,
            note = "Historic $100K BTC Breakthrough Target",
            isActive = true
        ),
        PriceAlert(
            id = "alt-2",
            symbol = "NVDA",
            targetPrice = 150.0,
            condition = AlertCondition.CROSS_ABOVE,
            note = "Nvidia all-time high resistance test",
            isActive = true
        ),
        PriceAlert(
            id = "alt-3",
            symbol = "ETH/USDT",
            targetPrice = 3500.0,
            condition = AlertCondition.CROSS_ABOVE,
            note = "Ethereum ascending triangle breakout",
            isActive = true
        )
    )

    private fun initialIdeas(): List<TradingIdea> = listOf(
        TradingIdea(
            id = "idea-1",
            author = "CryptoWizard_Pro",
            authorBadge = "Top Author",
            title = "Bitcoin Golden Cross: The Road to $105,000",
            symbol = "BTC/USDT",
            timeframe = "1D",
            bias = PositionType.LONG,
            likes = 1420,
            comments = 188,
            timeAgo = "2h ago",
            content = "Bitcoin has successfully backtested the 20-day EMA support on heavy institutional volume. The RSI reset from 78 down to 62 provides ample room for continuation. Watch for confirmed breakout above $97,500.",
            tags = listOf("#Bitcoin", "#Bullish", "#GoldenCross", "#Crypto")
        ),
        TradingIdea(
            id = "idea-2",
            author = "AlphaQuant_Labs",
            authorBadge = "Verified Pro",
            title = "NVDA Symmetrical Triangle Breakout Looming",
            symbol = "NVDA",
            timeframe = "4h",
            bias = PositionType.LONG,
            likes = 890,
            comments = 94,
            timeAgo = "4h ago",
            content = "Consolidation phase near upper Bollinger Band suggests massive energy build-up ahead of next AI datacenter revenue report. Stop-loss at $136 with initial target at $155.",
            tags = listOf("#Stocks", "#NVIDIA", "#TechGains", "#SwingTrade")
        ),
        TradingIdea(
            id = "idea-3",
            author = "MacroForex_Signals",
            authorBadge = "Pro Trader",
            title = "EUR/USD Double Top Reversal Play",
            symbol = "EUR/USD",
            timeframe = "1h",
            bias = PositionType.SHORT,
            likes = 540,
            comments = 42,
            timeAgo = "6h ago",
            content = "Bearish divergence on the hourly MACD histogram with failure to clear 1.0590 resistance. Looking for short scalps targeting the liquidity pool at 1.0480.",
            tags = listOf("#Forex", "#EURUSD", "#Scalping", "#Short")
        )
    )

    private fun initialNews(): List<MarketNewsItem> = listOf(
        MarketNewsItem(
            id = "news-1",
            headline = "Institutional Inflows Hit Record $3.2B as Crypto ETFs See Unprecedented Demand",
            source = "Bloomberg Markets",
            timeAgo = "18m ago",
            sentiment = NewsSentiment.BULLISH,
            category = "Crypto"
        ),
        MarketNewsItem(
            id = "news-2",
            headline = "Tech Megacaps Lead S&P 500 Rally Amid Strong AI Cloud Hardware Guidance",
            source = "Reuters Finance",
            timeAgo = "45m ago",
            sentiment = NewsSentiment.BULLISH,
            category = "Equities"
        ),
        MarketNewsItem(
            id = "news-3",
            headline = "Federal Reserve Signals Data-Dependent Stance on Upcoming Interest Rate Decisions",
            source = "Wall Street Journal",
            timeAgo = "2h ago",
            sentiment = NewsSentiment.NEUTRAL,
            category = "Macro"
        ),
        MarketNewsItem(
            id = "news-4",
            headline = "Gold Holds Near Record Highs as Safe-Haven Inflows Persist Worldwide",
            source = "Financial Times",
            timeAgo = "3h ago",
            sentiment = NewsSentiment.BULLISH,
            category = "Commodities"
        )
    )
}
