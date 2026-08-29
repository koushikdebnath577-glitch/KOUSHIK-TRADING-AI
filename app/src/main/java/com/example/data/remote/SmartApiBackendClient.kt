package com.example.data.remote

import com.example.data.model.Candle
import com.example.data.model.ConnectionStatus
import com.example.data.model.StockSymbol
import com.example.data.model.Timeframe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

data class LiveTick(
    val token: String,
    val symbol: String,
    val ltp: Double,
    val volume: Long,
    val timestamp: Long = System.currentTimeMillis()
)

data class BackendConfig(
    val serverUrl: String = "https://smartapi-proxy.internal.koushiktrading.ai/api/v1",
    val wsUrl: String = "wss://smartapi-proxy.internal.koushiktrading.ai/ws/ticks",
    val isSimulated: Boolean = true,
    val isConnected: Boolean = true,
    val useLiveBackend: Boolean = false,
    val clientIp: String = "192.168.1.1",
    val macAddress: String = "00:1A:2B:3C:4D:5E"
)

class SmartApiBackendClient {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.LIVE)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _tickFlow = MutableSharedFlow<LiveTick>(extraBufferCapacity = 64)
    val tickFlow: SharedFlow<LiveTick> = _tickFlow.asSharedFlow()

    private val _marketSymbols = MutableStateFlow<List<StockSymbol>>(emptyList())
    val marketSymbols: StateFlow<List<StockSymbol>> = _marketSymbols.asStateFlow()

    private var backendConfig = BackendConfig()
    private var simulationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // In-memory cache of candle history per symbol + timeframe
    private val candleCache = mutableMapOf<String, MutableList<Candle>>()

    init {
        initializeSymbols()
        startLiveFeed()
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

    fun reconnect() {
        scope.launch {
            _connectionStatus.value = ConnectionStatus.RECONNECTING
            delay(1200)
            _connectionStatus.value = ConnectionStatus.LIVE
        }
    }

    private fun startLiveFeed() {
        simulationJob?.cancel()
        simulationJob = scope.launch {
            while (isActive) {
                if (_connectionStatus.value == ConnectionStatus.LIVE) {
                    val currentList = _marketSymbols.value.toMutableList()
                    val index = Random.nextInt(currentList.size)
                    val stock = currentList[index]

                    // Calculate tick delta based on volatility
                    val volatility = stock.ltp * 0.0008
                    val delta = (Random.nextDouble(-volatility, volatility * 1.02))
                    val newPrice = kotlin.math.round((stock.ltp + delta) * 100.0) / 100.0
                    val newChange = newPrice - stock.previousClose
                    val newChangePercent = (newChange / stock.previousClose) * 100.0
                    val newHigh = kotlin.math.max(stock.high, newPrice)
                    val newLow = kotlin.math.min(stock.low, newPrice)
                    val tickVol = Random.nextLong(100, 2500)
                    val newVolume = stock.volume + tickVol

                    val updatedStock = stock.copy(
                        ltp = newPrice,
                        change = newChange,
                        changePercent = newChangePercent,
                        high = newHigh,
                        low = newLow,
                        volume = newVolume,
                        lastUpdated = System.currentTimeMillis()
                    )
                    currentList[index] = updatedStock
                    _marketSymbols.value = currentList

                    _tickFlow.emit(
                        LiveTick(
                            token = stock.token,
                            symbol = stock.symbol,
                            ltp = newPrice,
                            volume = tickVol,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                // High frequency tick pace (between 250ms and 750ms for realistic intraday dynamics)
                delay(Random.nextLong(250, 750))
            }
        }
    }

    /**
     * Retrieves historical candle data for a symbol and timeframe.
     * Uses in-memory cache to prevent repeated historical requests.
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

        // Form an opening range in earlier candles
        val orHigh = price * 1.008
        val orLow = price * 0.994
        val resistanceLevel = price * 1.012

        for (i in count downTo 1) {
            val candleTime = now - (i * intervalMs)
            val open = price

            // Market drift simulation with resistance rejections near the top
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

        // Adjust the last candle close to current LTP
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
}
