package com.example.engine

import com.example.data.model.Candle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object TechnicalIndicators {

    /**
     * Exponential Moving Average (EMA)
     */
    fun calculateEMA(candles: List<Candle>, period: Int): List<Double?> {
        if (candles.isEmpty()) return emptyList()
        val result = MutableList<Double?>(candles.size) { null }
        if (candles.size < period) return result

        val k = 2.0 / (period + 1)
        var sum = 0.0
        for (i in 0 until period) {
            sum += candles[i].close
        }
        var prevEma = sum / period
        result[period - 1] = prevEma

        for (i in period until candles.size) {
            val ema = (candles[i].close * k) + (prevEma * (1 - k))
            result[i] = ema
            prevEma = ema
        }
        return result
    }

    /**
     * Volume Weighted Average Price (VWAP)
     */
    fun calculateVWAP(candles: List<Candle>): List<Double?> {
        if (candles.isEmpty()) return emptyList()
        val result = MutableList<Double?>(candles.size) { null }
        var cumulativeTypicalPriceVolume = 0.0
        var cumulativeVolume = 0L

        for (i in candles.indices) {
            val c = candles[i]
            val typicalPrice = (c.high + c.low + c.close) / 3.0
            val volume = max(1L, c.volume)
            cumulativeTypicalPriceVolume += (typicalPrice * volume)
            cumulativeVolume += volume

            if (cumulativeVolume > 0) {
                result[i] = cumulativeTypicalPriceVolume / cumulativeVolume
            }
        }
        return result
    }

    /**
     * Relative Strength Index (RSI) with 14 period default
     */
    fun calculateRSI(candles: List<Candle>, period: Int = 14): List<Double?> {
        if (candles.size <= period) return List(candles.size) { null }
        val result = MutableList<Double?>(candles.size) { null }

        var gains = 0.0
        var losses = 0.0

        for (i in 1..period) {
            val diff = candles[i].close - candles[i - 1].close
            if (diff >= 0) gains += diff else losses += abs(diff)
        }

        var avgGain = gains / period
        var avgLoss = losses / period

        var rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
        result[period] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + rs))

        for (i in (period + 1) until candles.size) {
            val diff = candles[i].close - candles[i - 1].close
            val gain = if (diff > 0) diff else 0.0
            val loss = if (diff < 0) abs(diff) else 0.0

            avgGain = ((avgGain * (period - 1)) + gain) / period
            avgLoss = ((avgLoss * (period - 1)) + loss) / period

            rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            result[i] = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + rs))
        }

        return result
    }

    /**
     * Average True Range (ATR)
     */
    fun calculateATR(candles: List<Candle>, period: Int = 14): Double {
        if (candles.size < 2) return 1.0
        val trs = mutableListOf<Double>()
        for (i in 1 until candles.size) {
            val current = candles[i]
            val prev = candles[i - 1]
            val tr = max(
                current.high - current.low,
                max(abs(current.high - prev.close), abs(current.low - prev.close))
            )
            trs.add(tr)
        }
        val sublist = if (trs.size > period) trs.takeLast(period) else trs
        return if (sublist.isNotEmpty()) sublist.average() else 1.0
    }
}
