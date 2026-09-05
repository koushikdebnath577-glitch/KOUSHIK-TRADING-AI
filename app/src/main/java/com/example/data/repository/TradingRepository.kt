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

    private val _selectedSession = MutableStateFlow(TradingSession.TODAY)
    val selectedSession: StateFlow<TradingSession> = _selectedSession.asStateFlow()

    private val _selectedToken = MutableStateFlow("")
    val selectedToken: StateFlow<String> = _selectedToken.asStateFlow()

    private val _tokenResolutionDiagnostic = MutableStateFlow<String?>(null)
    val tokenResolutionDiagnostic: StateFlow<String?> = _tokenResolutionDiagnostic.asStateFlow()

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
    private val lastTickTimes = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private val localKnownTokens = mapOf(
        "NIFTY" to "99926000",
        "NIFTY 50" to "99926000",
        "NIFTY50" to "99926000",
        "BANKNIFTY" to "99926009",
        "NIFTY BANK" to "99926009",
        "NIFTYBANK" to "99926009",
        "FINNIFTY" to "99926037",
        "NIFTY FINANCIAL SERVICES" to "99926037",
        "NIFTY FIN SERVICE" to "99926037",
        "MIDCPNIFTY" to "99926074",
        "NIFTY MIDCAP SELECT" to "99926074",
        "NIFTY NEXT 50" to "99926013",
        "NIFTYNXT50" to "99926013",
        "NIFTY IT" to "99926008",
        "NIFTY AUTO" to "99926002",
        "NIFTY PHARMA" to "99926023",
        "NIFTY FMCG" to "99926021",
        "NIFTY METAL" to "99926030",
        "NIFTY REALTY" to "99926018",
        "NIFTY MEDIA" to "99926031",
        "NIFTY ENERGY" to "99926020",
        "NIFTY INFRA" to "99926019",
        "NIFTY HEALTHCARE" to "99926038",
        "NIFTY PSU BANK" to "99926025",
        "NIFTY PRIVATE BANK" to "99926047",
        "NIFTY CONSUMER DURABLES" to "99926040",
        "NIFTY MIDCAP 50" to "99926014",
        "NIFTY MIDCAP 100" to "99926011",
        "NIFTY SMALLCAP 100" to "99926032",
        "NIFTY 100" to "99926012",
        "NIFTY 200" to "99926033",
        "NIFTY 500" to "99926004",
        // Equities
        "BEL" to "383",
        "RELIANCE" to "2885",
        "HDFCBANK" to "1333",
        "TCS" to "11536",
        "HINDUNILVR" to "1394",
        "HUL" to "1394",
        "INFY" to "1594",
        "ICICIBANK" to "4963",
        "TATAMOTORS" to "3456",
        "SBIN" to "3045",
        "ITC" to "1660",
        "BHARTIARTL" to "10604",
        "LT" to "11483",
        "KOTAKBANK" to "1922",
        "AXISBANK" to "5900",
        "BAJFINANCE" to "317",
        "MARUTI" to "10999",
        "SUNPHARMA" to "3351",
        "TITAN" to "3506",
        "WIPRO" to "3787",
        "ASIANPAINT" to "236",
        "HCLTECH" to "7229",
        "ADANIENT" to "25",
        "ADANIPORTS" to "15083",
        "POWERGRID" to "14977",
        "NTPC" to "11630",
        "ULTRACEMCO" to "11532"
    )

    init {
        // Initial setup for default symbol
        val (initToken, initExchange) = resolveInstrumentInfo(_selectedSymbol.value)
        android.util.Log.i("SELECTED_STOCK", "[SELECTED_STOCK] symbol: ${_selectedSymbol.value}")
        android.util.Log.i("TOKEN_RESOLVED", "[TOKEN_RESOLVED] symbol: ${_selectedSymbol.value} | token: $initToken | exchange: $initExchange")
        smartApiClient.registerScrip(_selectedSymbol.value, initToken, initExchange)
        smartApiClient.subscribeToken(initToken)
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

    fun selectTradingSession(session: TradingSession) {
        if (_selectedSession.value == session) return
        _selectedSession.value = session
        loadCandlesForSelected()
    }

    private fun isIndexToken(token: String): Boolean {
        return token.startsWith("99926")
    }

    fun selectSymbol(symbol: String) {
        val trimmed = symbol.trim()
        val prevToken = _selectedToken.value
        _selectedSymbol.value = trimmed
        _tokenResolutionDiagnostic.value = null
        android.util.Log.i("SELECTED_STOCK", "[SELECTED_STOCK] symbol: $trimmed")

        scope.launch {
            val (token, exchange) = resolveInstrumentInfoAsync(trimmed)
            _selectedToken.value = token
            android.util.Log.i("TOKEN_RESOLVED", "[TOKEN_RESOLVED] symbol: $trimmed | token: $token | exchange: $exchange")

            if (token.isNotEmpty()) {
                smartApiClient.registerScrip(trimmed, token, exchange)
                // Unsubscribe previous selected stock token if different and not an index
                if (prevToken.isNotEmpty() && prevToken != token && !isIndexToken(prevToken)) {
                    smartApiClient.unsubscribeToken(prevToken)
                }
                smartApiClient.subscribeToken(token)
                _tokenResolutionDiagnostic.value = null
            } else {
                _tokenResolutionDiagnostic.value = "Token unresolvable for '$trimmed' in Angel One Scrip Master"
                android.util.Log.w("TOKEN_RESOLVED", "[TOKEN_UNRESOLVED] Failed to resolve token for: $trimmed in Angel One Scrip Master")
            }
            loadCandlesForSelected()

            // REST fallback: if WebSocket ticks are unavailable, immediately fetch selected stock quote
            launch {
                delay(1200)
                val lastTick = lastTickTimes[token] ?: 0L
                val isRecentTick = (System.currentTimeMillis() - lastTick) < 2500L
                if (!isRecentTick && token.isNotEmpty()) {
                    val quote = smartApiClient.fetchQuote(token, exchange)
                    if (quote != null && quote.ltp > 0.0) {
                        val agg = aggregator
                        if (_selectedSession.value == TradingSession.TODAY) {
                            val updatedCandle = agg?.injectCurrentSessionLtp(quote.ltp, quote.volume)
                            if (updatedCandle != null) {
                                _activeCandles.value = agg.candles
                                android.util.Log.i(
                                    "CHART_CANDLE_UPDATE",
                                    "[CHART_CANDLE_UPDATE] symbol: $trimmed | token: $token | candleClose: ${updatedCandle.close} | totalCandles: ${agg.candles.size} | isComplete: false | source=REST_QUOTE_FALLBACK"
                                )
                                val prevClose = quote.previousClose.takeIf { it > 0.0 } ?: quote.ltp
                                _keyLevels.value = KeyLevelDetector.detectKeyLevels(agg.candles, quote.ltp, prevClose)
                                recomputeAnalysis()
                            }
                        }
                    }
                }
            }
        }
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

    private fun resolveInstrumentInfo(symbol: String): Pair<String, String> {
        val trimmed = symbol.trim()
        val clean = trimmed.uppercase().removeSuffix("-EQ")

        // 1. Registered scrip cache in client
        val registered = smartApiClient.getRegisteredScrip(trimmed)
        if (registered != null && registered.first.isNotBlank()) {
            return registered
        }

        // 2. Search in stocks list
        val stock = marketSymbols.value.find {
            it.symbol.equals(trimmed, ignoreCase = true) ||
            it.symbol.equals(clean, ignoreCase = true) ||
            it.name.equals(trimmed, ignoreCase = true)
        }
        if (stock != null && stock.token.isNotBlank()) {
            return Pair(stock.token, stock.exchange.ifEmpty { "NSE" })
        }

        // 3. Search in live indices list
        val liveIndex = _indices.value.find {
            it.symbol.equals(trimmed, ignoreCase = true) ||
            (it.alias != null && it.alias.equals(trimmed, ignoreCase = true)) ||
            it.name.equals(trimmed, ignoreCase = true) ||
            it.id.equals(trimmed, ignoreCase = true)
        }
        if (liveIndex != null && liveIndex.token.isNotBlank()) {
            return Pair(liveIndex.token, liveIndex.exchange.ifEmpty { "NSE" })
        }

        // 4. Search in default master indices list
        val defaultIndex = IndicesDataProvider.DEFAULT_INDICES.find {
            it.symbol.equals(trimmed, ignoreCase = true) ||
            (it.alias != null && it.alias.equals(trimmed, ignoreCase = true)) ||
            it.name.equals(trimmed, ignoreCase = true) ||
            it.id.equals(trimmed, ignoreCase = true)
        }
        if (defaultIndex != null && defaultIndex.token.isNotBlank()) {
            return Pair(defaultIndex.token, defaultIndex.exchange.ifEmpty { "NSE" })
        }

        // 5. Local known tokens mapping
        val known = localKnownTokens[clean] ?: localKnownTokens[trimmed.uppercase()]
        if (!known.isNullOrBlank()) {
            return Pair(known, "NSE")
        }

        // 6. Numeric token check
        if (trimmed.isNotEmpty() && trimmed.all { it.isDigit() }) {
            return Pair(trimmed, "NSE")
        }

        if (clean.startsWith("NIFTY")) {
            return Pair("99926000", "NSE")
        }

        return Pair("", "NSE")
    }

    private suspend fun resolveInstrumentInfoAsync(symbol: String): Pair<String, String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val syncResolved = resolveInstrumentInfo(symbol)
        if (syncResolved.first.isNotBlank()) {
            return@withContext syncResolved
        }

        // Query backend exact scrip master endpoint
        val backendResolved = smartApiClient.resolveScripFromBackend(symbol)
        if (backendResolved != null && backendResolved.first.isNotBlank()) {
            smartApiClient.registerScrip(symbol, backendResolved.first, backendResolved.second)
            return@withContext backendResolved
        }

        if (symbol.trim().uppercase().startsWith("NIFTY")) {
            return@withContext Pair("99926000", "NSE")
        }
        Pair("", "NSE")
    }

    private fun getLtpForInstrument(symbol: String): Double? {
        val stock = marketSymbols.value.find { it.symbol.equals(symbol, ignoreCase = true) }
        if (stock != null && stock.ltp > 0.0) return stock.ltp
        val index = _indices.value.find {
            it.symbol.equals(symbol, ignoreCase = true) ||
            (it.alias != null && it.alias.equals(symbol, ignoreCase = true))
        }
        if (index != null && index.ltp > 0.0) return index.ltp
        return null
    }

    private fun getPrevCloseForInstrument(symbol: String): Double? {
        val stock = marketSymbols.value.find { it.symbol.equals(symbol, ignoreCase = true) }
        if (stock != null && stock.previousClose > 0.0) return stock.previousClose
        val index = _indices.value.find {
            it.symbol.equals(symbol, ignoreCase = true) ||
            (it.alias != null && it.alias.equals(symbol, ignoreCase = true))
        }
        if (index != null && index.prevClose > 0.0) return index.prevClose
        return null
    }

    private fun loadCandlesForSelected() {
        val symbol = _selectedSymbol.value
        val tf = _selectedTimeframe.value
        val session = _selectedSession.value
        val (token, exch) = resolveInstrumentInfo(symbol)

        // Show cached candles immediately if available for fast UI responsiveness (only for current session)
        if (session == TradingSession.TODAY) {
            val cachedCandles = smartApiClient.getHistoricalCandles(symbol, tf)
            if (cachedCandles.isNotEmpty()) {
                val agg = CandleAggregator(tf, cachedCandles)
                val currentLtp = getLtpForInstrument(symbol) ?: cachedCandles.last().close
                val prevClose = getPrevCloseForInstrument(symbol) ?: currentLtp
                if (currentLtp > 0.0) {
                    agg.injectCurrentSessionLtp(currentLtp)
                }
                aggregator = agg
                _activeCandles.value = agg.candles
                _keyLevels.value = KeyLevelDetector.detectKeyLevels(agg.candles, currentLtp, prevClose)
                recomputeAnalysis()
            }
        }

        // Asynchronously fetch fresh, authentic session candles from backend /api/candles
        scope.launch {
            val targetToken = if (token.isNotEmpty()) token else resolveInstrumentInfoAsync(symbol).first
            if (targetToken.isNotEmpty()) {
                val fetchedCandles = smartApiClient.fetchCandles(
                    symbolToken = targetToken,
                    exchange = exch,
                    timeframe = tf,
                    session = session
                )
                if (fetchedCandles.isNotEmpty()) {
                    val agg = CandleAggregator(tf, fetchedCandles)
                    val currentLtp = getLtpForInstrument(symbol) ?: fetchedCandles.last().close
                    val prevClose = getPrevCloseForInstrument(symbol) ?: currentLtp
                    if (currentLtp > 0.0 && session == TradingSession.TODAY) {
                        val injected = agg.injectCurrentSessionLtp(currentLtp)
                        if (injected != null) {
                            android.util.Log.i(
                                "CHART_CANDLE_UPDATE",
                                "[CHART_CANDLE_UPDATE] symbol: $symbol | token: $targetToken | candleClose: ${injected.close} | totalCandles: ${agg.candles.size} | isComplete: false | source=SESSION_INJECT"
                            )
                        }
                    }
                    aggregator = agg
                    _activeCandles.value = agg.candles
                    _keyLevels.value = KeyLevelDetector.detectKeyLevels(agg.candles, currentLtp, prevClose)
                    recomputeAnalysis()
                } else if (session == TradingSession.TODAY) {
                    val cached = smartApiClient.getHistoricalCandles(symbol, tf)
                    if (cached.isEmpty()) {
                        _activeCandles.value = emptyList()
                        recomputeAnalysis()
                    }
                } else {
                    _activeCandles.value = emptyList()
                    recomputeAnalysis()
                }
            }
        }
    }

    private fun handleLiveTick(tick: LiveTick) {
        val receivedAt = System.currentTimeMillis()
        lastTickTimes[tick.token] = receivedAt
        android.util.Log.i("REPOSITORY_LIVE_TICK", "[REPOSITORY_LIVE_TICK] token: ${tick.token} | symbol: ${tick.symbol} | ltp: ${tick.ltp}")

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
        val selectedToken = _selectedToken.value.ifEmpty { resolveInstrumentInfo(_selectedSymbol.value).first }
        val isSelected = (selectedToken.isNotEmpty() && tick.token == selectedToken) ||
            tick.symbol.equals(_selectedSymbol.value, ignoreCase = true) ||
            tick.symbol.equals(_selectedSymbol.value.removeSuffix("-EQ"), ignoreCase = true) ||
            smartApiClient.getRegisteredSymbol(tick.token)?.equals(_selectedSymbol.value, ignoreCase = true) == true ||
            (tick.token == "99926000" && (_selectedSymbol.value == "NIFTY 50" || _selectedSymbol.value == "NIFTY")) ||
            (tick.token == "99926009" && _selectedSymbol.value in listOf("BANKNIFTY", "NIFTY BANK")) ||
            (tick.token == "99926037" && _selectedSymbol.value in listOf("FINNIFTY", "NIFTY FINANCIAL SERVICES")) ||
            (tick.token in listOf("99926002", "99926029") && _selectedSymbol.value == "NIFTY AUTO") ||
            (tick.token == "99926008" && _selectedSymbol.value == "NIFTY IT") ||
            (tick.token == "383" && _selectedSymbol.value.equals("BEL", ignoreCase = true)) ||
            (tick.token == "1394" && (_selectedSymbol.value.equals("HINDUNILVR", ignoreCase = true) || _selectedSymbol.value.equals("HUL", ignoreCase = true))) ||
            marketSymbols.value.find { it.token == tick.token }?.symbol.equals(_selectedSymbol.value, ignoreCase = true) ||
            _indices.value.find { it.token == tick.token }?.let {
                it.symbol.equals(_selectedSymbol.value, ignoreCase = true) ||
                (it.alias != null && it.alias.equals(_selectedSymbol.value, ignoreCase = true))
            } == true

        if (isSelected) {
            // Instantly toggle live state to true, which removes the 'NOT LIVE' badge on the chart!
            smartApiClient.markLive()

            // Only update active chart candles from live ticks if user is viewing today's live session
            if (_selectedSession.value == TradingSession.TODAY) {
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
                android.util.Log.i(
                    "CHART_CANDLE_UPDATE",
                    "[CHART_CANDLE_UPDATE] symbol: ${_selectedSymbol.value} | token: ${tick.token} | candleClose: ${updatedCandle.close} | totalCandles: ${agg.candles.size} | isComplete: ${updatedCandle.isComplete}"
                )

                // Refresh key levels and analysis
                val stock = marketSymbols.value.find { it.symbol.equals(_selectedSymbol.value, ignoreCase = true) }
                val prevClose = stock?.previousClose ?: getPrevCloseForInstrument(_selectedSymbol.value) ?: tick.ltp
                _keyLevels.value = KeyLevelDetector.detectKeyLevels(agg.candles, tick.ltp, prevClose)

                recomputeAnalysis()
            }
        }
    }

    private fun recomputeAnalysis() {
        val symbol = _selectedSymbol.value
        val candles = _activeCandles.value
        if (candles.isEmpty()) return

        val stock = marketSymbols.value.find { it.symbol.equals(symbol, ignoreCase = true) }
        val ltp = stock?.ltp ?: getLtpForInstrument(symbol) ?: candles.last().close
        val prevClose = stock?.previousClose ?: getPrevCloseForInstrument(symbol) ?: ltp
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
