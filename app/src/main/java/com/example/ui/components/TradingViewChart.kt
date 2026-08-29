package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnalysisResult
import com.example.data.model.Candle
import com.example.data.model.KeyLevel
import com.example.data.model.Timeframe
import com.example.data.repository.TradingRepository
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalTextApi::class)
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
    modifier: Modifier = Modifier
) {
    var visibleCandleCount by remember { mutableStateOf(40) }
    var scrollOffset by remember { mutableStateOf(0) }
    var crosshairOffset by remember { mutableStateOf<Offset?>(null) }
    var selectedCandle by remember { mutableStateOf<Candle?>(null) }

    val textMeasurer = rememberTextMeasurer()

    // Pulse animation for live price marker
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(1.dp, BgCardBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp)
            .testTag("candlestick_chart_container")
    ) {
        // Timeframe Selector Row
        TimeframeSelectorBar(
            selectedTimeframe = selectedTimeframe,
            onTimeframeSelected = {
                onTimeframeSelected(it)
                scrollOffset = 0
            }
        )

        // Indicators Toggle Strip
        IndicatorToggleStrip(
            settings = indicatorSettings,
            onToggle = onToggleIndicator
        )

        // Crosshair HUD Info Bar if crosshair active
        if (selectedCandle != null) {
            val c = selectedCandle!!
            val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .background(BgCardElevated, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateFormat.format(Date(c.timestamp)),
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "O: ${c.open}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "H: ${c.high}",
                    color = BullishGreen,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "L: ${c.low}",
                    color = BearishRed,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "C: ${c.close}",
                    color = if (c.isBullish) BullishGreen else BearishRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Vol: ${formatVolume(c.volume)}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Main Chart Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .pointerInput(candles.size) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (zoom != 1f) {
                            val newCount = (visibleCandleCount / zoom).toInt().coerceIn(15, 120)
                            visibleCandleCount = newCount
                        }
                        if (pan.x != 0f) {
                            val delta = (pan.x / 12f).toInt()
                            scrollOffset = (scrollOffset - delta).coerceIn(0, max(0, candles.size - visibleCandleCount))
                        }
                    }
                }
                .pointerInput(candles.size) {
                    detectTapGestures(
                        onTap = { offset ->
                            crosshairOffset = if (crosshairOffset == null) offset else null
                            if (crosshairOffset == null) selectedCandle = null
                        },
                        onLongPress = { offset ->
                            crosshairOffset = offset
                        }
                    )
                }
                .pointerInput(candles.size) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            crosshairOffset = change.position
                        },
                        onDragEnd = {
                            // keep crosshair for inspection
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val priceScaleWidth = 55.dp.toPx()
                val timeScaleHeight = 22.dp.toPx()
                val rsiHeight = if (indicatorSettings.showRsi) 65.dp.toPx() else 0f
                val volumeHeight = 45.dp.toPx()

                val chartWidth = canvasWidth - priceScaleWidth
                val mainChartHeight = canvasHeight - timeScaleHeight - rsiHeight

                if (candles.isEmpty()) return@Canvas

                val totalCandles = candles.size
                val endIndex = (totalCandles - scrollOffset).coerceIn(1, totalCandles)
                val startIndex = max(0, endIndex - visibleCandleCount)
                val visibleCandles = candles.subList(startIndex, endIndex)
                if (visibleCandles.isEmpty()) return@Canvas

                val minPrice = visibleCandles.minOf { it.low } * 0.999
                val maxPrice = visibleCandles.maxOf { it.high } * 1.001
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

                // 1. Draw Grid Lines
                val gridCount = 5
                for (i in 0..gridCount) {
                    val y = (mainChartHeight / gridCount) * i
                    drawLine(
                        color = GridLineColor,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1f
                    )
                    // Price labels on right scale
                    val priceAtY = yToPrice(y)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = String.format(Locale.US, "%.1f", priceAtY),
                        topLeft = Offset(chartWidth + 6f, y - 8f),
                        style = TextStyle(color = TextTertiary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    )
                }

                // 2. Draw Key Levels
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
                            // Level Tag
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

                // 3. Draw Volume Histogram at bottom of main chart
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

                // 4. Draw Technical Indicator Lines (EMA 20, EMA 50, VWAP)
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

                // 5. Draw Candlesticks
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
                        strokeWidth = 1.4f
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

                // 6. Draw Live LTP Horizontal Pulsing Line & Price Badge
                val currentPriceY = priceToY(currentLtp)
                if (currentPriceY in 0f..mainChartHeight) {
                    drawLine(
                        color = CyanAccent.copy(alpha = pulseAlpha),
                        start = Offset(0f, currentPriceY),
                        end = Offset(chartWidth, currentPriceY),
                        strokeWidth = 1.8f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                    // Right Scale Pill
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

                // 7. Draw Buy/Sell Analysis Markers & Zones
                if (indicatorSettings.showAnalysisMarkers && analysisResult != null) {
                    val entry = analysisResult.conservativeEntry ?: analysisResult.normalEntry
                    if (entry != null) {
                        // Entry line (Cyan)
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
                        // Stop loss line (Red)
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
                        // Target 1 line (Green)
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

                // 8. Draw RSI Sub-Panel if enabled
                if (indicatorSettings.showRsi) {
                    val rsiTop = mainChartHeight
                    // Separator line
                    drawLine(
                        color = BgCardBorder,
                        start = Offset(0f, rsiTop),
                        end = Offset(canvasWidth, rsiTop),
                        strokeWidth = 1.5f
                    )
                    // RSI 70 overbought line
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

                    // RSI 30 oversold line
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

                    // Draw RSI Curve
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

                    // RSI Label
                    val currentRsi = rsi.lastOrNull { it != null } ?: 50.0
                    drawText(
                        textMeasurer = textMeasurer,
                        text = String.format(Locale.US, "RSI (14): %.1f", currentRsi),
                        topLeft = Offset(6f, rsiTop + 4f),
                        style = TextStyle(color = PurpleAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                }

                // 9. Draw Crosshair if active
                crosshairOffset?.let { pos ->
                    if (pos.x in 0f..chartWidth && pos.y in 0f..canvasHeight) {
                        // Vertical line
                        drawLine(
                            color = CrosshairColor,
                            start = Offset(pos.x, 0f),
                            end = Offset(pos.x, canvasHeight),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                        )
                        // Horizontal line
                        drawLine(
                            color = CrosshairColor,
                            start = Offset(0f, pos.y),
                            end = Offset(chartWidth, pos.y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                        )

                        // Calculate candle index at pos.x
                        val candleIndex = (pos.x / candleSlotWidth).toInt().coerceIn(0, visibleCandles.size - 1)
                        val hoveredCandle = visibleCandles[candleIndex]
                        selectedCandle = hoveredCandle

                        // Price HUD at crosshair Y
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
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeframeSelectorBar(
    selectedTimeframe: Timeframe,
    onTimeframeSelected: (Timeframe) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Timeframe.values().forEach { tf ->
            val isSelected = tf == selectedTimeframe
            val isSubSec = tf.isSubMinute
            Surface(
                onClick = { onTimeframeSelected(tf) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) CyanAccent else if (isSubSec) BgCardElevated else BgPillInactive,
                contentColor = if (isSelected) Color.White else if (isSubSec) CyanAccent else TextSecondary,
                border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, BgCardBorder) else null,
                modifier = Modifier
                    .height(28.dp)
                    .testTag("tf_button_${tf.label}")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 9.dp)
                ) {
                    Text(
                        text = tf.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
        shape = RoundedCornerShape(12.dp),
        color = if (active) color.copy(alpha = 0.18f) else BgPillInactive,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (active) color.copy(alpha = 0.6f) else BgCardBorder),
        modifier = Modifier.height(24.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = if (active) color else TextTertiary,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

private fun formatVolume(volume: Long): String {
    return when {
        volume >= 10_000_000 -> String.format(Locale.US, "%.1fCr", volume / 10_000_000.0)
        volume >= 100_000 -> String.format(Locale.US, "%.1fL", volume / 100_000.0)
        volume >= 1_000 -> String.format(Locale.US, "%.1fK", volume / 1_000.0)
        else -> volume.toString()
    }
}
