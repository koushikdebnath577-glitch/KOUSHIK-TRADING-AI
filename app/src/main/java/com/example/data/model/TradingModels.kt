package com.example.data.model

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val isComplete: Boolean = true
) {
    val isBullish: Boolean get() = close >= open
    val bodySize: Double get() = kotlin.math.abs(close - open)
    val upperWick: Double get() = high - kotlin.math.max(open, close)
    val lowerWick: Double get() = kotlin.math.min(open, close) - low
    val totalRange: Double get() = kotlin.math.max(0.01, high - low)
    val bodyPercentage: Double get() = (bodySize / totalRange) * 100.0
}

enum class Timeframe(val label: String, val seconds: Long, val isSubMinute: Boolean) {
    SEC_1("1s", 1L, true),
    SEC_5("5s", 5L, true),
    SEC_15("15s", 15L, true),
    SEC_30("30s", 30L, true),
    MIN_1("1m", 60L, false),
    MIN_3("3m", 180L, false),
    MIN_5("5m", 300L, false),
    MIN_15("15m", 900L, false),
    MIN_30("30m", 1800L, false),
    HOUR_1("1h", 3600L, false),
    DAY_1("1d", 86400L, false)
}

data class StockSymbol(
    val symbol: String,
    val name: String,
    val token: String,
    val exchange: String = "NSE",
    val ltp: Double,
    val change: Double,
    val changePercent: Double,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val previousClose: Double,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val companyName: String get() = name
    val dayHigh: Double get() = high
    val dayLow: Double get() = low
}

enum class KeyLevelType(val displayName: String, val colorHex: Long) {
    DAY_HIGH("Day High", 0xFFFF5252),
    DAY_LOW("Day Low", 0xFF00E676),
    PREV_DAY_HIGH("Prev Day High (PDH)", 0xFFFF7043),
    PREV_DAY_LOW("Prev Day Low (PDL)", 0xFF26A69A),
    OPENING_RANGE_HIGH("Opening Range High (ORH)", 0xFFFFB703),
    OPENING_RANGE_LOW("Opening Range Low (ORL)", 0xFF80CBC4),
    SWING_HIGH("Intraday Swing High", 0xFFE040FB),
    SWING_LOW("Intraday Swing Low", 0xFF00E5FF),
    RESISTANCE("Key Resistance", 0xFFFF1744),
    SUPPORT("Key Support", 0xFF00C853),
    LIQUIDITY_ZONE("Liquidity Pool", 0xFF7C4DFF)
}

enum class LevelStrength(val label: String) {
    WEAK("Weak"),
    MODERATE("Moderate"),
    STRONG("Strong")
}

data class KeyLevel(
    val price: Double,
    val type: KeyLevelType,
    val strength: LevelStrength,
    val touchCount: Int,
    val description: String
)

enum class StrategyType(val displayName: String) {
    RESISTANCE_REJECTION("Resistance Rejection Strategy"),
    MORNING_BREAKOUT("Morning Breakout Strategy")
}

enum class TradeDirection(val label: String) {
    SHORT("SHORT / SELL"),
    LONG("LONG / BUY"),
    NEUTRAL("NEUTRAL")
}

enum class ConfirmationStatus(val label: String) {
    STRONG_CONFIRMATION("STRONG CONFIRMATION"),
    WEAK_CONFIRMATION("WEAK CONFIRMATION"),
    CONFIRMATION_CANDLE("CONFIRMATION CANDLE"),
    NO_CONFIRMATION("NO CONFIRMATION")
}

enum class SetupGrade(val label: String, val minScore: Int, val maxScore: Int) {
    A_PLUS("A+ SETUP", 85, 100),
    A("A SETUP", 75, 84),
    B("B SETUP", 60, 74),
    NO_TRADE("NO TRADE", 0, 59)
}

enum class MomentumStatus(val label: String) {
    STRONG_MOMENTUM("STRONG MOMENTUM"),
    NORMAL_MOMENTUM("NORMAL MOMENTUM"),
    WEAK_MOMENTUM("WEAK MOMENTUM"),
    NO_MOMENTUM("NO MOMENTUM")
}

data class ScoreBreakdown(
    val levelQuality: Int,       // max 30
    val priceRejection: Int,     // max 25
    val confirmationCandle: Int, // max 25
    val structureConfirmation: Int, // max 20
    val totalScore: Int          // 0-100
)

data class EntryPlan(
    val entryType: String, // "AGGRESSIVE ENTRY", "NORMAL ENTRY", "CONSERVATIVE ENTRY"
    val entryPrice: Double,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val riskRupees: Double,
    val potentialRewardRupees: Double,
    val riskRewardRatio: Double,
    val executionRequirement: String
)

data class StopLossSuggestions(
    val structureStopLoss: Double,
    val tightStopLoss: Double,
    val invalidationStopLoss: Double,
    val explanation: String
)

data class AnalysisResult(
    val symbol: String,
    val currentPrice: Double,
    val strategy: StrategyType,
    val direction: TradeDirection,
    val strategyStatus: String, // e.g. "CONSERVATIVE SHORT ENTRY AVAILABLE", "WAIT FOR CONFIRMATION", "BREAKOUT CONFIRMED", "NO TRADE"
    val confirmationStatus: ConfirmationStatus,
    val confirmationDetails: List<String>,
    val isCandleForming: Boolean,
    val setupScore: Int,
    val scoreBreakdown: ScoreBreakdown,
    val setupGrade: SetupGrade,
    val noTradeWarning: String?, // null if clean trade setup, or string if warning
    val momentumStatus: MomentumStatus,
    val momentumNotes: String,
    val aggressiveEntry: EntryPlan?,
    val normalEntry: EntryPlan?,
    val conservativeEntry: EntryPlan?,
    val stopLossSuggestions: StopLossSuggestions,
    val activeKeyLevel: KeyLevel?,
    val reasonsForSetup: List<String>,
    val reasonsToAvoid: List<String>,
    val disclaimer: String = "ANALYSIS ONLY • NOT FINANCIAL ADVICE • MANUAL CONFIRMATION REQUIRED"
)

enum class ConnectionStatus(val label: String) {
    CONNECTED("CONNECTED"),
    CONNECTING("CONNECTING"),
    DISCONNECTED("DISCONNECTED");

    companion object {
        val LIVE = CONNECTED
        val RECONNECTING = CONNECTING
    }
}

data class TradeAlert(
    val id: String,
    val timestamp: Long,
    val symbol: String,
    val title: String,
    val message: String,
    val triggerPrice: Double,
    val isRead: Boolean = false,
    val isEnabled: Boolean = true
)
