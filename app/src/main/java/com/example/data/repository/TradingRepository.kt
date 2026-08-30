package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.BackendConfig
import com.example.data.remote.LiveTick
import com.example.data.remote.SmartApiBackendClient
import com.example.engine.CandleAggregator
import com.example.engine.KeyLevelDetector
import com.example.engine.TechnicalIndicators
import com.example.engine.TradingAnalysisEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class TradingRepository(
    private val appDao: AppDao,
    private val smartApiClient: SmartApiBackendClient = SmartApiBackendClient()
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val connectionStatus: StateFlow<ConnectionStatus> = smartApiClient.connectionStatus
    val marketSymbols: StateFlow<List<StockSymbol>> = smartApiClient.marketSymbols
    val watchlist: Flow<List<WatchlistEntity>> = appDao.getAllWatchlist()
    val alerts: Flow<List<AlertEntity>> = appDao.getAllAlerts()
    val savedPlans: Flow<List<SavedPlanEntity>> = appDao.getAllSavedPlans()

    private val _indices = MutableStateFlow<List<IndexItem>>(IndicesDataProvider.DEFAULT_INDICES)
    val indices: StateFlow<List<IndexItem>> = _indices.asStateFlow()

    private val _selectedSymbol = MutableStateFlow("NIFTY 50")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow(Timeframe.MIN_1)
    val selectedTimeframe: StateFlow<Timeframe> = _selectedTimeframe.asStateFlow()

    private val _selectedStrategy = MutableStateFlow(StrategyType.RESISTANCE_REJECTION)
    val selectedStrategy: StateFlow<StrategyType> = _selectedStrategy.asStateFlow()

    private val _activeCandles = MutableStateFlow<List<Candle>>(emptyList())
    val activeCandles: StateFlow<List<Candle>> = _activeCandles.asStateFlow()

    private val _analysisResult = MutableStateFlow<AnalysisResult?>(null)
    val analysisResult: StateFlow<AnalysisResult?> = _analysisResult.asStateFlow()

    private val _keyLevels = MutableStateFlow<List<KeyLevel>>(emptyList())
    val keyLevels: StateFlow<List<KeyLevel>> = _keyLevels.asStateFlow()

    private val _indicatorToggles = MutableStateFlow(
        IndicatorSettings(
            showEma20 = true,
            showEma50 = true,
            showVwap = true,
            showRsi = true,
            showKeyLevels = true,
            showAnalysisMarkers = true
        )
    )
    val indicatorToggles: StateFlow<IndicatorSettings> = _indicatorToggles.asStateFlow()

    private var aggregator: CandleAggregator? = null

    init {
        // Initial setup for default symbol
        loadCandlesForSelected()

        // Collect incoming live ticks
        scope.launch {
            smartApiClient.tickFlow.collect { tick ->
                handleLiveTick(tick)
            }
        }

        // Initialize default watchlist items if empty
        scope.launch {
            val currentList = appDao.getAllWatchlist().first()
            if (currentList.isEmpty()) {
                val defaults = listOf(
                    WatchlistEntity("NIFTY 50", "NIFTY 50 INDEX", "99926000", "NSE"),
                    WatchlistEntity("BANKNIFTY", "NIFTY BANK INDEX", "99926009", "NSE"),
                    WatchlistEntity("RELIANCE", "Reliance Industries Ltd", "2885", "NSE"),
                    WatchlistEntity("HDFCBANK", "HDFC Bank Ltd", "1333", "NSE"),
                    WatchlistEntity("TCS", "Tata Consultancy Services", "11536", "NSE")
                )
                defaults.forEach { appDao.insertWatchlist(it) }
            }
        }
    }

    data class IndicatorSettings(
        val showEma20: Boolean = true,
        val showEma50: Boolean = true,
        val showVwap: Boolean = true,
        val showRsi: Boolean = true,
        val showKeyLevels: Boolean = true,
        val showAnalysisMarkers: Boolean = true
    )

    fun selectSymbol(symbol: String) {
        if (_selectedSymbol.value == symbol) return
        _selectedSymbol.value = symbol
        loadCandlesForSelected()
    }

    fun selectTimeframe(timeframe: Timeframe) {
        if (_selectedTimeframe.value == timeframe) return
        _selectedTimeframe.value = timeframe
        loadCandlesForSelected()
    }

    fun selectStrategy(strategy: StrategyType) {
        _selectedStrategy.value = strategy
        recomputeAnalysis()
    }

    fun toggleIndicator(indicator: String) {
        val current = _indicatorToggles.value
        _indicatorToggles.value = when (indicator) {
            "EMA20" -> current.copy(showEma20 = !current.showEma20)
            "EMA50" -> current.copy(showEma50 = !current.showEma50)
            "VWAP" -> current.copy(showVwap = !current.showVwap)
            "RSI" -> current.copy(showRsi = !current.showRsi)
            "LEVELS" -> current.copy(showKeyLevels = !current.showKeyLevels)
            "MARKERS" -> current.copy(showAnalysisMarkers = !current.showAnalysisMarkers)
            else -> current
        }
    }

    private fun loadCandlesForSelected() {
        val symbol = _selectedSymbol.value
        val tf = _selectedTimeframe.value
        val baseCandles = smartApiClient.getHistoricalCandles(symbol, tf)
        aggregator = CandleAggregator(tf, baseCandles)
        _activeCandles.value = aggregator?.candles ?: baseCandles
        recomputeAnalysis()
    }

    private fun handleLiveTick(tick: LiveTick) {
        if (tick.symbol == _selectedSymbol.value) {
            val agg = aggregator ?: return
            val updatedCandle = agg.processTick(tick)
            _activeCandles.value = agg.candles

            // Refresh key levels and analysis
            val stock = marketSymbols.value.find { it.symbol == tick.symbol }
            val prevClose = stock?.previousClose ?: tick.ltp
            _keyLevels.value = KeyLevelDetector.detectKeyLevels(agg.candles, tick.ltp, prevClose)

            recomputeAnalysis()
        }
    }

    private fun recomputeAnalysis() {
        val symbol = _selectedSymbol.value
        val candles = _activeCandles.value
        if (candles.isEmpty()) return

        val stock = marketSymbols.value.find { it.symbol == symbol }
        val ltp = stock?.ltp ?: candles.last().close
        val prevClose = stock?.previousClose ?: ltp

        val result = TradingAnalysisEngine.performFullAnalysis(
            symbol = symbol,
            currentPrice = ltp,
            previousClose = prevClose,
            candles = candles,
            strategyType = _selectedStrategy.value
        )
        _analysisResult.value = result
    }

    fun isSymbolInWatchlist(symbol: String): Flow<Boolean> = appDao.isInWatchlist(symbol)

    suspend fun toggleWatchlist(symbol: String, name: String, token: String) {
        val exists = appDao.isInWatchlist(symbol).first()
        if (exists) {
            appDao.deleteWatchlistBySymbol(symbol)
        } else {
            appDao.insertWatchlist(WatchlistEntity(symbol, name, token, "NSE"))
        }
    }

    suspend fun saveTradePlan(plan: AnalysisResult, notes: String = "") {
        val entry = plan.conservativeEntry ?: plan.normalEntry ?: plan.aggressiveEntry
        val entity = SavedPlanEntity(
            id = "plan_${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            symbol = plan.symbol,
            strategyName = plan.strategy.displayName,
            direction = plan.direction.label,
            score = plan.setupScore,
            entryPrice = entry?.entryPrice ?: plan.currentPrice,
            stopLoss = entry?.stopLoss ?: plan.stopLossSuggestions.structureStopLoss,
            target1 = entry?.target1 ?: 0.0,
            target2 = entry?.target2 ?: 0.0,
            riskReward = entry?.riskRewardRatio ?: 1.5,
            notes = notes
        )
        appDao.insertSavedPlan(entity)
    }

    suspend fun deleteSavedPlan(planId: String) {
        appDao.deleteSavedPlan(planId)
    }

    suspend fun addAlert(alert: AlertEntity) {
        appDao.insertAlert(alert)
    }

    suspend fun toggleAlert(alertId: String, isEnabled: Boolean) {
        appDao.setAlertEnabled(alertId, isEnabled)
    }

    suspend fun deleteAlert(alertId: String) {
        appDao.deleteAlert(alertId)
    }

    fun reconnectFeed() {
        smartApiClient.reconnect()
    }

    fun updateBackendConfig(config: BackendConfig) {
        smartApiClient.updateConfig(config)
    }

    suspend fun searchStocks(query: String): List<com.example.data.model.StockSearchResult> {
        return smartApiClient.searchStocks(query)
    }

    suspend fun refreshIndices(): List<IndexItem> {
        val fetched = smartApiClient.fetchIndices()
        _indices.value = fetched
        return fetched
    }

    suspend fun fetchIndexConstituents(indexIdOrSymbol: String): List<StockSymbol> {
        return smartApiClient.fetchIndexConstituents(indexIdOrSymbol)
    }
}
