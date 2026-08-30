package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnalysisResult
import com.example.data.model.TradeDirection
import com.example.ui.theme.*

@Composable
fun TradePlanPanel(
    analysisResult: AnalysisResult,
    riskAmount: Double = 2500.0,
    onEditRisk: (() -> Unit)? = null,
    onSavePlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entry = analysisResult.conservativeEntry ?: analysisResult.normalEntry ?: analysisResult.aggressiveEntry
    val isShort = analysisResult.direction == TradeDirection.SHORT
    val riskPerShare = if (entry != null) kotlin.math.abs(entry.entryPrice - entry.stopLoss) else 0.0
    val calculatedQty = if (riskPerShare > 0.05) (riskAmount / riskPerShare).toInt().coerceAtLeast(1) else 1

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("trade_plan_panel")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Panel Header & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.ListAlt,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "LIVE TRADE PLAN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        letterSpacing = 0.8.sp
                    )
                }

                Button(
                    onClick = onSavePlan,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("save_trade_plan_button")
                ) {
                    Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Save Plan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgCardElevated)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlanRow(label = "Stock", value = analysisResult.symbol, valueColor = TextPrimary, isBold = true)
                PlanRow(label = "Strategy", value = analysisResult.strategy.displayName, valueColor = CyanAccent)
                PlanRow(
                    label = "Direction",
                    value = analysisResult.direction.label,
                    valueColor = if (isShort) BearishRed else BullishGreen,
                    isBold = true
                )
                PlanRow(label = "Setup Score", value = "${analysisResult.setupScore}/100 (${analysisResult.setupGrade.label})", valueColor = KeyLevelYellow)
                PlanRow(label = "Current Status", value = analysisResult.strategyStatus, valueColor = TextPrimary, isBold = true)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Price Levels Grid
            if (entry != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgCardElevated)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlanRow(label = "Entry Zone", value = "₹${entry.entryPrice}", valueColor = CyanAccent)
                    PlanRow(label = "Conservative Entry", value = "₹${analysisResult.conservativeEntry?.entryPrice ?: entry.entryPrice}", valueColor = BullishGreen)
                    PlanRow(label = "Structure Stop Loss", value = "₹${entry.stopLoss}", valueColor = BearishRed, isBold = true)
                    PlanRow(label = "Target 1 (1:1.5 RR)", value = "₹${entry.target1}", valueColor = BullishGreen)
                    PlanRow(label = "Target 2 (1:2.5 RR)", value = "₹${entry.target2}", valueColor = BullishGreenDark)
                    PlanRow(label = "Risk:Reward Ratio", value = "1 : ${entry.riskRewardRatio}", valueColor = KeyLevelYellow, isBold = true)
                    PlanRow(
                        label = "Risk Budget per Trade",
                        value = "₹${if (riskAmount % 1.0 == 0.0) riskAmount.toInt() else String.format(java.util.Locale.US, "%.2f", riskAmount)}",
                        valueColor = KeyLevelYellow,
                        isBold = true
                    )
                    PlanRow(
                        label = "Recommended Position Size",
                        value = "$calculatedQty shares",
                        valueColor = CyanAccent,
                        isBold = true
                    )
                    PlanRow(label = "Confirmation", value = analysisResult.confirmationStatus.label, valueColor = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reasons For Setup
            if (analysisResult.reasonsForSetup.isNotEmpty()) {
                Text(
                    text = "Reason for Setup:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BullishGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                analysisResult.reasonsForSetup.take(3).forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(text = "•", color = BullishGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = reason, fontSize = 11.sp, color = TextSecondary, lineHeight = 14.sp)
                    }
                }
            }

            // Reasons to Avoid
            if (analysisResult.reasonsToAvoid.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Reason to Avoid:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BearishRed
                )
                Spacer(modifier = Modifier.height(4.dp))
                analysisResult.reasonsToAvoid.take(3).forEach { avoid ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(text = "•", color = BearishRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = avoid, fontSize = 11.sp, color = TextSecondary, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanRow(
    label: String,
    value: String,
    valueColor: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$label:", fontSize = 11.sp, color = TextTertiary)
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}
