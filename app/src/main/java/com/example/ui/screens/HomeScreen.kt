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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SetupGrade
import com.example.data.model.StockSymbol
import com.example.data.model.StrategyType
import com.example.ui.components.DisclaimerBanner
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    symbols: List<StockSymbol>,
    defaultRiskAmount: Double = 2500.0,
    onEditRisk: () -> Unit = {},
    onSelectStockAndAnalyze: (String, StrategyType) -> Unit,
    onNavigateToMarkets: () -> Unit,
    onNavigateToWatchlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val indexSymbols = symbols.filter { it.symbol in listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "INDIA VIX") }
    val stockSymbols = symbols.filter { it.symbol !in listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "INDIA VIX") }

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
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "KOUSHIK TRADING AI",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Text(
                        text = "Intraday Market Structure & Precision Strategy Terminal",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CyanAccentBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BullishGreen))
                        Text(text = "NSE LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                color = TextSecondary
                            )
                            Text(
                                text = "₹${if (defaultRiskAmount % 1.0 == 0.0) defaultRiskAmount.toInt() else String.format(java.util.Locale.US, "%.2f", defaultRiskAmount)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Surface(
                        onClick = onEditRisk,
                        shape = RoundedCornerShape(6.dp),
                        color = CyanAccentBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("home_edit_risk_button")
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
                                color = CyanAccent
                            )
                        }
                    }
                }
            }
        }

        // Market Indices Carousel
        item {
            Column {
                Text(
                    text = "MAJOR INDICES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(indexSymbols) { sym ->
                        IndexCard(sym = sym, onClick = { onSelectStockAndAnalyze(sym.symbol, StrategyType.RESISTANCE_REJECTION) })
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
    sym: StockSymbol,
    onClick: () -> Unit
) {
    val isPos = sym.change >= 0
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
        modifier = Modifier
            .width(135.dp)
            .testTag("index_card_${sym.symbol}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = sym.symbol, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "₹${String.format(java.util.Locale.US, "%.2f", sym.ltp)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${if (isPos) "+" else ""}${String.format(java.util.Locale.US, "%.2f", sym.changePercent)}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPos) BullishGreen else BearishRed
            )
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPos = stock.change >= 0
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
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = stock.symbol, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Surface(shape = RoundedCornerShape(3.dp), color = BgCardElevated) {
                        Text(text = stock.exchange, fontSize = 8.sp, color = CyanAccent, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
                Text(text = stock.companyName, fontSize = 11.sp, color = TextTertiary, maxLines = 1)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format(java.util.Locale.US, "%.2f", stock.ltp)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = TextPrimary
                )
                Text(
                    text = "${if (isPos) "+" else ""}${String.format(java.util.Locale.US, "%.2f", stock.change)} (${if (isPos) "+" else ""}${String.format(java.util.Locale.US, "%.2f", stock.changePercent)}%)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isPos) BullishGreen else BearishRed
                )
            }
        }
    }
}
