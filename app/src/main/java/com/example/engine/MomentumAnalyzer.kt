package com.example.engine

import com.example.data.model.Candle
import com.example.data.model.MomentumStatus
import com.example.data.model.TradeDirection
import kotlin.math.abs

data class MomentumEval(
    val status: MomentumStatus,
    val score: Int, // max 15
    val notes: String
)

object MomentumAnalyzer {

    /**
     * Evaluates speed, candle body expansion, and consecutive directional follow-through.
     * Incorporates the 30-minute stagnant velocity rule.
     */
    fun evaluateMomentum(
        candles: List<Candle>,
        direction: TradeDirection,
        currentPrice: Double
    ): MomentumEval {
        if (candles.size < 5) {
            return MomentumEval(
                status = MomentumStatus.NORMAL_MOMENTUM,
                score = 8,
                notes = "Initial candle stream building baseline momentum"
            )
        }

        val last5 = candles.takeLast(5)
        val last15 = candles.takeLast(kotlin.math.min(15, candles.size))

        // Check directional movement
        val netChangeLast5 = last5.last().close - last5.first().open
        val avgBody = last5.map { it.bodySize }.average()
        val avgRange = last5.map { it.totalRange }.average()
        val bodyEfficiency = if (avgRange > 0) avgBody / avgRange else 0.5

        // Check if market has been drifting sideways for >15 candles (~15 to 30 mins)
        val highestInPeriod = last15.maxOf { it.high }
        val lowestInPeriod = last15.minOf { it.low }
        val periodSpread = highestInPeriod - lowestInPeriod
        val isSidewaysStagnant = periodSpread < (currentPrice * 0.0018)

        if (isSidewaysStagnant) {
            return MomentumEval(
                status = MomentumStatus.WEAK_MOMENTUM,
                score = 4,
                notes = "Momentum is weak (>30 min consolidation). Suggest reducing target expectations to T1 and trailing stop loss to break-even if market structure allows. Never move stop loss farther away or increase risk!"
            )
        }

        val alignsWithDirection = when (direction) {
            TradeDirection.SHORT -> netChangeLast5 < 0
            TradeDirection.LONG -> netChangeLast5 > 0
            TradeDirection.NEUTRAL -> false
        }

        return when {
            alignsWithDirection && bodyEfficiency > 0.60 -> MomentumEval(
                status = MomentumStatus.STRONG_MOMENTUM,
                score = 15,
                notes = "Strong institutional velocity with expansive candles and clean follow-through."
            )
            alignsWithDirection && bodyEfficiency >= 0.40 -> MomentumEval(
                status = MomentumStatus.NORMAL_MOMENTUM,
                score = 10,
                notes = "Healthy directional progress. Trail stops along swing pivots."
            )
            !alignsWithDirection && abs(netChangeLast5) > (currentPrice * 0.002) -> MomentumEval(
                status = MomentumStatus.NO_MOMENTUM,
                score = 2,
                notes = "Adverse counter-momentum detected. Capital preservation priority."
            )
            else -> MomentumEval(
                status = MomentumStatus.WEAK_MOMENTUM,
                score = 5,
                notes = "Low momentum chop. Reduce risk and avoid adding into stagnation."
            )
        }
    }
}
