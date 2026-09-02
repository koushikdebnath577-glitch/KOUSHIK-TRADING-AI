package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionStatus
import com.example.data.model.IndexItem
import com.example.data.model.SetupGrade
import com.example.data.model.StockSymbol
import com.example.data.model.StrategyType
import com.example.data.repository.IndicesDataProvider
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.formatMarketTimestamp
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    symbols: List<StockSymbol>,
    indices: List<IndexItem> = emptyList(),
    connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTING,
    defaultRiskAmount: Double = 2500.0,
    onEditRisk: () -> Unit = {},
    onSelectIndex: (IndexItem) -> Unit = {},
    onSelectStockAndAnalyze: (String, StrategyType) -> Unit,
    onNavigateToMarkets: () -> Unit,
    onNavigateToWatchlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayIndices = if (indices.isNotEmpty()) indices else IndicesDataProvider.DEFAULT_INDICES
    val stockSymbols = symbols.filter { sym -> displayIndices.none { it.symbol == sym.symbol || it.alias == sym.symbol } }
    val isLive = connectionStatus == ConnectionStatus.LIVE

    val headerStatusColor = when (connectionStatus) {
        ConnectionStatus.LIVE -> BullishGreen
        ConnectionStatus.CONNECTED_WAITING_FOR_TICK -> CyanAccent
        ConnectionStatus.CONNECTING -> KeyLevelYellow
        ConnectionStatus.DISCONNECTED -> BearishRed
        ConnectionStatus.ERROR -> BearishRed
    }

    var selectedIndexCategory by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("ALL") }

    val filteredIndices = androidx.compose.runtime.remember(displayIndices, selectedIndexCategory) {
        when (selectedIndexCategory) {
            "BROAD MARKET" -> displayIndices.filter { it.category.equals("Broad Market", ignoreCase = true) }
            "SECTORAL" -> displayIndices.filter { it.category.equals("Sectoral", ignoreCase = true) }
            "THEMATIC" -> displayIndices.filter { it.category.equals("Thematic", ignoreCase = true) }
            else -> displayIndices
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BgDarkNavy)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Header Banner
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                ) {
                    Text(
                        text = "KOUSHIK TRADING AI",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = 0.8.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "Intraday Market Structure & Precision Strategy Terminal",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = headerStatusColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, headerStatusColor.copy(alpha = 0.4f)),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(headerStatusColor))
                        Text(
                            text = if (isLive) "NSE LIVE" else connectionStatus.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = headerStatusColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Legal & Safety Disclaimer
        item {
            DisclaimerBanner()
        }

        // Intraday Risk Quick Access Card
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BgCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                modifier = Modifier.fillMaxWidth().testTag("home_risk_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KeyLevelYellowBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = KeyLevelYellow,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Intraday Risk per Trade",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                            Text(
                                text = "₹${if (defaultRiskAmount % 1.0 == 0.0) defaultRiskAmount.toInt() else String.format(java.util.Locale.US, "%.2f", defaultRiskAmount)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        onClick = onEditRisk,
                        shape = RoundedCornerShape(6.dp),
                        color = CyanAccentBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                        modifier = Modifier.wrapContentWidth().testTag("home_edit_risk_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                                text = "Edit",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Market Indices Section (All 23 Supported Indices)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "NSE INDICES (${displayIndices.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary,
                                letterSpacing = 0.5.sp
                            )
                            Surface(shape = RoundedCornerShape(4.dp), color = CyanAccentBg) {
                                Text(
                                    text = "TAP FOR CONSTITUENTS",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "View Markets",
                        fontSize = 11.sp,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onNavigateToMarkets() }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Category Chips for Indices
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val categories = listOf("ALL", "BROAD MARKET", "SECTORAL", "THEMATIC")
                    items(categories) { cat ->
                        val isSelected = cat == selectedIndexCategory
                        Surface(
                            onClick = { selectedIndexCategory = cat },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CyanAccent else BgPillInactive,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredIndices, key = { it.symbol }) { idx ->
                        IndexCard(
                            index = idx,
                            isLive = isLive,
                            onClick = { onSelectIndex(idx) }
                        )
                    }
                }
            }
        }

        // Strategy Quick Launch Hero Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CORE INTRADAY STRATEGIES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StrategyHeroCard(
                        title = "Resistance Rejection",
                        subtitle = "Liquidity Sweeps & Bearish Confirmation",
                        icon = Icons.Default.TrendingDown,
                        accentColor = BearishRed,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectStockAndAnalyze("NIFTY 50", StrategyType.RESISTANCE_REJECTION) }
                    )

                    StrategyHeroCard(
                        title = "Morning Breakout",
                        subtitle = "Opening Range & Conservative Retest",
                        icon = Icons.Default.TrendingUp,
                        accentColor = BullishGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectStockAndAnalyze("BANKNIFTY", StrategyType.MORNING_BREAKOUT) }
                    )
                }
            }
        }

        // High Probability Setups Today Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE HIGH-PROBABILITY WATCHLIST",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "View All",
                    fontSize = 11.sp,
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToMarkets() }
                        .padding(4.dp)
                )
            }
        }

        items(stockSymbols.take(5)) { stock ->
            StockItemCard(
                stock = stock,
                isLive = isLive,
                onClick = { onSelectStockAndAnalyze(stock.symbol, StrategyType.RESISTANCE_REJECTION) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IndexCard(
    index: IndexItem,
    isLive: Boolean,
    onClick: () -> Unit
) {
    val isPos = index.change >= 0
    val displayPrice = when {
        index.ltp > 0.0 -> index.ltp
        index.prevClose > 0.0 -> index.prevClose
        else -> 0.0
    }
    val priceLabel = when {
        isLive && index.ltp > 0.0 -> "LIVE"
        index.ltp > 0.0 -> "LAST AVAILABLE"
        index.prevClose > 0.0 -> "PREV CLOSE"
        else -> "UNAVAILABLE"
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
        modifier = Modifier
            .width(175.dp)
            .testTag("index_card_${index.symbol}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = index.symbol,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = BgCardElevated
                ) {
                    Text(
                        text = "${index.constituentCount}",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (displayPrice > 0.0) {
                Text(
                    text = "₹${String.format(java.util.Locale.US, "%,.2f", displayPrice)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = if (isLive) (if (isPos) BullishGreen else BearishRed) else TextPrimary,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLive) {
                        Text(
                            text = "${if (isPos) "+" else ""}${String.format(java.util.Locale.US, "%.2f", index.changePercent)}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPos) BullishGreen else BearishRed,
                            maxLines = 1,
                            softWrap = false
                        )
                    } else {
                        Text(
                            text = "$priceLabel • NOT LIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = KeyLevelYellow,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Text(
                        text = index.category,
                        fontSize = 8.sp,
                        color = TextTertiary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            } else {
                Text(
                    text = "Price unavailable",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextTertiary,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "LIVE DATA UNAVAILABLE",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = KeyLevelYellow,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun StrategyHeroCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 10.sp, color = TextSecondary, lineHeight = 13.sp)
        }
    }
}

@Composable
fun StockItemCard(
    stock: StockSymbol,
    isLive: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPos = stock.change >= 0
    val displayPrice = when {
        stock.ltp > 0.0 -> stock.ltp
        stock.previousClose > 0.0 -> stock.previousClose
        else -> 0.0
    }
    val priceLabel = when {
        isLive && stock.ltp > 0.0 -> "LIVE"
        stock.ltp > 0.0 -> "LAST AVAILABLE"
        stock.previousClose > 0.0 -> "PREV CLOSE"
        else -> "UNAVAILABLE"
    }
    val formattedTime = formatMarketTimestamp(stock.lastUpdated)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("stock_item_${stock.symbol}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = stock.symbol, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                    Surface(shape = RoundedCornerShape(3.dp), color = BgCardElevated) {
                        Text(text = stock.exchange, fontSize = 8.sp, color = CyanAccent, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
                Text(
                    text = stock.companyName,
                    fontSize = 11.sp,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.wrapContentWidth()
            ) {
                if (displayPrice > 0.0) {
                    Text(
                        text = "₹${String.format(java.util.Locale.US, "%,.2f", displayPrice)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = if (isLive) (if (isPos) BullishGreen else BearishRed) else TextPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (isLive) {
                        Text(
                            text = "${if (isPos) "+" else ""}${String.format(java.util.Locale.US, "%.2f", stock.change)} (${if (isPos) "+" else ""}${String.format(java.util.Locale.US, "%.2f", stock.changePercent)}%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isPos) BullishGreen else BearishRed,
                            maxLines = 1,
                            softWrap = false
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "$priceLabel • NOT LIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = KeyLevelYellow,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (formattedTime.isNotEmpty()) {
                                Text(
                                    text = "• $formattedTime",
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextTertiary,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "LIVE DATA UNAVAILABLE",
                        fontSize = 8.sp,
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
