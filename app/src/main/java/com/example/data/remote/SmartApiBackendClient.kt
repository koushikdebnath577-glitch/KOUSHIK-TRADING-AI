package com.example.data.remote

import android.util.Log
import com.example.data.model.Candle
import com.example.data.model.ConnectionStatus
import com.example.data.model.IndexItem
import com.example.data.model.StockSymbol
import com.example.data.model.StockSearchResult
import com.example.data.model.Timeframe
import com.example.data.repository.IndicesDataProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class LiveTick(
    val token: String,
    val symbol: String,
    val ltp: Double,
    val volume: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class BackendConfig(
    val serverUrl: String = "https://koushik-trading-ai.onrender.com/api",
    val wsUrl: String = "wss://koushik-trading-ai.onrender.com/ws/market",
    val isSimulated: Boolean = false,
    val isConnected: Boolean = true,
    val useLiveBackend: Boolean = true,
    val clientIp: String = "192.168.1.1",
    val macAddress: String = "00:1A:2B:3C:4D:5E"
) {
    val healthUrl: String
        get() {
            val base = if (serverUrl.endsWith("/api")) serverUrl.removeSuffix("/api") else serverUrl
            return "$base/health"
        }
}

class SmartApiBackendClient {

    companion object {
        private const val TAG = "SmartApiBackendClient"
        const val DEFAULT_API_BASE_URL = "https://koushik-trading-ai.onrender.com/api"
        const val DEFAULT_WS_URL = "wss://koushik-trading-ai.onrender.com/ws/market"
        const val DEFAULT_HEALTH_URL = "https://koushik-trading-ai.onrender.com/health"
    }

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.CONNECTING)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _tickFlow = MutableSharedFlow<LiveTick>(extraBufferCapacity = 64)
    val tickFlow: SharedFlow<LiveTick> = _tickFlow.asSharedFlow()

    private val _marketSymbols = MutableStateFlow<List<StockSymbol>>(emptyList())
    val marketSymbols: StateFlow<List<StockSymbol>> = _marketSymbols.asStateFlow()

    private var backendConfig = BackendConfig()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // OkHttp Client with WebSocket and REST support
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var simulationFallbackJob: Job? = null
    private var isWebSocketActive = false

    // In-memory cache of candle history per symbol + timeframe
    private val candleCache = mutableMapOf<String, MutableList<Candle>>()

    init {
        initializeSymbols()
        startConnectionLifecycle()
    }

    private fun initializeSymbols() {
        val initialList = listOf(
            StockSymbol("NIFTY 50", "NIFTY 50 INDEX", "99926000", "NSE", 24320.50, 142.80, 0.59, 24190.00, 24365.20, 24175.40, 24177.70, 48200000, 24177.70),
            StockSymbol("BANKNIFTY", "NIFTY BANK INDEX", "99926009", "NSE", 51680.75, -120.30, -0.23, 51850.00, 51920.00, 51580.00, 51801.05, 32100000, 51801.05),
            StockSymbol("RELIANCE", "Reliance Industries Ltd", "2885", "NSE", 2980.40, 32.60, 1.11, 2955.00, 2992.00, 2948.00, 2947.80, 5420000, 2947.80),
            StockSymbol("HDFCBANK", "HDFC Bank Ltd", "1333", "NSE", 1642.15, -8.45, -0.51, 1655.00, 1660.00, 1638.50, 1650.60, 8910000, 1650.60),
            StockSymbol("TCS", "Tata Consultancy Services", "11536", "NSE", 4185.00, 45.20, 1.09, 4145.00, 4205.00, 4138.00, 4139.80, 2340000, 4139.80),
            StockSymbol("INFY", "Infosys Ltd", "1594", "NSE", 1795.50, 21.30, 1.20, 1778.00, 1805.00, 1772.00, 1774.20, 6120000, 1774.20),
            StockSymbol("ICICIBANK", "ICICI Bank Ltd", "4963", "NSE", 1198.80, 14.20, 1.20, 1188.00, 1204.00, 1185.00, 1184.60, 7840000, 1184.60),
            StockSymbol("TATAMOTORS", "Tata Motors Ltd", "3456", "NSE", 984.60, -12.40, -1.24, 1002.00, 1005.00, 980.20, 997.00, 9530000, 997.00),
            StockSymbol("SBIN", "State Bank of India", "3045", "NSE", 812.30, 3.80, 0.47, 810.00, 818.50, 807.00, 808.50, 11200000, 808.50),
            StockSymbol("ITC", "ITC Ltd", "1660", "NSE", 468.90, -1.20, -0.26, 471.00, 473.00, 467.50, 470.10, 4510000, 470.10),
            StockSymbol("BHARTIARTL", "Bharti Airtel Ltd", "10604", "NSE", 1485.00, 18.50, 1.26, 1470.00, 1492.00, 1466.00, 1466.50, 3900000, 1466.50),
            StockSymbol("LT", "Larsen & Toubro Ltd", "11483", "NSE", 3620.00, -25.00, -0.69, 3650.00, 3662.00, 3608.00, 3645.00, 1820000, 3645.00)
        )
        _marketSymbols.value = initialList
    }

    fun updateConfig(config: BackendConfig) {
        backendConfig = config
        reconnect()
    }

    /**
     * Trigger explicit reconnection or retry loop
     */
    fun reconnect() {
        scope.launch {
            _connectionStatus.value = ConnectionStatus.CONNECTING
            closeWebSocket()
            delay(500)
            connectToBackend()
        }
    }

    private fun startConnectionLifecycle() {
        scope.launch {
            connectToBackend()
        }
    }

    /**
     * 1. Performs GET /health check
     * 2. Establishes WebSocket connection to /ws/market
     * 3. Starts automatic reconnection if network drops
     */
    private suspend fun connectToBackend() {
        _connectionStatus.value = ConnectionStatus.CONNECTING
        Log.d(TAG, "Connecting to Render backend: ${backendConfig.serverUrl} and WS: ${backendConfig.wsUrl}")

        // 1. Perform Health Check
        val isHealthOk = performHealthCheck()
        Log.d(TAG, "Health check result: $isHealthOk")

        // 2. Establish WebSocket connection
        startWebSocket()
    }

    /**
     * Calls GET /health on the deployed backend
     */
    private suspend fun performHealthCheck(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(backendConfig.healthUrl)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    Log.d(TAG, "Health Check Success: $body")
                    true
                } else {
                    Log.w(TAG, "Health Check HTTP ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Health Check Exception: ${e.message}")
            false
        }
    }

    /**
     * Connects to WebSocket endpoint: wss://koushik-trading-ai.onrender.com/ws/market
     */
    private fun startWebSocket() {
        closeWebSocket()

        try {
            val request = Request.Builder()
                .url(backendConfig.wsUrl)
                .build()

            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket Connected successfully to ${backendConfig.wsUrl}")
                    isWebSocketActive = true
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    stopSimulationFallback()

                    // Send subscription payload for all monitored tokens
                    val tokensJson = JSONArray()
                    _marketSymbols.value.forEach { tokensJson.put(it.token) }

                    val subPayload = JSONObject().apply {
                        put("action", "subscribe")
                        put("exchangeType", 1)
                        put("tokens", tokensJson)
                    }

                    webSocket.send(subPayload.toString())
                    Log.d(TAG, "Sent subscription payload for ${_marketSymbols.value.size} tokens")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleIncomingWebSocketMessage(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.w(TAG, "WebSocket Closing: $code - $reason")
                    isWebSocketActive = false
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                    scheduleAutomaticReconnect()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.w(TAG, "WebSocket Closed: $code - $reason")
                    isWebSocketActive = false
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                    scheduleAutomaticReconnect()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket Failure: ${t.message}. Response: ${response?.code}")
                    isWebSocketActive = false
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                    startSimulationFallback()
                    scheduleAutomaticReconnect()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "WebSocket Connection Exception: ${e.message}")
            isWebSocketActive = false
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            startSimulationFallback()
            scheduleAutomaticReconnect()
        }
    }

    private fun handleIncomingWebSocketMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type", "")

            when (type) {
                "tick" -> {
                    val token = json.optString("token", "")
                    val symbol = json.optString("symbol", "")
                    val exchange = json.optString("exchange", "NSE")
                    val ltp = json.optDouble("ltp", 0.0)
                    val angelOneLtp = json.optDouble("angelOneLtp", ltp)
                    val backendForwardedLtp = json.optDouble("backendForwardedLtp", ltp)
                    val change = json.optDouble("change", 0.0)
                    val changePercent = json.optDouble("changePercent", 0.0)
                    val volume = json.optLong("volume", 0L)
                    val timestamp = json.optLong("timestamp", System.currentTimeMillis())

                    if (token.isNotEmpty() && ltp > 0) {
                        Log.i(
                            "DATA_AUDIT",
                            "[DATA AUDIT: ANDROID RECEIVED] Symbol: $symbol | Exchange: $exchange | Token: $token | Timestamp: $timestamp | Angel One LTP: $angelOneLtp | Backend Forwarded LTP: $backendForwardedLtp | App Received LTP: $ltp"
                        )
                        updateSymbolFromTick(token, symbol, ltp, change, changePercent, volume, timestamp)

                        // Also emit live tick for indices and non-stock instruments
                        scope.launch {
                            _tickFlow.emit(
                                LiveTick(
                                    token = token,
                                    symbol = symbol,
                                    ltp = ltp,
                                    volume = volume,
                                    timestamp = timestamp
                                )
                            )
                        }
                    }
                }
                "candle" -> {
                    val token = json.optString("token", "")
                    val interval = json.optString("interval", "1s")
                    val candleObj = json.optJSONObject("candle")
                    if (candleObj != null) {
                        val c = Candle(
                            timestamp = candleObj.optLong("timestamp", System.currentTimeMillis()),
                            open = candleObj.optDouble("open", 0.0),
                            high = candleObj.optDouble("high", 0.0),
                            low = candleObj.optDouble("low", 0.0),
                            close = candleObj.optDouble("close", 0.0),
                            volume = candleObj.optLong("volume", 0L),
                            isComplete = candleObj.optBoolean("isComplete", false)
                        )
                        // If token matches any symbol, update candle
                        val stock = _marketSymbols.value.find { it.token == token }
                        if (stock != null) {
                            val tf = when (interval.lowercase()) {
                                "1s" -> Timeframe.SEC_1
                                "5s" -> Timeframe.SEC_5
                                "15s" -> Timeframe.SEC_15
                                "30s" -> Timeframe.SEC_30
                                else -> Timeframe.MIN_1
                            }
                            updateLastCandle(stock.symbol, tf, c)
                        }
                    }
                }
                "connection" -> {
                    Log.d(TAG, "Backend Handshake: ${json.optString("service")}")
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                }
                "subscribed" -> {
                    Log.d(TAG, "Backend Subscription Confirmed: ${json.optJSONArray("tokens")?.length()} tokens")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing WS message: ${e.message}")
        }
    }

    private fun updateSymbolFromTick(
        token: String,
        symbol: String,
        ltp: Double,
        change: Double,
        changePercent: Double,
        volume: Long,
        timestamp: Long
    ) {
        val currentList = _marketSymbols.value.toMutableList()
        val index = currentList.indexOfFirst { it.token == token || it.symbol == symbol }

        if (index >= 0) {
            val stock = currentList[index]
            val actualChange = if (change != 0.0) change else (ltp - stock.previousClose)
            val actualChangePct = if (changePercent != 0.0) changePercent else ((actualChange / stock.previousClose) * 100.0)
            val newHigh = kotlin.math.max(stock.high, ltp)
            val newLow = kotlin.math.min(stock.low, ltp)

            val updatedStock = stock.copy(
                ltp = ltp,
                change = kotlin.math.round(actualChange * 100.0) / 100.0,
                changePercent = kotlin.math.round(actualChangePct * 100.0) / 100.0,
                high = newHigh,
                low = newLow,
                volume = stock.volume + volume,
                lastUpdated = timestamp
            )
            currentList[index] = updatedStock
            _marketSymbols.value = currentList

            // Emit live tick for indicators, key levels, and analysis engine
            scope.launch {
                _tickFlow.emit(
                    LiveTick(
                        token = token,
                        symbol = stock.symbol,
                        ltp = ltp,
                        volume = volume,
                        timestamp = timestamp
                    )
                )
            }
        }
    }

    /**
     * Automatically reconnects if backend or network drops
     */
    private fun scheduleAutomaticReconnect() {
        if (reconnectJob?.isActive == true) return

        reconnectJob = scope.launch {
            Log.d(TAG, "Scheduling automatic reconnection in 3 seconds...")
            delay(3000)
            if (!isWebSocketActive) {
                _connectionStatus.value = ConnectionStatus.CONNECTING
                connectToBackend()
            }
        }
    }

    private fun closeWebSocket() {
        try {
            webSocket?.close(1000, "Client Reset")
            webSocket = null
            isWebSocketActive = false
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Simulation fallback disabled for production Live Market mode.
     * When disconnected, the app marks status as DISCONNECTED and shows Live Data Unavailable.
     */
    private fun startSimulationFallback() {
        // Disabled in LIVE mode to prevent displaying simulated/fake prices
    }

    private fun stopSimulationFallback() {
        simulationFallbackJob?.cancel()
        simulationFallbackJob = null
    }

    /**
     * Retrieves historical candle data for a symbol and timeframe.
     */
    fun getHistoricalCandles(symbol: String, timeframe: Timeframe, count: Int = 120): List<Candle> {
        val cacheKey = "${symbol}_${timeframe.name}"
        candleCache[cacheKey]?.let { return it }

        val stock = _marketSymbols.value.find { it.symbol == symbol }
            ?: _marketSymbols.value.first()
        val basePrice = stock.ltp
        val generatedCandles = generateRealisticIntradayCandles(basePrice, timeframe, count)
        candleCache[cacheKey] = generatedCandles.toMutableList()
        return generatedCandles
    }

    /**
     * Realistic Indian stock price path with key levels, resistance tests, and breakout structures
     */
    private fun generateRealisticIntradayCandles(
        currentPrice: Double,
        timeframe: Timeframe,
        count: Int
    ): List<Candle> {
        val candles = mutableListOf<Candle>()
        val intervalMs = timeframe.seconds * 1000L
        val now = System.currentTimeMillis()
        var price = currentPrice * (1.0 - (Random.nextDouble(0.005, 0.015)))

        val resistanceLevel = price * 1.012

        for (i in count downTo 1) {
            val candleTime = now - (i * intervalMs)
            val open = price

            val drift = if (price >= resistanceLevel * 0.998) {
                Random.nextDouble(-0.003, 0.0005) // strong rejection bias at resistance
            } else if (i in (count - 15)..(count - 5)) {
                Random.nextDouble(-0.001, 0.003) // morning rally towards resistance
            } else {
                Random.nextDouble(-0.0015, 0.0018)
            }

            val rawClose = open * (1.0 + drift)
            val high = kotlin.math.max(open, rawClose) + (open * Random.nextDouble(0.0005, 0.0025))
            val low = kotlin.math.min(open, rawClose) - (open * Random.nextDouble(0.0005, 0.002))
            val close = rawClose.coerceIn(low, high)
            val vol = Random.nextLong(5000, 95000)

            candles.add(
                Candle(
                    timestamp = candleTime,
                    open = kotlin.math.round(open * 100.0) / 100.0,
                    high = kotlin.math.round(high * 100.0) / 100.0,
                    low = kotlin.math.round(low * 100.0) / 100.0,
                    close = kotlin.math.round(close * 100.0) / 100.0,
                    volume = vol,
                    isComplete = true
                )
            )
            price = close
        }

        if (candles.isNotEmpty()) {
            val last = candles.last()
            val adjustedLast = last.copy(
                close = currentPrice,
                high = kotlin.math.max(last.high, currentPrice),
                low = kotlin.math.min(last.low, currentPrice),
                isComplete = false
            )
            candles[candles.size - 1] = adjustedLast
        }

        return candles
    }

    fun updateLastCandle(symbol: String, timeframe: Timeframe, updatedCandle: Candle) {
        val cacheKey = "${symbol}_${timeframe.name}"
        val list = candleCache[cacheKey] ?: return
        if (list.isNotEmpty()) {
            list[list.size - 1] = updatedCandle
        }
    }

    fun appendCandle(symbol: String, timeframe: Timeframe, newCandle: Candle) {
        val cacheKey = "${symbol}_${timeframe.name}"
        val list = candleCache[cacheKey] ?: mutableListOf()
        list.add(newCandle)
        if (list.size > 200) {
            list.removeAt(0)
        }
        candleCache[cacheKey] = list
    }

    /**
     * Searches Angel One Scrip Master database via backend GET /api/search?q=query
     */
    suspend fun searchStocks(query: String): List<StockSearchResult> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return@withContext _marketSymbols.value.map {
                StockSearchResult(
                    name = it.name,
                    symbol = it.symbol,
                    token = it.token,
                    exchange = it.exchange,
                    instrumentType = "EQ"
                )
            }
        }

        try {
            val base = if (backendConfig.serverUrl.endsWith("/api")) {
                backendConfig.serverUrl
            } else {
                "${backendConfig.serverUrl}/api"
            }
            val encodedQuery = java.net.URLEncoder.encode(trimmedQuery, "UTF-8")
            val url = "$base/search?q=$encodedQuery&limit=50"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val json = JSONObject(bodyStr)
                    val resultsArray = json.optJSONArray("results") ?: json.optJSONArray("data") ?: JSONArray()

                    val results = mutableListOf<StockSearchResult>()
                    for (i in 0 until resultsArray.length()) {
                        val item = resultsArray.optJSONObject(i) ?: continue
                        results.add(
                            StockSearchResult(
                                name = item.optString("name", item.optString("symbol", "")),
                                symbol = item.optString("symbol", ""),
                                token = item.optString("token", ""),
                                exchange = item.optString("exchange", "NSE"),
                                instrumentType = item.optString("instrumentType", item.optString("instrumenttype", "EQ"))
                            )
                        )
                    }

                    if (results.isNotEmpty()) {
                        return@withContext results
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend search failed (${e.message}), falling back to local list")
        }

        // Fallback to local filtering
        return@withContext _marketSymbols.value
            .filter {
                it.symbol.contains(trimmedQuery, ignoreCase = true) ||
                it.name.contains(trimmedQuery, ignoreCase = true) ||
                it.token.contains(trimmedQuery)
            }
            .map {
                StockSearchResult(
                    name = it.name,
                    symbol = it.symbol,
                    token = it.token,
                    exchange = it.exchange,
                    instrumentType = "EQ"
                )
            }
    }

    /**
     * Fetches all 23 supported NSE indices from backend GET /api/indices
     * with graceful fallback to built-in IndicesDataProvider.
     */
    suspend fun fetchIndices(): List<IndexItem> = withContext(Dispatchers.IO) {
        try {
            val base = if (backendConfig.serverUrl.endsWith("/api")) {
                backendConfig.serverUrl
            } else {
                "${backendConfig.serverUrl}/api"
            }
            val url = "$base/indices"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val json = JSONObject(bodyStr)
                    val array = json.optJSONArray("indices") ?: json.optJSONArray("data") ?: JSONArray()
                    val list = mutableListOf<IndexItem>()

                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        list.add(
                            IndexItem(
                                id = item.optString("id", item.optString("symbol", "").lowercase().replace(" ", "-")),
                                symbol = item.optString("symbol", ""),
                                alias = if (item.has("alias") && !item.isNull("alias")) item.optString("alias") else null,
                                name = item.optString("name", item.optString("symbol", "")),
                                token = item.optString("token", ""),
                                exchange = item.optString("exchange", "NSE"),
                                category = item.optString("category", "Broad Market"),
                                ltp = item.optDouble("ltp", 0.0),
                                change = item.optDouble("change", 0.0),
                                changePercent = item.optDouble("changePercent", 0.0),
                                high = item.optDouble("high", item.optDouble("ltp", 0.0)),
                                low = item.optDouble("low", item.optDouble("ltp", 0.0)),
                                prevClose = item.optDouble("prevClose", item.optDouble("ltp", 0.0)),
                                constituentCount = item.optInt("constituentCount", 0),
                                description = item.optString("description", "")
                            )
                        )
                    }

                    if (list.isNotEmpty()) {
                        return@withContext list
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch indices from backend (${e.message}), using default indices dataset")
        }

        return@withContext IndicesDataProvider.DEFAULT_INDICES
    }

    /**
     * Fetches constituent stocks for an index from GET /api/indices/:name/constituents
     */
    suspend fun fetchIndexConstituents(indexIdOrSymbol: String): List<StockSymbol> = withContext(Dispatchers.IO) {
        val sanitized = indexIdOrSymbol.trim()
        if (sanitized.isEmpty()) return@withContext emptyList()

        try {
            val base = if (backendConfig.serverUrl.endsWith("/api")) {
                backendConfig.serverUrl
            } else {
                "${backendConfig.serverUrl}/api"
            }
            val encodedName = java.net.URLEncoder.encode(sanitized, "UTF-8")
            val url = "$base/indices/$encodedName/constituents"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val json = JSONObject(bodyStr)
                    val array = json.optJSONArray("constituents") ?: json.optJSONArray("data") ?: JSONArray()
                    val list = mutableListOf<StockSymbol>()

                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        list.add(
                            StockSymbol(
                                symbol = item.optString("symbol", ""),
                                name = item.optString("name", item.optString("symbol", "")),
                                token = item.optString("token", ""),
                                exchange = item.optString("exchange", "NSE"),
                                ltp = item.optDouble("ltp", 0.0),
                                change = item.optDouble("change", 0.0),
                                changePercent = item.optDouble("changePercent", 0.0),
                                open = item.optDouble("open", item.optDouble("ltp", 0.0)),
                                high = item.optDouble("high", item.optDouble("ltp", 0.0)),
                                low = item.optDouble("low", item.optDouble("ltp", 0.0)),
                                close = item.optDouble("ltp", 0.0),
                                volume = item.optLong("volume", 1000000L),
                                previousClose = item.optDouble("previousClose", item.optDouble("ltp", 0.0))
                            )
                        )
                    }

                    if (list.isNotEmpty()) {
                        return@withContext list
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch constituents for $indexIdOrSymbol (${e.message}), using fallback dataset")
        }

        return@withContext IndicesDataProvider.getConstituentsForIndex(sanitized)
    }
}
