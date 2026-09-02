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

        // Fetch latest live indices from backend
        scope.launch {
            try {
                refreshIndices()
            } catch (e: Exception) {
                // Handled gracefully
            }
        }

        // Collect incoming live ticks
        scope.launch {
            smartApiClient.tickFlow.collect { tick ->
                handleLiveTick(tick)
            }
        }

        // Synchronize _indices whenever marketSymbols get updated (from REST quote or WebSocket)
        scope.launch {
            smartApiClient.marketSymbols.collect { symbols ->
                val currentIndices = _indices.value.toMutableList()
                var updated = false
                symbols.forEach { sym ->
                    val idx = currentIndices.indexOfFirst {
                        it.token == sym.token ||
                        it.symbol.equals(sym.symbol, ignoreCase = true) ||
                        (it.alias != null && it.alias.equals(sym.symbol, ignoreCase = true))
                    }
                    if (idx >= 0) {
                        val old = currentIndices[idx]
                        if (sym.ltp > 0.0 || sym.previousClose > 0.0) {
                            currentIndices[idx] = old.copy(
                                ltp = sym.ltp,
                                change = sym.change,
                                changePercent = sym.changePercent,
                                high = if (sym.high > 0.0) sym.high else sym.ltp,
                                low = if (sym.low > 0.0) sym.low else sym.ltp,
                                prevClose = if (sym.previousClose > 0.0) sym.previousClose else old.prevClose
                            )
                            updated = true
                            android.util.Log.i("HOME_DISPLAY", "[HOME_DISPLAY] Updated Index: ${old.symbol} (Token: ${old.token}) LTP: ${sym.ltp} PrevClose: ${sym.previousClose}")
                        }
                    }
                }
                if (updated) {
                    _indices.value = currentIndices
                }
            }
        }

        // Recompute analysis whenever connection status changes (e.g. from LIVE to DISCONNECTED/AWAITING)
        scope.launch {
            smartApiClient.connectionStatus.collect {
                recomputeAnalysis()
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

        if (baseCandles.isNotEmpty()) {
            aggregator = CandleAggregator(tf, baseCandles)
            _activeCandles.value = aggregator?.candles ?: baseCandles

            val stock = marketSymbols.value.find { it.symbol == symbol }
            val ltp = stock?.ltp ?: baseCandles.last().close
            val prevClose = stock?.previousClose ?: ltp
            _keyLevels.value = KeyLevelDetector.detectKeyLevels(baseCandles, ltp, prevClose)
            recomputeAnalysis()
        } else {
            // Find token and exchange
            val stock = marketSymbols.value.find { it.symbol == symbol }
            val token = stock?.token ?: when (symbol) {
                "NIFTY 50" -> "99926000"
                "BANKNIFTY", "NIFTY BANK" -> "99926009"
                "FINNIFTY", "NIFTY FINANCIAL SERVICES" -> "99926037"
                "RELIANCE" -> "2885"
                "HDFCBANK" -> "1333"
                "TCS" -> "11536"
                "INFY" -> "1594"
                "ICICIBANK" -> "4963"
                "TATAMOTORS" -> "3456"
                "SBIN" -> "3045"
                "ITC" -> "1660"
                "BHARTIARTL" -> "10604"
                "LT" -> "11483"
                else -> "99926000"
            }
            val exch = stock?.exchange ?: "NSE"

            scope.launch {
                val fetchedCandles = smartApiClient.fetchCandles(token, exch, tf)
                if (fetchedCandles.isNotEmpty()) {
                    aggregator = CandleAggregator(tf, fetchedCandles)
                    _activeCandles.value = aggregator?.candles ?: fetchedCandles

                    val currentStock = marketSymbols.value.find { it.symbol == symbol }
                    val currentLtp = currentStock?.ltp ?: fetchedCandles.last().close
                    val prevClose = currentStock?.previousClose ?: currentLtp
                    _keyLevels.value = KeyLevelDetector.detectKeyLevels(fetchedCandles, currentLtp, prevClose)
                    recomputeAnalysis()
                } else {
                    _activeCandles.value = emptyList()
                    recomputeAnalysis()
                }
            }
        }
    }

    private fun handleLiveTick(tick: LiveTick) {
        // 1. Update index state if this tick belongs to an index
        val currentIndices = _indices.value.toMutableList()
        val indexIdx = currentIndices.indexOfFirst {
            it.token == tick.token ||
            it.symbol.equals(tick.symbol, ignoreCase = true) ||
            (it.alias != null && it.alias.equals(tick.symbol, ignoreCase = true)) ||
            (tick.token == "99926000" && it.symbol == "NIFTY 50") ||
            (tick.token == "99926009" && it.symbol in listOf("BANKNIFTY", "NIFTY BANK")) ||
            (tick.token == "99926037" && it.symbol in listOf("FINNIFTY", "NIFTY FINANCIAL SERVICES"))
        }
        if (indexIdx >= 0) {
            val oldIdx = currentIndices[indexIdx]
            val prevClose = if (oldIdx.prevClose > 0) oldIdx.prevClose else tick.ltp
            val change = tick.ltp - prevClose
            val changePercent = if (prevClose > 0) (change / prevClose) * 100.0 else 0.0
            val high = if (oldIdx.high > 0) kotlin.math.max(oldIdx.high, tick.ltp) else tick.ltp
            val low = if (oldIdx.low > 0) kotlin.math.min(oldIdx.low, tick.ltp) else tick.ltp

            currentIndices[indexIdx] = oldIdx.copy(
                ltp = tick.ltp,
                change = kotlin.math.round(change * 100.0) / 100.0,
                changePercent = kotlin.math.round(changePercent * 100.0) / 100.0,
                high = high,
                low = low,
                prevClose = prevClose
            )
            _indices.value = currentIndices
            android.util.Log.i("DATA_AUDIT", "[DATA AUDIT: REPOSITORY UPDATED INDEX] Symbol: ${oldIdx.symbol} | Token: ${oldIdx.token} | Live LTP: ${tick.ltp}")
        }

        // 2. Check if this tick matches the currently selected chart symbol
        val isSelected = tick.symbol.equals(_selectedSymbol.value, ignoreCase = true) ||
            (tick.token == "99926000" && (_selectedSymbol.value == "NIFTY 50" || _selectedSymbol.value == "NIFTY")) ||
            (tick.token == "99926009" && _selectedSymbol.value in listOf("BANKNIFTY", "NIFTY BANK")) ||
            (tick.token == "99926037" && _selectedSymbol.value in listOf("FINNIFTY", "NIFTY FINANCIAL SERVICES")) ||
            marketSymbols.value.find { it.token == tick.token }?.symbol.equals(_selectedSymbol.value, ignoreCase = true)

        if (isSelected) {
            var agg = aggregator
            if (agg == null) {
                val initialCandles = if (_activeCandles.value.isNotEmpty()) {
                    _activeCandles.value
                } else {
                    listOf(
                        Candle(
                            timestamp = tick.timestamp,
                            open = tick.ltp,
                            high = tick.ltp,
                            low = tick.ltp,
                            close = tick.ltp,
                            volume = tick.volume,
                            isComplete = false
                        )
                    )
                }
                agg = CandleAggregator(_selectedTimeframe.value, initialCandles)
                aggregator = agg
            }

            val updatedCandle = agg.processTick(tick)
            _activeCandles.value = agg.candles
            android.util.Log.i("CHART_DATA", "[CHART_DATA] Tick updated chart for ${_selectedSymbol.value} (Token: ${tick.token}) - LTP: ${tick.ltp} Total Candles: ${agg.candles.size}")

            // Refresh key levels and analysis
            val stock = marketSymbols.value.find { it.symbol == _selectedSymbol.value }
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
        val isLive = smartApiClient.connectionStatus.value == ConnectionStatus.LIVE

        val result = TradingAnalysisEngine.performFullAnalysis(
            symbol = symbol,
            currentPrice = ltp,
            previousClose = prevClose,
            candles = candles,
            strategyType = _selectedStrategy.value,
            isLiveFeed = isLive
        )
        _analysisResult.value = result

        android.util.Log.i(
            "ANALYZE_DISPLAY",
            "[ANALYZE_DISPLAY] Symbol: $symbol | Strategy: ${_selectedStrategy.value.displayName} | Price: $ltp | PrevClose: $prevClose | CandlesCount: ${candles.size} | LiveFeed: $isLive | Score: ${result.setupScore} | Direction: ${result.direction.label} | Status: ${result.strategyStatus}"
        )
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
