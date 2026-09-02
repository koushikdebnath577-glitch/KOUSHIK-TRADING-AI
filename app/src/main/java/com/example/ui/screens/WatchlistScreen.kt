package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AlertEntity
import com.example.data.local.SavedPlanEntity
import com.example.data.local.WatchlistEntity
import com.example.data.model.ConnectionStatus
import com.example.data.model.StockSearchResult
import com.example.data.model.StockSymbol
import com.example.ui.components.formatMarketTimestamp
import com.example.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WatchlistScreen(
    watchlist: List<WatchlistEntity>,
    marketSymbols: List<StockSymbol>,
    connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTING,
    savedPlans: List<SavedPlanEntity>,
    alerts: List<AlertEntity>,
    onSelectStock: (String) -> Unit,
    onRemoveFromWatchlist: (String) -> Unit,
    onAddToWatchlist: (String, String, String) -> Unit = { _, _, _ -> },
    onSearchScripMaster: suspend (String) -> List<StockSearchResult> = { emptyList() },
    onDeleteSavedPlan: (String) -> Unit,
    onToggleAlert: (String, Boolean) -> Unit,
    onDeleteAlert: (String) -> Unit,
    onCreateAlert: (String, String, String, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Watchlist, 1: Saved Plans, 2: Active Alerts
    var showCreateAlertDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDarkNavy)
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 6.dp)
            ) {
                Text(
                    text = "WATCHLIST & TRADE PLANS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp,
                    maxLines = 1
                )
                Text(
                    text = "Persisted local database storage for monitored setups",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 14.sp
                )
            }

            if (selectedTab == 0) {
                Button(
                    onClick = { showSearchDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp).wrapContentWidth().testTag("add_stock_watchlist_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Stock", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Add Stock", fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
                }
            } else if (selectedTab == 2) {
                IconButton(
                    onClick = { showCreateAlertDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Add Alert", tint = CyanAccent)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BgCard)
                .border(1.dp, BgCardBorder, RoundedCornerShape(12.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabButton(title = "Watchlist (${watchlist.size})", isSelected = selectedTab == 0) { selectedTab = 0 }
            TabButton(title = "Saved Plans (${savedPlans.size})", isSelected = selectedTab == 1) { selectedTab = 1 }
            TabButton(title = "Alerts (${alerts.size})", isSelected = selectedTab == 2) { selectedTab = 2 }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> WatchlistContent(
                watchlist = watchlist,
                marketSymbols = marketSymbols,
                connectionStatus = connectionStatus,
                onSelectStock = onSelectStock,
                onRemove = onRemoveFromWatchlist,
                onOpenSearch = { showSearchDialog = true }
            )
            1 -> SavedPlansContent(
                plans = savedPlans,
                onSelectStock = onSelectStock,
                onDeletePlan = onDeleteSavedPlan
            )
            2 -> AlertsContent(
                alerts = alerts,
                onToggleAlert = onToggleAlert,
                onDeleteAlert = onDeleteAlert
            )
        }
    }

    if (showSearchDialog) {
        SearchScripMasterDialog(
            watchlist = watchlist,
            onDismiss = { showSearchDialog = false },
            onSearch = onSearchScripMaster,
            onAddToWatchlist = { symbol, name, token ->
                onAddToWatchlist(symbol, name, token)
            },
            onAnalyzeStock = { symbol ->
                showSearchDialog = false
                onSelectStock(symbol)
            }
        )
    }

    if (showCreateAlertDialog) {
        CreateAlertDialog(
            onDismiss = { showCreateAlertDialog = false },
            onCreate = { symbol, title, msg, price ->
                onCreateAlert(symbol, title, msg, price)
                showCreateAlertDialog = false
            }
        )
    }
}

@Composable
private fun RowScope.TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) BgPillInactive else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)) else null,
        modifier = Modifier
            .weight(1f)
            .height(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TextPrimary else TextTertiary
            )
        }
    }
}

@Composable
private fun WatchlistContent(
    watchlist: List<WatchlistEntity>,
    marketSymbols: List<StockSymbol>,
    connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTING,
    onSelectStock: (String) -> Unit,
    onRemove: (String) -> Unit,
    onOpenSearch: () -> Unit
) {
    val isLive = connectionStatus == ConnectionStatus.LIVE
    if (watchlist.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = CyanAccent.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "No stocks in watchlist yet",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Search official Angel One Scrip Master to add NSE Equities",
                color = TextTertiary,
                fontSize = 12.sp
            )
            Button(
                onClick = onOpenSearch,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("empty_watchlist_search_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Search Angel One Scrip Master", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BgCardElevated)
                    .clickable { onOpenSearch() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Search Angel One Scrip Master (e.g. HDFC, RELIANCE, TATA...)",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = CyanAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "+ ADD",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        items(watchlist, key = { it.symbol }) { item ->
            val liveStock = marketSymbols.find { it.symbol == item.symbol }
            val ltp = liveStock?.ltp ?: 0.0
            val prevClose = liveStock?.previousClose ?: 0.0
            val change = liveStock?.change ?: 0.0
            val changePercent = liveStock?.changePercent ?: 0.0
            val isPos = change >= 0
            val displayPrice = when {
                ltp > 0.0 -> ltp
                prevClose > 0.0 -> prevClose
                else -> 0.0
            }
            val priceLabel = when {
                isLive && ltp > 0.0 -> "LIVE"
                ltp > 0.0 -> "LAST AVAILABLE"
                prevClose > 0.0 -> "PREV CLOSE"
                else -> "UNAVAILABLE"
            }
            val formattedTime = liveStock?.let { formatMarketTimestamp(it.lastUpdated) } ?: ""

            Card(
                onClick = { onSelectStock(item.symbol) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("watchlist_card_${item.symbol}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = item.symbol, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                            Surface(shape = RoundedCornerShape(3.dp), color = BgCardElevated) {
                                Text(
                                    text = item.exchange,
                                    fontSize = 8.sp,
                                    color = CyanAccent,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            text = item.companyName,
                            fontSize = 11.sp,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            if (displayPrice > 0.0) {
                                Text(
                                    text = "₹${String.format(Locale.US, "%,.2f", displayPrice)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = if (isLive) (if (isPos) BullishGreen else BearishRed) else TextPrimary,
                                    maxLines = 1
                                )
                                if (isLive) {
                                    Text(
                                        text = "${if (isPos) "+" else ""}${String.format(Locale.US, "%.2f", change)} (${if (isPos) "+" else ""}${String.format(Locale.US, "%.2f", changePercent)}%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isPos) BullishGreen else BearishRed,
                                        maxLines = 1
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = "$priceLabel • NOT LIVE",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = KeyLevelYellow,
                                            maxLines = 1
                                        )
                                        if (formattedTime.isNotEmpty()) {
                                            Text(
                                                text = "• $formattedTime",
                                                fontSize = 8.sp,
                                                color = TextTertiary,
                                                maxLines = 1
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
                                    maxLines = 1
                                )
                                Text(
                                    text = "LIVE DATA UNAVAILABLE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KeyLevelYellow,
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(
                            onClick = { onRemove(item.symbol) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = BearishRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedPlansContent(
    plans: List<SavedPlanEntity>,
    onSelectStock: (String) -> Unit,
    onDeletePlan: (String) -> Unit
) {
    if (plans.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No saved trade plans yet. Tap 'Save Plan' in Analyze screen.", color = TextTertiary, fontSize = 12.sp)
        }
        return
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        items(plans, key = { it.id }) { plan ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        ) {
                            Text(text = plan.symbol, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                            Surface(shape = RoundedCornerShape(4.dp), color = if (plan.direction == "SHORT") BearishRedBg else BullishGreenBg) {
                                Text(
                                    text = "${plan.direction} • ${plan.strategyName}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (plan.direction == "SHORT") BearishRed else BullishGreen,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeletePlan(plan.id) },
                            modifier = Modifier.size(28.dp).wrapContentWidth()
                        ) {
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextTertiary)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BgCardElevated)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Entry", fontSize = 9.sp, color = TextTertiary, maxLines = 1)
                            Text(text = "₹${plan.entryPrice}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent, maxLines = 1)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Stop Loss", fontSize = 9.sp, color = TextTertiary, maxLines = 1)
                            Text(text = "₹${plan.stopLoss}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BearishRed, maxLines = 1)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Target 1", fontSize = 9.sp, color = TextTertiary, maxLines = 1)
                            Text(text = "₹${plan.target1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BullishGreen, maxLines = 1)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Score", fontSize = 9.sp, color = TextTertiary, maxLines = 1)
                            Text(text = "${plan.score}/100", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KeyLevelYellow, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Saved: ${dateFormat.format(Date(plan.timestamp))}",
                            fontSize = 9.sp,
                            color = TextTertiary,
                            modifier = Modifier.weight(1f).padding(end = 6.dp),
                            maxLines = 1
                        )
                        Button(
                            onClick = { onSelectStock(plan.symbol) },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp).wrapContentWidth()
                        ) {
                            Text(text = "Load in Chart", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertsContent(
    alerts: List<AlertEntity>,
    onToggleAlert: (String, Boolean) -> Unit,
    onDeleteAlert: (String) -> Unit
) {
    if (alerts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No alerts configured. Tap '+' above to create an alert.", color = TextTertiary, fontSize = 12.sp)
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        items(alerts, key = { it.id }) { alert ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
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
                            Text(text = alert.symbol, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "• ₹${alert.triggerPrice}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KeyLevelYellow)
                        }
                        Text(text = alert.title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                        Text(text = alert.message, fontSize = 10.sp, color = TextTertiary)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Switch(
                            checked = alert.isEnabled,
                            onCheckedChange = { onToggleAlert(alert.id, it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = CyanAccentBg)
                        )
                        IconButton(
                            onClick = { onDeleteAlert(alert.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextTertiary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateAlertDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Double) -> Unit
) {
    var symbol by remember { mutableStateOf("NIFTY 50") }
    var title by remember { mutableStateOf("Resistance Rejection Trigger") }
    var priceText by remember { mutableStateOf("24500.00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Create Price & Level Alert", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Stock / Index Symbol") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Alert Title") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Trigger Price (₹)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    onCreate(symbol, title, "Price reached trigger level of ₹$p", p)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
            ) {
                Text("Set Alert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextTertiary) }
        },
        containerColor = BgCardElevated
    )
}

@Composable
private fun SearchScripMasterDialog(
    watchlist: List<WatchlistEntity>,
    onDismiss: () -> Unit,
    onSearch: suspend (String) -> List<StockSearchResult>,
    onAddToWatchlist: (String, String, String) -> Unit,
    onAnalyzeStock: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<StockSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val quickQueries = listOf("HDFC", "RELIANCE", "TATA", "INFY", "SBIN", "ITC")

    LaunchedEffect(Unit) {
        isSearching = true
        searchResults = onSearch("")
        isSearching = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ANGEL ONE SCRIP MASTER",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Search NSE Equities & Tradable Instruments",
                        fontSize = 10.sp,
                        color = CyanAccent
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextTertiary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { newQuery ->
                        searchQuery = newQuery
                        searchJob?.cancel()
                        searchJob = scope.launch {
                            delay(250) // Debounce typing
                            isSearching = true
                            searchResults = onSearch(newQuery)
                            isSearching = false
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Search symbol / stock name...",
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                scope.launch {
                                    isSearching = true
                                    searchResults = onSearch("")
                                    isSearching = false
                                }
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextTertiary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scrip_master_search_input"),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            searchJob?.cancel()
                            searchJob = scope.launch {
                                isSearching = true
                                searchResults = onSearch(searchQuery)
                                isSearching = false
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BgCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = BgDarkNavy,
                        unfocusedContainerColor = BgDarkNavy
                    ),
                    singleLine = true
                )

                // Quick Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickQueries.forEach { chipText ->
                        Surface(
                            onClick = {
                                searchQuery = chipText
                                scope.launch {
                                    isSearching = true
                                    searchResults = onSearch(chipText)
                                    isSearching = false
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (searchQuery.equals(chipText, ignoreCase = true)) CyanAccent else BgCard,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (searchQuery.equals(chipText, ignoreCase = true)) CyanAccent else BgCardBorder
                            )
                        ) {
                            Text(
                                text = chipText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (searchQuery.equals(chipText, ignoreCase = true)) Color.Black else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = CyanAccent,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                } else if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "No scrips found for \"$searchQuery\"", color = TextSecondary, fontSize = 12.sp)
                            Text(text = "Try searching by official NSE ticker (e.g. RELIANCE, SBIN)", color = TextTertiary, fontSize = 10.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(searchResults, key = { "${it.token}_${it.symbol}" }) { scrip ->
                            val cleanSym = scrip.symbol.removeSuffix("-EQ")
                            val isAdded = watchlist.any { it.symbol == cleanSym || it.symbol == scrip.symbol }

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = BgDarkNavy),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = scrip.symbol,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = BgCardElevated
                                            ) {
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

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            onClick = {
                                                onAddToWatchlist(cleanSym, scrip.name, scrip.token)
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isAdded) BgCardElevated else CyanAccent.copy(alpha = 0.18f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isAdded) BgCardBorder else CyanAccent.copy(alpha = 0.4f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isAdded) Icons.Default.Check else Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = if (isAdded) BullishGreen else CyanAccent,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = if (isAdded) "Saved" else "Watchlist",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAdded) BullishGreen else CyanAccent
                                                )
                                            }
                                        }

                                        Surface(
                                            onClick = {
                                                onAnalyzeStock(cleanSym)
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            color = CyanAccent
                                        ) {
                                            Text(
                                                text = "Analyze",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
        containerColor = BgCardElevated
    )
}
