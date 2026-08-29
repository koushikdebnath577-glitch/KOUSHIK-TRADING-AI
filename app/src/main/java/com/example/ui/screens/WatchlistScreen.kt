package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.local.AlertEntity
import com.example.data.local.SavedPlanEntity
import com.example.data.local.WatchlistEntity
import com.example.data.model.StockSymbol
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WatchlistScreen(
    watchlist: List<WatchlistEntity>,
    marketSymbols: List<StockSymbol>,
    savedPlans: List<SavedPlanEntity>,
    alerts: List<AlertEntity>,
    onSelectStock: (String) -> Unit,
    onRemoveFromWatchlist: (String) -> Unit,
    onDeleteSavedPlan: (String) -> Unit,
    onToggleAlert: (String, Boolean) -> Unit,
    onDeleteAlert: (String) -> Unit,
    onCreateAlert: (String, String, String, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Watchlist, 1: Saved Plans, 2: Active Alerts
    var showCreateAlertDialog by remember { mutableStateOf(false) }

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
            Column {
                Text(
                    text = "WATCHLIST & TRADE PLANS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Persisted local database storage for monitored setups",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            if (selectedTab == 2) {
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
                onSelectStock = onSelectStock,
                onRemove = onRemoveFromWatchlist
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
    onSelectStock: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    if (watchlist.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No stocks in watchlist. Star stocks in Markets to add.", color = TextTertiary, fontSize = 12.sp)
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        items(watchlist, key = { it.symbol }) { item ->
            val liveStock = marketSymbols.find { it.symbol == item.symbol }
            val ltp = liveStock?.ltp ?: 0.0
            val change = liveStock?.change ?: 0.0
            val changePercent = liveStock?.changePercent ?: 0.0
            val isPos = change >= 0

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
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = item.symbol, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Surface(shape = RoundedCornerShape(3.dp), color = BgCardElevated) {
                                Text(text = item.exchange, fontSize = 8.sp, color = CyanAccent, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        Text(text = item.companyName, fontSize = 11.sp, color = TextTertiary, maxLines = 1)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", ltp)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = TextPrimary
                            )
                            Text(
                                text = "${if (isPos) "+" else ""}${String.format(Locale.US, "%.2f", change)} (${if (isPos) "+" else ""}${String.format(Locale.US, "%.2f", changePercent)}%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isPos) BullishGreen else BearishRed
                            )
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = plan.symbol, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Surface(shape = RoundedCornerShape(4.dp), color = if (plan.direction == "SHORT") BearishRedBg else BullishGreenBg) {
                                Text(
                                    text = "${plan.direction} • ${plan.strategyName}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (plan.direction == "SHORT") BearishRed else BullishGreen,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeletePlan(plan.id) },
                            modifier = Modifier.size(28.dp)
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Entry", fontSize = 9.sp, color = TextTertiary)
                            Text(text = "₹${plan.entryPrice}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                        }
                        Column {
                            Text(text = "Stop Loss", fontSize = 9.sp, color = TextTertiary)
                            Text(text = "₹${plan.stopLoss}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BearishRed)
                        }
                        Column {
                            Text(text = "Target 1", fontSize = 9.sp, color = TextTertiary)
                            Text(text = "₹${plan.target1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BullishGreen)
                        }
                        Column {
                            Text(text = "Score", fontSize = 9.sp, color = TextTertiary)
                            Text(text = "${plan.score}/100", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KeyLevelYellow)
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
                            color = TextTertiary
                        )
                        Button(
                            onClick = { onSelectStock(plan.symbol) },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(text = "Load in Chart", fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
