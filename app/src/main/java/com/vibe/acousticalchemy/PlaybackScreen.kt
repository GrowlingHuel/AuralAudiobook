package com.vibe.acousticalchemy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import kotlin.math.*

@Composable
fun PlaybackScreen(
    currentBookTitle: String,
    visibleSentences: List<String>,
    activeIndex: Int,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onRewind: () -> Unit,
    onBack: () -> Unit
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    // Auto-Scroll Logic
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && activeIndex < visibleSentences.size) {
            // center the item
            listState.animateScrollToItem(activeIndex, scrollOffset = -200) 
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050510))) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                    Text("< Back", color = Color.Gray)
                }
            }

            // Title
            Text(
                text = currentBookTitle.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = Color.Cyan,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Visual Teleprompter (Windowed)
            val windowSize = 100
            val startWindow = max(0, activeIndex - windowSize / 2)
            val endWindow = min(visibleSentences.size, startWindow + windowSize)
            val windowedSentences = visibleSentences.subList(startWindow, endWindow)
            val windowOffset = startWindow

            // We must adjust activeIndex relative to the window for the LazyColumn
            val relativeActiveIndex = activeIndex - windowOffset

            androidx.compose.foundation.lazy.LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 150.dp) // padding to focus center
            ) {
                items(windowedSentences.size) { index ->
                    val globalIndex = windowOffset + index
                    val isActive = globalIndex == activeIndex
                    val text = windowedSentences[index]
                    
                    Text(
                        text = text,
                        style = if (isActive) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                        color = if (isActive) Color.Cyan else Color.Gray.copy(alpha = 0.5f),
                        fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
            }
            
            // Force scroll when activeIndex changes
            LaunchedEffect(activeIndex) {
                if (relativeActiveIndex in 0 until windowedSentences.size) {
                    listState.animateScrollToItem(relativeActiveIndex, scrollOffset = -200)
                }
            }

            // Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stop/Flush
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                ) {
                    Text("STOP")
                }
                
                // Restart (<<)
                FilledIconButton(onClick = onRestart) {
                     Icon(Icons.Default.ArrowBack, contentDescription = "Restart")
                }
                
                // Rewind (Circular Arrow)
                FilledIconButton(onClick = onRewind) {
                     Icon(Icons.Default.Refresh, contentDescription = "Rewind")
                }

                // Play/Pause
                Button(
                    onClick = onPlayPause,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan),
                    modifier = Modifier.size(80.dp) // Large button
                ) {
                    Text(if (isPlaying) "PAUSE" else "PLAY", color = Color.Black)
                }
            }
        }
    }
}
