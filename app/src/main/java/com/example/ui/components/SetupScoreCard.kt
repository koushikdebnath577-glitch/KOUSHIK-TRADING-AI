package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnalysisResult
import com.example.data.model.ScoreBreakdown
import com.example.data.model.SetupGrade
import com.example.ui.theme.*

@Composable
fun SetupScoreCard(
    analysisResult: AnalysisResult,
    modifier: Modifier = Modifier
) {
    val score = analysisResult.setupScore
    val grade = analysisResult.setupGrade
    val breakdown = analysisResult.scoreBreakdown

    val gradeColor = when (grade) {
        SetupGrade.A_PLUS -> BullishGreen
        SetupGrade.A -> CyanAccent
        SetupGrade.B -> KeyLevelYellow
        SetupGrade.NO_TRADE -> BearishRed
    }

    val gradeBg = when (grade) {
        SetupGrade.A_PLUS -> BullishGreenBg
        SetupGrade.A -> CyanAccentBg
        SetupGrade.B -> KeyLevelYellowBg
        SetupGrade.NO_TRADE -> BearishRedBg
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("setup_score_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(gradeColor)
                    )
                    Text(
                        text = "TRADE QUALITY SCORE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        letterSpacing = 0.8.sp,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = gradeBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, gradeColor.copy(alpha = 0.35f)),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Text(
                        text = grade.label,
                        color = gradeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Score Large Gauge Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Score Number
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = gradeColor
                    )
                    Text(
                        text = "/ 100",
                        fontSize = 12.sp,
                        color = TextTertiary
                    )
                }

                // Progress Bars Breakdown
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ScoreBarItem(
                        title = "Level Quality",
                        score = breakdown.levelQuality,
                        max = 30,
                        barColor = KeyLevelYellow
                    )
                    ScoreBarItem(
                        title = "Price Rejection",
                        score = breakdown.priceRejection,
                        max = 25,
                        barColor = CyanAccent
                    )
                    ScoreBarItem(
                        title = "Confirmation Candle",
                        score = breakdown.confirmationCandle,
                        max = 25,
                        barColor = BullishGreen
                    )
                    ScoreBarItem(
                        title = "Structure Confirmation",
                        score = breakdown.structureConfirmation,
                        max = 20,
                        barColor = PurpleAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Threshold Note
            Text(
                text = if (score >= 80) "★ High conviction setup. Meets institutional criteria."
                else if (score >= 60) "⚠ Moderate quality. Requires strict manual confirmation."
                else "✖ Quality below 60. Strict capital protection active (No Trade).",
                fontSize = 11.sp,
                color = if (score >= 80) BullishGreen else if (score >= 60) KeyLevelYellow else BearishRed,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ScoreBarItem(
    title: String,
    score: Int,
    max: Int,
    barColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontSize = 10.sp, color = TextSecondary)
            Text(text = "$score/$max", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { (score.toFloat() / max).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = BgCardBorder
        )
    }
}
