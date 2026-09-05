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
    val selectedSession: TradingSession = TradingSession.TODAY,
    val selectedStrategy: StrategyType = StrategyType.RESISTANCE_REJECTION,
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTING,
    val diagnosticMessage: String? = null,
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
            repository.marketSymbols.collect { symbols ->
                if (symbols.isEmpty()) return@collect
                val symbolByToken = symbols.associateBy { it.token }
                val symbolByName = symbols.associateBy { it.symbol.uppercase().removeSuffix("-EQ") }
                _uiState.update { state ->
                    if (state.indexConstituents.isEmpty()) return@update state
                    val updatedConstituents = state.indexConstituents.map { stock ->
                        val cleanSym = stock.symbol.uppercase().removeSuffix("-EQ")
                        val live = symbolByToken[stock.token] ?: symbolByName[cleanSym]
                        if (live != null && (live.ltp > 0.0 || live.previousClose > 0.0)) {
                            stock.copy(
                                ltp = if (live.ltp > 0.0) live.ltp else stock.ltp,
                                previousClose = if (live.previousClose > 0.0) live.previousClose else stock.previousClose,
                                change = if (live.change != 0.0) live.change else (if (live.ltp > 0.0 && stock.previousClose > 0.0) live.ltp - stock.previousClose else stock.change),
                                changePercent = if (live.changePercent != 0.0) live.changePercent else stock.changePercent,
                                open = if (live.open > 0.0) live.open else stock.open,
                                high = if (live.high > 0.0) maxOf(stock.high, live.high) else stock.high,
                                low = if (live.low > 0.0 && stock.low > 0.0) minOf(stock.low, live.low) else (if (live.low > 0.0) live.low else stock.low),
                                close = if (live.close > 0.0) live.close else stock.close,
                                volume = if (live.volume > 0L) live.volume else stock.volume,
                                lastUpdated = live.lastUpdated ?: stock.lastUpdated ?: System.currentTimeMillis()
                            )
                        } else {
                            stock
                        }
                    }
                    state.copy(indexConstituents = updatedConstituents)
                }
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
            repository.selectedSession.collect { sess ->
                _uiState.update { it.copy(selectedSession = sess) }
            }
        }
        viewModelScope.launch {
            repository.tokenResolutionDiagnostic.collect { diag ->
                _uiState.update { it.copy(diagnosticMessage = diag) }
            }
        }
        viewModelScope.launch {
            combine(
                repository.selectedSymbol,
                repository.marketSymbols,
                repository.indices
            ) { currentSym, symbols, indicesList ->
                val clean = currentSym.trim().uppercase().removeSuffix("-EQ")
                val stockFromMarket = symbols.find {
                    it.symbol.equals(currentSym, ignoreCase = true) ||
                    it.symbol.equals(clean, ignoreCase = true) ||
                    it.name.equals(currentSym, ignoreCase = true)
                }
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
        val initialConstituents = com.example.data.repository.IndicesDataProvider.getConstituentsForIndex(index.id.ifEmpty { index.symbol })
        _uiState.update { 
            it.copy(
                selectedIndex = index,
                isIndexDetailVisible = true,
                indexConstituents = if (initialConstituents.isNotEmpty()) initialConstituents else it.indexConstituents,
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

                // Batch REST Price Fetching: Trigger REST quote fetch for all stock tokens immediately
                val tokens = constituents.map { it.token }.filter { it.isNotBlank() }
                if (tokens.isNotEmpty()) {
                    fetchBatchQuotes(tokens)
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

    fun fetchBatchQuotes(tokens: List<String>) {
        val cleanTokens = tokens.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (cleanTokens.isEmpty()) return
        viewModelScope.launch {
            try {
                val batchQuotes = repository.fetchQuotesBatch(cleanTokens)
                if (batchQuotes.isNotEmpty()) {
                    val quoteByToken = batchQuotes.associateBy { it.token }
                    val quoteBySym = batchQuotes.associateBy { it.symbol.uppercase().removeSuffix("-EQ") }
                    _uiState.update { state ->
                        val updated = state.indexConstituents.map { stock ->
                            val cleanSym = stock.symbol.uppercase().removeSuffix("-EQ")
                            val quote = quoteByToken[stock.token] ?: quoteBySym[cleanSym]
                            if (quote != null && (quote.ltp > 0.0 || quote.previousClose > 0.0)) {
                                stock.copy(
                                    ltp = if (quote.ltp > 0.0) quote.ltp else stock.ltp,
                                    previousClose = if (quote.previousClose > 0.0) quote.previousClose else stock.previousClose,
                                    change = quote.change,
                                    changePercent = quote.changePercent,
                                    open = if (quote.open > 0.0) quote.open else stock.open,
                                    high = if (quote.high > 0.0) quote.high else stock.high,
                                    low = if (quote.low > 0.0) quote.low else stock.low,
                                    close = if (quote.close > 0.0) quote.close else stock.close,
                                    volume = if (quote.volume > 0L) quote.volume else stock.volume,
                                    lastUpdated = quote.lastUpdated ?: System.currentTimeMillis()
                                )
                            } else {
                                stock
                            }
                        }
                        state.copy(indexConstituents = updated)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("TradingViewModel", "fetchBatchQuotes error: ${e.message}")
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

    fun selectTradingSession(session: TradingSession) {
        repository.selectTradingSession(session)
        _uiState.update { it.copy(selectedSession = session) }
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
