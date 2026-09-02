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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatMarketTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return try {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }
}

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
    previousClose: Double = 0.0,
    lastUpdatedTimestamp: Long = 0L,
    onReconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLive = status == ConnectionStatus.LIVE
    val isPositive = change >= 0
    val statusColor = when (status) {
        ConnectionStatus.LIVE -> BullishGreen
        ConnectionStatus.CONNECTED_WAITING_FOR_TICK -> CyanAccent
        ConnectionStatus.CONNECTING -> KeyLevelYellow
        ConnectionStatus.DISCONNECTED -> BearishRed
        ConnectionStatus.ERROR -> BearishRed
    }

    val displayPrice = when {
        ltp > 0.0 -> ltp
        previousClose > 0.0 -> previousClose
        else -> 0.0
    }

    val priceLabel = when {
        isLive && ltp > 0.0 -> "LIVE"
        ltp > 0.0 -> "LAST AVAILABLE PRICE"
        previousClose > 0.0 -> "PREVIOUS CLOSE"
        else -> "PRICE UNAVAILABLE"
    }

    val formattedTime = formatMarketTimestamp(lastUpdatedTimestamp)

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
            // Left: App name + Stock symbol + Connection Status Pill
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Text(
                    text = "KOUSHIK TRADING AI",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = selectedSymbol,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Surface(
                        onClick = onReconnectClick,
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = status.label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Right: Price & Change in monospace
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                if (displayPrice > 0.0) {
                    Text(
                        text = "₹${String.format(Locale.US, "%,.2f", displayPrice)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isLive) (if (isPositive) BullishGreen else BearishRed) else TextPrimary,
                        maxLines = 1,
                        softWrap = false
                    )

                    if (isLive) {
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.2f", change)} (${if (isPositive) "+" else ""}${String.format(Locale.US, "%.2f", changePercent)}%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isPositive) BullishGreen else BearishRed,
                            maxLines = 1,
                            softWrap = false
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.wrapContentWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = KeyLevelYellowBg
                            ) {
                                Text(
                                    text = priceLabel,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KeyLevelYellow,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            if (formattedTime.isNotEmpty()) {
                                Text(
                                    text = formattedTime,
                                    fontSize = 8.sp,
                                    color = TextTertiary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Price unavailable",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextTertiary,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "LIVE DATA UNAVAILABLE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = KeyLevelYellow,
                        maxLines = 1,
                        softWrap = false
                    )
                }
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false).padding(end = 4.dp)
                        ) {
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
                                color = TextPrimary,
                                maxLines = 1
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.wrapContentWidth()
                        ) {
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

