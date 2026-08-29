package com.example.engine

import com.example.data.model.Candle
import com.example.data.model.KeyLevel
import com.example.data.model.StopLossSuggestions
import com.example.data.model.TradeDirection
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object StopLossEngine {

    /**
     * Calculates Structure Stop Loss, Tight Stop Loss, and Invalidation Stop Loss based on market structure.
     */
    fun calculateStopLoss(
        direction: TradeDirection,
        currentPrice: Double,
        activeLevel: KeyLevel?,
        candles: List<Candle>,
        atr: Double
    ): StopLossSuggestions {
        val lastFew = candles.takeLast(min(10, candles.size))
        val highestInWick = if (lastFew.isNotEmpty()) lastFew.maxOf { it.high } else currentPrice * 1.005
        val lowestInWick = if (lastFew.isNotEmpty()) lastFew.minOf { it.low } else currentPrice * 0.995

        val buffer = max(0.5, atr * 0.2)

        return if (direction == TradeDirection.SHORT) {
            val structureAnchor = activeLevel?.price ?: highestInWick
            val structureSL = round(max(highestInWick, structureAnchor) + buffer)
            val tightSL = round(highestInWick + (buffer * 0.5))
            val invalidationSL = round(max(highestInWick, structureAnchor) + (atr * 0.8))

            StopLossSuggestions(
                structureStopLoss = structureSL,
                tightStopLoss = tightSL,
                invalidationStopLoss = invalidationSL,
                explanation = "Placed above rejection swing high / key level buffer (₹$structureSL)"
            )
        } else {
            val structureAnchor = activeLevel?.price ?: lowestInWick
            val structureSL = round(min(lowestInWick, structureAnchor) - buffer)
            val tightSL = round(lowestInWick - (buffer * 0.5))
            val invalidationSL = round(min(lowestInWick, structureAnchor) - (atr * 0.8))

            StopLossSuggestions(
                structureStopLoss = structureSL,
                tightStopLoss = tightSL,
                invalidationStopLoss = invalidationSL,
                explanation = "Placed safely below demand rejection low / support buffer (₹$structureSL)"
            )
        }
    }

    private fun round(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0
}
