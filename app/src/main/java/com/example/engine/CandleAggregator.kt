package com.example.engine

import com.example.data.model.Candle
import com.example.data.model.Timeframe
import com.example.data.remote.LiveTick
import kotlin.math.max
import kotlin.math.min

class CandleAggregator(
    val timeframe: Timeframe,
    initialCandles: List<Candle> = emptyList()
) {
    private val _candles = initialCandles.toMutableList()
    val candles: List<Candle> get() = _candles.toList()

    private val intervalMillis: Long = timeframe.seconds * 1000L

    /**
     * Process an incoming live tick.
     * Updates current forming candle or creates a new candle when the timeframe boundary crosses.
     */
    fun processTick(tick: LiveTick): Candle {
        val tickTime = tick.timestamp
        val tickPrice = tick.ltp
        val tickVol = tick.volume

        if (_candles.isEmpty()) {
            val bucketStart = (tickTime / intervalMillis) * intervalMillis
            val newCandle = Candle(
                timestamp = bucketStart,
                open = tickPrice,
                high = tickPrice,
                low = tickPrice,
                close = tickPrice,
                volume = tickVol,
                isComplete = false
            )
            _candles.add(newCandle)
            return newCandle
        }

        val lastIndex = _candles.size - 1
        val lastCandle = _candles[lastIndex]
        val candleBucketStart = (tickTime / intervalMillis) * intervalMillis

        return if (candleBucketStart == lastCandle.timestamp) {
            // Update existing forming candle
            val updated = lastCandle.copy(
                high = max(lastCandle.high, tickPrice),
                low = min(lastCandle.low, tickPrice),
                close = tickPrice,
                volume = lastCandle.volume + tickVol,
                isComplete = false
            )
            _candles[lastIndex] = updated
            updated
        } else if (candleBucketStart > lastCandle.timestamp) {
            // Check if last candle belongs to the same trading day / session
            val isSameSession = isSameTradingDay(lastCandle.timestamp, tickTime)
            _candles[lastIndex] = lastCandle.copy(isComplete = true)
            // If cross-session (e.g. yesterday to today), new candle opens at current tick price, not yesterday's close
            val openPrice = if (isSameSession) lastCandle.close else tickPrice
            val newCandle = Candle(
                timestamp = candleBucketStart,
                open = openPrice,
                high = max(openPrice, tickPrice),
                low = min(openPrice, tickPrice),
                close = tickPrice,
                volume = tickVol,
                isComplete = false
            )
            _candles.add(newCandle)
            // Limit memory buffer
            if (_candles.size > 250) {
                _candles.removeAt(0)
            }
            newCandle
        } else {
            lastCandle
        }
    }

    /**
     * Injects the current LTP into the latest candle ONLY if it belongs to the current trading session.
     * Never alters or pollutes previous session/historical candles.
     */
    fun injectCurrentSessionLtp(ltp: Double, volume: Long = 0L): Candle? {
        if (_candles.isEmpty() || ltp <= 0.0) return null
        val lastIndex = _candles.size - 1
        val lastCandle = _candles[lastIndex]

        if (!isSameTradingDay(lastCandle.timestamp, System.currentTimeMillis())) {
            // Belongs to prior trading session; do not inject or alter historical candle series
            return null
        }

        val updated = lastCandle.copy(
            high = max(lastCandle.high, ltp),
            low = min(lastCandle.low, ltp),
            close = ltp,
            volume = if (volume > 0L) lastCandle.volume + volume else lastCandle.volume,
            isComplete = false
        )
        _candles[lastIndex] = updated
        return updated
    }

    fun setBaseCandles(newBase: List<Candle>) {
        _candles.clear()
        _candles.addAll(newBase)
    }

    companion object {
        fun isSameTradingDay(timestamp1: Long, timestamp2: Long): Boolean {
            if (timestamp1 <= 0L || timestamp2 <= 0L) return false
            val tz = java.util.TimeZone.getTimeZone("Asia/Kolkata")
            val cal1 = java.util.Calendar.getInstance(tz).apply { timeInMillis = timestamp1 }
            val cal2 = java.util.Calendar.getInstance(tz).apply { timeInMillis = timestamp2 }
            return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                   cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
        }
    }
}
