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
            // Close previous candle and open a new one
            _candles[lastIndex] = lastCandle.copy(isComplete = true)
            val newCandle = Candle(
                timestamp = candleBucketStart,
                open = lastCandle.close, // open at previous close for seamless continuity
                high = max(lastCandle.close, tickPrice),
                low = min(lastCandle.close, tickPrice),
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

    fun setBaseCandles(newBase: List<Candle>) {
        _candles.clear()
        _candles.addAll(newBase)
    }
}
