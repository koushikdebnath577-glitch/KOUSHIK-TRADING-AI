package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AlertEntity
import com.example.data.local.AppDatabase
import com.example.data.local.SavedPlanEntity
import com.example.data.local.UserPreferences
import com.example.data.local.WatchlistEntity
import com.example.data.model.*
import com.example.data.remote.BackendConfig
import com.example.data.repository.TradingRepository
import com.example.engine.TechnicalIndicators
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TradingUiState(
    val selectedSymbol: String = "NIFTY 50",
    val selectedStock: StockSymbol? = null,
    val selectedTimeframe: Timeframe = Timeframe.MIN_1,
    val selectedStrategy: StrategyType = StrategyType.RESISTANCE_REJECTION,
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTING,
    val candles: List<Candle> = emptyList(),
    val keyLevels: List<KeyLevel> = emptyList(),
    val analysisResult: AnalysisResult? = null,
    val ema20: List<Double?> = emptyList(),
    val ema50: List<Double?> = emptyList(),
    val vwap: List<Double?> = emptyList(),
    val rsi: List<Double?> = emptyList(),
    val indicatorSettings: TradingRepository.IndicatorSettings = TradingRepository.IndicatorSettings(),
    val searchQuery: String = "",
    val activeTab: Int = 2, // 0: Home, 1: Markets, 2: Analyze, 3: Watchlist, 4: Settings
    val defaultRiskAmount: Double = UserPreferences.DEFAULT_RISK_AMOUNT,
    val defaultTargetRR: Double = UserPreferences.DEFAULT_TARGET_RR,
    val userSavedNotes: String = "",
    val isPlanSavedSnackbarShown: Boolean = false,
    val activeBannerDismissed: Boolean = false,
    val indices: List<IndexItem> = emptyList(),
    val selectedIndex: IndexItem? = null,
    val indexConstituents: List<StockSymbol> = emptyList(),
    val indicesLoadingState: LoadingState = LoadingState.Idle,
    val constituentsLoadingState: LoadingState = LoadingState.Idle,
    val selectedIndexCategory: String = "All",
    val isIndexDetailVisible: Boolean = false
)

class TradingViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)
    private val db = AppDatabase.getDatabase(application)
    private val repository = TradingRepository(db.appDao())

    private val _uiState = MutableStateFlow(
        TradingUiState(
            defaultRiskAmount = userPreferences.getIntradayRisk(),
            defaultTargetRR = userPreferences.getTargetRR()
        )
    )
    val uiState: StateFlow<TradingUiState> = _uiState.asStateFlow()

    val marketSymbols: StateFlow<List<StockSymbol>> = repository.marketSymbols
    val watchlist: StateFlow<List<WatchlistEntity>> = repository.watchlist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val alerts: StateFlow<List<AlertEntity>> = repository.alerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val savedPlans: StateFlow<List<SavedPlanEntity>> = repository.savedPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Observe and update UI state reactively
        viewModelScope.launch {
            repository.selectedSymbol.collect { sym ->
                _uiState.update { it.copy(selectedSymbol = sym) }
            }
        }
        viewModelScope.launch {
            repository.selectedTimeframe.collect { tf ->
                _uiState.update { it.copy(selectedTimeframe = tf) }
            }
        }
        viewModelScope.launch {
            repository.selectedStrategy.collect { strat ->
                _uiState.update { it.copy(selectedStrategy = strat) }
            }
        }
        viewModelScope.launch {
            repository.connectionStatus.collect { conn ->
                _uiState.update { it.copy(connectionStatus = conn) }
            }
        }
        viewModelScope.launch {
            repository.keyLevels.collect { levels ->
                _uiState.update { it.copy(keyLevels = levels) }
            }
        }
        viewModelScope.launch {
            repository.analysisResult.collect { result ->
                _uiState.update { it.copy(analysisResult = result) }
            }
        }
        viewModelScope.launch {
            combine(
                repository.selectedSymbol,
                repository.marketSymbols,
                repository.indices
            ) { currentSym, symbols, indicesList ->
                val stockFromMarket = symbols.find { it.symbol.equals(currentSym, ignoreCase = true) }
                val stock = if (stockFromMarket != null && stockFromMarket.ltp > 0.0) {
                    stockFromMarket
                } else {
                    val matchingIndex = indicesList.find {
                        it.symbol.equals(currentSym, ignoreCase = true) ||
                        (it.alias != null && it.alias.equals(currentSym, ignoreCase = true))
                    }
                    if (matchingIndex != null) {
                        StockSymbol(
                            symbol = matchingIndex.symbol,
                            name = matchingIndex.name,
                            token = matchingIndex.token,
                            exchange = matchingIndex.exchange,
                            ltp = matchingIndex.ltp,
                            change = matchingIndex.change,
                            changePercent = matchingIndex.changePercent,
                            open = matchingIndex.prevClose,
                            high = matchingIndex.high,
                            low = matchingIndex.low,
                            close = matchingIndex.ltp,
                            volume = 0L,
                            previousClose = matchingIndex.prevClose
                        )
                    } else {
                        stockFromMarket
                    }
                }
                _uiState.update { it.copy(selectedStock = stock) }
            }.collect()
        }
        viewModelScope.launch {
            combine(
                repository.activeCandles,
                repository.indicatorToggles
            ) { candles, ind ->
                val ema20 = if (ind.showEma20) TechnicalIndicators.calculateEMA(candles, 20) else emptyList()
                val ema50 = if (ind.showEma50) TechnicalIndicators.calculateEMA(candles, 50) else emptyList()
                val vwap = if (ind.showVwap) TechnicalIndicators.calculateVWAP(candles) else emptyList()
                val rsi = if (ind.showRsi) TechnicalIndicators.calculateRSI(candles, 14) else emptyList()

                _uiState.update { current ->
                    current.copy(
                        candles = candles,
                        indicatorSettings = ind,
                        ema20 = ema20,
                        ema50 = ema50,
                        vwap = vwap,
                        rsi = rsi
                    )
                }
            }.collect()
        }
        viewModelScope.launch {
            repository.indices.collect { indicesList ->
                _uiState.update { current ->
                    current.copy(
                        indices = indicesList,
                        selectedIndex = if (current.selectedIndex == null && indicesList.isNotEmpty()) {
                            indicesList.first()
                        } else {
                            indicesList.find { it.id == current.selectedIndex?.id || it.symbol == current.selectedIndex?.symbol } ?: current.selectedIndex
                        }
                    )
                }
            }
        }
        viewModelScope.launch {
            refreshIndices()
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(activeTab = tabIndex) }
    }

    fun selectIndexCategory(category: String) {
        _uiState.update { it.copy(selectedIndexCategory = category) }
    }

    fun openIndexDetail(index: IndexItem) {
        _uiState.update { 
            it.copy(
                selectedIndex = index,
                isIndexDetailVisible = true,
                constituentsLoadingState = LoadingState.Loading
            ) 
        }
        loadConstituentsForIndex(index)
    }

    fun closeIndexDetail() {
        _uiState.update { it.copy(isIndexDetailVisible = false) }
    }

    fun selectIndex(index: IndexItem) {
        _uiState.update { it.copy(selectedIndex = index) }
        loadConstituentsForIndex(index)
    }

    fun refreshIndices() {
        viewModelScope.launch {
            _uiState.update { it.copy(indicesLoadingState = LoadingState.Loading) }
            try {
                val list = repository.refreshIndices()
                _uiState.update { 
                    it.copy(
                        indices = list,
                        indicesLoadingState = LoadingState.Success
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        indicesLoadingState = LoadingState.Error(e.message ?: "Failed to load indices")
                    ) 
                }
            }
        }
    }

    fun loadConstituentsForIndex(index: IndexItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(constituentsLoadingState = LoadingState.Loading) }
            try {
                val constituents = repository.fetchIndexConstituents(index.id.ifEmpty { index.symbol })
                _uiState.update { 
                    it.copy(
                        indexConstituents = constituents,
                        constituentsLoadingState = LoadingState.Success
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        constituentsLoadingState = LoadingState.Error(e.message ?: "Failed to load constituents")
                    ) 
                }
            }
        }
    }

    fun selectStock(symbol: String) {
        repository.selectSymbol(symbol)
        _uiState.update { it.copy(selectedSymbol = symbol) }
    }

    fun selectTimeframe(timeframe: Timeframe) {
        repository.selectTimeframe(timeframe)
        _uiState.update { it.copy(selectedTimeframe = timeframe) }
    }

    fun selectStrategy(strategy: StrategyType) {
        repository.selectStrategy(strategy)
        _uiState.update { it.copy(selectedStrategy = strategy) }
    }

    fun toggleIndicator(name: String) {
        repository.toggleIndicator(name)
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    suspend fun searchStocks(query: String): List<com.example.data.model.StockSearchResult> {
        return repository.searchStocks(query)
    }

    fun toggleWatchlist(symbol: String, name: String, token: String) {
        viewModelScope.launch {
            repository.toggleWatchlist(symbol, name, token)
        }
    }

    fun saveCurrentPlan(notes: String = "") {
        viewModelScope.launch {
            val analysis = _uiState.value.analysisResult ?: return@launch
            repository.saveTradePlan(analysis, notes)
            _uiState.update { it.copy(isPlanSavedSnackbarShown = true) }
        }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(isPlanSavedSnackbarShown = false) }
    }

    fun deleteSavedPlan(planId: String) {
        viewModelScope.launch {
            repository.deleteSavedPlan(planId)
        }
    }

    fun createAlert(symbol: String, title: String, message: String, price: Double) {
        viewModelScope.launch {
            val alert = AlertEntity(
                id = "alert_${System.currentTimeMillis()}",
                timestamp = System.currentTimeMillis(),
                symbol = symbol,
                title = title,
                message = message,
                triggerPrice = price,
                isEnabled = true
            )
            repository.addAlert(alert)
        }
    }

    fun toggleAlert(alertId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAlert(alertId, isEnabled)
        }
    }

    fun deleteAlert(alertId: String) {
        viewModelScope.launch {
            repository.deleteAlert(alertId)
        }
    }

    fun reconnect() {
        repository.reconnectFeed()
    }

    fun updateRiskSettings(risk: Double, targetRR: Double) {
        if (risk > 0.0) {
            userPreferences.saveIntradayRisk(risk)
        }
        if (targetRR > 0.0) {
            userPreferences.saveTargetRR(targetRR)
        }
        _uiState.update { it.copy(defaultRiskAmount = if (risk > 0.0) risk else it.defaultRiskAmount, defaultTargetRR = if (targetRR > 0.0) targetRR else it.defaultTargetRR) }
    }

    fun updateRiskAmount(risk: Double) {
        if (risk > 0.0) {
            userPreferences.saveIntradayRisk(risk)
            _uiState.update { it.copy(defaultRiskAmount = risk) }
        }
    }

    fun updateTargetRR(targetRR: Double) {
        if (targetRR > 0.0) {
            userPreferences.saveTargetRR(targetRR)
            _uiState.update { it.copy(defaultTargetRR = targetRR) }
        }
    }

    fun updateBackendConfig(config: BackendConfig) {
        repository.updateBackendConfig(config)
    }
}
