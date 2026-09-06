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
import com.example.data.model.TradingSession
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
    selectedSession: TradingSession = TradingSession.TODAY,
    onSessionSelected: (TradingSession) -> Unit = {},
    diagnosticMessage: String? = null,
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
                    selectedSession = selectedSession,
                    onSessionSelected = onSessionSelected,
                    diagnosticMessage = diagnosticMessage,
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
            selectedSession = selectedSession,
            onSessionSelected = onSessionSelected,
            diagnosticMessage = diagnosticMessage,
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
    selectedSession: TradingSession,
    onSessionSelected: (TradingSession) -> Unit,
    diagnosticMessage: String?,
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
    val chartController = remember { TradingViewChartController() }

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
        val effectivePrice = if (currentLtp > 0.0) currentLtp else (candles.lastOrNull()?.close ?: 0.0)

        // Chart Header Toolbar: Title, Fullscreen, Tool Actions
        ChartHeaderBar(
            symbol = analysisResult?.symbol ?: "CHART",
            currentLtp = currentLtp,
            effectivePrice = effectivePrice,
            selectedSession = selectedSession,
            diagnosticMessage = diagnosticMessage,
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
                chartController.zoomIn()
            },
            onZoomOut = {
                chartController.zoomOut()
            },
            onResetScale = {
                chartController.fitContent()
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
                chartController.fitContent()
                scrollOffset = 0
                autoFollow = true
            },
            selectedSession = selectedSession,
            onSessionSelected = onSessionSelected
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
        val chartBoxModifier = if (isFullscreen) Modifier.weight(1f) else Modifier.height(380.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(chartBoxModifier)
        ) {
            TradingViewLightweightChartView(
                candles = candles,
                keyLevels = keyLevels,
                currentLtp = currentLtp,
                lastUpdatedTimestamp = lastUpdatedTimestamp,
                ema20 = ema20,
                ema50 = ema50,
                vwap = vwap,
                indicatorSettings = indicatorSettings,
                controller = chartController,
                modifier = Modifier.fillMaxSize()
            )

            // Empty chart state overlay when no ticks / candles received yet
            if (candles.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
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
        }
    }
}

@Composable
private fun ChartHeaderBar(
    symbol: String,
    currentLtp: Double,
    effectivePrice: Double = currentLtp,
    selectedSession: TradingSession = TradingSession.TODAY,
    diagnosticMessage: String? = null,
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
            val priceToShow = if (currentLtp > 0.0) currentLtp else effectivePrice
            if (priceToShow > 0.0) {
                Text(
                    text = "₹${String.format(Locale.US, "%,.2f", priceToShow)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isLive && currentLtp > 0.0) CyanAccent else TextSecondary,
                    maxLines = 1
                )
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = if (isLive && currentLtp > 0.0 && selectedSession == TradingSession.TODAY) BullishGreenBg else KeyLevelYellowBg
                ) {
                    val badgeLabel = when {
                        selectedSession != TradingSession.TODAY -> selectedSession.displayName.uppercase()
                        isLive && currentLtp > 0.0 -> "LIVE"
                        currentLtp > 0.0 -> "NOT LIVE"
                        else -> "CLOSE"
                    }
                    Text(
                        text = badgeLabel,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLive && currentLtp > 0.0 && selectedSession == TradingSession.TODAY) BullishGreen else KeyLevelYellow,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            } else if (diagnosticMessage != null) {
                Text(
                    text = diagnosticMessage,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = BearishRed,
                    maxLines = 1
                )
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
    onTimeframeSelected: (Timeframe) -> Unit,
    selectedSession: TradingSession = TradingSession.TODAY,
    onSessionSelected: (TradingSession) -> Unit = {}
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Trading session selector chips
        items(TradingSession.values()) { sess ->
            val isSelected = sess == selectedSession
            Surface(
                onClick = { onSessionSelected(sess) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) PurpleAccent.copy(alpha = 0.25f) else BgPillInactive,
                contentColor = if (isSelected) PurpleAccent else TextSecondary,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PurpleAccent else BgCardBorder),
                modifier = Modifier
                    .height(26.dp)
                    .testTag("session_button_${sess.name}")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = sess.displayName,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(1.dp)
                    .height(16.dp)
                    .background(BgCardBorder)
            )
        }

        // Timeframe chips
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
