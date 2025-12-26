package com.vibe.acousticalchemy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import kotlin.math.pow
import kotlin.math.sqrt

// Star Coordinates (Logical [-1.0, 1.0])
val starAdam = Offset(0.5f, 0.8f)   
val starBella = Offset(-0.7f, -0.4f)
val starSky = Offset(0.7f, -0.3f)

@Composable
fun VoiceLabScreen(
    voiceManager: VoiceManager,
    isPlaying: Boolean,
    onSpeakMobyDick: (FloatArray) -> Unit
) {
    // Current Probe Logic Position [-1, 1]
    var probePos by remember { mutableStateOf(Offset(0f, 0f)) } 
    var calculatedStyle by remember { mutableStateOf(FloatArray(256)) }

    // Load voices
    val voiceAdam = remember { voiceManager.loadVoice("am_adam.bin") }
    val voiceBella = remember { voiceManager.loadVoice("af_bella.bin") }
    val voiceSky   = remember { voiceManager.loadVoice("af_sky.bin") } 

    // Animation state for Probe Pulse (active when playing)
    val infiniteTransition = rememberInfiniteTransition(label = "ProbePulse")
    
    // Pulse Logic:
    val pulseAnim = if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ProbePulseAnim"
        ).value
    } else {
        1.0f
    }

    // Helper to update blend based on logical probe position
    fun updateBlend(pos: Offset) {
        val dAdam = distance(pos, starAdam)
        val dBella = distance(pos, starBella)
        val dSky = distance(pos, starSky)

        val wAdam = 1f / (dAdam + 0.1f)
        val wBella = 1f / (dBella + 0.1f)
        val wSky = 1f / (dSky + 0.1f)

        calculatedStyle = voiceManager.weightedBlend(
            listOf(voiceAdam, voiceBella, voiceSky),
            listOf(wAdam, wBella, wSky)
        )
    }

    LaunchedEffect(probePos) {
        updateBlend(probePos)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Nebula Voice Lab", color = Color.White)
        
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val w = maxWidth.value 
            
            Canvas(modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val width = size.width
                        val height = size.height
                        val scale = minOf(width, height) / 2 * 0.9f
                        
                        // Convert dragAmount (pixels) to Logical Delta
                        val dx = dragAmount.x / scale
                        val dy = -dragAmount.y / scale // Inverse Y
                        
                        // Update Probe
                        var newX = probePos.x + dx
                        var newY = probePos.y + dy
                        
                        // Clamp to [-1, 1]
                        newX = newX.coerceIn(-1.0f, 1.0f)
                        newY = newY.coerceIn(-1.0f, 1.0f)
                        
                        probePos = Offset(newX, newY)
                    }
                }
            ) {
                // Draw Loop
                val width = size.width
                val height = size.height
                val centerX = width / 2
                val centerY = height / 2
                val scale = minOf(width, height) / 2 * 0.9f

                fun logicToCanvas(logic: Offset): Offset {
                    val x = centerX + logic.x * scale
                    val y = centerY - logic.y * scale 
                    return Offset(x, y)
                }

                // Star Coords
                val pAdam = logicToCanvas(starAdam)
                val pBella = logicToCanvas(starBella)
                val pSky = logicToCanvas(starSky)
                
                val pProbe = logicToCanvas(probePos)

                // Background
                drawRect(Color(0xFF101020)) // Deep Dark Blue/Black

                // Connectors
                drawLine(Color.Cyan.copy(alpha=0.5f), pProbe, pAdam, strokeWidth = 3f)
                drawLine(Color.Magenta.copy(alpha=0.5f), pProbe, pBella, strokeWidth = 3f)
                drawLine(Color.Yellow.copy(alpha=0.5f), pProbe, pSky, strokeWidth = 3f)

                // Stars
                drawCircle(Color.Cyan, radius = 25f, center = pAdam)
                drawCircle(Color.Magenta, radius = 25f, center = pBella)
                drawCircle(Color.Yellow, radius = 25f, center = pSky)

                // Probe
                drawCircle(Color.White.copy(alpha = pulseAnim), radius = 40f, center = pProbe)
                // Glow Ring
                drawCircle(Color.White.copy(alpha = pulseAnim * 0.5f), radius = 60f, center = pProbe)
            }
        }

        Button(onClick = { onSpeakMobyDick(calculatedStyle) }) {
            Text("Listen to First Page")
        }
    }
}

// Helper
fun distance(a: Offset, b: Offset): Float {
    return sqrt((a.x - b.x).pow(2) + (a.y - b.y).pow(2))
}
