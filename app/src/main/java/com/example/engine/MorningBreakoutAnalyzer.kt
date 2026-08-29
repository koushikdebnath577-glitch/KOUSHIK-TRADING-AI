package com.example.engine

import com.example.data.model.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object MorningBreakoutAnalyzer {

    fun analyze(
        candles: List<Candle>,
        currentPrice: Double,
        keyLevels: List<KeyLevel>,
        atr: Double
    ): AnalysisResult {
        // Find Opening Range High and Low
        val orhLevel = keyLevels.find { it.type == KeyLevelType.OPENING_RANGE_HIGH }
        val orlLevel = keyLevels.find { it.type == KeyLevelType.OPENING_RANGE_LOW }

        val orh = orhLevel?.price ?: (candles.take(15).maxOfOrNull { it.high } ?: currentPrice * 1.006)
        val orl = orlLevel?.price ?: (candles.take(15).minOfOrNull { it.low } ?: currentPrice * 0.994)

        val isAboveOrh = currentPrice > orh
        val isBelowOrl = currentPrice < orl
        val isInsideRange = currentPrice in orl..orh

        val direction = when {
            isAboveOrh -> TradeDirection.LONG
            isBelowOrl -> TradeDirection.SHORT
            else -> TradeDirection.LONG
        }

        val activeTargetLevel = if (direction == TradeDirection.LONG) orhLevel else orlLevel
        val referencePrice = if (direction == TradeDirection.LONG) orh else orl

        // Check level quality score
        val levelScore = when (activeTargetLevel?.strength) {
            LevelStrength.STRONG -> 28
            LevelStrength.MODERATE -> 22
            else -> 15
        }

        // Confirmation candle evaluation
        val confirmationEval = if (direction == TradeDirection.LONG) {
            ConfirmationCandleEngine.evaluateBullishConfirmation(candles, orh)
        } else {
            ConfirmationCandleEngine.evaluateBearishConfirmation(candles, orl)
        }

        val lastFew = candles.takeLast(min(6, candles.size))
        val isExtended = abs(currentPrice - referencePrice) > (referencePrice * 0.012)
        val isRetest = abs(currentPrice - referencePrice) <= (referencePrice * 0.0025)

        // Structure score
        var structureScore = when {
            isRetest && confirmationEval.isConfirmed -> 20
            !isInsideRange && confirmationEval.isConfirmed && !isExtended -> 16
            !isInsideRange -> 10
            else -> 5
        }

        val momentumEval = MomentumAnalyzer.evaluateMomentum(candles, direction, currentPrice)

        var rejectionOrBreakoutScore = when {
            !isInsideRange && momentumEval.status == MomentumStatus.STRONG_MOMENTUM -> 24
            !isInsideRange && !isExtended -> 18
            isRetest -> 20
            else -> 8
        }

        val totalScore = (levelScore + rejectionOrBreakoutScore + confirmationEval.candleScore + structureScore).coerceIn(0, 100)

        val scoreBreakdown = ScoreBreakdown(
            levelQuality = levelScore,
            priceRejection = rejectionOrBreakoutScore,
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

        val isCandleForming = candles.isNotEmpty() && !candles.last().isComplete

        // Breakout statuses
        val status = when {
            isInsideRange && abs(currentPrice - orh) < (orh * 0.003) -> "BREAKOUT WATCH"
            isInsideRange && abs(currentPrice - orl) < (orl * 0.003) -> "BREAKOUT WATCH"
            isInsideRange -> "NO TRADE"
            isExtended -> "RETEST WAIT"
            isRetest && confirmationEval.isConfirmed -> "CONSERVATIVE ENTRY"
            !isInsideRange && confirmationEval.status == ConfirmationStatus.STRONG_CONFIRMATION -> "BREAKOUT CONFIRMED"
            !isInsideRange && confirmationEval.status == ConfirmationStatus.NO_CONFIRMATION -> "BREAKOUT FAILED"
            else -> "BREAKOUT WATCH"
        }

        val stopLossSuggestions = StopLossEngine.calculateStopLoss(
            direction = direction,
            currentPrice = currentPrice,
            activeLevel = activeTargetLevel,
            candles = candles,
            atr = atr
        )

        val entryPrice = round(currentPrice)
        val slPrice = stopLossSuggestions.structureStopLoss
        val riskPerShare = max(0.20, abs(entryPrice - slPrice))
        val target1 = if (direction == TradeDirection.LONG) round(entryPrice + (riskPerShare * 1.5)) else round(entryPrice - (riskPerShare * 1.5))
        val target2 = if (direction == TradeDirection.LONG) round(entryPrice + (riskPerShare * 2.5)) else round(entryPrice - (riskPerShare * 2.5))
        val rr = if (riskPerShare > 0) abs(target1 - entryPrice) / riskPerShare else 1.5

        val conservativeEntryPlan = EntryPlan(
            entryType = "CONSERVATIVE ENTRY",
            entryPrice = entryPrice,
            stopLoss = slPrice,
            target1 = target1,
            target2 = target2,
            riskRupees = round(riskPerShare * 100),
            potentialRewardRupees = round(abs(target1 - entryPrice) * 100),
            riskRewardRatio = round(rr),
            executionRequirement = "Requires Breakout Retest holding and confirmation candle CLOSE (₹${round(referencePrice)})"
        )

        val normalEntryPlan = EntryPlan(
            entryType = "NORMAL ENTRY",
            entryPrice = entryPrice,
            stopLoss = stopLossSuggestions.tightStopLoss,
            target1 = target1,
            target2 = target2,
            riskRupees = round(max(0.20, abs(entryPrice - stopLossSuggestions.tightStopLoss)) * 100),
            potentialRewardRupees = round(abs(target1 - entryPrice) * 100),
            riskRewardRatio = 1.8,
            executionRequirement = "Enter on breakout candle close with momentum"
        )

        val aggressiveEntryPlan = EntryPlan(
            entryType = "AGGRESSIVE ENTRY",
            entryPrice = round(referencePrice),
            stopLoss = stopLossSuggestions.invalidationStopLoss,
            target1 = target1,
            target2 = target2,
            riskRupees = round(max(0.50, abs(referencePrice - stopLossSuggestions.invalidationStopLoss)) * 100),
            potentialRewardRupees = round(abs(target1 - referencePrice) * 100),
            riskRewardRatio = 2.0,
            executionRequirement = "Market order as price crosses opening range boundary"
        )

        val slDistancePercent = riskPerShare / entryPrice
        val noTradeCheck = NoTradeFilter.evaluate(
            candles = candles,
            currentPrice = currentPrice,
            activeLevel = activeTargetLevel,
            confirmationStatus = confirmationEval.status,
            momentumStatus = momentumEval.status,
            riskRewardRatio = rr,
            stopLossDistancePercent = slDistancePercent
        )

        val reasonsForSetup = mutableListOf<String>()
        if (direction == TradeDirection.LONG) {
            reasonsForSetup.add("Price broke above Opening Range High (₹${round(orh)})")
        } else {
            reasonsForSetup.add("Price broke below Opening Range Low (₹${round(orl)})")
        }
        reasonsForSetup.addAll(confirmationEval.details)

        return AnalysisResult(
            symbol = "",
            currentPrice = currentPrice,
            strategy = StrategyType.MORNING_BREAKOUT,
            direction = direction,
            strategyStatus = if (noTradeCheck.isNoTrade && status == "NO TRADE") "NO TRADE" else status,
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
            activeKeyLevel = activeTargetLevel,
            reasonsForSetup = reasonsForSetup,
            reasonsToAvoid = noTradeCheck.reasonsToAvoid
        )
    }

    private fun round(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0
}
