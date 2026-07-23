package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.data.HydrationLog
import com.example.ui.HydrationViewModel
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TurquoiseAccent
import com.example.ui.theme.TurquoisePrimary
import com.example.ui.theme.TurquoiseSecondary
import com.example.ui.theme.TurquoiseTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Simple in-memory ViewModel setup without database persistence
        val viewModel = ViewModelProvider(this)[HydrationViewModel::class.java]

        setContent {
            MyApplicationTheme {
                HydrationAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HydrationAppScreen(
    viewModel: HydrationViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayTotal by viewModel.todayTotal.collectAsState()
    val dailyGoal by viewModel.dailyGoal.collectAsState()
    val todayLogs by viewModel.todayLogs.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()

    // Dialog state for reset confirmation
    var showResetDialog by remember { mutableStateOf(false) }

    // Single Toast management to avoid queuing up multiple toasts when tapped rapidly
    var currentToast by remember { mutableStateOf<Toast?>(null) }
    val showToast = { message: String ->
        currentToast?.cancel()
        val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        currentToast = toast
        toast.show()
    }

    // Celebratory congratulations states
    var showCongratulations by remember { mutableStateOf(false) }
    var hasShownCongratulations by remember { mutableStateOf(false) }

    // Smart logic to trigger the congratulations pop-up once when target is hit
    LaunchedEffect(todayTotal) {
        if (todayTotal >= dailyGoal && todayTotal > 0) {
            if (!hasShownCongratulations) {
                showCongratulations = true
                hasShownCongratulations = true
            }
        } else {
            // Reset if they drop below the goal (e.g. by deleting a log)
            hasShownCongratulations = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("hydration_screen_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header with App Title & Subtle Date Display
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.WaterDrop,
                            contentDescription = "Icône d'eau",
                            tint = TurquoisePrimary,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(end = 4.dp)
                        )
                        Text(
                            text = "AquaTrack",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = TurquoisePrimary,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }

                    // Friendly Current Date label in French
                    val formattedDate = remember(currentDate) {
                        try {
                            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val date = inputFormat.parse(currentDate)
                            val outputFormat = SimpleDateFormat("EEEE d MMMM", Locale.FRENCH)
                            date?.let { outputFormat.format(it).replaceFirstChar { char -> char.uppercase() } } ?: currentDate
                        } catch (e: Exception) {
                            currentDate
                        }
                    }

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        ),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }

            // 2. Custom Animated Progress Circle Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Custom drawn circular tracker with text in middle
                        HydrationProgressCircle(
                            current = todayTotal,
                            goal = dailyGoal,
                            modifier = Modifier
                                .size(240.dp)
                                .padding(12.dp)
                                .testTag("progress_circle")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Motivational message card based on target progress
                        val progressPercentage = if (dailyGoal > 0) (todayTotal * 100) / dailyGoal else 0
                        val motivationalText = when {
                            progressPercentage == 0 -> "Restez hydraté aujourd'hui ! 💧"
                            progressPercentage < 25 -> "Bon début ! Un verre à la fois. 👍"
                            progressPercentage < 50 -> "Vous avancez bien ! Continuez. 🚀"
                            progressPercentage < 75 -> "Plus de la moitié ! Excellent ! 🌟"
                            progressPercentage < 100 -> "Presque au but ! Encore un effort ! 💪"
                            else -> "Objectif atteint ! Travail fantastique ! 🎉🏆"
                        }

                        Text(
                            text = motivationalText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (progressPercentage >= 100) TurquoisePrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            // 3. Persistent Celebration Banner when objective is met
            item {
                AnimatedVisibility(
                    visible = todayTotal >= dailyGoal,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = TurquoisePrimary.copy(alpha = 0.12f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(listOf(TurquoisePrimary, TurquoiseSecondary))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🏆",
                                fontSize = 32.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Félicitations !",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TurquoisePrimary
                                    )
                                )
                                Text(
                                    text = "Objectif d'hydratation atteint ! Excellent travail d'hydratation aujourd'hui ! 🎉💧",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 4. Primary Action Buttons (Capped to Goal & No clutter)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Main "+ 250 ml" Button with liquid wave and splash effect
                    LiquidAddButton(
                        onClick = {
                            if (todayTotal >= dailyGoal) {
                                showToast("Limite autorisée de 2L déjà atteinte ! 🎉")
                            } else {
                                viewModel.addWater(250)
                                showToast("+250 ml ajoutés")
                            }
                        },
                        modifier = Modifier.weight(2f)
                    )

                    // Reset Button
                    Button(
                        onClick = { showResetDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("reset_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Réinitialiser",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // 5. Today's History Section Header
            item {
                Text(
                    text = "Historique d'aujourd'hui",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Start
                )
            }

            // 6. History Logs List inside the flat LazyColumn to allow scrolling through
            if (todayLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalDrink,
                                    contentDescription = "Aucun log",
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Aucune boisson enregistrée.\nBuvez de l'eau pour commencer !",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        lineHeight = 18.sp
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                items(todayLogs, key = { it.id }) { log ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                    ) {
                        HistoryLogItem(
                            log = log,
                            onDelete = { viewModel.deleteLog(log) }
                        )
                    }
                }
            }
        }
    }

    // Material 3 Custom Reset Confirmation Dialog
    if (showResetDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Réinitialiser l'hydratation ?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Voulez-vous vraiment effacer toutes les entrées d'eau pour aujourd'hui ? Cette action est irréversible.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToday()
                        showResetDialog = false
                        showToast("Hydratation réinitialisée")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Réinitialiser")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetDialog = false }
                ) {
                    Text("Annuler")
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Gorgeous modal congratulations dialog when target goal is met
    if (showCongratulations) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCongratulations = false },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🎉 Objectif Atteint ! 🏆",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TurquoisePrimary
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WaterDrop,
                        contentDescription = null,
                        tint = TurquoisePrimary,
                        modifier = Modifier
                            .size(72.dp)
                            .padding(bottom = 12.dp)
                    )
                    Text(
                        text = "Félicitations ! Vous avez atteint votre objectif d'hydratation de 2L pour aujourd'hui. 💧✨\n\nVotre corps vous remercie ! Restez en forme et continuez ainsi !",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { showCongratulations = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TurquoiseAccent,
                            contentColor = Color(0xFF00363A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Text(
                            text = "Génial ! 👍",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
fun HydrationProgressCircle(
    current: Int,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (current.toFloat() / goal.toFloat()).coerceIn(0f, 1f) else 0f
    
    // Smooth animated water fill level
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "animated_water_level"
    )

    // Continuous liquid wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_wave_transition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Dynamic glow color
    val glowColor by animateColorAsState(
        targetValue = if (current >= goal) TurquoisePrimary else TurquoiseSecondary,
        label = "animated_glow_color"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val radius = diameter / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Inner clipping circle for liquid water inside the ring
            val innerRadius = radius - (strokeWidth / 2f)
            val liquidClipPath = Path().apply {
                addOval(Rect(center = center, radius = innerRadius))
            }

            // 1. Draw Liquid Water Fill inside the circle if progress > 0
            if (animatedProgress > 0.001f) {
                clipPath(liquidClipPath) {
                    val liquidHeight = (innerRadius * 2f) * animatedProgress
                    val waterSurfaceY = (center.y + innerRadius) - liquidHeight

                    // Wave 1: Background liquid wave (slightly lighter translucent blue/turquoise)
                    val wave1Path = Path().apply {
                        moveTo(center.x - innerRadius - 10f, center.y + innerRadius + 10f)
                        val stepCount = 50
                        val widthSpan = (innerRadius * 2f) + 20f
                        val startX = center.x - innerRadius - 10f
                        for (i in 0..stepCount) {
                            val x = startX + (widthSpan * i / stepCount)
                            val normalizedX = (i.toFloat() / stepCount) * 2f * PI.toFloat()
                            val y = waterSurfaceY + sin(normalizedX + wavePhase) * 6.dp.toPx()
                            lineTo(x, y)
                        }
                        lineTo(center.x + innerRadius + 10f, center.y + innerRadius + 10f)
                        close()
                    }
                    drawPath(
                        path = wave1Path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                TurquoiseSecondary.copy(alpha = 0.35f),
                                TurquoisePrimary.copy(alpha = 0.55f)
                            )
                        )
                    )

                    // Wave 2: Foreground liquid wave (deeper blue/turquoise offset phase)
                    val wave2Path = Path().apply {
                        moveTo(center.x - innerRadius - 10f, center.y + innerRadius + 10f)
                        val stepCount = 50
                        val widthSpan = (innerRadius * 2f) + 20f
                        val startX = center.x - innerRadius - 10f
                        for (i in 0..stepCount) {
                            val x = startX + (widthSpan * i / stepCount)
                            val normalizedX = (i.toFloat() / stepCount) * 2.5f * PI.toFloat()
                            val y = waterSurfaceY + sin(normalizedX + wavePhase + (PI.toFloat() / 2f)) * 5.dp.toPx()
                            lineTo(x, y)
                        }
                        lineTo(center.x + innerRadius + 10f, center.y + innerRadius + 10f)
                        close()
                    }
                    drawPath(
                        path = wave2Path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                TurquoisePrimary.copy(alpha = 0.65f),
                                TurquoiseAccent.copy(alpha = 0.85f)
                            )
                        )
                    )
                }
            }

            // 2. Draw outer track background circle
            drawCircle(
                color = TurquoiseSecondary.copy(alpha = 0.12f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 3. Draw active progress arc around circle
            val sweepAngle = animatedProgress * 360f
            drawArc(
                color = glowColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 4. Subtle glowing outer ring when target is hit (100%)
            if (current >= goal) {
                drawCircle(
                    color = TurquoisePrimary.copy(alpha = 0.25f),
                    radius = radius + strokeWidth,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Inside Circle Text & Indicators
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.WaterDrop,
                contentDescription = null,
                tint = if (animatedProgress > 0.55f) Color.White else glowColor,
                modifier = Modifier
                    .size(44.dp)
                    .scale(if (current >= goal) 1.25f else 1.0f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${String.format("%,d", current).replace(',', ' ')} ml",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (animatedProgress > 0.55f) Color.White else TurquoisePrimary,
                    letterSpacing = (-0.5).sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "sur ${String.format("%,d", goal).replace(',', ' ')} ml",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (animatedProgress > 0.55f) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            val percent = if (goal > 0) (current * 100) / goal else 0
            Text(
                text = "$percent %",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (animatedProgress > 0.55f) Color.White else glowColor
                )
            )
        }
    }
}

@Composable
fun LiquidAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val splashProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    val buttonScale = remember { androidx.compose.animation.core.Animatable(1f) }

    val handleClick = {
        coroutineScope.launch {
            // Button bounce physics
            launch {
                buttonScale.animateTo(0.92f, tween(60))
                buttonScale.animateTo(
                    1f,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            // Liquid splash wave effect
            launch {
                splashProgress.snapTo(0f)
                splashProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                )
            }
        }
        onClick()
    }

    Button(
        onClick = handleClick,
        modifier = modifier
            .scale(buttonScale.value)
            .height(56.dp)
            .testTag("add_250_button"),
        colors = ButtonDefaults.buttonColors(
            containerColor = TurquoiseAccent,
            contentColor = Color(0xFF00363A)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Liquid wave and droplet splash canvas inside button
            if (splashProgress.value in 0.001f..0.999f) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val p = splashProgress.value
                    val w = size.width
                    val h = size.height

                    // Expanding radial liquid wave
                    val radius = (w * 0.75f) * p
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.6f * (1f - p)),
                                TurquoisePrimary.copy(alpha = 0.4f * (1f - p)),
                                Color.Transparent
                            ),
                            center = Offset(w / 2f, h / 2f),
                            radius = radius.coerceAtLeast(1f)
                        ),
                        center = Offset(w / 2f, h / 2f),
                        radius = radius
                    )

                    // Liquid splash droplets radiating outward
                    val dropletCount = 8
                    for (i in 0 until dropletCount) {
                        val angle = (i.toFloat() / dropletCount) * 2f * PI.toFloat()
                        val distance = p * (w * 0.4f)
                        val dx = (w / 2f) + cos(angle) * distance
                        val dy = (h / 2f) + sin(angle) * (h * 0.35f * p)
                        val dropRadius = (1f - p) * 6.dp.toPx()
                        if (dropRadius > 0f) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.9f * (1f - p)),
                                radius = dropRadius,
                                center = Offset(dx, dy)
                            )
                        }
                    }
                }
            }

            // Button label & icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Ajouter",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ajouter 250 ml",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}

@Composable
fun HistoryLogItem(
    log: HydrationLog,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDeleting by remember { mutableStateOf(false) }
    val deleteAnimProgress = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(isDeleting) {
        if (isDeleting) {
            deleteAnimProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
            onDelete()
        }
    }

    val progress = deleteAnimProgress.value
    val formattedTime = remember(log.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.timestamp))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = (1f - progress).coerceIn(0f, 1f)
                scaleX = (1f - progress * 0.15f).coerceIn(0.5f, 1f)
                scaleY = (1f - progress * 0.4f).coerceIn(0.1f, 1f)
            }
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = TurquoisePrimary.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalDrink,
                                contentDescription = null,
                                tint = TurquoisePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${log.amountMl} ml",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Ajouté à $formattedTime",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                // Quick Delete button with proper touch target size
                IconButton(
                    onClick = {
                        if (!isDeleting) {
                            isDeleting = true
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_log_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Supprimer l'entrée",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Liquid dissolve / splash overlay on delete
            if (isDeleting && progress in 0.001f..0.999f) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height

                    // Liquid wave drain overlay
                    val drainX = w * (1f - progress)
                    val wavePath = Path().apply {
                        moveTo(w, 0f)
                        lineTo(drainX, 0f)
                        val steps = 25
                        for (i in 0..steps) {
                            val py = (h / steps) * i
                            val waveX = drainX - sin((i / steps.toFloat()) * 3f * PI.toFloat() + progress * 8f) * 12.dp.toPx()
                            lineTo(waveX, py)
                        }
                        lineTo(w, h)
                        close()
                    }

                    drawPath(
                        path = wavePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                TurquoiseAccent.copy(alpha = 0.7f * (1f - progress)),
                                TurquoisePrimary.copy(alpha = 0.9f * (1f - progress))
                            )
                        )
                    )

                    // Water splash droplets bursting from delete button area
                    val dropCount = 10
                    val originX = w * 0.9f
                    val originY = h * 0.5f
                    for (i in 0 until dropCount) {
                        val angle = (i.toFloat() / dropCount) * 2f * PI.toFloat()
                        val dist = progress * 70.dp.toPx()
                        val dx = originX + cos(angle) * dist
                        val dy = originY + sin(angle) * dist
                        val radius = (1f - progress) * 6.dp.toPx()
                        if (radius > 0f) {
                            drawCircle(
                                color = TurquoiseAccent.copy(alpha = 0.95f * (1f - progress)),
                                radius = radius,
                                center = Offset(dx, dy)
                            )
                        }
                    }
                }
            }
        }
    }
}
