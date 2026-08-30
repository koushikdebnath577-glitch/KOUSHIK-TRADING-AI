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
    fun testPositionSizingCalculations() {
        val entryPrice = 25000.0
        val stopLoss = 24950.0
        val riskPerShare = kotlin.math.abs(entryPrice - stopLoss) // 50.0

        // Test ₹2500 default risk
        val risk2500 = 2500.0
        val qty2500 = (risk2500 / riskPerShare).toInt()
        assertEquals(50, qty2500)
        assertEquals(2500.0, qty2500 * riskPerShare, 0.01)

        // Test custom ₹500 risk
        val risk500 = 500.0
        val qty500 = (risk500 / riskPerShare).toInt()
        assertEquals(10, qty500)
        assertEquals(500.0, qty500 * riskPerShare, 0.01)

        // Test custom ₹5000 risk
        val risk5000 = 5000.0
        val qty5000 = (risk5000 / riskPerShare).toInt()
        assertEquals(100, qty5000)
        assertEquals(5000.0, qty5000 * riskPerShare, 0.01)
    }
}
