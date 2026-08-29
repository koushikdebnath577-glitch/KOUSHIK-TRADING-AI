package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionStatus
import com.example.data.model.KeyLevel
import com.example.data.model.LevelStrength
import com.example.ui.theme.*

@Composable
fun DisclaimerBanner(
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = OrangeWarningBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, OrangeWarning.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("disclaimer_banner")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ANALYSIS ONLY • NOT FINANCIAL ADVICE • MANUAL CONFIRMATION REQUIRED",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = OrangeWarning,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Intraday signals require independent verification. Capital protection rules active.",
                fontSize = 9.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ConnectionStatusHeader(
    status: ConnectionStatus,
    selectedSymbol: String,
    ltp: Double,
    change: Double,
    changePercent: Double,
    onReconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPositive = change >= 0
    val statusColor = when (status) {
        ConnectionStatus.LIVE -> BullishGreen
        ConnectionStatus.RECONNECTING -> KeyLevelYellow
        ConnectionStatus.DISCONNECTED -> BearishRed
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: App name + Stock symbol + Live Pill
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "KOUSHIK TRADING AI",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = selectedSymbol,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Surface(
                        onClick = onReconnectClick,
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = status.label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Right: Price & Change in monospace
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "₹${String.format(java.util.Locale.US, "%.2f", ltp)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isPositive) BullishGreen else BearishRed
                )
                Text(
                    text = "${if (isPositive) "+" else ""}${String.format(java.util.Locale.US, "%.2f", change)} (${if (isPositive) "+" else ""}${String.format(java.util.Locale.US, "%.2f", changePercent)}%)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isPositive) BullishGreen else BearishRed
                )
            }
        }
    }
}

@Composable
fun KeyLevelsSummaryList(
    levels: List<KeyLevel>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("key_levels_summary_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KEY MARKET LEVELS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "${levels.size} Detected",
                    fontSize = 10.sp,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                levels.take(6).forEach { level ->
                    val color = Color(level.type.colorHex)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgCardElevated)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Text(
                                text = level.type.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (level.strength) {
                                    LevelStrength.STRONG -> BullishGreenBg
                                    LevelStrength.MODERATE -> KeyLevelYellowBg
                                    LevelStrength.WEAK -> BgCardBorder
                                }
                            ) {
                                Text(
                                    text = "${level.strength.label} (${level.touchCount}T)",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (level.strength) {
                                        LevelStrength.STRONG -> BullishGreen
                                        LevelStrength.MODERATE -> KeyLevelYellow
                                        LevelStrength.WEAK -> TextTertiary
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "₹${level.price}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = color
                            )
                        }
                    }
                }
            }
        }
    }
}

