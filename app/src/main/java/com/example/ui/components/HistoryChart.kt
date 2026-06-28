package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NaturalPrimary
import com.example.ui.theme.NaturalAccentBg
import com.example.ui.theme.NaturalBorder
import com.example.ui.viewmodel.DayStat

@Composable
fun HistoryChart(
    stats: List<DayStat>,
    dailyGoalMl: Int,
    modifier: Modifier = Modifier
) {
    val barColorGradient = Brush.verticalGradient(
        colors = listOf(NaturalPrimary, NaturalAccentBg)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("history_chart")
    ) {
        Text(
            text = "Weekly Hydration History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasHeight = size.height
                val canvasWidth = size.width
                
                if (stats.isEmpty()) return@Canvas
                
                // Max intake or dailyGoal to scale the chart
                val maxIntake = stats.maxOfOrNull { it.amountMl } ?: 0
                val scaleMax = (maxIntake.coerceAtLeast(dailyGoalMl) * 1.15f).coerceAtLeast(1000f)
                
                // Draw target goal horizontal dashed line
                val yGoal = canvasHeight - (dailyGoalMl / scaleMax) * canvasHeight
                drawLine(
                    color = NaturalPrimary.copy(alpha = 0.5f),
                    start = Offset(0f, yGoal),
                    end = Offset(canvasWidth, yGoal),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                )

                // Draw bars
                val totalBars = stats.size
                val spaceBetween = 24.dp.toPx()
                val availableWidth = canvasWidth - (spaceBetween * (totalBars - 1))
                val barWidth = availableWidth / totalBars

                stats.forEachIndexed { index, stat ->
                    val barHeightFraction = stat.amountMl / scaleMax
                    val barHeight = barHeightFraction * canvasHeight
                    val x = index * (barWidth + spaceBetween)
                    val y = canvasHeight - barHeight

                    // Draw shadow track
                    drawRoundRect(
                        color = NaturalBorder.copy(alpha = 0.3f),
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, canvasHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )

                    // Draw actual bar
                    if (barHeight > 0) {
                        drawRoundRect(
                            brush = barColorGradient,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Horizontal labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            stats.forEach { stat ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stat.dateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${stat.amountMl}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (stat.amountMl >= dailyGoalMl) NaturalPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
