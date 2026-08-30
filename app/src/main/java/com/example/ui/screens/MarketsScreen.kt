package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.WatchlistEntity
import com.example.data.model.StockSearchResult
import com.example.data.model.StockSymbol
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun MarketsScreen(
    symbols: List<StockSymbol>,
    watchlist: List<WatchlistEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onStockSelected: (String) -> Unit,
    onToggleWatchlist: (String, String, String) -> Unit,
    onSearchScripMaster: (suspend (String) -> List<StockSearchResult>)? = null,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var scripMasterResults by remember { mutableStateOf<List<StockSearchResult>>(emptyList()) }
    var isSearchingScripMaster by remember { mutableStateOf(false) }

    val watchlistSymbols = remember(watchlist) { watchlist.map { it.symbol }.toSet() }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && onSearchScripMaster != null) {
            delay(300)
            isSearchingScripMaster = true
            try {
                scripMasterResults = onSearchScripMaster(searchQuery)
            } catch (_: Exception) {
                scripMasterResults = emptyList()
            } finally {
                isSearchingScripMaster = false
            }
        } else {
            scripMasterResults = emptyList()
            isSearchingScripMaster = false
        }
    }

    val filteredSymbols = remember(symbols, searchQuery, selectedFilter) {
        val base = if (searchQuery.isBlank()) symbols else {
            symbols.filter {
                it.symbol.contains(searchQuery, ignoreCase = true) ||
                it.companyName.contains(searchQuery, ignoreCase = true) ||
                it.token.contains(searchQuery)
            }
        }
        when (selectedFilter) {
            "INDICES" -> base.filter { it.symbol in listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "INDIA VIX") }
            "GAINERS" -> base.filter { it.change > 0 }.sortedByDescending { it.changePercent }
            "LOSERS" -> base.filter { it.change < 0 }.sortedBy { it.changePercent }
            "HIGH VOL" -> base.sortedByDescending { it.volume }
            else -> base
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDarkNavy)
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Title Row
        Text(
            text = "NSE / BSE LIVE MARKETS",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            letterSpacing = 0.5.sp
        )
        Text(
            text = "Select any stock or index to launch real-time AI structure analysis",
            fontSize = 11.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search stocks, indices, e.g. RELIANCE, NIFTY...", fontSize = 12.sp, color = TextTertiary) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyanAccent) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
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
                .testTag("stock_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Tags Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val filters = listOf("ALL", "INDICES", "GAINERS", "LOSERS", "HIGH VOL")
            items(filters) { f ->
                val isSelected = f == selectedFilter
                Surface(
                    onClick = { selectedFilter = f },
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

        Spacer(modifier = Modifier.height(10.dp))

        // Stock List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredSymbols, key = { it.symbol }) { stock ->
                val isStarred = watchlistSymbols.contains(stock.symbol)
                val isPos = stock.change >= 0

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("market_row_${stock.symbol}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Symbol & Company
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = stock.symbol,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Surface(shape = RoundedCornerShape(3.dp), color = BgCardElevated) {
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
                            // Day High / Low range indicator
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
                                        .width(60.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (isPos) BullishGreen else BearishRed,
                                    trackColor = BgCardBorder
                                )
                                Text(text = "H: ₹${stock.dayHigh}", fontSize = 9.sp, color = TextTertiary)
                            }
                        }

                        // Right: Price + Analyze Button + Watchlist Star
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${String.format(java.util.Locale.US, "%.2f", stock.ltp)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${if (isPos) "+" else ""}${String.format(java.util.Locale.US, "%.2f", stock.changePercent)}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isPos) BullishGreen else BearishRed
                                )
                            }

                            // Star Button for Watchlist
                            IconButton(
                                onClick = { onToggleWatchlist(stock.symbol, stock.companyName, stock.token) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Watchlist",
                                    tint = if (isStarred) KeyLevelYellow else TextTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Analyze Action Button
                            Button(
                                onClick = { onStockSelected(stock.symbol) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(text = "Analyze", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Scrip Master Live Search Results
            if (searchQuery.isNotBlank()) {
                val existingSymbols = filteredSymbols.map { it.symbol }.toSet()
                val additionalScrips = scripMasterResults.filter {
                    val clean = it.symbol.removeSuffix("-EQ")
                    !existingSymbols.contains(clean) && !existingSymbols.contains(it.symbol)
                }

                if (isSearchingScripMaster) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = CyanAccent,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Searching Angel One Scrip Master...", fontSize = 11.sp, color = TextTertiary)
                        }
                    }
                } else if (additionalScrips.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ANGEL ONE SCRIP MASTER RESULTS (${additionalScrips.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }
                    }

                    items(additionalScrips, key = { "scrip_${it.token}_${it.symbol}" }) { scrip ->
                        val cleanSym = scrip.symbol.removeSuffix("-EQ")
                        val isStarred = watchlistSymbols.contains(cleanSym) || watchlistSymbols.contains(scrip.symbol)

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = BgCardElevated),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = scrip.symbol,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Surface(shape = RoundedCornerShape(3.dp), color = BgDarkNavy) {
                                            Text(
                                                text = "${scrip.exchange} • Token ${scrip.token}",
                                                fontSize = 8.sp,
                                                color = CyanAccent,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = scrip.name,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { onToggleWatchlist(cleanSym, scrip.name, scrip.token) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Watchlist",
                                            tint = if (isStarred) KeyLevelYellow else TextTertiary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Button(
                                        onClick = { onStockSelected(cleanSym) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.White),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(text = "Analyze", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
