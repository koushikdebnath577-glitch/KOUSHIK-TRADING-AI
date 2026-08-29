package com.example

import com.example.data.model.*
import com.example.engine.*
import org.junit.Assert.*
import org.junit.Test

class TradingEngineTest {

    private fun generateMockCandles(basePrice: Double = 24500.0, count: Int = 30): List<Candle> {
        val candles = mutableListOf<Candle>()
        var price = basePrice
        val now = System.currentTimeMillis()
        for (i in 0 until count) {
            val open = price
            val high = price + 15.0
            val low = price - 10.0
            val close = price + 5.0
            candles.add(
                Candle(
                    timestamp = now - ((count - i) * 60000L),
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = 15000L,
                    isComplete = true
                )
            )
            price = close
        }
        return candles
    }

    @Test
    fun testTechnicalIndicators() {
        val candles = generateMockCandles(24000.0, 40)

        // EMA 20
        val ema20 = TechnicalIndicators.calculateEMA(candles, 20)
        assertEquals(40, ema20.size)
        assertNotNull(ema20.last())

        // VWAP
        val vwap = TechnicalIndicators.calculateVWAP(candles)
        assertEquals(40, vwap.size)
        assertNotNull(vwap.last())
        assertTrue(vwap.last()!! > 23000.0)

        // RSI
        val rsi = TechnicalIndicators.calculateRSI(candles, 14)
        assertEquals(40, rsi.size)
        assertNotNull(rsi.last())
        assertTrue(rsi.last()!! in 0.0..100.0)

        // ATR
        val atr = TechnicalIndicators.calculateATR(candles, 14)
        assertTrue(atr > 0.0)
    }

    @Test
    fun testKeyLevelDetection() {
        val candles = generateMockCandles(25000.0, 30)
        val levels = KeyLevelDetector.detectKeyLevels(candles, 25100.0, 24900.0)

        assertTrue(levels.isNotEmpty())
        assertTrue(levels.any { it.type == KeyLevelType.DAY_HIGH })
        assertTrue(levels.any { it.type == KeyLevelType.DAY_LOW })
    }

    @Test
    fun testResistanceRejectionAnalysis() {
        val candles = generateMockCandles(25000.0, 35)
        val result = TradingAnalysisEngine.performFullAnalysis(
            symbol = "NIFTY 50",
            currentPrice = 25050.0,
            previousClose = 24950.0,
            candles = candles,
            strategyType = StrategyType.RESISTANCE_REJECTION
        )

        assertNotNull(result)
        assertEquals("NIFTY 50", result.symbol)
        assertEquals(StrategyType.RESISTANCE_REJECTION, result.strategy)
        assertEquals(TradeDirection.SHORT, result.direction)
        assertNotNull(result.conservativeEntry)
        assertTrue(result.setupScore in 0..100)
    }

    @Test
    fun testMorningBreakoutAnalysis() {
        val candles = generateMockCandles(25000.0, 35)
        val result = TradingAnalysisEngine.performFullAnalysis(
            symbol = "BANKNIFTY",
            currentPrice = 25200.0,
            previousClose = 24900.0,
            candles = candles,
            strategyType = StrategyType.MORNING_BREAKOUT
        )

        assertNotNull(result)
        assertEquals("BANKNIFTY", result.symbol)
        assertEquals(StrategyType.MORNING_BREAKOUT, result.strategy)
        assertNotNull(result.conservativeEntry)
        assertTrue(result.setupScore in 0..100)
    }

    @Test
    fun testNoTradeFilterSafety() {
        val candles = generateMockCandles(25000.0, 10)
        val noTradeCheck = NoTradeFilter.evaluate(
            candles = candles,
            currentPrice = 25000.0,
            activeLevel = null,
            confirmationStatus = ConfirmationStatus.NO_CONFIRMATION,
            momentumStatus = MomentumStatus.WEAK_MOMENTUM,
            riskRewardRatio = 1.0,
            stopLossDistancePercent = 0.02
        )

        assertTrue(noTradeCheck.isNoTrade)
        assertNotNull(noTradeCheck.warningTitle)
        assertTrue(noTradeCheck.reasonsToAvoid.isNotEmpty())
    }
}
