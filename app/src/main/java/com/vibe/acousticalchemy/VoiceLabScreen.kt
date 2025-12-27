package com.vibe.acousticalchemy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceLabScreen(
    voiceManager: VoiceManager,
    isPlaying: Boolean,
    onSpeak: (String, FloatArray) -> Unit
) {
    // Probe Position (Logical 2D: x [-1,1], y [-1,1])
    var probe by remember { mutableStateOf(Offset(0f, 0f)) } 
    var calculatedStyle by remember { mutableStateOf(FloatArray(256)) }
    var inputText by remember { mutableStateOf("Call me Ishmael.") }

    // Filters
    var filterMale by remember { mutableStateOf(true) }
    var filterFemale by remember { mutableStateOf(true) }
    var filterAmerican by remember { mutableStateOf(true) }
    var filterBritish by remember { mutableStateOf(true) }
    
    val allStars = VoiceRegistry.voices
    
    // Filtered Stars
    val activeStars by remember(filterMale, filterFemale, filterAmerican, filterBritish) {
        derivedStateOf {
            allStars.filter { star ->
                val g = if (star.gender == "Female") filterFemale else filterMale
                val a = when(star.ethnicity) {
                    "American" -> filterAmerican
                    "British" -> filterBritish
                    else -> true 
                }
                g && a
            }
        }
    }

    val loadedVoices = remember {
        allStars.associate { it.id to voiceManager.loadVoice(it.id) }
    }

    // Animation state
    val infiniteTransition = rememberInfiniteTransition(label = "StarPulse")
    val pulseAnim = if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Pulse"
        ).value
    } else {
        1.0f
    }

    // Update Blend Logic
    fun updateBlend(p: Offset) {
        if (activeStars.isEmpty()) return

        val distances = activeStars.map { star ->
            val dist = sqrt(
                (p.x - star.position.x).pow(2) + 
                (p.y - star.position.y).pow(2) + 
                (0f - star.position.z).pow(2) 
            )
            star to dist
        }.sortedBy { it.second }.take(5)

        val targetVectors = mutableListOf<FloatArray>()
        val targetWeights = mutableListOf<Float>()

        distances.forEach { (star, dist) ->
            loadedVoices[star.id]?.let { vec ->
                targetVectors.add(vec)
                targetWeights.add(1f / (dist + 0.01f))
            }
        }
        
        if (targetVectors.isNotEmpty()) {
            calculatedStyle = voiceManager.weightedBlend(targetVectors.toList(), targetWeights.toList())
        }
    }

    LaunchedEffect(probe, activeStars) {
        updateBlend(probe)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF050510)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Filter Header with Chips
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text("Nebula Voice Lab", color = Color.Cyan, style = MaterialTheme.typography.headlineSmall)
            
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterFemale,
                    onClick = { filterFemale = !filterFemale },
                    label = { Text("Female") },
                    leadingIcon = if (filterFemale) { { Icon(Icons.Default.Check, null) } } else null
                )
                FilterChip(
                    selected = filterMale,
                    onClick = { filterMale = !filterMale },
                    label = { Text("Male") },
                    leadingIcon = if (filterMale) { { Icon(Icons.Default.Check, null) } } else null
                )
                FilterChip(
                    selected = filterAmerican,
                    onClick = { filterAmerican = !filterAmerican },
                    label = { Text("American") },
                    leadingIcon = if (filterAmerican) { { Icon(Icons.Default.Check, null) } } else null
                )
                FilterChip(
                    selected = filterBritish,
                    onClick = { filterBritish = !filterBritish },
                    label = { Text("British") },
                    leadingIcon = if (filterBritish) { { Icon(Icons.Default.Check, null) } } else null
                )
            }
        }
        
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .background(Color.Black)
        ) {
            val density = LocalDensity.current
            val w = with(density) { maxWidth.toPx() }
            val h = with(density) { maxHeight.toPx() }
            
            Canvas(modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        
                        // Scale drag to normalized coords [-1, 1]
                        val dx = (dragAmount.x / w) * 2f 
                        val dy = -(dragAmount.y / h) * 2f 
                        
                        val newX = (probe.x + dx).coerceIn(-1f, 1f)
                        val newY = (probe.y + dy).coerceIn(-1f, 1f)
                        probe = Offset(newX, newY)
                    }
                }
            ) {
                val centerX = w / 2
                val centerY = h / 2
                
                // Logic [-1, 1] maps to -> [w * 0.85, h * 0.85] bounds
                val scaleX = (w * 0.85f) / 2f
                val scaleY = (h * 0.85f) / 2f

                fun toCanvas(p: Offset): Offset {
                    return Offset(
                        centerX + p.x * scaleX,
                        centerY - p.y * scaleY
                    )
                }

                // Draw Stars
                val paint = Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    textSize = 32f
                    color = android.graphics.Color.YELLOW
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                // Calculate closest star for Label
                var closestStar: VoiceArchetype? = null
                var closestDist = Float.MAX_VALUE
                
                activeStars.forEach { star ->
                    // 2D Projection of star
                    val starP = Offset(star.position.x, star.position.y)
                    val pos = toCanvas(starP)
                    
                    val z = star.position.z 
                    
                    val pDist = (probe - starP).getDistance()
                    if (pDist < closestDist) {
                        closestDist = pDist
                        closestStar = star
                    }

                    // Visuals
                    val radius = (10f * (1f - (z * 0.3f))) * (if (isPlaying) pulseAnim else 1f)
                    val alpha = (1f - ((z + 1f) / 4f)).coerceIn(0.2f, 1f)
                    
                    drawCircle(Color.Cyan.copy(alpha = alpha), radius = radius, center = pos)
                }
                
                // Always draw closer star label high contrast
                closestStar?.let { star ->
                     val starP = Offset(star.position.x, star.position.y)
                     val pos = toCanvas(starP)
                     drawContext.canvas.nativeCanvas.drawText(
                        star.name.uppercase(),
                        pos.x,
                        pos.y - 45f,
                        paint
                     )
                }

                // Draw Probe
                val pProbe = toCanvas(probe)
                drawCircle(Color.White, radius = 15f, center = pProbe)
                drawCircle(Color.White.copy(alpha = 0.3f), radius = 35f * (if(isPlaying) pulseAnim else 1f), center = pProbe)
            }
        }
        
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Enter text to speak") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.Cyan,
                unfocusedBorderColor = Color.Gray
            )
        )
        
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { onSpeak(inputText, calculatedStyle) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan, contentColor = Color.Black)
        ) {
            Text("Synthesize")
        }
    }
}
