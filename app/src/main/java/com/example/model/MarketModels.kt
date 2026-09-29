package com.example.model

enum class AssetClass(val displayName: String) {
    ALL("All"),
    INDIA("India (NSE)"),
    CRYPTO("Crypto"),
    FOREX("Forex"),
    COMMODITIES("Gold & Comm"),
    STOCKS("US Stocks"),
    INDICES("Indices")
}

enum class ChartType(val displayName: String) {
    CANDLESTICK("Candles"),
    HEIKIN_ASHI("Heikin Ashi"),
    LINE("Line"),
    AREA("Area"),
    HOLLOW("Hollow")
}

enum class Timeframe(val label: String, val seconds: Long) {
    SEC_1("1s", 1L),
    MIN_1("1m", 60L),
    MIN_5("5m", 300L),
    MIN_15("15m", 900L),
    HOUR_1("1h", 3600L),
    HOUR_4("4h", 14400L),
    DAY_1("1D", 86400L),
    WEEK_1("1W", 604800L)
}

enum class TechnicalRating(val label: String, val score: Int) {
    STRONG_SELL("Strong Sell", -2),
    SELL("Sell", -1),
    NEUTRAL("Neutral", 0),
    BUY("Buy", 1),
    STRONG_BUY("Strong Buy", 2)
}

data class TechnicalAnalysis(
    val overall: TechnicalRating,
    val oscillators: TechnicalRating,
    val movingAverages: TechnicalRating,
    val buySignals: Int,
    val neutralSignals: Int,
    val sellSignals: Int,
    val rsi: Double,
    val macd: Double,
    val stochK: Double,
    val adx: Double
)

data class MarketAsset(
    val id: String,
    val symbol: String,
    val name: String,
    val exchange: String,
    val assetClass: AssetClass,
    val price: Double,
    val changeNominal: Double,
    val changePercent: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24h: String,
    val marketCap: String,
    val sparkline: List<Double>,
    val currency: String = "USD", // "INR" or "USD"
    val technicalRating: TechnicalRating = TechnicalRating.BUY,
    val isFavorite: Boolean = false,
    val lastUpdateDirection: Int = 0 // 1 for up (green flash), -1 for down (red flash), 0 neutral
)

data class Candle(
    val timestamp: Long,
    val timeLabel: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    // Technical Indicators calculated for this candle
    val ema20: Double? = null,
    val ema50: Double? = null,
    val ema200: Double? = null,
    val bbUpper: Double? = null,
    val bbMiddle: Double? = null,
    val bbLower: Double? = null,
    val rsi: Double? = null,
    val macd: Double? = null,
    val macdSignal: Double? = null,
    val macdHist: Double? = null,
    val vwap: Double? = null
)

data class IndicatorConfig(
    val showVolume: Boolean = true,
    val showEma20: Boolean = true,
    val showEma50: Boolean = true,
    val showEma200: Boolean = false,
    val showBollinger: Boolean = true,
    val showRsi: Boolean = true,
    val showMacd: Boolean = false,
    val showVwap: Boolean = false
)

enum class DrawingTool(val displayName: String) {
    NONE("Cursor"),
    HORIZONTAL_LINE("H-Line"),
    TRENDLINE("Trendline"),
    SUPPORT_RESISTANCE("Support/Resistance")
}

data class DrawnHorizontalLine(
    val price: Double,
    val label: String,
    val id: String = java.util.UUID.randomUUID().toString().take(6),
    val color: Long = 0xFF2962FF
)

data class DrawnTrendLine(
    val id: String,
    val startTime: Long,
    val startPrice: Double,
    val endTime: Long,
    val endPrice: Double
)

enum class AlertCondition(val label: String) {
    CROSS_ABOVE("Crossing Up"),
    CROSS_BELOW("Crossing Down"),
    GREATER_THAN("Greater Than"),
    LESS_THAN("Less Than")
}

data class PriceAlert(
    val id: String,
    val symbol: String,
    val targetPrice: Double,
    val condition: AlertCondition,
    val note: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val isTriggered: Boolean = false
)

enum class OrderType {
    MARKET,
    LIMIT
}

enum class PositionType(val label: String) {
    LONG("Buy / Long"),
    SHORT("Sell / Short")
}

data class PaperTrade(
    val id: String,
    val symbol: String,
    val type: PositionType,
    val entryPrice: Double,
    val currentPrice: Double,
    val quantity: Double,
    val leverage: Int = 1,
    val stopLoss: Double? = null,
    val takeProfit: Double? = null,
    val openTimestamp: Long = System.currentTimeMillis(),
    val isClosed: Boolean = false,
    val closePrice: Double? = null,
    val currency: String = "USD"
) {
    val margin: Double
        get() = (entryPrice * quantity) / leverage

    val pnl: Double
        get() {
            val priceDiff = if (type == PositionType.LONG) {
                currentPrice - entryPrice
            } else {
                entryPrice - currentPrice
            }
            return priceDiff * quantity * leverage
        }

    val pnlPercent: Double
        get() = if (margin > 0) (pnl / margin) * 100.0 else 0.0
}

enum class BrokerType(
    val title: String,
    val subtitle: String = "",
    val shortName: String = "",
    val isOfficialPartner: Boolean = false,
    val defaultCurrency: String = "USD"
) {
    PAPER_TRADING("Paper Trading ($100k USD)", "Simulated trading by TradingView", "Paper", false, "USD"),
    DHAN_BROKER("Dhan Connect (₹2.5L INR)", "Official TradingView India Partner · NSE/BSE/MCX", "Dhan", true, "INR"),
    ZERODHA_BROKER("Zerodha Kite", "Kite Connect v3 · India NSE", "Zerodha", false, "INR"),
    ANGEL_ONE("Angel One", "SmartAPI Integration · India", "Angel One", false, "INR"),
    BINANCE_BROKER("Binance Futures", "Crypto Spot & USDT-M Futures API", "Binance", false, "USD"),
    INTERACTIVE_BROKERS("Interactive Brokers", "Global Equities, Options & Futures", "IBKR", false, "USD")
}

data class ConnectedBrokerAccount(
    val brokerType: BrokerType,
    val isConnected: Boolean = false,
    val accountId: String = "",
    val accountName: String = "Trading Account",
    val apiKeyOrToken: String = "",
    val availableCash: Double = 100000.0,
    val collateralMargin: Double = 50000.0,
    val usedMargin: Double = 0.0,
    val currency: String = "USD",
    val connectionStatus: String = "Not Connected"
)

data class DhanAccount(
    val isConnected: Boolean = false,
    val clientId: String = "",
    val clientName: String = "Trader",
    val availableCash: Double = 250000.0, // ₹2,50,000 INR
    val collateralMargin: Double = 150000.0,
    val usedMargin: Double = 0.0,
    val portfolioValue: Double = 485000.0,
    val brokerStatus: String = "Active (Dhan TradingView Direct)"
)

data class TradingIdea(
    val id: String,
    val author: String,
    val authorBadge: String,
    val title: String,
    val symbol: String,
    val timeframe: String,
    val bias: PositionType,
    val likes: Int,
    val comments: Int,
    val timeAgo: String,
    val content: String,
    val tags: List<String>
)

enum class NewsSentiment {
    BULLISH,
    NEUTRAL,
    BEARISH
}

data class MarketNewsItem(
    val id: String,
    val headline: String,
    val source: String,
    val timeAgo: String,
    val sentiment: NewsSentiment,
    val category: String
)
