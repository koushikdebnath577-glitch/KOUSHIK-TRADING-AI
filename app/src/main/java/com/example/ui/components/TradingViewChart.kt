package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AnalysisResult
import com.example.data.model.Candle
import com.example.data.model.ConnectionStatus
import com.example.data.model.KeyLevel
import com.example.data.model.Timeframe
import com.example.data.repository.TradingRepository
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class ChartTool {
    NONE,
    CROSSHAIR,
    MEASURE,
    LINE_HORIZONTAL,
    LINE_SUPPORT,
    LINE_RESISTANCE,
    LINE_TREND
}

enum class DrawingType {
    HORIZONTAL,
    SUPPORT,
    RESISTANCE,
    TREND
}

data class ChartDrawing(
    val id: String = UUID.randomUUID().toString(),
    val type: DrawingType,
    val price1: Double,
    val price2: Double = price1,
    val candleTime1: Long = 0L,
    val candleTime2: Long = 0L,
    val label: String = ""
)

data class MeasurementData(
    val startPrice: Double,
    val endPrice: Double,
    val startTime: Long,
    val endTime: Long,
    val startCandleIndex: Int,
    val endCandleIndex: Int
) {
    val priceDiff: Double get() = endPrice - startPrice
    val percentMove: Double get() = if (startPrice > 0) (priceDiff / startPrice) * 100.0 else 0.0
    val points: Double get() = abs(priceDiff)
    val barsCount: Int get() = abs(endCandleIndex - startCandleIndex) + 1
}

@Composable
fun TradingViewChart(
    candles: List<Candle>,
    keyLevels: List<KeyLevel>,
    currentLtp: Double,
    analysisResult: AnalysisResult?,
    ema20: List<Double?>,
    ema50: List<Double?>,
    vwap: List<Double?>,
    rsi: List<Double?>,
    indicatorSettings: TradingRepository.IndicatorSettings,
    selectedTimeframe: Timeframe,
    onTimeframeSelected: (Timeframe) -> Unit,
    onToggleIndicator: (String) -> Unit,
    connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTING,
    lastUpdatedTimestamp: Long = 0L,
    modifier: Modifier = Modifier
) {
    var isFullscreen by remember { mutableStateOf(false) }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgDarkNavy)
            ) {
                ChartCoreContent(
                    candles = candles,
                    keyLevels = keyLevels,
                    currentLtp = currentLtp,
                    analysisResult = analysisResult,
                    ema20 = ema20,
                    ema50 = ema50,
                    vwap = vwap,
                    rsi = rsi,
                    indicatorSettings = indicatorSettings,
                    selectedTimeframe = selectedTimeframe,
                    onTimeframeSelected = onTimeframeSelected,
                    onToggleIndicator = onToggleIndicator,
                    connectionStatus = connectionStatus,
                    lastUpdatedTimestamp = lastUpdatedTimestamp,
                    isFullscreen = true,
                    onToggleFullscreen = { isFullscreen = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        ChartCoreContent(
            candles = candles,
            keyLevels = keyLevels,
            currentLtp = currentLtp,
            analysisResult = analysisResult,
            ema20 = ema20,
            ema50 = ema50,
            vwap = vwap,
            rsi = rsi,
            indicatorSettings = indicatorSettings,
            selectedTimeframe = selectedTimeframe,
            onTimeframeSelected = onTimeframeSelected,
            onToggleIndicator = onToggleIndicator,
            connectionStatus = connectionStatus,
            lastUpdatedTimestamp = lastUpdatedTimestamp,
            isFullscreen = false,
            onToggleFullscreen = { isFullscreen = true },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun ChartCoreContent(
    candles: List<Candle>,
    keyLevels: List<KeyLevel>,
    currentLtp: Double,
    analysisResult: AnalysisResult?,
    ema20: List<Double?>,
    ema50: List<Double?>,
    vwap: List<Double?>,
    rsi: List<Double?>,
    indicatorSettings: TradingRepository.IndicatorSettings,
    selectedTimeframe: Timeframe,
    onTimeframeSelected: (Timeframe) -> Unit,
    onToggleIndicator: (String) -> Unit,
    connectionStatus: ConnectionStatus,
    lastUpdatedTimestamp: Long,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visibleCandleCount by remember { mutableStateOf(40) }
    var scrollOffset by remember { mutableStateOf(0) }
    var crosshairOffset by remember { mutableStateOf<Offset?>(null) }
    var selectedCandle by remember { mutableStateOf<Candle?>(null) }
    var autoFollow by remember { mutableStateOf(true) }
    var activeTool by remember { mutableStateOf(ChartTool.NONE) }
    var verticalScaleMultiplier by remember { mutableStateOf(1.0f) }

    // Measurement tool state
    var measurement by remember { mutableStateOf<MeasurementData?>(null) }
    var measureStartPoint by remember { mutableStateOf<Pair<Double, Candle>?>(null) }

    // Drawing tools state
    var drawings by remember { mutableStateOf<List<ChartDrawing>>(emptyList()) }
    var trendStartPoint by remember { mutableStateOf<Pair<Double, Candle>?>(null) }

    val textMeasurer = rememberTextMeasurer()

    // Pulse animation for live price marker
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // When new candles arrive, auto follow if enabled
    LaunchedEffect(candles.size, candles.lastOrNull()?.timestamp) {
        if (autoFollow && scrollOffset > 0) {
            scrollOffset = 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isFullscreen) {
                    Modifier
                        .fillMaxSize()
                        .background(BgDarkNavy)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                } else {
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(BgCard)
                        .border(1.dp, BgCardBorder, RoundedCornerShape(16.dp))
                        .padding(vertical = 8.dp)
                }
            )
            .testTag("candlestick_chart_container")
    ) {
        // Chart Header Toolbar: Title, Fullscreen, Tool Actions
        ChartHeaderBar(
            symbol = analysisResult?.symbol ?: "CHART",
            currentLtp = currentLtp,
            connectionStatus = connectionStatus,
            lastUpdatedTimestamp = lastUpdatedTimestamp,
            isFullscreen = isFullscreen,
            activeTool = activeTool,
            drawingCount = drawings.size,
            onSelectTool = { tool ->
                if (activeTool == tool) {
                    activeTool = ChartTool.NONE
                    measureStartPoint = null
                    trendStartPoint = null
                } else {
                    activeTool = tool
                    if (tool != ChartTool.CROSSHAIR) {
                        crosshairOffset = null
                        selectedCandle = null
                    }
                }
            },
            onZoomIn = {
                visibleCandleCount = (visibleCandleCount - 8).coerceAtLeast(12)
            },
            onZoomOut = {
                visibleCandleCount = (visibleCandleCount + 8).coerceAtMost(140)
            },
            onResetScale = {
                visibleCandleCount = 40
                scrollOffset = 0
                autoFollow = true
                verticalScaleMultiplier = 1.0f
                crosshairOffset = null
                selectedCandle = null
                measurement = null
                measureStartPoint = null
                trendStartPoint = null
                activeTool = ChartTool.NONE
            },
            onClearDrawings = {
                drawings = emptyList()
                measurement = null
                measureStartPoint = null
                trendStartPoint = null
            },
            onToggleFullscreen = onToggleFullscreen
        )

        // Timeframe Selector Bar
        TimeframeSelectorBar(
            selectedTimeframe = selectedTimeframe,
            onTimeframeSelected = {
                onTimeframeSelected(it)
                scrollOffset = 0
                autoFollow = true
            }
        )

        // Indicators Toggle Strip
        IndicatorToggleStrip(
            settings = indicatorSettings,
            onToggle = onToggleIndicator
        )

        // Tool status message banner (when active)
        if (activeTool != ChartTool.NONE && activeTool != ChartTool.CROSSHAIR) {
            ToolInstructionBanner(
                tool = activeTool,
                isMeasuring = measureStartPoint != null,
                isDrawingTrend = trendStartPoint != null,
                onCancel = {
                    activeTool = ChartTool.NONE
                    measureStartPoint = null
                    trendStartPoint = null
                }
            )
        }

        // Active Measurement Stats HUD
        measurement?.let { m ->
            MeasurementOverlayHUD(
                measurement = m,
                onClear = { measurement = null }
            )
        }

        // Crosshair HUD Info Bar if crosshair active
        if (selectedCandle != null) {
            val c = selectedCandle!!
            val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
            val candleChange = c.close - c.open
            val candleChangePercent = if (c.open > 0) (candleChange / c.open) * 100.0 else 0.0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 3.dp)
                    .background(BgCardElevated, RoundedCornerShape(6.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(c.timestamp)),
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "O:${String.format(Locale.US, "%.1f", c.open)}",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "H:${String.format(Locale.US, "%.1f", c.high)}",
                    color = BullishGreen,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "L:${String.format(Locale.US, "%.1f", c.low)}",
                    color = BearishRed,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "C:${String.format(Locale.US, "%.1f", c.close)}",
                    color = if (c.isBullish) BullishGreen else BearishRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${if (candleChange >= 0) "+" else ""}${String.format(Locale.US, "%.2f", candleChangePercent)}%",
                    color = if (candleChange >= 0) BullishGreen else BearishRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Main Chart Canvas Container
        val chartBoxModifier = if (isFullscreen) Modifier.weight(1f) else Modifier.height(320.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(chartBoxModifier)
                .pointerInput(candles.size, activeTool) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (zoom != 1f) {
                            val newCount = (visibleCandleCount / zoom).toInt().coerceIn(12, 140)
                            visibleCandleCount = newCount
                        }
                        if (pan.x != 0f) {
                            val delta = (pan.x / 10f).toInt()
                            val maxScroll = max(0, candles.size - visibleCandleCount)
                            scrollOffset = (scrollOffset - delta).coerceIn(0, maxScroll)
                            if (scrollOffset > 0) {
                                autoFollow = false
                            }
                        }
                        if (pan.y != 0f && activeTool == ChartTool.NONE) {
                            verticalScaleMultiplier = (verticalScaleMultiplier - (pan.y / 1000f)).coerceIn(0.5f, 2.5f)
                        }
                    }
                }
                .pointerInput(candles.size, activeTool, drawings.size, measureStartPoint, trendStartPoint) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (candles.isEmpty()) return@detectTapGestures

                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val priceScaleWidth = 55.dp.toPx()
                            val chartWidth = canvasWidth - priceScaleWidth
                            val mainChartHeight = canvasHeight - 22.dp.toPx() - (if (indicatorSettings.showRsi) 65.dp.toPx() else 0f)

                            val totalCandles = candles.size
                            val endIndex = (totalCandles - scrollOffset).coerceIn(1, totalCandles)
                            val startIndex = max(0, endIndex - visibleCandleCount)
                            val visibleCandles = candles.subList(startIndex, endIndex)
                            if (visibleCandles.isEmpty()) return@detectTapGestures

                            val baseMin = visibleCandles.minOf { it.low } * 0.999
                            val baseMax = visibleCandles.maxOf { it.high } * 1.001
                            val midPrice = (baseMin + baseMax) / 2.0
                            val halfRange = ((baseMax - baseMin) / 2.0) * verticalScaleMultiplier
                            val minPrice = midPrice - halfRange
                            val maxPrice = midPrice + halfRange
                            val priceRange = max(0.01, maxPrice - minPrice)

                            val candleSlotWidth = chartWidth / visibleCandles.size
                            val candleIndex = (offset.x / candleSlotWidth).toInt().coerceIn(0, visibleCandles.size - 1)
                            val tappedCandle = visibleCandles[candleIndex]
                            val normalizedY = (mainChartHeight - 10f - offset.y) / (mainChartHeight - 20f)
                            val tappedPrice = minPrice + (normalizedY * priceRange)

                            when (activeTool) {
                                ChartTool.CROSSHAIR -> {
                                    crosshairOffset = if (crosshairOffset == null) offset else null
                                    if (crosshairOffset == null) selectedCandle = null
                                }
                                ChartTool.MEASURE -> {
                                    if (measureStartPoint == null) {
                                        measureStartPoint = Pair(tappedPrice, tappedCandle)
                                    } else {
                                        val start = measureStartPoint!!
                                        val sIndex = candles.indexOfFirst { it.timestamp == start.second.timestamp }
                                        val eIndex = candles.indexOfFirst { it.timestamp == tappedCandle.timestamp }
                                        measurement = MeasurementData(
                                            startPrice = start.first,
                                            endPrice = tappedPrice,
                                            startTime = start.second.timestamp,
                                            endTime = tappedCandle.timestamp,
                                            startCandleIndex = if (sIndex >= 0) sIndex else 0,
                                            endCandleIndex = if (eIndex >= 0) eIndex else 0
                                        )
                                        measureStartPoint = null
                                        activeTool = ChartTool.NONE
                                    }
                                }
                                ChartTool.LINE_HORIZONTAL -> {
                                    drawings = drawings + ChartDrawing(
                                        type = DrawingType.HORIZONTAL,
                                        price1 = tappedPrice,
                                        label = "H-LINE ₹${String.format(Locale.US, "%.2f", tappedPrice)}"
                                    )
                                    activeTool = ChartTool.NONE
                                }
                                ChartTool.LINE_SUPPORT -> {
                                    drawings = drawings + ChartDrawing(
                                        type = DrawingType.SUPPORT,
                                        price1 = tappedPrice,
                                        label = "SUPPORT ₹${String.format(Locale.US, "%.2f", tappedPrice)}"
                                    )
                                    activeTool = ChartTool.NONE
                                }
                                ChartTool.LINE_RESISTANCE -> {
                                    drawings = drawings + ChartDrawing(
                                        type = DrawingType.RESISTANCE,
                                        price1 = tappedPrice,
                                        label = "RESISTANCE ₹${String.format(Locale.US, "%.2f", tappedPrice)}"
                                    )
                                    activeTool = ChartTool.NONE
                                }
                                ChartTool.LINE_TREND -> {
                                    if (trendStartPoint == null) {
                                        trendStartPoint = Pair(tappedPrice, tappedCandle)
                                    } else {
                                        val start = trendStartPoint!!
                                        drawings = drawings + ChartDrawing(
                                            type = DrawingType.TREND,
                                            price1 = start.first,
                                            price2 = tappedPrice,
                                            candleTime1 = start.second.timestamp,
                                            candleTime2 = tappedCandle.timestamp,
                                            label = "TREND"
                                        )
                                        trendStartPoint = null
                                        activeTool = ChartTool.NONE
                                    }
                                }
                                ChartTool.NONE -> {
                                    crosshairOffset = if (crosshairOffset == null) offset else null
                                    if (crosshairOffset == null) selectedCandle = null
                                }
                            }
                        }
                    )
                }
                .pointerInput(candles.size, activeTool) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            if (activeTool == ChartTool.CROSSHAIR || activeTool == ChartTool.NONE) {
                                crosshairOffset = change.position
                            }
                        },
                        onDragEnd = {}
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val priceScaleWidth = 55.dp.toPx()
                val timeScaleHeight = 22.dp.toPx()
                val rsiHeight = if (indicatorSettings.showRsi) 65.dp.toPx() else 0f
                val volumeHeight = 40.dp.toPx()

                val chartWidth = canvasWidth - priceScaleWidth
                val mainChartHeight = canvasHeight - timeScaleHeight - rsiHeight

                if (candles.isEmpty()) return@Canvas

                val totalCandles = candles.size
                val endIndex = (totalCandles - scrollOffset).coerceIn(1, totalCandles)
                val startIndex = max(0, endIndex - visibleCandleCount)
                val visibleCandles = candles.subList(startIndex, endIndex)
                if (visibleCandles.isEmpty()) return@Canvas

                val baseMin = visibleCandles.minOf { it.low } * 0.999
                val baseMax = visibleCandles.maxOf { it.high } * 1.001
                val midPrice = (baseMin + baseMax) / 2.0
                val halfRange = ((baseMax - baseMin) / 2.0) * verticalScaleMultiplier
                val minPrice = midPrice - halfRange
                val maxPrice = midPrice + halfRange
                val priceRange = max(0.01, maxPrice - minPrice)

                val candleSlotWidth = chartWidth / visibleCandles.size
                val candleBodyWidth = max(2f, candleSlotWidth * 0.70f)

                fun priceToY(price: Double): Float {
                    val normalized = (price - minPrice) / priceRange
                    return (mainChartHeight - (normalized * (mainChartHeight - 20f)) - 10f).toFloat()
                }

                fun yToPrice(y: Float): Double {
                    val normalized = (mainChartHeight - 10f - y) / (mainChartHeight - 20f)
                    return minPrice + (normalized * priceRange)
                }

                fun indexToX(index: Int): Float {
                    return (index * candleSlotWidth) + (candleSlotWidth / 2f)
                }

                // 1. Grid Lines & Price Axis
                val gridCount = 5
                for (i in 0..gridCount) {
                    val y = (mainChartHeight / gridCount) * i
                    drawLine(
                        color = GridLineColor,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1f
                    )
                    val priceAtY = yToPrice(y)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = String.format(Locale.US, "%.1f", priceAtY),
                        topLeft = Offset(chartWidth + 6f, y - 8f),
                        style = TextStyle(color = TextTertiary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    )
                }

                // Time axis vertical grid
                val timeGridStep = max(1, visibleCandles.size / 5)
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                for (i in visibleCandles.indices step timeGridStep) {
                    val x = indexToX(i)
                    drawLine(
                        color = GridLineColor.copy(alpha = 0.5f),
                        start = Offset(x, 0f),
                        end = Offset(x, mainChartHeight),
                        strokeWidth = 1f
                    )
                    val c = visibleCandles[i]
                    drawText(
                        textMeasurer = textMeasurer,
                        text = timeFormat.format(Date(c.timestamp)),
                        topLeft = Offset(x - 14f, mainChartHeight + 4f),
                        style = TextStyle(color = TextTertiary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    )
                }

                // 2. Key Levels
                if (indicatorSettings.showKeyLevels) {
                    for (level in keyLevels) {
                        if (level.price in minPrice..maxPrice) {
                            val levelY = priceToY(level.price)
                            val levelColor = Color(level.type.colorHex)
                            val pathEffect = when (level.strength) {
                                com.example.data.model.LevelStrength.STRONG -> null
                                com.example.data.model.LevelStrength.MODERATE -> PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
                                com.example.data.model.LevelStrength.WEAK -> PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                            }
                            drawLine(
                                color = levelColor.copy(alpha = 0.85f),
                                start = Offset(0f, levelY),
                                end = Offset(chartWidth, levelY),
                                strokeWidth = if (level.strength == com.example.data.model.LevelStrength.STRONG) 2.2f else 1.2f,
                                pathEffect = pathEffect
                            )
                            drawRect(
                                color = levelColor.copy(alpha = 0.25f),
                                topLeft = Offset(4f, levelY - 14f),
                                size = Size(90f, 16f)
                            )
                            drawText(
                                textMeasurer = textMeasurer,
                                text = level.type.displayName.take(14),
                                topLeft = Offset(6f, levelY - 14f),
                                style = TextStyle(color = levelColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // 3. Volume Histogram
                val maxVolume = max(1L, visibleCandles.maxOf { it.volume })
                for (i in visibleCandles.indices) {
                    val c = visibleCandles[i]
                    val x = indexToX(i)
                    val volBarHeight = ((c.volume.toFloat() / maxVolume) * volumeHeight).coerceIn(2f, volumeHeight)
                    val volY = mainChartHeight - volBarHeight
                    val volColor = if (c.isBullish) BullishGreen.copy(alpha = 0.35f) else BearishRed.copy(alpha = 0.35f)
                    drawRect(
                        color = volColor,
                        topLeft = Offset(x - (candleBodyWidth / 2f), volY),
                        size = Size(candleBodyWidth, volBarHeight)
                    )
                }

                // 4. Indicator Lines
                fun drawIndicatorLine(data: List<Double?>, color: Color, strokeWidth: Float = 2f) {
                    if (data.size < totalCandles) return
                    val visibleData = data.subList(startIndex, endIndex)
                    val path = Path()
                    var started = false
                    for (i in visibleData.indices) {
                        val v = visibleData[i] ?: continue
                        val x = indexToX(i)
                        val y = priceToY(v)
                        if (!started) {
                            path.moveTo(x, y)
                            started = true
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    if (started) {
                        drawPath(path, color = color, style = Stroke(width = strokeWidth))
                    }
                }

                if (indicatorSettings.showEma20) drawIndicatorLine(ema20, CyanAccent, 2f)
                if (indicatorSettings.showEma50) drawIndicatorLine(ema50, PurpleAccent, 2f)
                if (indicatorSettings.showVwap) drawIndicatorLine(vwap, KeyLevelYellow, 2.5f)

                // 5. Candlesticks
                for (i in visibleCandles.indices) {
                    val c = visibleCandles[i]
                    val x = indexToX(i)
                    val highY = priceToY(c.high)
                    val lowY = priceToY(c.low)
                    val openY = priceToY(c.open)
                    val closeY = priceToY(c.close)

                    val candleColor = if (c.isBullish) BullishGreen else BearishRed

                    // Wick
                    drawLine(
                        color = candleColor,
                        start = Offset(x, highY),
                        end = Offset(x, lowY),
                        strokeWidth = max(1.2f, candleBodyWidth * 0.18f)
                    )

                    // Body
                    val bodyTop = min(openY, closeY)
                    val bodyHeight = max(2f, abs(openY - closeY))
                    drawRect(
                        color = candleColor,
                        topLeft = Offset(x - (candleBodyWidth / 2f), bodyTop),
                        size = Size(candleBodyWidth, bodyHeight)
                    )
                }

                // 6. User Drawings
                for (drawing in drawings) {
                    when (drawing.type) {
                        DrawingType.HORIZONTAL -> {
                            val y = priceToY(drawing.price1)
                            if (y in 0f..mainChartHeight) {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.8f),
                                    start = Offset(0f, y),
                                    end = Offset(chartWidth, y),
                                    strokeWidth = 1.5f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                                )
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.9f),
                                    topLeft = Offset(chartWidth + 2f, y - 8f),
                                    size = Size(priceScaleWidth - 4f, 16f),
                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                )
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = String.format(Locale.US, "%.1f", drawing.price1),
                                    topLeft = Offset(chartWidth + 4f, y - 6f),
                                    style = TextStyle(color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                )
                            }
                        }
                        DrawingType.SUPPORT -> {
                            val y = priceToY(drawing.price1)
                            if (y in 0f..mainChartHeight) {
                                drawLine(
                                    color = BullishGreen,
                                    start = Offset(0f, y),
                                    end = Offset(chartWidth, y),
                                    strokeWidth = 2.2f
                                )
                                drawRoundRect(
                                    color = BullishGreen,
                                    topLeft = Offset(chartWidth + 2f, y - 9f),
                                    size = Size(priceScaleWidth - 4f, 18f),
                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                )
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = String.format(Locale.US, "%.1f", drawing.price1),
                                    topLeft = Offset(chartWidth + 4f, y - 7f),
                                    style = TextStyle(color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                )
                            }
                        }
                        DrawingType.RESISTANCE -> {
                            val y = priceToY(drawing.price1)
                            if (y in 0f..mainChartHeight) {
                                drawLine(
                                    color = BearishRed,
                                    start = Offset(0f, y),
                                    end = Offset(chartWidth, y),
                                    strokeWidth = 2.2f
                                )
                                drawRoundRect(
                                    color = BearishRed,
                                    topLeft = Offset(chartWidth + 2f, y - 9f),
                                    size = Size(priceScaleWidth - 4f, 18f),
                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                )
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = String.format(Locale.US, "%.1f", drawing.price1),
                                    topLeft = Offset(chartWidth + 4f, y - 7f),
                                    style = TextStyle(color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                )
                            }
                        }
                        DrawingType.TREND -> {
                            val idx1 = visibleCandles.indexOfFirst { it.timestamp == drawing.candleTime1 }
                            val idx2 = visibleCandles.indexOfFirst { it.timestamp == drawing.candleTime2 }
                            if (idx1 >= 0 || idx2 >= 0) {
                                val x1 = if (idx1 >= 0) indexToX(idx1) else 0f
                                val y1 = priceToY(drawing.price1)
                                val x2 = if (idx2 >= 0) indexToX(idx2) else chartWidth
                                val y2 = priceToY(drawing.price2)

                                drawLine(
                                    color = KeyLevelYellow,
                                    start = Offset(x1, y1),
                                    end = Offset(x2, y2),
                                    strokeWidth = 2.5f
                                )
                                drawCircle(color = KeyLevelYellow, radius = 4f, center = Offset(x1, y1))
                                drawCircle(color = KeyLevelYellow, radius = 4f, center = Offset(x2, y2))
                            }
                        }
                    }
                }

                // 7. Live LTP Pulsing Line
                val currentPriceY = priceToY(currentLtp)
                if (currentPriceY in 0f..mainChartHeight) {
                    drawLine(
                        color = CyanAccent.copy(alpha = pulseAlpha),
                        start = Offset(0f, currentPriceY),
                        end = Offset(chartWidth, currentPriceY),
                        strokeWidth = 1.8f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                    drawRoundRect(
                        color = CyanAccent,
                        topLeft = Offset(chartWidth + 2f, currentPriceY - 11f),
                        size = Size(priceScaleWidth - 4f, 22f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = String.format(Locale.US, "%.2f", currentLtp),
                        topLeft = Offset(chartWidth + 5f, currentPriceY - 7f),
                        style = TextStyle(color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    )
                }

                // 8. Buy/Sell Analysis Zones
                if (indicatorSettings.showAnalysisMarkers && analysisResult != null) {
                    val entry = analysisResult.conservativeEntry ?: analysisResult.normalEntry
                    if (entry != null) {
                        val entryY = priceToY(entry.entryPrice)
                        if (entryY in 0f..mainChartHeight) {
                            drawLine(
                                color = CyanAccent,
                                start = Offset(0f, entryY),
                                end = Offset(chartWidth, entryY),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                            )
                        }
                        val slY = priceToY(entry.stopLoss)
                        if (slY in 0f..mainChartHeight) {
                            drawLine(
                                color = BearishRed,
                                start = Offset(0f, slY),
                                end = Offset(chartWidth, slY),
                                strokeWidth = 2f
                            )
                            drawRect(
                                color = BearishRedBg,
                                topLeft = Offset(0f, min(entryY, slY)),
                                size = Size(chartWidth, abs(slY - entryY))
                            )
                        }
                        val t1Y = priceToY(entry.target1)
                        if (t1Y in 0f..mainChartHeight) {
                            drawLine(
                                color = BullishGreen,
                                start = Offset(0f, t1Y),
                                end = Offset(chartWidth, t1Y),
                                strokeWidth = 2f
                            )
                            drawRect(
                                color = BullishGreenBg,
                                topLeft = Offset(0f, min(entryY, t1Y)),
                                size = Size(chartWidth, abs(t1Y - entryY))
                            )
                        }
                    }
                }

                // 9. Measurement Box
                measurement?.let { m ->
                    val idx1 = visibleCandles.indexOfFirst { it.timestamp == m.startTime }
                    val idx2 = visibleCandles.indexOfFirst { it.timestamp == m.endTime }
                    if (idx1 >= 0 || idx2 >= 0) {
                        val x1 = if (idx1 >= 0) indexToX(idx1) else 0f
                        val y1 = priceToY(m.startPrice)
                        val x2 = if (idx2 >= 0) indexToX(idx2) else chartWidth
                        val y2 = priceToY(m.endPrice)

                        val boxLeft = min(x1, x2)
                        val boxTop = min(y1, y2)
                        val boxWidth = max(4f, abs(x2 - x1))
                        val boxHeight = max(4f, abs(y2 - y1))
                        val isMovePos = m.priceDiff >= 0
                        val measureColor = if (isMovePos) BullishGreen else BearishRed

                        drawRect(
                            color = measureColor.copy(alpha = 0.18f),
                            topLeft = Offset(boxLeft, boxTop),
                            size = Size(boxWidth, boxHeight)
                        )
                        drawRect(
                            color = measureColor.copy(alpha = 0.8f),
                            topLeft = Offset(boxLeft, boxTop),
                            size = Size(boxWidth, boxHeight),
                            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
                        )
                        drawLine(
                            color = measureColor,
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 2f
                        )
                        drawCircle(color = measureColor, radius = 5f, center = Offset(x1, y1))
                        drawCircle(color = measureColor, radius = 5f, center = Offset(x2, y2))
                    }
                }

                // 10. RSI Sub-Panel
                if (indicatorSettings.showRsi) {
                    val rsiTop = mainChartHeight
                    drawLine(
                        color = BgCardBorder,
                        start = Offset(0f, rsiTop),
                        end = Offset(canvasWidth, rsiTop),
                        strokeWidth = 1.5f
                    )
                    val rsi70Y = rsiTop + (rsiHeight * 0.30f)
                    drawLine(
                        color = BearishRed.copy(alpha = 0.5f),
                        start = Offset(0f, rsi70Y),
                        end = Offset(chartWidth, rsi70Y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "70",
                        topLeft = Offset(chartWidth + 6f, rsi70Y - 6f),
                        style = TextStyle(color = BearishRed.copy(alpha = 0.7f), fontSize = 8.sp)
                    )

                    val rsi30Y = rsiTop + (rsiHeight * 0.70f)
                    drawLine(
                        color = BullishGreen.copy(alpha = 0.5f),
                        start = Offset(0f, rsi30Y),
                        end = Offset(chartWidth, rsi30Y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "30",
                        topLeft = Offset(chartWidth + 6f, rsi30Y - 6f),
                        style = TextStyle(color = BullishGreen.copy(alpha = 0.7f), fontSize = 8.sp)
                    )

                    if (rsi.size >= totalCandles) {
                        val visibleRsi = rsi.subList(startIndex, endIndex)
                        val rsiPath = Path()
                        var rsiStarted = false
                        for (i in visibleRsi.indices) {
                            val rVal = visibleRsi[i] ?: continue
                            val x = indexToX(i)
                            val y = rsiTop + ((1.0 - (rVal / 100.0)).toFloat() * rsiHeight)
                            if (!rsiStarted) {
                                rsiPath.moveTo(x, y)
                                rsiStarted = true
                            } else {
                                rsiPath.lineTo(x, y)
                            }
                        }
                        if (rsiStarted) {
                            drawPath(rsiPath, color = PurpleAccent, style = Stroke(width = 2f))
                        }
                    }

                    val currentRsi = rsi.lastOrNull { it != null } ?: 50.0
                    drawText(
                        textMeasurer = textMeasurer,
                        text = String.format(Locale.US, "RSI (14): %.1f", currentRsi),
                        topLeft = Offset(6f, rsiTop + 4f),
                        style = TextStyle(color = PurpleAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                }

                // 11. Crosshair Lines
                crosshairOffset?.let { pos ->
                    if (pos.x in 0f..chartWidth && pos.y in 0f..canvasHeight) {
                        drawLine(
                            color = CrosshairColor,
                            start = Offset(pos.x, 0f),
                            end = Offset(pos.x, canvasHeight),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                        )
                        drawLine(
                            color = CrosshairColor,
                            start = Offset(0f, pos.y),
                            end = Offset(chartWidth, pos.y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                        )

                        val candleIndex = (pos.x / candleSlotWidth).toInt().coerceIn(0, visibleCandles.size - 1)
                        val hoveredCandle = visibleCandles[candleIndex]
                        selectedCandle = hoveredCandle

                        val hoveredPrice = yToPrice(pos.y)
                        drawRoundRect(
                            color = BgCardElevated,
                            topLeft = Offset(chartWidth + 2f, pos.y - 10f),
                            size = Size(priceScaleWidth - 4f, 20f),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                        drawText(
                            textMeasurer = textMeasurer,
                            text = String.format(Locale.US, "%.2f", hoveredPrice),
                            topLeft = Offset(chartWidth + 5f, pos.y - 6f),
                            style = TextStyle(color = TextPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        )

                        val hoveredTime = timeFormat.format(Date(hoveredCandle.timestamp))
                        drawRoundRect(
                            color = BgCardElevated,
                            topLeft = Offset(pos.x - 22f, mainChartHeight + 2f),
                            size = Size(44f, 16f),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                        drawText(
                            textMeasurer = textMeasurer,
                            text = hoveredTime,
                            topLeft = Offset(pos.x - 18f, mainChartHeight + 4f),
                            style = TextStyle(color = CyanAccent, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Non-live historical data banner indicator on chart canvas
            if (candles.isNotEmpty() && connectionStatus != ConnectionStatus.LIVE) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BgCardElevated.copy(alpha = 0.92f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KeyLevelYellow.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(KeyLevelYellow)
                        )
                        Text(
                            text = "LAST AVAILABLE / HISTORICAL DATA — NOT LIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = KeyLevelYellow
                        )
                    }
                }
            }

            // Empty chart state overlay when no ticks / candles received yet
            if (candles.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Live chart data unavailable",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Text(
                        text = "Awaiting Live Feed from Angel One WebSocket",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }
            }

            // Floating "Jump to Live" button when scrolled back
            if (scrollOffset > 0) {
                Surface(
                    onClick = {
                        scrollOffset = 0
                        autoFollow = true
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = CyanAccent,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 65.dp, bottom = 28.dp)
                        .testTag("jump_to_live_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                        )
                        Text(
                            text = ">>> Live",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartHeaderBar(
    symbol: String,
    currentLtp: Double,
    connectionStatus: ConnectionStatus,
    lastUpdatedTimestamp: Long,
    isFullscreen: Boolean,
    activeTool: ChartTool,
    drawingCount: Int,
    onSelectTool: (ChartTool) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetScale: () -> Unit,
    onClearDrawings: () -> Unit,
    onToggleFullscreen: () -> Unit
) {
    val isLive = connectionStatus == ConnectionStatus.LIVE

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Symbol & Price + Status Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(end = 6.dp)
        ) {
            Text(
                text = symbol,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                maxLines = 1
            )
            if (currentLtp > 0.0) {
                Text(
                    text = "₹${String.format(Locale.US, "%,.2f", currentLtp)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isLive) CyanAccent else TextSecondary,
                    maxLines = 1
                )
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = if (isLive) BullishGreenBg else KeyLevelYellowBg
                ) {
                    Text(
                        text = if (isLive) "LIVE" else "NOT LIVE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLive) BullishGreen else KeyLevelYellow,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            } else {
                Text(
                    text = "Price unavailable",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextTertiary,
                    maxLines = 1
                )
            }
        }

        // Right: Tool buttons strip (scrollable horizontally if constrained)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .weight(1f, fill = false)
                .horizontalScroll(rememberScrollState())
        ) {
            // Crosshair Tool
            ChartToolIconButton(
                icon = Icons.Default.Add,
                label = "Crosshair",
                isActive = activeTool == ChartTool.CROSSHAIR,
                onClick = { onSelectTool(ChartTool.CROSSHAIR) }
            )

            // Measure Tool
            ChartToolIconButton(
                icon = Icons.Default.Straighten,
                label = "Measure",
                isActive = activeTool == ChartTool.MEASURE,
                onClick = { onSelectTool(ChartTool.MEASURE) }
            )

            // Drawing Menu Button
            var showDrawingDropdown by remember { mutableStateOf(false) }
            Box {
                ChartToolIconButton(
                    icon = Icons.Default.Draw,
                    label = "Drawings ($drawingCount)",
                    isActive = activeTool in listOf(ChartTool.LINE_HORIZONTAL, ChartTool.LINE_SUPPORT, ChartTool.LINE_RESISTANCE, ChartTool.LINE_TREND),
                    onClick = { showDrawingDropdown = true }
                )
                DropdownMenu(
                    expanded = showDrawingDropdown,
                    onDismissRequest = { showDrawingDropdown = false },
                    modifier = Modifier.background(BgCardElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Horizontal Line", color = TextPrimary, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.HorizontalRule, contentDescription = null, tint = TextPrimary) },
                        onClick = {
                            showDrawingDropdown = false
                            onSelectTool(ChartTool.LINE_HORIZONTAL)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Support Line (Green)", color = BullishGreen, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null, tint = BullishGreen) },
                        onClick = {
                            showDrawingDropdown = false
                            onSelectTool(ChartTool.LINE_SUPPORT)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Resistance Line (Red)", color = BearishRed, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.TrendingDown, contentDescription = null, tint = BearishRed) },
                        onClick = {
                            showDrawingDropdown = false
                            onSelectTool(ChartTool.LINE_RESISTANCE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Trend Line (2-Point)", color = KeyLevelYellow, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.ShowChart, contentDescription = null, tint = KeyLevelYellow) },
                        onClick = {
                            showDrawingDropdown = false
                            onSelectTool(ChartTool.LINE_TREND)
                        }
                    )
                    if (drawingCount > 0) {
                        HorizontalDivider(color = BgCardBorder)
                        DropdownMenuItem(
                            text = { Text("Clear All Drawings", color = BearishRed, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = BearishRed) },
                            onClick = {
                                showDrawingDropdown = false
                                onClearDrawings()
                            }
                        )
                    }
                }
            }

            // Zoom In (+)
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }

            // Zoom Out (-)
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }

            // Reset Chart View
            IconButton(
                onClick = onResetScale,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Reset Scale", tint = TextSecondary, modifier = Modifier.size(17.dp))
            }

            // Fullscreen Toggle
            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("chart_fullscreen_toggle")
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                    tint = CyanAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ChartToolIconButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isActive) CyanAccent else BgCardElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) CyanAccent else BgCardBorder),
        modifier = Modifier.height(26.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.Black else TextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ToolInstructionBanner(
    tool: ChartTool,
    isMeasuring: Boolean,
    isDrawingTrend: Boolean,
    onCancel: () -> Unit
) {
    val message = when (tool) {
        ChartTool.MEASURE -> if (isMeasuring) "Tap ending candle/price point..." else "Measure: Tap starting candle/price point..."
        ChartTool.LINE_HORIZONTAL -> "Tap price level to place Horizontal Guideline"
        ChartTool.LINE_SUPPORT -> "Tap price level to place Support Line (Green)"
        ChartTool.LINE_RESISTANCE -> "Tap price level to place Resistance Line (Red)"
        ChartTool.LINE_TREND -> if (isDrawingTrend) "Tap ending point for Trend Line..." else "Trend Line: Tap starting point..."
        else -> ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .background(CyanAccentBg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(13.dp))
            Text(
                text = message,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyanAccent
            )
        }
        Text(
            text = "Cancel",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = BearishRed,
            modifier = Modifier.clickable { onCancel() }
        )
    }
}

@Composable
private fun MeasurementOverlayHUD(
    measurement: MeasurementData,
    onClear: () -> Unit
) {
    val isPos = measurement.priceDiff >= 0
    val color = if (isPos) BullishGreen else BearishRed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPos) BullishGreenBg else BearishRedBg)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Straighten, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))

            Column {
                Text(
                    text = "MOVE: ${if (isPos) "+" else ""}${String.format(Locale.US, "%.2f", measurement.priceDiff)} (${if (isPos) "+" else ""}${String.format(Locale.US, "%.2f", measurement.percentMove)}%)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = color,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${measurement.barsCount} Bars • ${String.format(Locale.US, "%.2f", measurement.points)} pts",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        IconButton(
            onClick = onClear,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Clear Measurement", tint = TextTertiary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun TimeframeSelectorBar(
    selectedTimeframe: Timeframe,
    onTimeframeSelected: (Timeframe) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(Timeframe.values()) { tf ->
            val isSelected = tf == selectedTimeframe
            val isSubSec = tf.isSubMinute
            Surface(
                onClick = { onTimeframeSelected(tf) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) CyanAccent else if (isSubSec) BgCardElevated else BgPillInactive,
                contentColor = if (isSelected) Color.Black else if (isSubSec) CyanAccent else TextSecondary,
                border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder) else null,
                modifier = Modifier
                    .height(26.dp)
                    .testTag("tf_button_${tf.label}")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = tf.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun IndicatorToggleStrip(
    settings: TradingRepository.IndicatorSettings,
    onToggle: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IndicatorPill(label = "EMA 20", active = settings.showEma20, color = CyanAccent) { onToggle("EMA20") }
        IndicatorPill(label = "EMA 50", active = settings.showEma50, color = PurpleAccent) { onToggle("EMA50") }
        IndicatorPill(label = "VWAP", active = settings.showVwap, color = KeyLevelYellow) { onToggle("VWAP") }
        IndicatorPill(label = "RSI", active = settings.showRsi, color = PurpleAccent) { onToggle("RSI") }
        IndicatorPill(label = "Levels", active = settings.showKeyLevels, color = TextPrimary) { onToggle("LEVELS") }
        IndicatorPill(label = "Zones", active = settings.showAnalysisMarkers, color = BullishGreen) { onToggle("MARKERS") }
    }
}

@Composable
private fun IndicatorPill(
    label: String,
    active: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (active) color.copy(alpha = 0.18f) else BgPillInactive,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (active) color.copy(alpha = 0.6f) else BgCardBorder),
        modifier = Modifier.height(22.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 7.dp)
        ) {
            Text(
                text = label,
                fontSize = 9.sp,
                color = if (active) color else TextTertiary,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
