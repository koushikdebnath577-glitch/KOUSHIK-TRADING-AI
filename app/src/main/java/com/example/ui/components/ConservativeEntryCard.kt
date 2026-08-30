package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.EntryPlan
import com.example.ui.theme.*

@Composable
fun ConservativeEntryCard(
    analysisResult: AnalysisResult,
    riskAmount: Double = 2500.0,
    onEditRisk: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedEntryTab by remember { mutableStateOf(2) } // 0: Aggressive, 1: Normal, 2: Conservative

    val activePlan: EntryPlan? = when (selectedEntryTab) {
        0 -> analysisResult.aggressiveEntry
        1 -> analysisResult.normalEntry
        else -> analysisResult.conservativeEntry
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("conservative_entry_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "CONSERVATIVE ENTRY ASSISTANT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        letterSpacing = 0.8.sp
                    )
                }

                // Forming Candle Warning Badge
                if (analysisResult.isCandleForming) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = OrangeWarningBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangeWarning.copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = OrangeWarning,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "WAIT FOR CANDLE CLOSE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeWarning
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Entry Style Tabs (Aggressive / Normal / Conservative)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgCardElevated)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EntryStyleTab(
                    title = "AGGRESSIVE",
                    isSelected = selectedEntryTab == 0,
                    badgeColor = BearishRed
                ) { selectedEntryTab = 0 }

                EntryStyleTab(
                    title = "NORMAL",
                    isSelected = selectedEntryTab == 1,
                    badgeColor = KeyLevelYellow
                ) { selectedEntryTab = 1 }

                EntryStyleTab(
                    title = "CONSERVATIVE",
                    isSelected = selectedEntryTab == 2,
                    badgeColor = BullishGreen
                ) { selectedEntryTab = 2 }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activePlan != null) {
                // Key Numerical Parameters Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        label = "Entry Price",
                        value = "₹${activePlan.entryPrice}",
                        color = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "Stop Loss",
                        value = "₹${activePlan.stopLoss}",
                        color = BearishRed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "Target 1",
                        value = "₹${activePlan.target1}",
                        color = BullishGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "Target 2",
                        value = "₹${activePlan.target2}",
                        color = BullishGreenDark,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Intraday Risk Budget & Position Sizing Section
                val riskPerShare = kotlin.math.abs(activePlan.entryPrice - activePlan.stopLoss)
                val calculatedQty = if (riskPerShare > 0.05) {
                    (riskAmount / riskPerShare).toInt().coerceAtLeast(1)
                } else 1
                val totalRiskAtSL = calculatedQty * riskPerShare
                val reward1PerShare = kotlin.math.abs(activePlan.target1 - activePlan.entryPrice)
                val reward2PerShare = kotlin.math.abs(activePlan.target2 - activePlan.entryPrice)
                val totalRewardAtT1 = calculatedQty * reward1PerShare
                val totalRewardAtT2 = calculatedQty * reward2PerShare

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BgCardElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                    modifier = Modifier.fillMaxWidth().testTag("intraday_risk_position_sizing_section")
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Risk Budget Header with Edit Action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = KeyLevelYellow,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "INTRADAY RISK BUDGET:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "₹${if (riskAmount % 1.0 == 0.0) riskAmount.toInt() else String.format(java.util.Locale.US, "%.2f", riskAmount)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KeyLevelYellow
                                )
                            }

                            if (onEditRisk != null) {
                                Surface(
                                    onClick = onEditRisk,
                                    shape = RoundedCornerShape(6.dp),
                                    color = CyanAccentBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
                                    modifier = Modifier.testTag("edit_risk_button_in_card")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Risk",
                                            tint = CyanAccent,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "Edit Risk",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanAccent
                                        )
                                    }
                                }
                            }
                        }

                        // Calculated Position Sizing Metrics Grid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BgDarkNavy)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Recommended Qty", fontSize = 9.sp, color = TextSecondary)
                                Text(
                                    text = "$calculatedQty shares",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                            }
                            Column {
                                Text(text = "Total Risk @ SL", fontSize = 9.sp, color = TextSecondary)
                                Text(
                                    text = "₹${String.format(java.util.Locale.US, "%.1f", totalRiskAtSL)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BearishRed
                                )
                            }
                            Column {
                                Text(text = "Profit @ T1", fontSize = 9.sp, color = TextSecondary)
                                Text(
                                    text = "+₹${String.format(java.util.Locale.US, "%.1f", totalRewardAtT1)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BullishGreen
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Risk : Reward", fontSize = 9.sp, color = TextSecondary)
                                Text(
                                    text = "1 : ${activePlan.riskRewardRatio}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (activePlan.riskRewardRatio >= 1.5) BullishGreen else OrangeWarning
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Execution Requirement / Safety Rule
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(BgCard)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = activePlan.executionRequirement,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            } else {
                Text(
                    text = "Awaiting trade setup criteria before calculating entry metrics.",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun RowScope.EntryStyleTab(
    title: String,
    isSelected: Boolean,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) BgCardElevated else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f)) else null,
        modifier = Modifier
            .weight(1f)
            .height(30.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) badgeColor else TextTertiary
            )
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(BgDarkNavy)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 9.sp, color = TextTertiary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
    }
}
