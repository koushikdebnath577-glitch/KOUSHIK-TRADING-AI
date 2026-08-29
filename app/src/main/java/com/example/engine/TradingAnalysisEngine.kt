package com.example.engine

import com.example.data.model.*

object TradingAnalysisEngine {

    fun performFullAnalysis(
        symbol: String,
        currentPrice: Double,
        previousClose: Double,
        candles: List<Candle>,
        strategyType: StrategyType = StrategyType.RESISTANCE_REJECTION
    ): AnalysisResult {
        if (candles.isEmpty()) {
            return fallbackEmptyResult(symbol, currentPrice, strategyType)
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

        return baseResult.copy(symbol = symbol)
    }

    private fun fallbackEmptyResult(symbol: String, currentPrice: Double, strategyType: StrategyType): AnalysisResult {
        return AnalysisResult(
            symbol = symbol,
            currentPrice = currentPrice,
            strategy = strategyType,
            direction = TradeDirection.NEUTRAL,
            strategyStatus = "NO SETUP",
            confirmationStatus = ConfirmationStatus.NO_CONFIRMATION,
            confirmationDetails = listOf("Waiting for market ticks"),
            isCandleForming = false,
            setupScore = 40,
            scoreBreakdown = ScoreBreakdown(10, 10, 10, 10, 40),
            setupGrade = SetupGrade.NO_TRADE,
            noTradeWarning = "WAIT FOR BETTER SETUP",
            momentumStatus = MomentumStatus.NO_MOMENTUM,
            momentumNotes = "No candle data",
            aggressiveEntry = null,
            normalEntry = null,
            conservativeEntry = null,
            stopLossSuggestions = StopLossSuggestions(currentPrice * 1.01, currentPrice * 1.005, currentPrice * 1.015, "Baseline"),
            activeKeyLevel = null,
            reasonsForSetup = emptyList(),
            reasonsToAvoid = listOf("Waiting for live data feed")
        )
    }
}
