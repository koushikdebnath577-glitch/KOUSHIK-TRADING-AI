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
    val apiBaseUrl: String
        get() {
            val trimmed = serverUrl.trim().removeSuffix("/")
            return if (trimmed.endsWith("/api")) trimmed else "$trimmed/api"
        }

    val healthUrl: String
        get() {
            val trimmed = serverUrl.trim().removeSuffix("/")
            val root = if (trimmed.endsWith("/api")) trimmed.removeSuffix("/api") else trimmed
            return "$root/health"
        }
}

class SmartApiBackendClient {

    companion object {
        private const val TAG = "SmartApiBackendClient"
        const val PRODUCTION_BASE_URL = "https://koushik-trading-ai.onrender.com"
        const val DEFAULT_API_BASE_URL = "$PRODUCTION_BASE_URL/api"
        const val DEFAULT_HEALTH_URL = "$PRODUCTION_BASE_URL/health"
        const val DEFAULT_WS_URL = "wss://koushik-trading-ai.onrender.com/ws/market"
    }

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.CONNECTING)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _tickFlow = MutableSharedFlow<LiveTick>(extraBufferCapacity = 64)
    val tickFlow: SharedFlow<LiveTick> = _tickFlow.asSharedFlow()

    private val _marketSymbols = MutableStateFlow<List<StockSymbol>>(emptyList())
    val marketSymbols: StateFlow<List<StockSymbol>> = _marketSymbols.asStateFlow()

    private var backendConfig = BackendConfig()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // OkHttp Client with production timeouts (30s connect/read/write to accommodate Render cold starts)
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var quoteSyncJob: Job? = null
    private var isWebSocketActive = false
    private var lastTickReceivedAt: Long = 0L

    // In-memory cache of candle history per symbol + timeframe
    private val candleCache = mutableMapOf<String, MutableList<Candle>>()

    init {
        initializeSymbols()
        startConnectionLifecycle()
        startFreshnessMonitor()
    }

    private fun startFreshnessMonitor() {
        scope.launch {
            while (true) {
                delay(3000)
                if (_connectionStatus.value == ConnectionStatus.LIVE) {
                    if (System.currentTimeMillis() - lastTickReceivedAt > 15_000L) {
                        Log.w(TAG, "[FRESHNESS CHECK] No ticks received for >15s. Updating status to CONNECTED_WAITING_FOR_TICK")
                        _connectionStatus.value = ConnectionStatus.CONNECTED_WAITING_FOR_TICK
                    }
                }
            }
        }
    }

    private fun initializeSymbols() {
        val initialList = listOf(
            StockSymbol("NIFTY 50", "NIFTY 50 INDEX", "99926000", "NSE", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0),
            StockSymbol("BANKNIFTY", "NIFTY BANK INDEX", "99926009", "NSE", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0),
            StockSymbol("RELIANCE", "Reliance Industries Ltd", "2885", "NSE", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0),
            StockSymbol("HDFCBANK", "HDFC Bank Ltd", "1333", "NSE", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0),
            StockSymbol("TCS", "Tata Consultancy Services", "11536", "NSE", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0),
            StockSymbol("INFY", "Infosys Ltd", "1594", "NSE", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0),
            StockSymbol("ICICIBANK", "ICICI Bank Ltd", "4963", "NSE", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0),
            StockSymbol("TATAMOTORS", "Tata Motors Ltd", "3456", "NSE", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0),
            StockSymbol("SBIN", "State Bank of India", "3045", "NSE", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0),
            StockSymbol("ITC", "ITC Ltd", "1660", "NSE", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0),
            StockSymbol("BHARTIARTL", "Bharti Airtel Ltd", "10604", "NSE", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0),
            StockSymbol("LT", "Larsen & Toubro Ltd", "11483", "NSE", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0.0)
        )
        _marketSymbols.value = initialList
    }

    fun updateConfig(config: BackendConfig) {
        backendConfig = config
        Log.i(TAG, "[BACKEND CONFIG UPDATED]\nBACKEND URL: ${backendConfig.apiBaseUrl}\nHEALTH URL: ${backendConfig.healthUrl}\nWS URL: ${backendConfig.wsUrl}")
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
            Log.i(TAG, "[BACKEND INITIALIZATION]\nBACKEND URL: ${backendConfig.apiBaseUrl}\nHEALTH URL: ${backendConfig.healthUrl}\nWS URL: ${backendConfig.wsUrl}")
            connectToBackend()
        }
    }

    /**
     * 1. Performs GET /health check with exponential backoff for Render cold starts
     * 2. Establishes WebSocket connection to wss://koushik-trading-ai.onrender.com/ws/market
     * 3. Starts automatic reconnection if network drops
     */
    private suspend fun connectToBackend() {
        _connectionStatus.value = ConnectionStatus.CONNECTING
        Log.i(TAG, "[CONNECT TO BACKEND]\nBACKEND URL: ${backendConfig.apiBaseUrl}\nHEALTH URL: ${backendConfig.healthUrl}\nWS URL: ${backendConfig.wsUrl}\nWebSocket connection state: ${_connectionStatus.value.label}")

        // 1. Perform Health Check with retry
        val isHealthOk = performHealthCheck()
        Log.i(TAG, "[HEALTH CHECK RESULT] Health OK: $isHealthOk | HEALTH URL: ${backendConfig.healthUrl}")

        // 2. Establish WebSocket connection
        startWebSocket()
    }

    /**
     * Calls GET /health on the deployed backend with retry logic to handle cold start delays.
     */
    private suspend fun performHealthCheck(): Boolean = withContext(Dispatchers.IO) {
        val targetHealthUrl = backendConfig.healthUrl
        val maxRetries = 4
        val backoffDelays = listOf(0L, 2000L, 4000L, 6000L)

        for (attempt in 1..maxRetries) {
            if (backoffDelays[attempt - 1] > 0L) {
                Log.d(TAG, "Waiting ${backoffDelays[attempt - 1]}ms before health check retry (Attempt $attempt/$maxRetries)...")
                delay(backoffDelays[attempt - 1])
            }

            Log.i(TAG, "[HEALTH CHECK ATTEMPT $attempt/$maxRetries] Requesting HEALTH URL: $targetHealthUrl (BACKEND URL: ${backendConfig.apiBaseUrl})")

            try {
                val request = Request.Builder()
                    .url(targetHealthUrl)
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val code = response.code
                    val body = response.body?.string() ?: ""

                    Log.i(
                        TAG,
                        "[HEALTH CHECK HTTP RESPONSE]\n" +
                        "BACKEND URL: ${backendConfig.apiBaseUrl}\n" +
                        "HEALTH URL: $targetHealthUrl\n" +
                        "HTTP response code: $code\n" +
                        "HTTP response body: $body"
                    )

                    if (response.isSuccessful) {
                        return@withContext true
                    } else {
                        Log.w(TAG, "Health check returned HTTP $code on attempt $attempt/$maxRetries")
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "[HEALTH CHECK EXCEPTION] (Attempt $attempt/$maxRetries)\n" +
                    "BACKEND URL: ${backendConfig.apiBaseUrl}\n" +
                    "HEALTH URL: $targetHealthUrl\n" +
                    "Error: ${e.javaClass.simpleName}: ${e.message}"
                )
            }
        }

        Log.e(TAG, "All $maxRetries health check attempts failed. Backend may still be spinning up.")
        false
    }

    /**
     * Connects to WebSocket endpoint: wss://koushik-trading-ai.onrender.com/ws/market
     */
    private fun startWebSocket() {
        closeWebSocket()

        try {
            val targetWsUrl = backendConfig.wsUrl
            Log.i(TAG, "[WS INITIATING]\nBACKEND URL: ${backendConfig.apiBaseUrl}\nWebSocket URL: $targetWsUrl\nWebSocket connection state: ${_connectionStatus.value.label}")

            val request = Request.Builder()
                .url(targetWsUrl)
                .build()

            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    isWebSocketActive = true
                    _connectionStatus.value = ConnectionStatus.CONNECTED_WAITING_FOR_TICK
                    Log.i(
                        TAG,
                        "[WS OPEN]\n" +
                        "BACKEND URL: ${backendConfig.apiBaseUrl}\n" +
                        "WebSocket URL: $targetWsUrl\n" +
                        "WebSocket connection state: ${_connectionStatus.value.label}\n" +
                        "HTTP response code: ${response.code}\n" +
                        "HTTP response message: ${response.message}"
                    )

                    // Send subscription payload for all monitored tokens
                    val tokensJson = JSONArray()
                    _marketSymbols.value.forEach { tokensJson.put(it.token) }

                    val subPayload = JSONObject().apply {
                        put("action", "subscribe")
                        put("exchangeType", 1)
                        put("tokens", tokensJson)
                    }

                    webSocket.send(subPayload.toString())
                    Log.i(TAG, "Sent subscription payload for ${_marketSymbols.value.size} tokens")

                    // Immediately synchronize real quotes via REST while waiting for live ticks
                    syncInitialQuotes()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleIncomingWebSocketMessage(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.w(TAG, "[WS CLOSING] code: $code - reason: $reason | WebSocket connection state: ${_connectionStatus.value.label}")
                    isWebSocketActive = false
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                    scheduleAutomaticReconnect()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.w(TAG, "[WS CLOSED] code: $code - reason: $reason | WebSocket connection state: ${_connectionStatus.value.label}")
                    isWebSocketActive = false
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                    scheduleAutomaticReconnect()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(
                        TAG,
                        "[WS FAILURE]\n" +
                        "BACKEND URL: ${backendConfig.apiBaseUrl}\n" +
                        "WebSocket URL: $targetWsUrl\n" +
                        "WebSocket connection state: ERROR\n" +
                        "HTTP response code: ${response?.code ?: "N/A"}\n" +
                        "Error: ${t.javaClass.simpleName}: ${t.message}"
                    )
                    isWebSocketActive = false
                    _connectionStatus.value = ConnectionStatus.ERROR
                    scheduleAutomaticReconnect()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "[WS EXCEPTION] Failed to start WebSocket: ${e.message}")
            isWebSocketActive = false
            _connectionStatus.value = ConnectionStatus.ERROR
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

                    if (token.isNotEmpty() && ltp > 0.0) {
                        lastTickReceivedAt = System.currentTimeMillis()
                        _connectionStatus.value = ConnectionStatus.LIVE

                        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date(lastTickReceivedAt))
                        Log.i(
                            TAG,
                            "[LIVE TICK RECEIVED]\n" +
                            "BACKEND URL: ${backendConfig.apiBaseUrl}\n" +
                            "WebSocket connection state: ${_connectionStatus.value.label}\n" +
                            "Token: $token | Symbol: $symbol | Exchange: $exchange\n" +
                            "LTP: $ltp | Change: $change ($changePercent%)\n" +
                            "last tick received timestamp: $lastTickReceivedAt ($dateStr)\n" +
                            "Angel One LTP: $angelOneLtp | Backend Forwarded LTP: $backendForwardedLtp"
                        )
                        Log.i(
                            "LIVE DATA",
                            "[LIVE DATA]\ntoken: $token\nsymbol: $symbol\nltp: $ltp\nexchangeTimestamp: $timestamp\nreceivedAt: $lastTickReceivedAt\nsource=ANGEL_ONE"
                        )
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
                    Log.i(TAG, "[BACKEND HANDSHAKE] Service: ${json.optString("service")} | WebSocket connection state: ${_connectionStatus.value.label}")
                    if (_connectionStatus.value != ConnectionStatus.LIVE) {
                        _connectionStatus.value = ConnectionStatus.CONNECTED_WAITING_FOR_TICK
                    }
                }
                "subscribed" -> {
                    Log.i(TAG, "[BACKEND SUBSCRIPTION CONFIRMED] Tokens count: ${json.optJSONArray("tokens")?.length()}")
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
            val actualChange = if (change != 0.0) change else if (stock.previousClose > 0.0) (ltp - stock.previousClose) else 0.0
            val actualChangePct = if (changePercent != 0.0) changePercent else if (stock.previousClose > 0.0) ((actualChange / stock.previousClose) * 100.0) else 0.0
            val newHigh = if (stock.high > 0.0) kotlin.math.max(stock.high, ltp) else ltp
            val newLow = if (stock.low > 0.0) kotlin.math.min(stock.low, ltp) else ltp

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
            Log.i(TAG, "[SCHEDULING RECONNECT] Will attempt reconnection in 3 seconds... (BACKEND URL: ${backendConfig.apiBaseUrl})")
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
     * Retrieves historical candle data for a symbol and timeframe.
     * Returns real candles from live cache or empty list when no feed has been received.
     */
    fun getHistoricalCandles(symbol: String, timeframe: Timeframe, count: Int = 120): List<Candle> {
        val cacheKey = "${symbol}_${timeframe.name}"
        return candleCache[cacheKey] ?: emptyList()
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
     * Synchronizes real quotes from the Angel One SmartAPI backend for all tracked symbols.
     * This is called on connection establishment so users see verified prices (LAST AVAILABLE / PREVIOUS CLOSE)
     * immediately while WebSocket waits for incoming market ticks.
     */
    fun syncInitialQuotes() {
        quoteSyncJob?.cancel()
        quoteSyncJob = scope.launch {
            Log.i(TAG, "[SYNC INITIAL QUOTES] Fetching real REST quotes from SmartAPI for tracked symbols...")
            val symbolsSnapshot = _marketSymbols.value
            for (stock in symbolsSnapshot) {
                try {
                    fetchQuote(stock.token, stock.exchange)
                    delay(120) // spacing out calls
                } catch (e: Exception) {
                    Log.w(TAG, "[SYNC QUOTE ERROR] Token ${stock.token} (${stock.symbol}): ${e.message}")
                }
            }
        }
    }

    /**
     * Calls GET /api/quote?symboltoken=TOKEN&exchange=EXCHANGE
     * Fetches real LTP and Previous Close directly from Angel One SmartAPI via backend.
     * Never simulates or invents prices.
     */
    suspend fun fetchQuote(symbolToken: String, exchange: String = "NSE"): StockSymbol? = withContext(Dispatchers.IO) {
        val sanitizedToken = symbolToken.trim()
        if (sanitizedToken.isEmpty()) return@withContext null

        val targetUrl = "${backendConfig.apiBaseUrl}/quote?symboltoken=$sanitizedToken&exchange=$exchange"
        val maxRetries = 2
        val backoffDelays = listOf(0L, 1000L)

        for (attempt in 1..maxRetries) {
            if (backoffDelays[attempt - 1] > 0L) {
                delay(backoffDelays[attempt - 1])
            }

            try {
                val request = Request.Builder()
                    .url(targetUrl)
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val code = response.code
                    val bodyStr = response.body?.string() ?: ""

                    Log.i(
                        TAG,
                        "[FETCH QUOTE RESPONSE]\n" +
                        "Token: $sanitizedToken | Exchange: $exchange\n" +
                        "HTTP response code: $code\n" +
                        "HTTP response body: $bodyStr"
                    )

                    if (response.isSuccessful && bodyStr.isNotBlank()) {
                        val json = JSONObject(bodyStr)
                        if (json.optBoolean("status", false)) {
                            val dataObj = json.optJSONObject("data")
                            var token = sanitizedToken
                            var symbol = ""
                            var name = ""
                            var ltp = 0.0
                            var prevClose = 0.0
                            var open = 0.0
                            var high = 0.0
                            var low = 0.0
                            var change = 0.0
                            var changePercent = 0.0
                            var volume = 0L

                            if (dataObj != null) {
                                val fetchedArr = dataObj.optJSONArray("fetched")
                                if (fetchedArr != null && fetchedArr.length() > 0) {
                                    val item = fetchedArr.getJSONObject(0)
                                    token = item.optString("symbolToken", sanitizedToken)
                                    symbol = item.optString("tradingSymbol", "")
                                    ltp = item.optDouble("ltp", 0.0)
                                    prevClose = item.optDouble("close", 0.0)
                                    open = item.optDouble("open", 0.0)
                                    high = item.optDouble("high", 0.0)
                                    low = item.optDouble("low", 0.0)
                                    change = item.optDouble("netChange", if (prevClose > 0.0 && ltp > 0.0) ltp - prevClose else 0.0)
                                    changePercent = item.optDouble("percentChange", if (prevClose > 0.0 && change != 0.0) (change / prevClose) * 100.0 else 0.0)
                                    volume = item.optLong("tradeVolume", 0L)
                                } else {
                                    token = dataObj.optString("token", dataObj.optString("symbolToken", sanitizedToken))
                                    symbol = dataObj.optString("symbol", dataObj.optString("tradingSymbol", ""))
                                    name = dataObj.optString("name", "")
                                    ltp = dataObj.optDouble("ltp", 0.0)
                                    prevClose = dataObj.optDouble("previousClose", dataObj.optDouble("prevClose", dataObj.optDouble("close", 0.0)))
                                    open = dataObj.optDouble("open", 0.0)
                                    high = dataObj.optDouble("high", 0.0)
                                    low = dataObj.optDouble("low", 0.0)
                                    change = dataObj.optDouble("change", dataObj.optDouble("netChange", if (prevClose > 0.0 && ltp > 0.0) ltp - prevClose else 0.0))
                                    changePercent = dataObj.optDouble("changePercent", dataObj.optDouble("percentChange", if (prevClose > 0.0 && change != 0.0) (change / prevClose) * 100.0 else 0.0))
                                    volume = dataObj.optLong("volume", dataObj.optLong("tradeVolume", 0L))
                                }
                            }

                            if (ltp > 0.0 || prevClose > 0.0) {
                                val currentList = _marketSymbols.value.toMutableList()
                                val stockIdx = currentList.indexOfFirst {
                                    it.token == token ||
                                    (symbol.isNotEmpty() && it.symbol.equals(symbol.removeSuffix("-EQ"), ignoreCase = true))
                                }

                                val finalSymbol = if (stockIdx >= 0) currentList[stockIdx].symbol else (if (symbol.isNotEmpty()) symbol.removeSuffix("-EQ") else "TOKEN_$token")
                                val finalName = if (stockIdx >= 0) currentList[stockIdx].companyName else (if (name.isNotEmpty()) name else finalSymbol)

                                val calculatedChange = if (change != 0.0) change else if (prevClose > 0.0 && ltp > 0.0) (ltp - prevClose) else 0.0
                                val calculatedPct = if (changePercent != 0.0) changePercent else if (prevClose > 0.0 && calculatedChange != 0.0) ((calculatedChange / prevClose) * 100.0) else 0.0

                                val stock = StockSymbol(
                                    symbol = finalSymbol,
                                    name = finalName,
                                    token = token,
                                    exchange = exchange,
                                    ltp = ltp,
                                    change = kotlin.math.round(calculatedChange * 100.0) / 100.0,
                                    changePercent = kotlin.math.round(calculatedPct * 100.0) / 100.0,
                                    open = if (open > 0.0) open else (if (prevClose > 0.0) prevClose else ltp),
                                    high = if (high > 0.0) high else ltp,
                                    low = if (low > 0.0) low else ltp,
                                    close = if (ltp > 0.0) ltp else prevClose,
                                    volume = volume,
                                    previousClose = if (prevClose > 0.0) prevClose else ltp,
                                    lastUpdated = System.currentTimeMillis()
                                )

                                if (stockIdx >= 0) {
                                    currentList[stockIdx] = stock
                                } else {
                                    currentList.add(stock)
                                }
                                _marketSymbols.value = currentList

                                // Standardized Diagnostic Logs
                                Log.i("ANGELONE_RAW_TICK", "[ANGELONE_RAW_TICK] Token: $token | Symbol: $finalSymbol | Raw Angel LTP: $ltp | PrevClose: $prevClose | Source=SmartAPI REST Quote")
                                Log.i("BACKEND_FORWARD", "[BACKEND_FORWARD] Token: $token | Symbol: $finalSymbol | LTP: $ltp | PrevClose: $prevClose")
                                Log.i("ANDROID_RECEIVED", "[ANDROID_RECEIVED] Status: ${_connectionStatus.value.label} | Symbol: $finalSymbol | LTP: $ltp | PrevClose: $prevClose | Source=REST_QUOTE")
                                Log.i("LIVE DATA", "[LIVE DATA] token: $token | symbol: $finalSymbol | ltp: $ltp | status: ${_connectionStatus.value.label} | source=REST_QUOTE_SYNC")

                                return@withContext stock
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "[FETCH QUOTE EXCEPTION] Attempt $attempt/$maxRetries failed for $sanitizedToken: ${e.message}")
            }
        }

        return@withContext null
    }

    /**
     * Fetches candlestick data from GET /api/candles
     * Supports both sub-second intervals (1s, 5s, 15s, 30s) and standard intervals (ONE_MINUTE, FIVE_MINUTE, etc.)
     */
    suspend fun fetchCandles(
        symbolToken: String,
        exchange: String = "NSE",
        timeframe: Timeframe = Timeframe.MIN_1,
        count: Int = 120
    ): List<Candle> = withContext(Dispatchers.IO) {
        val sanitizedToken = symbolToken.trim()
        if (sanitizedToken.isEmpty()) return@withContext emptyList()

        val intervalStr = when (timeframe) {
            Timeframe.SEC_1 -> "1s"
            Timeframe.SEC_5 -> "5s"
            Timeframe.SEC_15 -> "15s"
            Timeframe.SEC_30 -> "30s"
            Timeframe.MIN_1 -> "ONE_MINUTE"
            Timeframe.MIN_3 -> "THREE_MINUTE"
            Timeframe.MIN_5 -> "FIVE_MINUTE"
            Timeframe.MIN_15 -> "FIFTEEN_MINUTE"
            Timeframe.MIN_30 -> "THIRTY_MINUTE"
            Timeframe.HOUR_1 -> "ONE_HOUR"
            Timeframe.DAY_1 -> "ONE_DAY"
        }

        val targetUrl = "${backendConfig.apiBaseUrl}/candles?symboltoken=$sanitizedToken&exchange=$exchange&interval=$intervalStr&count=$count"
        val maxRetries = 2
        val backoffDelays = listOf(0L, 1000L)

        for (attempt in 1..maxRetries) {
            if (backoffDelays[attempt - 1] > 0L) {
                delay(backoffDelays[attempt - 1])
            }

            try {
                val request = Request.Builder()
                    .url(targetUrl)
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val code = response.code
                    val bodyStr = response.body?.string() ?: ""

                    if (response.isSuccessful && bodyStr.isNotBlank()) {
                        val json = JSONObject(bodyStr)
                        if (json.optBoolean("status", false)) {
                            val dataArr = json.optJSONArray("data") ?: JSONArray()
                            val candleList = mutableListOf<Candle>()

                            for (i in 0 until dataArr.length()) {
                                val item = dataArr.optJSONObject(i) ?: continue
                                candleList.add(
                                    Candle(
                                        timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                                        open = item.optDouble("open", 0.0),
                                        high = item.optDouble("high", 0.0),
                                        low = item.optDouble("low", 0.0),
                                        close = item.optDouble("close", 0.0),
                                        volume = item.optLong("volume", 0L),
                                        isComplete = item.optBoolean("isComplete", true)
                                    )
                                )
                            }

                            if (candleList.isNotEmpty()) {
                                val stock = _marketSymbols.value.find { it.token == sanitizedToken }
                                val symbolKey = stock?.symbol ?: sanitizedToken
                                val cacheKey = "${symbolKey}_${timeframe.name}"
                                candleCache[cacheKey] = candleList.toMutableList()

                                Log.i("CHART_DATA", "[CHART_DATA] Loaded ${candleList.size} candles for $symbolKey ($sanitizedToken) at $timeframe from backend endpoint")
                                return@withContext candleList
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "[FETCH CANDLES EXCEPTION] Attempt $attempt/$maxRetries for token $sanitizedToken: ${e.message}")
            }
        }

        return@withContext emptyList()
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

        val encodedQuery = java.net.URLEncoder.encode(trimmedQuery, "UTF-8")
        val targetUrl = "${backendConfig.apiBaseUrl}/search?q=$encodedQuery&limit=50"
        val maxRetries = 2
        val backoffDelays = listOf(0L, 1500L)

        for (attempt in 1..maxRetries) {
            if (backoffDelays[attempt - 1] > 0L) {
                delay(backoffDelays[attempt - 1])
            }

            try {
                val request = Request.Builder()
                    .url(targetUrl)
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val code = response.code
                    val bodyStr = response.body?.string() ?: ""

                    Log.i(
                        TAG,
                        "[SEARCH STOCKS RESPONSE]\n" +
                        "BACKEND URL: ${backendConfig.apiBaseUrl}\n" +
                        "Query: $trimmedQuery | HTTP response code: $code\n" +
                        "HTTP response body length: ${bodyStr.length} chars"
                    )

                    if (response.isSuccessful && bodyStr.isNotBlank()) {
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
                Log.w(TAG, "[SEARCH STOCKS EXCEPTION] Attempt $attempt/$maxRetries failed: ${e.message}")
            }
        }

        // Fallback to local master list
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
     * with retry for Render cold-starts and clean fallback metadata (0.0 prices, no simulated data).
     */
    suspend fun fetchIndices(): List<IndexItem> = withContext(Dispatchers.IO) {
        val targetUrl = "${backendConfig.apiBaseUrl}/indices"
        val maxRetries = 3
        val backoffDelays = listOf(0L, 2000L, 4000L)

        for (attempt in 1..maxRetries) {
            if (backoffDelays[attempt - 1] > 0L) {
                delay(backoffDelays[attempt - 1])
            }

            try {
                Log.i(TAG, "[FETCH INDICES ATTEMPT $attempt/$maxRetries]\nBACKEND URL: ${backendConfig.apiBaseUrl}\nRequest URL: $targetUrl")
                val request = Request.Builder()
                    .url(targetUrl)
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val code = response.code
                    val bodyStr = response.body?.string() ?: ""

                    Log.i(
                        TAG,
                        "[FETCH INDICES RESPONSE]\n" +
                        "BACKEND URL: ${backendConfig.apiBaseUrl}\n" +
                        "HTTP response code: $code\n" +
                        "HTTP response body length: ${bodyStr.length} chars"
                    )

                    if (response.isSuccessful && bodyStr.isNotBlank()) {
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
                Log.w(TAG, "[FETCH INDICES EXCEPTION] Attempt $attempt/$maxRetries failed: ${e.message}")
            }
        }

        Log.w(TAG, "Backend /indices unreachable or timeout. Using base index definitions awaiting live feed.")
        return@withContext IndicesDataProvider.DEFAULT_INDICES
    }

    /**
     * Fetches constituent stocks for an index from GET /api/indices/:name/constituents
     */
    suspend fun fetchIndexConstituents(indexIdOrSymbol: String): List<StockSymbol> = withContext(Dispatchers.IO) {
        val sanitized = indexIdOrSymbol.trim()
        if (sanitized.isEmpty()) return@withContext emptyList()

        val encodedName = java.net.URLEncoder.encode(sanitized, "UTF-8")
        val targetUrl = "${backendConfig.apiBaseUrl}/indices/$encodedName/constituents"
        val maxRetries = 3
        val backoffDelays = listOf(0L, 2000L, 4000L)

        for (attempt in 1..maxRetries) {
            if (backoffDelays[attempt - 1] > 0L) {
                delay(backoffDelays[attempt - 1])
            }

            try {
                val request = Request.Builder()
                    .url(targetUrl)
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val code = response.code
                    val bodyStr = response.body?.string() ?: ""

                    Log.i(
                        TAG,
                        "[FETCH CONSTITUENTS RESPONSE]\n" +
                        "BACKEND URL: ${backendConfig.apiBaseUrl}\n" +
                        "Index: $sanitized | HTTP response code: $code\n" +
                        "HTTP response body length: ${bodyStr.length} chars"
                    )

                    if (response.isSuccessful && bodyStr.isNotBlank()) {
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
                                    volume = item.optLong("volume", 0L),
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
                Log.w(TAG, "[FETCH CONSTITUENTS EXCEPTION] Attempt $attempt/$maxRetries for $indexIdOrSymbol failed: ${e.message}")
            }
        }

        return@withContext IndicesDataProvider.getConstituentsForIndex(sanitized)
    }
}
