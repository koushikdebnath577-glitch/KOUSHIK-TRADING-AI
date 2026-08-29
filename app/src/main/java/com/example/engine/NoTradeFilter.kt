package com.example.engine

import com.example.data.model.Candle
import com.example.data.model.ConfirmationStatus
import com.example.data.model.KeyLevel
import com.example.data.model.MomentumStatus
import kotlin.math.abs

data class NoTradeCheck(
    val isNoTrade: Boolean,
    val warningTitle: String?,
    val warningReason: String?,
    val reasonsToAvoid: List<String>
)

object NoTradeFilter {

    /**
     * Strictly verifies if market conditions warrant a hard NO TRADE or WAIT FOR BETTER SETUP filter.
     */
    fun evaluate(
        candles: List<Candle>,
        currentPrice: Double,
        activeLevel: KeyLevel?,
        confirmationStatus: ConfirmationStatus,
        momentumStatus: MomentumStatus,
        riskRewardRatio: Double,
        stopLossDistancePercent: Double
    ): NoTradeCheck {
        val reasonsToAvoid = mutableListOf<String>()

        // Check 1: Price is floating between key levels without reference
        if (activeLevel == null) {
            reasonsToAvoid.add("Price is floating between key structural levels with no edge")
        } else {
            val distToLevel = abs(currentPrice - activeLevel.price) / currentPrice
            if (distToLevel > 0.015) {
                reasonsToAvoid.add("Price has already moved too far from key anchor (Late entry risk)")
            }
        }

        // Check 2: No confirmation candle
        if (confirmationStatus == ConfirmationStatus.NO_CONFIRMATION) {
            reasonsToAvoid.add("No decisive confirmation candle has closed")
        }

        // Check 3: Choppy market / tight sideways range
        if (momentumStatus == MomentumStatus.WEAK_MOMENTUM || momentumStatus == MomentumStatus.NO_MOMENTUM) {
            reasonsToAvoid.add("Sideways low momentum / choppy consolidation detected")
        }

        // Check 4: Poor risk-reward (< 1.5)
        if (riskRewardRatio < 1.5 && riskRewardRatio > 0.0) {
            reasonsToAvoid.add("Poor Risk:Reward ratio (${String.format("%.1f", riskRewardRatio)}:1) does not meet minimum 1.5:1 requirement")
        }

        // Check 5: Stop loss too large (> 1.2% on intraday)
        if (stopLossDistancePercent > 0.012) {
            reasonsToAvoid.add("Stop loss distance (${String.format("%.2f", stopLossDistancePercent * 100)}%) is too wide for safe capital allocation")
        }

        val isNoTrade = reasonsToAvoid.size >= 2 || confirmationStatus == ConfirmationStatus.NO_CONFIRMATION

        val warningTitle = if (isNoTrade) {
            if (reasonsToAvoid.any { it.contains("choppy", ignoreCase = true) || it.contains("floating", ignoreCase = true) }) {
                "NO TRADE TODAY"
            } else {
                "WAIT FOR BETTER SETUP"
            }
        } else null

        val warningReason = if (isNoTrade) {
            reasonsToAvoid.joinToString(" • ")
        } else null

        return NoTradeCheck(
            isNoTrade = isNoTrade,
            warningTitle = warningTitle,
            warningReason = warningReason,
            reasonsToAvoid = reasonsToAvoid
        )
    }
}
