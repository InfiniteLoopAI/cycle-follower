package com.infiniteloop.cyclefollower.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infiniteloop.cyclefollower.domain.CycleStatus
import com.infiniteloop.cyclefollower.domain.Level
import com.infiniteloop.cyclefollower.ui.theme.phasePalette
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
fun BulletList(
    items: List<String>,
    modifier: Modifier = Modifier,
    marker: String = "•",
    markerColor: Color = MaterialTheme.colorScheme.primary,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = marker,
                    color = markerColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(20.dp),
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

enum class CalloutTone { INFO, WARNING, GOLDEN }

@Composable
fun Callout(text: String, tone: CalloutTone = CalloutTone.INFO, title: String? = null) {
    val (bg, fg) = when (tone) {
        CalloutTone.INFO -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        CalloutTone.WARNING -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        CalloutTone.GOLDEN -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Box(
        Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = fg)
                Spacer(Modifier.height(6.dp))
            }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = fg)
        }
    }
}

@Composable
fun LevelMeter(label: String, level: Level, accent: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                level.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(5) { index ->
                val filled = index < level.score
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .background(
                            if (filled) accent else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(3.dp),
                        ),
                )
            }
        }
    }
}

@Composable
fun Pill(text: String, background: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
    }
}

/**
 * The whole cycle as a ring: one arc per phase, sized by how many days it covers, with a marker
 * showing where today sits. Reading the shape of her month is faster than reading a number.
 */
@Composable
fun CycleRing(
    status: CycleStatus,
    centerTop: String,
    centerMain: String,
    centerBottom: String,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val markerRing = MaterialTheme.colorScheme.surface

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(230.dp)) {
            val strokeWidth = 26.dp.toPx()
            val diameter = minOf(size.width, size.height) - strokeWidth - 8.dp.toPx()
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val radius = diameter / 2f
            val center = Offset(topLeft.x + radius, topLeft.y + radius)
            val total = status.cycleLength.toFloat().coerceAtLeast(1f)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                topLeft = topLeft,
                size = arcSize,
            )

            status.timeline.forEach { span ->
                val start = ((span.startDay - 1) / total).coerceIn(0f, 1f)
                val end = (span.endDay / total).coerceIn(0f, 1f)
                val sweep = ((end - start) * 360f - 2f).coerceAtLeast(0f)
                if (sweep <= 0f) return@forEach
                drawArc(
                    color = phasePalette(span.phase, dark).accent,
                    startAngle = -90f + start * 360f + 1f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth),
                    topLeft = topLeft,
                    size = arcSize,
                )
            }

            val progress = ((status.cycleDay - 0.5f) / total).coerceIn(0f, 1f)
            val angleRad = Math.toRadians((-90f + progress * 360f).toDouble())
            val markerCenter = Offset(
                center.x + (radius * cos(angleRad)).toFloat(),
                center.y + (radius * sin(angleRad)).toFloat(),
            )
            drawCircle(color = markerRing, radius = strokeWidth * 0.42f, center = markerCenter)
            drawCircle(
                color = phasePalette(status.phase, dark).accent,
                radius = strokeWidth * 0.26f,
                center = markerCenter,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerTop,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                centerMain,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                centerBottom,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = phasePalette(status.phase, dark).accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp),
            )
        }
    }
}

@Composable
fun PhaseLegendRow(status: CycleStatus) {
    val dark = isSystemInDarkTheme()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        status.timeline.forEach { span ->
            val palette = phasePalette(span.phase, dark)
            val isNow = status.cycleDay in span
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (isNow) palette.container else Color.Transparent,
                        RoundedCornerShape(12.dp),
                    )
                    .then(
                        if (isNow) Modifier.border(1.dp, palette.accent, RoundedCornerShape(12.dp))
                        else Modifier,
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(palette.accent, RoundedCornerShape(50)),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    span.phase.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (span.length == 1) "day ${span.startDay}" else "days ${span.startDay}-${span.endDay}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
