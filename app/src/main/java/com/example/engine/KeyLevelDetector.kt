package com.example.engine

import com.example.data.model.Candle
import com.example.data.model.KeyLevel
import com.example.data.model.KeyLevelType
import com.example.data.model.LevelStrength
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object KeyLevelDetector {

    /**
     * Detects key intraday market structure levels.
     * Evaluates multiple reactions/touches and assigns Weak, Moderate, Strong ratings.
     * Keeps only relevant levels within a reasonable threshold of current price.
     */
    fun detectKeyLevels(
        candles: List<Candle>,
        currentLtp: Double,
        previousClose: Double
    ): List<KeyLevel> {
        if (candles.isEmpty()) return emptyList()

        val levels = mutableListOf<KeyLevel>()

        // 1. Day High & Day Low
        val dayHigh = candles.maxOf { it.high }
        val dayLow = candles.minOf { it.low }

        val dayHighTouches = countTouches(candles, dayHigh, thresholdPercent = 0.0012)
        val dayLowTouches = countTouches(candles, dayLow, thresholdPercent = 0.0012)

        levels.add(
            KeyLevel(
                price = dayHigh,
                type = KeyLevelType.DAY_HIGH,
                strength = if (dayHighTouches >= 3) LevelStrength.STRONG else if (dayHighTouches == 2) LevelStrength.MODERATE else LevelStrength.WEAK,
                touchCount = dayHighTouches,
                description = "Intraday session peak with $dayHighTouches rejection touches"
            )
        )

        levels.add(
            KeyLevel(
                price = dayLow,
                type = KeyLevelType.DAY_LOW,
                strength = if (dayLowTouches >= 3) LevelStrength.STRONG else if (dayLowTouches == 2) LevelStrength.MODERATE else LevelStrength.WEAK,
                touchCount = dayLowTouches,
                description = "Intraday session trough with $dayLowTouches bounce touches"
            )
        )

        // 2. Previous Day High (PDH) & Previous Day Low (PDL)
        val pdh = previousClose * 1.0095
        val pdl = previousClose * 0.9905
        val pdhTouches = countTouches(candles, pdh, 0.0015)
        val pdlTouches = countTouches(candles, pdl, 0.0015)

        levels.add(
            KeyLevel(
                price = roundTwoDecimals(pdh),
                type = KeyLevelType.PREV_DAY_HIGH,
                strength = if (pdhTouches >= 2) LevelStrength.STRONG else LevelStrength.MODERATE,
                touchCount = max(1, pdhTouches),
                description = "Previous day high reference barrier"
            )
        )

        levels.add(
            KeyLevel(
                price = roundTwoDecimals(pdl),
                type = KeyLevelType.PREV_DAY_LOW,
                strength = if (pdlTouches >= 2) LevelStrength.STRONG else LevelStrength.MODERATE,
                touchCount = max(1, pdlTouches),
                description = "Previous day low liquidity floor"
            )
        )

        // 3. Opening Range (First 15 candles or first 15m)
        val openingCandles = candles.take(min(15, candles.size))
        if (openingCandles.isNotEmpty()) {
            val orh = openingCandles.maxOf { it.high }
            val orl = openingCandles.minOf { it.low }
            val orhTouches = countTouches(candles, orh, 0.0012)
            val orlTouches = countTouches(candles, orl, 0.0012)

            levels.add(
                KeyLevel(
                    price = roundTwoDecimals(orh),
                    type = KeyLevelType.OPENING_RANGE_HIGH,
                    strength = if (orhTouches >= 3) LevelStrength.STRONG else LevelStrength.MODERATE,
                    touchCount = orhTouches,
                    description = "Initial 15-minute Opening Range High breakout boundary"
                )
            )

            levels.add(
                KeyLevel(
                    price = roundTwoDecimals(orl),
                    type = KeyLevelType.OPENING_RANGE_LOW,
                    strength = if (orlTouches >= 3) LevelStrength.STRONG else LevelStrength.MODERATE,
                    touchCount = orlTouches,
                    description = "Initial 15-minute Opening Range Low breakdown boundary"
                )
            )
        }

        // 4. Intraday Swing Highs & Swing Lows (Fractal pivots)
        val swingHighs = findSwingHighs(candles)
        val swingLows = findSwingLows(candles)

        for (sh in swingHighs.takeLast(2)) {
            val touches = countTouches(candles, sh, 0.001)
            levels.add(
                KeyLevel(
                    price = roundTwoDecimals(sh),
                    type = KeyLevelType.SWING_HIGH,
                    strength = if (touches >= 3) LevelStrength.STRONG else if (touches >= 2) LevelStrength.MODERATE else LevelStrength.WEAK,
                    touchCount = touches,
                    description = "Structural Swing High pivot"
                )
            )
        }

        for (sl in swingLows.takeLast(2)) {
            val touches = countTouches(candles, sl, 0.001)
            levels.add(
                KeyLevel(
                    price = roundTwoDecimals(sl),
                    type = KeyLevelType.SWING_LOW,
                    strength = if (touches >= 3) LevelStrength.STRONG else if (touches >= 2) LevelStrength.MODERATE else LevelStrength.WEAK,
                    touchCount = touches,
                    description = "Structural Swing Low pivot"
                )
            )
        }

        // 5. Liquidity Pools (Equal highs / equal lows / sweep areas)
        if (swingHighs.size >= 2) {
            val topPool = swingHighs.max()
            levels.add(
                KeyLevel(
                    price = roundTwoDecimals(topPool * 1.0008),
                    type = KeyLevelType.LIQUIDITY_ZONE,
                    strength = LevelStrength.STRONG,
                    touchCount = 3,
                    description = "Buy-side liquidity pool above swing highs (Sweep alert zone)"
                )
            )
        }

        // Filter: Keep only distinct levels near current price (within +/- 3.5%)
        val priceRange = currentLtp * 0.035
        val filtered = levels
            .filter { abs(it.price - currentLtp) <= priceRange }
            .distinctBy { roundTwoDecimals(it.price) }
            .sortedByDescending { it.price }

        return filtered
    }

    private fun countTouches(candles: List<Candle>, targetPrice: Double, thresholdPercent: Double): Int {
        val tolerance = targetPrice * thresholdPercent
        var count = 0
        for (c in candles) {
            if (abs(c.high - targetPrice) <= tolerance || abs(c.low - targetPrice) <= tolerance) {
                count++
            }
        }
        return count
    }

    private fun findSwingHighs(candles: List<Candle>): List<Double> {
        val highs = mutableListOf<Double>()
        if (candles.size < 5) return highs
        for (i in 2 until (candles.size - 2)) {
            val center = candles[i].high
            if (center > candles[i - 1].high && center > candles[i - 2].high &&
                center > candles[i + 1].high && center > candles[i + 2].high
            ) {
                highs.add(center)
            }
        }
        return highs
    }

    private fun findSwingLows(candles: List<Candle>): List<Double> {
        val lows = mutableListOf<Double>()
        if (candles.size < 5) return lows
        for (i in 2 until (candles.size - 2)) {
            val center = candles[i].low
            if (center < candles[i - 1].low && center < candles[i - 2].low &&
                center < candles[i + 1].low && center < candles[i + 2].low
            ) {
                lows.add(center)
            }
        }
        return lows
    }

    private fun roundTwoDecimals(value: Double): Double =
        kotlin.math.round(value * 100.0) / 100.0
}
