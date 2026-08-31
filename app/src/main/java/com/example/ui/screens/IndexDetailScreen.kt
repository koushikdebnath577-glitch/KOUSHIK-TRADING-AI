package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.WatchlistEntity
import com.example.data.model.IndexItem
import com.example.data.model.LoadingState
import com.example.data.model.StockSymbol
import com.example.data.model.StrategyType
import com.example.ui.theme.*

@Composable
fun IndexDetailScreen(
    index: IndexItem,
    constituents: List<StockSymbol>,
    loadingState: LoadingState,
    watchlist: List<WatchlistEntity>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSelectStockAndAnalyze: (String, StrategyType) -> Unit,
    onToggleWatchlist: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf("ALL") }

    val watchlistSymbols = remember(watchlist) { watchlist.map { it.symbol }.toSet() }

    val isPos = index.change >= 0

    val filteredConstituents = remember(constituents, searchQuery, selectedSort) {
        val base = if (searchQuery.isBlank()) constituents else {
            constituents.filter {
                it.symbol.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.token.contains(searchQuery)
            }
        }
        when (selectedSort) {
            "GAINERS" -> base.filter { it.change >= 0 }.sortedByDescending { it.changePercent }
            "LOSERS" -> base.filter { it.change < 0 }.sortedBy { it.changePercent }
            "HIGH VOL" -> base.sortedByDescending { it.volume }
            else -> base
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDarkNavy)
    ) {
        // Top App Bar
        Surface(
            color = BgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("index_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Indices",
                            tint = TextPrimary
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = index.symbol,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CyanAccentBg
                            ) {
                                Text(
                                    text = index.category,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CyanAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = index.name,
                            fontSize = 11.sp,
                            color = TextTertiary,
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("index_detail_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Data",
                        tint = CyanAccent
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Index Overview Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("index_detail_hero_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "INDEX PRICE (LTP)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTertiary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "₹${String.format(java.util.Locale.US, "%,.2f", index.ltp)}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPos) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isPos) BullishGreen else BearishRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${if (isPos) "+" else ""}${String.format(java.util.Locale.US, "%.2f", index.change)} (${if (isPos) "+" else ""}${String.format(java.util.Locale.US, "%.2f", index.changePercent)}%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPos) BullishGreen else BearishRed
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BgCardElevated,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "CONSTITUENTS",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextTertiary
                                        )
                                        Text(
                                            text = "${index.constituentCount.coerceAtLeast(constituents.size)} Stocks",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = CyanAccent
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // High / Low Range Indicator
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Low: ₹${String.format(java.util.Locale.US, "%.2f", index.low)}", fontSize = 10.sp, color = TextTertiary)
                                Text(text = "High: ₹${String.format(java.util.Locale.US, "%.2f", index.high)}", fontSize = 10.sp, color = TextTertiary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = {
                                    val span = maxOf(1.0, index.high - index.low)
                                    ((index.ltp - index.low) / span).toFloat().coerceIn(0f, 1f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (isPos) BullishGreen else BearishRed,
                                trackColor = BgCardBorder
                            )
                        }

                        if (index.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = index.description,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Launch Chart / Analysis on Index
                        Button(
                            onClick = { onSelectStockAndAnalyze(index.symbol, StrategyType.RESISTANCE_REJECTION) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccentBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("analyze_index_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShowChart,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Analyze ${index.symbol} Index Chart",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                            }
                        }
                    }
                }
            }

            // Constituent Search and Filter
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CONSTITUENT STOCKS (${filteredConstituents.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextTertiary,
                        letterSpacing = 0.5.sp
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter constituent stocks...", fontSize = 12.sp, color = TextTertiary) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyanAccent) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextTertiary)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BgCardElevated,
                            unfocusedContainerColor = BgCard,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BgCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("constituent_search_input")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filters = listOf("ALL", "GAINERS", "LOSERS", "HIGH VOL")
                        items(filters) { f ->
                            val isSelected = f == selectedSort
                            Surface(
                                onClick = { selectedSort = f },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) CyanAccent else BgPillInactive,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Text(
                                        text = f,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Loading state
            if (loadingState is LoadingState.Loading && constituents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = CyanAccent,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Loading ${index.symbol} constituents...",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else if (filteredConstituents.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No constituent stocks found",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) "No stocks match '$searchQuery'" else "Constituents are currently loading or unavailable.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Button(
                                onClick = onRefresh,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                            ) {
                                Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                items(filteredConstituents, key = { it.symbol }) { stock ->
                    val isStarred = watchlistSymbols.contains(stock.symbol)
                    val isStockPos = stock.change >= 0

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("constituent_stock_card_${stock.symbol}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Stock info
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stock.symbol,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = BgCardElevated
                                    ) {
                                        Text(
                                            text = stock.exchange,
                                            fontSize = 8.sp,
                                            color = CyanAccent,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = stock.companyName,
                                    fontSize = 11.sp,
                                    color = TextTertiary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = "L: ₹${stock.dayLow}", fontSize = 9.sp, color = TextTertiary)
                                    LinearProgressIndicator(
                                        progress = {
                                            val span = maxOf(0.1, stock.dayHigh - stock.dayLow)
                                            ((stock.ltp - stock.dayLow) / span).toFloat().coerceIn(0f, 1f)
                                        },
                                        modifier = Modifier
                                            .width(50.dp)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = if (isStockPos) BullishGreen else BearishRed,
                                        trackColor = BgCardBorder
                                    )
                                    Text(text = "H: ₹${stock.dayHigh}", fontSize = 9.sp, color = TextTertiary)
                                }
                            }

                            // Right: Price & Quick Action Buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${String.format(java.util.Locale.US, "%.2f", stock.ltp)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${if (isStockPos) "+" else ""}${String.format(java.util.Locale.US, "%.2f", stock.changePercent)}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isStockPos) BullishGreen else BearishRed
                                    )
                                }

                                // Analyze strategy button
                                Surface(
                                    onClick = { onSelectStockAndAnalyze(stock.symbol, StrategyType.RESISTANCE_REJECTION) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = CyanAccentBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                                    modifier = Modifier.testTag("analyze_stock_btn_${stock.symbol}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShowChart,
                                            contentDescription = "Analyze",
                                            tint = CyanAccent,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Analyze",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanAccent
                                        )
                                    }
                                }

                                // Watchlist Star
                                IconButton(
                                    onClick = { onToggleWatchlist(stock.symbol, stock.companyName, stock.token) },
                                    modifier = Modifier.size(32.dp).testTag("star_stock_btn_${stock.symbol}")
                                ) {
                                    Icon(
                                        imageVector = if (isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = if (isStarred) "Remove from watchlist" else "Add to watchlist",
                                        tint = if (isStarred) KeyLevelYellow else TextTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
