package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.*
import com.example.data.repository.TradingRepository
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun AnalyzeScreen(
    selectedSymbol: String,
    stock: StockSymbol?,
    connectionStatus: ConnectionStatus,
    candles: List<Candle>,
    keyLevels: List<KeyLevel>,
    analysisResult: AnalysisResult?,
    ema20: List<Double?>,
    ema50: List<Double?>,
    vwap: List<Double?>,
    rsi: List<Double?>,
    indicatorSettings: TradingRepository.IndicatorSettings,
    selectedTimeframe: Timeframe,
    selectedSession: TradingSession = TradingSession.TODAY,
    onSessionSelected: (TradingSession) -> Unit = {},
    diagnosticMessage: String? = null,
    selectedStrategy: StrategyType,
    defaultRiskAmount: Double = 2500.0,
    onTimeframeSelected: (Timeframe) -> Unit,
    onStrategySelected: (StrategyType) -> Unit,
    onToggleIndicator: (String) -> Unit,
    onEditRisk: () -> Unit = {},
    onSavePlan: () -> Unit,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLtp = stock?.ltp ?: 0.0
    val displayLtp = if (currentLtp > 0.0) currentLtp else (candles.lastOrNull()?.close ?: 0.0)
    val prevClose = stock?.previousClose ?: 0.0
    val displayPrevClose = if (prevClose > 0.0) prevClose else displayLtp
    val change = stock?.change ?: if (displayPrevClose > 0 && displayLtp > 0) (displayLtp - displayPrevClose) else 0.0
    val changePercent = stock?.changePercent ?: if (displayPrevClose > 0 && displayLtp > 0) ((displayLtp - displayPrevClose) / displayPrevClose) * 100.0 else 0.0
    val lastUpdated = stock?.lastUpdated ?: 0L

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgDarkNavy)
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Connection and Stock Header
        item {
            ConnectionStatusHeader(
                status = connectionStatus,
                selectedSymbol = selectedSymbol,
                ltp = displayLtp,
                change = change,
                changePercent = changePercent,
                previousClose = displayPrevClose,
                lastUpdatedTimestamp = lastUpdated,
                onReconnectClick = onReconnect
            )
        }

        // 2. Mandatory Disclaimer Banner
        item {
            DisclaimerBanner()
        }

        // 3. Strategy Selector & Status Ribbon
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgCard)
                        .border(1.dp, BgCardBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StrategyTabButton(
                        title = "Resistance Rejection",
                        icon = Icons.Default.TrendingDown,
                        isSelected = selectedStrategy == StrategyType.RESISTANCE_REJECTION,
                        activeColor = BearishRed,
                        modifier = Modifier.weight(1f)
                    ) {
                        onStrategySelected(StrategyType.RESISTANCE_REJECTION)
                    }

                    StrategyTabButton(
                        title = "Morning Breakout",
                        icon = Icons.Default.TrendingUp,
                        isSelected = selectedStrategy == StrategyType.MORNING_BREAKOUT,
                        activeColor = BullishGreen,
                        modifier = Modifier.weight(1f)
                    ) {
                        onStrategySelected(StrategyType.MORNING_BREAKOUT)
                    }
                }

                // Strategy Status Banner
                if (analysisResult != null) {
                    StrategyStatusBanner(analysisResult = analysisResult)
                }

                // Intraday Risk Quick Access Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                    modifier = Modifier.fillMaxWidth().testTag("analyze_risk_quick_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = KeyLevelYellow,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(text = "Intraday Risk:", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = "₹${if (defaultRiskAmount % 1.0 == 0.0) defaultRiskAmount.toInt() else String.format(java.util.Locale.US, "%.2f", defaultRiskAmount)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = KeyLevelYellow
                            )
                        }

                        Surface(
                            onClick = onEditRisk,
                            shape = RoundedCornerShape(6.dp),
                            color = CyanAccentBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("analyze_edit_risk_button")
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
                                    modifier = Modifier.size(12.dp)
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
            }
        }

        // 4. Interactive TradingView-style Candlestick Chart
        item {
            TradingViewChart(
                candles = candles,
                keyLevels = keyLevels,
                currentLtp = currentLtp,
                analysisResult = analysisResult,
                ema20 = ema20,
                ema50 = ema50,
                vwap = vwap,
                rsi = rsi,
                indicatorSettings = indicatorSettings,
                selectedTimeframe = selectedTimeframe,
                onTimeframeSelected = onTimeframeSelected,
                onToggleIndicator = onToggleIndicator,
                selectedSession = selectedSession,
                onSessionSelected = onSessionSelected,
                diagnosticMessage = diagnosticMessage,
                connectionStatus = connectionStatus,
                lastUpdatedTimestamp = lastUpdated
            )
        }

        // 5. Strict No Trade / Wait for Setup Warning (if triggered)
        if (analysisResult?.noTradeWarning != null) {
            item {
                NoTradeWarningCard(
                    warningTitle = analysisResult.noTradeWarning!!,
                    reasonsToAvoid = analysisResult.reasonsToAvoid
                )
            }
        }

        // 6. Setup Quality Score Card (Score /100 + breakdown)
        if (analysisResult != null) {
            item {
                SetupScoreCard(analysisResult = analysisResult)
            }
        }

        // 7. Conservative Entry Assistant Card (Aggressive / Normal / Conservative)
        if (analysisResult != null) {
            item {
                ConservativeEntryCard(
                    analysisResult = analysisResult,
                    riskAmount = defaultRiskAmount,
                    onEditRisk = onEditRisk
                )
            }
        }

        // 8. Confirmation Candle Analysis & Details
        if (analysisResult != null) {
            item {
                ConfirmationStatusBadge(
                    status = analysisResult.confirmationStatus,
                    details = analysisResult.confirmationDetails
                )
            }
        }

        // 9. Momentum Status & Speed Guard
        if (analysisResult != null) {
            item {
                MomentumStatusCard(
                    status = analysisResult.momentumStatus,
                    notes = analysisResult.momentumNotes
                )
            }
        }

        // 10. Stop Loss Suggestions (Structure SL, Tight SL, Invalidation SL)
        if (analysisResult != null) {
            item {
                StopLossSuggestionsCard(
                    suggestions = analysisResult.stopLossSuggestions,
                    currentPrice = currentLtp
                )
            }
        }

        // 11. Automatic Key Levels Summary
        item {
            KeyLevelsSummaryList(levels = keyLevels)
        }

        // 12. Live Trade Plan Panel
        if (analysisResult != null) {
            item {
                TradePlanPanel(
                    analysisResult = analysisResult,
                    riskAmount = defaultRiskAmount,
                    onEditRisk = onEditRisk,
                    onSavePlan = onSavePlan
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StrategyTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) BgPillInactive else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, activeColor.copy(alpha = 0.6f)) else null,
        modifier = modifier.height(36.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) activeColor else TextTertiary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TextPrimary else TextSecondary
            )
        }
    }
}

@Composable
private fun StrategyStatusBanner(
    analysisResult: AnalysisResult
) {
    val statusText = analysisResult.strategyStatus
    val statusColor = when {
        statusText.contains("ENTRY", ignoreCase = true) || statusText.contains("CONFIRMED", ignoreCase = true) -> BullishGreen
        statusText.contains("WATCH", ignoreCase = true) -> CyanAccent
        statusText.contains("WAIT", ignoreCase = true) -> KeyLevelYellow
        statusText.contains("NO TRADE", ignoreCase = true) || statusText.contains("FAILED", ignoreCase = true) -> BearishRed
        else -> TextSecondary
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = BgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "CURRENT SETUP STATE", fontSize = 9.sp, color = TextTertiary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    letterSpacing = 0.3.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (analysisResult.direction == TradeDirection.SHORT) BearishRedBg else BullishGreenBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, (if (analysisResult.direction == TradeDirection.SHORT) BearishRed else BullishGreen).copy(alpha = 0.4f))
            ) {
                Text(
                    text = analysisResult.direction.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (analysisResult.direction == TradeDirection.SHORT) BearishRed else BullishGreen,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MomentumStatusCard(
    status: MomentumStatus,
    notes: String
) {
    val color = when (status) {
        MomentumStatus.STRONG_MOMENTUM -> BullishGreen
        MomentumStatus.NORMAL_MOMENTUM -> CyanAccent
        MomentumStatus.WEAK_MOMENTUM -> OrangeWarning
        MomentumStatus.NO_MOMENTUM -> BearishRed
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "MOMENTUM & SPEED TRACKER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent, letterSpacing = 0.8.sp)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = color.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = status.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = notes, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun StopLossSuggestionsCard(
    suggestions: StopLossSuggestions,
    currentPrice: Double
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "STOP LOSS SUGGESTIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent, letterSpacing = 0.8.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SLBox(title = "Structure SL", price = suggestions.structureStopLoss, color = BearishRed, modifier = Modifier.weight(1f))
                SLBox(title = "Tight SL", price = suggestions.tightStopLoss, color = KeyLevelYellow, modifier = Modifier.weight(1f))
                SLBox(title = "Invalidation SL", price = suggestions.invalidationStopLoss, color = PurpleAccent, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = suggestions.explanation, fontSize = 10.sp, color = TextTertiary)
        }
    }
}

@Composable
private fun SLBox(
    title: String,
    price: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BgCardElevated)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 9.sp, color = TextTertiary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "₹$price",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}
