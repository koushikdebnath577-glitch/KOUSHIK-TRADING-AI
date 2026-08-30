package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.StrategyType
import com.example.ui.components.EditRiskDialog
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TradingViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TradingAppRoot()
            }
        }
    }
}

@Composable
fun TradingAppRoot(
    viewModel: TradingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val marketSymbols by viewModel.marketSymbols.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val savedPlans by viewModel.savedPlans.collectAsState()

    var showEditRiskDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showEditRiskDialog) {
        EditRiskDialog(
            currentRisk = uiState.defaultRiskAmount,
            onDismiss = { showEditRiskDialog = false },
            onSaveRisk = { newRisk ->
                viewModel.updateRiskAmount(newRisk)
                scope.launch {
                    val formatted = if (newRisk % 1.0 == 0.0) newRisk.toInt().toString() else String.format(java.util.Locale.US, "%.2f", newRisk)
                    snackbarHostState.showSnackbar(
                        message = "Intraday Risk updated to ₹$formatted per trade",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }

    LaunchedEffect(uiState.isPlanSavedSnackbarShown) {
        if (uiState.isPlanSavedSnackbarShown) {
            snackbarHostState.showSnackbar(
                message = "Trade plan for ${uiState.selectedSymbol} successfully saved!",
                duration = SnackbarDuration.Short
            )
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDarkNavy),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                color = BgCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                NavigationBar(
                    containerColor = BgCard,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .testTag("bottom_navigation_bar")
                        .fillMaxWidth()
                ) {
                    val tabs = listOf(
                        NavigationTabItem("Home", Icons.Filled.Home, Icons.Outlined.Home, 0),
                        NavigationTabItem("Markets", Icons.Filled.BarChart, Icons.Outlined.BarChart, 1),
                        NavigationTabItem("Analyze", Icons.Filled.ShowChart, Icons.Outlined.ShowChart, 2),
                        NavigationTabItem("Watchlist", Icons.Filled.Star, Icons.Outlined.StarBorder, 3),
                        NavigationTabItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, 4)
                    )

                    tabs.forEach { item ->
                        val isSelected = uiState.activeTab == item.index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(item.index) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.title.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    letterSpacing = 0.5.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyanAccent,
                                selectedTextColor = CyanAccent,
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary,
                                indicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.testTag("nav_tab_${item.title.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgDarkNavy)
        ) {
            when (uiState.activeTab) {
                0 -> HomeScreen(
                    symbols = marketSymbols,
                    defaultRiskAmount = uiState.defaultRiskAmount,
                    onEditRisk = { showEditRiskDialog = true },
                    onSelectStockAndAnalyze = { symbol, strategy ->
                        viewModel.selectStock(symbol)
                        viewModel.selectStrategy(strategy)
                        viewModel.selectTab(2)
                    },
                    onNavigateToMarkets = { viewModel.selectTab(1) },
                    onNavigateToWatchlist = { viewModel.selectTab(3) }
                )
                1 -> MarketsScreen(
                    symbols = marketSymbols,
                    watchlist = watchlist,
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    onStockSelected = { symbol ->
                        viewModel.selectStock(symbol)
                        viewModel.selectTab(2)
                    },
                    onToggleWatchlist = { symbol, name, token ->
                        viewModel.toggleWatchlist(symbol, name, token)
                    }
                )
                2 -> AnalyzeScreen(
                    selectedSymbol = uiState.selectedSymbol,
                    stock = uiState.selectedStock,
                    connectionStatus = uiState.connectionStatus,
                    candles = uiState.candles,
                    keyLevels = uiState.keyLevels,
                    analysisResult = uiState.analysisResult,
                    ema20 = uiState.ema20,
                    ema50 = uiState.ema50,
                    vwap = uiState.vwap,
                    rsi = uiState.rsi,
                    indicatorSettings = uiState.indicatorSettings,
                    selectedTimeframe = uiState.selectedTimeframe,
                    selectedStrategy = uiState.selectedStrategy,
                    defaultRiskAmount = uiState.defaultRiskAmount,
                    onTimeframeSelected = { viewModel.selectTimeframe(it) },
                    onStrategySelected = { viewModel.selectStrategy(it) },
                    onToggleIndicator = { viewModel.toggleIndicator(it) },
                    onEditRisk = { showEditRiskDialog = true },
                    onSavePlan = { viewModel.saveCurrentPlan() },
                    onReconnect = { viewModel.reconnect() }
                )
                3 -> WatchlistScreen(
                    watchlist = watchlist,
                    marketSymbols = marketSymbols,
                    savedPlans = savedPlans,
                    alerts = alerts,
                    onSelectStock = { symbol ->
                        viewModel.selectStock(symbol)
                        viewModel.selectTab(2)
                    },
                    onRemoveFromWatchlist = { symbol ->
                        viewModel.toggleWatchlist(symbol, "", "")
                    },
                    onDeleteSavedPlan = { planId ->
                        viewModel.deleteSavedPlan(planId)
                    },
                    onToggleAlert = { id, enabled ->
                        viewModel.toggleAlert(id, enabled)
                    },
                    onDeleteAlert = { id ->
                        viewModel.deleteAlert(id)
                    },
                    onCreateAlert = { symbol, title, msg, price ->
                        viewModel.createAlert(symbol, title, msg, price)
                    }
                )
                4 -> SettingsScreen(
                    defaultRisk = uiState.defaultRiskAmount,
                    defaultTargetRR = uiState.defaultTargetRR,
                    onUpdateRisk = { risk, rr -> viewModel.updateRiskSettings(risk, rr) },
                    onUpdateBackendConfig = { config -> viewModel.updateBackendConfig(config) }
                )
            }
        }
    }
}

private data class NavigationTabItem(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val index: Int
)
