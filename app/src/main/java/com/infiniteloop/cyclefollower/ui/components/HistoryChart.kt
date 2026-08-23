package com.infiniteloop.cyclefollower.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Cycle lengths as bars against her own average, so an odd month is visible rather than buried
 * in a variability figure. The app already knew all of this; it just printed it as two numbers.
 */
@Composable
fun CycleHistoryChart(lengths: List<Int>, average: Int, variability: Int) {
    if (lengths.isEmpty()) {
        Text(
            "Log a second period and this fills in.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val dark = isSystemInDarkTheme()
    val normal = MaterialTheme.colorScheme.primary
    val odd = if (dark) Color(0xFFF0A97C) else Color(0xFFB35309)
    // Anything more than three days off her own average is worth pointing at.
    val oddThreshold = maxOf(3, variability + 1)

    val floor = (lengths.min() - 2).coerceAtLeast(18)
    val ceiling = (lengths.max() + 2).coerceAtMost(50)
    val span = (ceiling - floor).coerceAtLeast(1)
    val chartHeight = 132.dp

    Column {
        Box(Modifier.fillMaxWidth().height(chartHeight)) {
            // The average band sits behind the bars.
            val bandTop = ((ceiling - (average + variability)).toFloat() / span).coerceIn(0f, 1f)
            val bandBottom = ((ceiling - (average - variability)).toFloat() / span).coerceIn(0f, 1f)
            Column(Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(chartHeight * bandTop))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((chartHeight * (bandBottom - bandTop)).coerceAtLeast(2.dp))
                        .background(normal.copy(alpha = 0.12f), RoundedCornerShape(3.dp)),
                )
            }

            Row(
                Modifier.fillMaxWidth().height(chartHeight),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                lengths.forEach { length ->
                    val fraction = ((length - floor).toFloat() / span).coerceIn(0.06f, 1f)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(chartHeight * fraction)
                            .background(
                                if (abs(length - average) >= oddThreshold) odd else normal,
                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                            ),
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            lengths.forEach { length ->
                Text(
                    length.toString(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(normal, "Within her normal")
            LegendDot(odd, "Unusual for her")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Band is her average of $average days, give or take $variability.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Oldest first, so the chart reads left to right in time. */
fun cycleLengthSeries(gaps: List<Int>, limit: Int = 12): List<Int> = gaps.take(limit).reversed()

fun averageOf(values: List<Int>): Int = if (values.isEmpty()) 0 else values.average().roundToInt()
