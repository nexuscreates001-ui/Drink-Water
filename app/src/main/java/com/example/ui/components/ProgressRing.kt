package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NaturalPrimary
import com.example.ui.theme.NaturalAccentBg
import com.example.ui.theme.NaturalBorder

@Composable
fun ProgressRing(
    progress: Float, // 0.0 to 1.0+
    currentMl: Int,
    targetMl: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000),
        label = "HydrationProgressRing"
    )

    val percentage = (progress * 100).toInt()
    val isGoalMet = currentMl >= targetMl

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("progress_ring"),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing shadows and rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            
            // Draw background track
            drawCircle(
                color = NaturalBorder.copy(alpha = 0.4f),
                style = Stroke(width = strokeWidth)
            )

            // Draw progress arc with gradient brush
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        NaturalPrimary,
                        NaturalAccentBg,
                        NaturalPrimary
                    )
                ),
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Inner textual presentation
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isGoalMet) "Goal Met! 🎉" else "Hydrated",
                style = MaterialTheme.typography.labelLarge,
                color = if (isGoalMet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("progress_percentage_text")
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "$currentMl / $targetMl ml",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("progress_fraction_text")
            )
            
            if (!isGoalMet) {
                val left = (targetMl - currentMl).coerceAtLeast(0)
                Text(
                    text = "$left ml left",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                Text(
                    text = "Awesome Job!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
