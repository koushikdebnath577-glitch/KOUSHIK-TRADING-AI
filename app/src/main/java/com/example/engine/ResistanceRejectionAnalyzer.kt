package com.example.engine

import com.example.data.model.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ResistanceRejectionAnalyzer {

    fun analyze(
        candles: List<Candle>,
        currentPrice: Double,
        keyLevels: List<KeyLevel>,
        atr: Double
    ): AnalysisResult {
        // Find nearest resistance / day high / swing high / liquidity pool above or at price
        val resistanceLevels = keyLevels.filter {
            it.type in listOf(
                KeyLevelType.RESISTANCE,
                KeyLevelType.DAY_HIGH,
                KeyLevelType.PREV_DAY_HIGH,
                KeyLevelType.OPENING_RANGE_HIGH,
                KeyLevelType.SWING_HIGH,
                KeyLevelType.LIQUIDITY_ZONE
            )
        }.sortedBy { abs(it.price - currentPrice) }

        val activeLevel = resistanceLevels.firstOrNull()

        val isNearLevel = activeLevel != null && (currentPrice >= activeLevel.price * 0.994 && currentPrice <= activeLevel.price * 1.008)
        val isAboveLevel = activeLevel != null && currentPrice > activeLevel.price
        val isRejectionFromLevel = activeLevel != null && currentPrice < activeLevel.price && (candles.takeLast(5).any { it.high >= activeLevel.price })

        // Check level quality score (max 30)
        val levelScore = when (activeLevel?.strength) {
            LevelStrength.STRONG -> 28
            LevelStrength.MODERATE -> 20
            LevelStrength.WEAK -> 10
            null -> 4
        }

        // Check price rejection score (max 25)
        val lastFew = candles.takeLast(min(5, candles.size))
        val maxUpperWick = if (lastFew.isNotEmpty()) lastFew.maxOf { it.upperWick / it.totalRange } else 0.0
        val hadSweep = activeLevel != null && lastFew.any { it.high > activeLevel.price && it.close < activeLevel.price }

        var rejectionScore = when {
            hadSweep -> 24
            maxUpperWick >= 0.40 -> 20
            maxUpperWick >= 0.25 -> 14
            isRejectionFromLevel -> 10
            else -> 4
        }

        // Confirmation Candle Evaluation (max 25)
        val confirmationEval = ConfirmationCandleEngine.evaluateBearishConfirmation(
            candles = candles,
            resistancePrice = activeLevel?.price ?: currentPrice
        )

        // Structure Confirmation Score (max 20)
        var structureScore = when {
            hadSweep && isRejectionFromLevel -> 18
            isRejectionFromLevel -> 14
            isNearLevel -> 8
            else -> 3
        }

        val totalScore = (levelScore + rejectionScore + confirmationEval.candleScore + structureScore).coerceIn(0, 100)

        val scoreBreakdown = ScoreBreakdown(
            levelQuality = levelScore,
            priceRejection = rejectionScore,
            confirmationCandle = confirmationEval.candleScore,
            structureConfirmation = structureScore,
            totalScore = totalScore
        )

        val setupGrade = when {
            totalScore >= 85 -> SetupGrade.A_PLUS
            totalScore >= 75 -> SetupGrade.A
            totalScore >= 60 -> SetupGrade.B
            else -> SetupGrade.NO_TRADE
        }

        val momentumEval = MomentumAnalyzer.evaluateMomentum(candles, TradeDirection.SHORT, currentPrice)

        val isCandleForming = candles.isNotEmpty() && !candles.last().isComplete

        // Determine Status
        val status = when {
            !isNearLevel && !isRejectionFromLevel -> "NO SETUP"
            isNearLevel && !isRejectionFromLevel && !confirmationEval.isConfirmed -> "WATCH"
            isNearLevel && isCandleForming && !confirmationEval.isConfirmed -> "WAIT FOR CONFIRMATION"
            hadSweep && confirmationEval.status == ConfirmationStatus.STRONG_CONFIRMATION && setupGrade != SetupGrade.NO_TRADE -> "CONSERVATIVE SHORT ENTRY AVAILABLE"
            confirmationEval.isConfirmed && setupGrade != SetupGrade.NO_TRADE -> "CONFIRMED"
            else -> "WAIT FOR CONFIRMATION"
        }

        val stopLossSuggestions = StopLossEngine.calculateStopLoss(
            direction = TradeDirection.SHORT,
            currentPrice = currentPrice,
            activeLevel = activeLevel,
            candles = candles,
            atr = atr
        )

        // Entry Calculations
        val conservativeEntryPrice = round(currentPrice)
        val conservativeSL = stopLossSuggestions.structureStopLoss
        val riskPerShare = max(0.20, conservativeSL - conservativeEntryPrice)
        val target1 = round(conservativeEntryPrice - (riskPerShare * 1.5))
        val target2 = round(conservativeEntryPrice - (riskPerShare * 2.5))
        val riskReward = if (riskPerShare > 0) (conservativeEntryPrice - target1) / riskPerShare else 1.5

        val conservativeEntryPlan = EntryPlan(
            entryType = "CONSERVATIVE ENTRY",
            entryPrice = conservativeEntryPrice,
            stopLoss = conservativeSL,
            target1 = target1,
            target2 = target2,
            riskRupees = round(riskPerShare * 100),
            potentialRewardRupees = round((conservativeEntryPrice - target1) * 100),
            riskRewardRatio = round(riskReward),
            executionRequirement = "Requires Bearish Confirmation Candle CLOSE below resistance (₹${round(activeLevel?.price ?: conservativeSL)})"
        )

        val normalEntryPlan = EntryPlan(
            entryType = "NORMAL ENTRY",
            entryPrice = round(currentPrice * 1.001),
            stopLoss = stopLossSuggestions.tightStopLoss,
            target1 = target1,
            target2 = target2,
            riskRupees = round(max(0.20, stopLossSuggestions.tightStopLoss - currentPrice) * 100),
            potentialRewardRupees = round((currentPrice - target1) * 100),
            riskRewardRatio = 1.8,
            executionRequirement = "Enter on rejection candle low breach"
        )

        val aggressiveEntryPlan = EntryPlan(
            entryType = "AGGRESSIVE ENTRY",
            entryPrice = round(activeLevel?.price ?: currentPrice),
            stopLoss = stopLossSuggestions.invalidationStopLoss,
            target1 = target1,
            target2 = target2,
            riskRupees = round(max(0.50, stopLossSuggestions.invalidationStopLoss - (activeLevel?.price ?: currentPrice)) * 100),
            potentialRewardRupees = round(((activeLevel?.price ?: currentPrice) - target1) * 100),
            riskRewardRatio = 2.2,
            executionRequirement = "Limit sell at resistance touch prior to confirmation"
        )

        val slDistancePercent = riskPerShare / conservativeEntryPrice
        val noTradeCheck = NoTradeFilter.evaluate(
            candles = candles,
            currentPrice = currentPrice,
            activeLevel = activeLevel,
            confirmationStatus = confirmationEval.status,
            momentumStatus = momentumEval.status,
            riskRewardRatio = riskReward,
            stopLossDistancePercent = slDistancePercent
        )

        val reasonsForSetup = mutableListOf<String>()
        if (activeLevel != null) reasonsForSetup.add("Price reacted at ${activeLevel.type.displayName} (₹${round(activeLevel.price)}) with ${activeLevel.touchCount} touch reactions")
        if (hadSweep) reasonsForSetup.add("Liquidity sweep above resistance trapped aggressive breakout buyers")
        reasonsForSetup.addAll(confirmationEval.details)

        return AnalysisResult(
            symbol = "",
            currentPrice = currentPrice,
            strategy = StrategyType.RESISTANCE_REJECTION,
            direction = TradeDirection.SHORT,
            strategyStatus = if (noTradeCheck.isNoTrade && status == "NO SETUP") "NO SETUP" else status,
            confirmationStatus = confirmationEval.status,
            confirmationDetails = confirmationEval.details,
            isCandleForming = isCandleForming,
            setupScore = totalScore,
            scoreBreakdown = scoreBreakdown,
            setupGrade = setupGrade,
            noTradeWarning = noTradeCheck.warningTitle,
            momentumStatus = momentumEval.status,
            momentumNotes = momentumEval.notes,
            aggressiveEntry = aggressiveEntryPlan,
            normalEntry = normalEntryPlan,
            conservativeEntry = conservativeEntryPlan,
            stopLossSuggestions = stopLossSuggestions,
            activeKeyLevel = activeLevel,
            reasonsForSetup = reasonsForSetup,
            reasonsToAvoid = noTradeCheck.reasonsToAvoid
        )
    }

    private fun round(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0
}
