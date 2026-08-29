package com.example.engine

import com.example.data.model.Candle
import com.example.data.model.ConfirmationStatus
import com.example.data.model.ScoreBreakdown
import kotlin.math.abs
import kotlin.math.max

data class ConfirmationEval(
    val status: ConfirmationStatus,
    val isConfirmed: Boolean,
    val details: List<String>,
    val candleScore: Int // max 25
)

object ConfirmationCandleEngine {

    /**
     * Dedicated Confirmation Candle evaluation for SHORT setups (Resistance Rejection / Breakout Failures)
     */
    fun evaluateBearishConfirmation(
        candles: List<Candle>,
        resistancePrice: Double
    ): ConfirmationEval {
        if (candles.size < 3) {
            return ConfirmationEval(
                status = ConfirmationStatus.NO_CONFIRMATION,
                isConfirmed = false,
                details = listOf("Insufficient candle history for confirmation"),
                candleScore = 5
            )
        }

        val lastClosedCandle = if (!candles.last().isComplete && candles.size >= 2) {
            candles[candles.size - 2]
        } else {
            candles.last()
        }
        val prevCandle = if (candles.size >= 3) candles[candles.size - 3] else candles.first()

        val details = mutableListOf<String>()
        var score = 0

        // A. Bearish close
        val isBearish = lastClosedCandle.close < lastClosedCandle.open
        if (isBearish) {
            score += 6
            details.add("Closed firmly bearish")
        }

        // B. Rejection upper wick at key level
        val upperWick = lastClosedCandle.upperWick
        val body = lastClosedCandle.bodySize
        val totalRange = lastClosedCandle.totalRange
        val hasRejectionWick = (upperWick / totalRange) >= 0.35 || (lastClosedCandle.high >= resistancePrice && lastClosedCandle.close < resistancePrice)
        if (hasRejectionWick) {
            score += 7
            details.add("Upper wick rejection off resistance level (₹${round(resistancePrice)})")
        }

        // C. Bearish Engulfing pattern or breakdown of previous candle low
        val isEngulfing = isBearish && lastClosedCandle.open >= prevCandle.close && lastClosedCandle.close <= prevCandle.open
        val brokePrevLow = lastClosedCandle.close < prevCandle.low
        if (isEngulfing) {
            score += 7
            details.add("Bearish Engulfing pattern formed")
        } else if (brokePrevLow) {
            score += 5
            details.add("Closed below previous candle structure low")
        }

        // D. Body size conviction
        if (lastClosedCandle.bodyPercentage >= 45.0) {
            score += 5
            details.add("Decisive candle body expansion without indecision")
        }

        val finalScore = score.coerceIn(0, 25)
        val status = when {
            finalScore >= 20 -> ConfirmationStatus.STRONG_CONFIRMATION
            finalScore >= 12 -> ConfirmationStatus.CONFIRMATION_CANDLE
            finalScore >= 6 -> ConfirmationStatus.WEAK_CONFIRMATION
            else -> ConfirmationStatus.NO_CONFIRMATION
        }

        return ConfirmationEval(
            status = status,
            isConfirmed = finalScore >= 12,
            details = details.ifEmpty { listOf("No decisive bearish reversal candle yet") },
            candleScore = finalScore
        )
    }

    /**
     * Dedicated Confirmation Candle evaluation for LONG setups (Breakout / Support Bounces)
     */
    fun evaluateBullishConfirmation(
        candles: List<Candle>,
        supportOrBreakoutPrice: Double
    ): ConfirmationEval {
        if (candles.size < 3) {
            return ConfirmationEval(
                status = ConfirmationStatus.NO_CONFIRMATION,
                isConfirmed = false,
                details = listOf("Insufficient candle history for confirmation"),
                candleScore = 5
            )
        }

        val lastClosedCandle = if (!candles.last().isComplete && candles.size >= 2) {
            candles[candles.size - 2]
        } else {
            candles.last()
        }
        val prevCandle = if (candles.size >= 3) candles[candles.size - 3] else candles.first()

        val details = mutableListOf<String>()
        var score = 0

        val isBullish = lastClosedCandle.close > lastClosedCandle.open
        if (isBullish) {
            score += 6
            details.add("Closed strongly bullish")
        }

        val lowerWick = lastClosedCandle.lowerWick
        val totalRange = lastClosedCandle.totalRange
        val hasBounceWick = (lowerWick / totalRange) >= 0.35 || (lastClosedCandle.low <= supportOrBreakoutPrice && lastClosedCandle.close > supportOrBreakoutPrice)
        if (hasBounceWick) {
            score += 7
            details.add("Strong buyer absorption wick at support / retest (₹${round(supportOrBreakoutPrice)})")
        }

        val isBullishEngulfing = isBullish && lastClosedCandle.open <= prevCandle.close && lastClosedCandle.close >= prevCandle.open
        val brokePrevHigh = lastClosedCandle.close > prevCandle.high
        if (isBullishEngulfing) {
            score += 7
            details.add("Bullish Engulfing momentum candle")
        } else if (brokePrevHigh) {
            score += 5
            details.add("Closed above prior candle swing high")
        }

        if (lastClosedCandle.bodyPercentage >= 45.0) {
            score += 5
            details.add("Expansive green candle body showing institutional demand")
        }

        val finalScore = score.coerceIn(0, 25)
        val status = when {
            finalScore >= 20 -> ConfirmationStatus.STRONG_CONFIRMATION
            finalScore >= 12 -> ConfirmationStatus.CONFIRMATION_CANDLE
            finalScore >= 6 -> ConfirmationStatus.WEAK_CONFIRMATION
            else -> ConfirmationStatus.NO_CONFIRMATION
        }

        return ConfirmationEval(
            status = status,
            isConfirmed = finalScore >= 12,
            details = details.ifEmpty { listOf("Awaiting clear bullish confirmation candle close") },
            candleScore = finalScore
        )
    }

    private fun round(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0
}
