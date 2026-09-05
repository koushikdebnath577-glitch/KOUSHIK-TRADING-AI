package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.Candle
import com.example.data.model.KeyLevel
import com.example.data.model.KeyLevelType
import com.example.data.repository.TradingRepository
import com.example.ui.theme.BgDarkNavy
import java.lang.ref.WeakReference

class TradingViewChartController {
    var webViewRef: WeakReference<WebView>? = null

    fun zoomIn() {
        webViewRef?.get()?.evaluateJavascript("window.zoomIn();", null)
    }

    fun zoomOut() {
        webViewRef?.get()?.evaluateJavascript("window.zoomOut();", null)
    }

    fun fitContent() {
        webViewRef?.get()?.evaluateJavascript("window.fitContent();", null)
    }
}

class ChartJavaScriptInterface(private val onReadyCallback: () -> Unit) {
    @JavascriptInterface
    fun onChartReady() {
        onReadyCallback()
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun TradingViewLightweightChartView(
    candles: List<Candle>,
    keyLevels: List<KeyLevel>,
    currentLtp: Double,
    lastUpdatedTimestamp: Long,
    ema20: List<Double?>,
    ema50: List<Double?>,
    vwap: List<Double?>,
    indicatorSettings: TradingRepository.IndicatorSettings,
    controller: TradingViewChartController? = null,
    modifier: Modifier = Modifier
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isChartReady by remember { mutableStateOf(false) }

    val sortedCandles = remember(candles) {
        candles.sortedBy { it.timestamp }.distinctBy { it.timestamp / 1000 }
    }

    // Push historical candles whenever sortedCandles updates
    LaunchedEffect(sortedCandles, isChartReady) {
        val wv = webViewInstance ?: return@LaunchedEffect
        if (!isChartReady) return@LaunchedEffect

        if (sortedCandles.isEmpty()) {
            wv.evaluateJavascript("window.setCandles([], []);", null)
            return@LaunchedEffect
        }

        val sbCandles = StringBuilder("[")
        val sbVolume = StringBuilder("[")
        for (i in sortedCandles.indices) {
            val c = sortedCandles[i]
            val sec = c.timestamp / 1000
            if (i > 0) {
                sbCandles.append(",")
                sbVolume.append(",")
            }
            sbCandles.append("{\"time\":").append(sec)
                .append(",\"open\":").append(c.open)
                .append(",\"high\":").append(c.high)
                .append(",\"low\":").append(c.low)
                .append(",\"close\":").append(c.close)
                .append("}")

            val col = if (c.isBullish) "#26A69A" else "#EF5350"
            sbVolume.append("{\"time\":").append(sec)
                .append(",\"value\":").append(c.volume)
                .append(",\"color\":\"").append(col)
                .append("\"}")
        }
        sbCandles.append("]")
        sbVolume.append("]")

        wv.evaluateJavascript("window.setCandles($sbCandles, $sbVolume);", null)
    }

    // Live tick update pipeline via evaluateJavascript
    LaunchedEffect(currentLtp, lastUpdatedTimestamp, isChartReady) {
        val wv = webViewInstance ?: return@LaunchedEffect
        if (!isChartReady || currentLtp <= 0.0) return@LaunchedEffect

        val timeSec = if (lastUpdatedTimestamp > 0) {
            lastUpdatedTimestamp / 1000
        } else {
            sortedCandles.lastOrNull()?.timestamp?.div(1000) ?: (System.currentTimeMillis() / 1000)
        }
        wv.evaluateJavascript("window.updateLiveTick($currentLtp, $timeSec);", null)
    }

    // Indicators update pipeline (EMA20, EMA50, VWAP)
    LaunchedEffect(indicatorSettings, ema20, ema50, vwap, sortedCandles, isChartReady) {
        val wv = webViewInstance ?: return@LaunchedEffect
        if (!isChartReady) return@LaunchedEffect

        val ema20Json = if (indicatorSettings.showEma20) buildLineData(sortedCandles, ema20) else "[]"
        val ema50Json = if (indicatorSettings.showEma50) buildLineData(sortedCandles, ema50) else "[]"
        val vwapJson = if (indicatorSettings.showVwap) buildLineData(sortedCandles, vwap) else "[]"

        wv.evaluateJavascript("window.setIndicators($ema20Json, $ema50Json, $vwapJson);", null)
    }

    // Key Levels update pipeline
    LaunchedEffect(indicatorSettings.showKeyLevels, keyLevels, isChartReady) {
        val wv = webViewInstance ?: return@LaunchedEffect
        if (!isChartReady) return@LaunchedEffect

        val levelsJson = if (indicatorSettings.showKeyLevels) buildLevelsData(keyLevels) else "[]"
        wv.evaluateJavascript("window.setKeyLevels($levelsJson);", null)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDarkNavy)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                createTradingViewWebView(ctx, controller) {
                    isChartReady = true
                }.also { wv ->
                    webViewInstance = wv
                    controller?.webViewRef = WeakReference(wv)
                }
            },
            update = { wv ->
                webViewInstance = wv
                controller?.webViewRef = WeakReference(wv)
            }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
private fun createTradingViewWebView(
    context: Context,
    controller: TradingViewChartController?,
    onReady: () -> Unit
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(Color.parseColor("#0D1117"))

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(true)
            builtInZoomControls = false
            displayZoomControls = false
        }

        webChromeClient = WebChromeClient()

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.postDelayed({
                    view.evaluateJavascript(
                        "if (typeof isReady !== 'undefined' && isReady) { window.AndroidInterface.onChartReady(); }",
                        null
                    )
                }, 100)
            }

            @Deprecated("Deprecated in Java")
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                android.util.Log.w("TradingViewChart", "WebView error: $errorCode - $description for $failingUrl")
            }
        }

        addJavascriptInterface(
            ChartJavaScriptInterface {
                post {
                    onReady()
                }
            },
            "AndroidInterface"
        )

        // Ensure horizontal drag and multi-finger pinch-to-zoom don't get intercepted by parent LazyColumn
        setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }

        loadUrl("file:///android_asset/tradingview_chart.html")
        controller?.webViewRef = WeakReference(this)
    }
}

private fun buildLineData(candles: List<Candle>, values: List<Double?>): String {
    if (candles.isEmpty() || values.isEmpty()) return "[]"
    val count = minOf(candles.size, values.size)
    val sb = StringBuilder("[")
    var hasItem = false
    for (i in 0 until count) {
        val v = values[i] ?: continue
        if (v <= 0.0) continue
        val sec = candles[i].timestamp / 1000
        if (hasItem) sb.append(",")
        sb.append("{\"time\":").append(sec).append(",\"value\":").append(v).append("}")
        hasItem = true
    }
    sb.append("]")
    return sb.toString()
}

private fun buildLevelsData(keyLevels: List<KeyLevel>): String {
    if (keyLevels.isEmpty()) return "[]"
    val sb = StringBuilder("[")
    keyLevels.forEachIndexed { i, lvl ->
        if (i > 0) sb.append(",")
        val colorHex = when (lvl.type) {
            KeyLevelType.DAY_HIGH, KeyLevelType.PREV_DAY_HIGH, KeyLevelType.RESISTANCE -> "#FF5252"
            KeyLevelType.DAY_LOW, KeyLevelType.PREV_DAY_LOW, KeyLevelType.SUPPORT -> "#00E676"
            KeyLevelType.OPENING_RANGE_HIGH, KeyLevelType.OPENING_RANGE_LOW -> "#FFB703"
            else -> "#58A6FF"
        }
        val safeTitle = lvl.type.displayName.replace("\"", "")
        sb.append("{\"price\":").append(lvl.price)
            .append(",\"title\":\"").append(safeTitle)
            .append("\",\"color\":\"").append(colorHex)
            .append("\"}")
    }
    sb.append("]")
    return sb.toString()
}
