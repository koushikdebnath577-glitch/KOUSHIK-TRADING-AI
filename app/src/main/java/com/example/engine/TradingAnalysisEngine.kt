package com.example.engine

import com.example.data.model.*

object TradingAnalysisEngine {

    fun performFullAnalysis(
        symbol: String,
        currentPrice: Double,
        previousClose: Double,
        candles: List<Candle>,
        strategyType: StrategyType = StrategyType.RESISTANCE_REJECTION,
        isLiveFeed: Boolean = false
    ): AnalysisResult {
        if (candles.isEmpty() || currentPrice <= 0.0) {
            return fallbackEmptyResult(symbol, currentPrice, strategyType, isLiveFeed)
        }

        val keyLevels = KeyLevelDetector.detectKeyLevels(candles, currentPrice, previousClose)
        val atr = TechnicalIndicators.calculateATR(candles, 14)

        val baseResult = when (strategyType) {
            StrategyType.RESISTANCE_REJECTION -> ResistanceRejectionAnalyzer.analyze(
                candles = candles,
                currentPrice = currentPrice,
                keyLevels = keyLevels,
                atr = atr
            )
            StrategyType.MORNING_BREAKOUT -> MorningBreakoutAnalyzer.analyze(
                candles = candles,
                currentPrice = currentPrice,
                keyLevels = keyLevels,
                atr = atr
            )
        }

        // STRICT INTRADAY SAFETY RULE:
        // Resistance Rejection, Morning Breakout, and all other intraday signals
        // must NOT become CONFIRMED when live data is unavailable or stale.
        return if (!isLiveFeed) {
            baseResult.copy(
                symbol = symbol,
                strategyStatus = "WAITING FOR LIVE DATA",
                confirmationStatus = ConfirmationStatus.NO_CONFIRMATION,
                setupGrade = SetupGrade.NO_TRADE,
                noTradeWarning = "WAITING FOR LIVE DATA — Intraday setups require active Angel One SmartAPI live stream",
                confirmationDetails = listOf(
                    "Live market feed required for intraday trade confirmations",
                    "Strategy signal evaluation is suspended while awaiting live feed"
                ),
                aggressiveEntry = null,
                normalEntry = null,
                conservativeEntry = null,
                reasonsToAvoid = listOf(
                    "Live data unavailable or disconnected",
                    "Never calculate or execute trade setups on stale / offline data"
                )
            )
        } else {
            baseResult.copy(symbol = symbol)
        }
    }

    private fun fallbackEmptyResult(symbol: String, currentPrice: Double, strategyType: StrategyType, isLive: Boolean): AnalysisResult {
        return AnalysisResult(
            symbol = symbol,
            currentPrice = currentPrice,
            strategy = strategyType,
            direction = TradeDirection.NEUTRAL,
            strategyStatus = if (isLive) "NO SETUP" else "WAITING FOR LIVE DATA",
            confirmationStatus = ConfirmationStatus.NO_CONFIRMATION,
            confirmationDetails = listOf(if (isLive) "Waiting for trade setup formation" else "Live market feed required for intraday trade confirmations"),
            isCandleForming = false,
            setupScore = 40,
            scoreBreakdown = ScoreBreakdown(10, 10, 10, 10, 40),
            setupGrade = SetupGrade.NO_TRADE,
            noTradeWarning = if (isLive) "WAIT FOR BETTER SETUP" else "WAITING FOR LIVE DATA",
            momentumStatus = MomentumStatus.NO_MOMENTUM,
            momentumNotes = "No live candle data",
            aggressiveEntry = null,
            normalEntry = null,
            conservativeEntry = null,
            stopLossSuggestions = StopLossSuggestions(if (currentPrice > 0) currentPrice * 1.01 else 0.0, if (currentPrice > 0) currentPrice * 1.005 else 0.0, if (currentPrice > 0) currentPrice * 1.015 else 0.0, "Baseline"),
            activeKeyLevel = null,
            reasonsForSetup = emptyList(),
            reasonsToAvoid = listOf("Live data unavailable — waiting for market ticks")
        )
    }
}
