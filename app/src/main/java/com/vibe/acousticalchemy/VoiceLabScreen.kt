package com.vibe.acousticalchemy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    onSpeak: (String, FloatArray) -> Unit,
    onAudition: (String, FloatArray) -> Unit
) {
    // State
    var selectedVoiceId by remember { mutableStateOf<String?>(null) }
    var inputText by remember { mutableStateOf("Call me Ishmael.") }
    
    // Filters
    var selectedTextures by remember { mutableStateOf(setOf("Resonant", "Calm", "Nasal", "Gravelly", "Ethereal", "Dynamic")) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val allStars = remember(context) { VoiceRegistry.getVoices(context) }
    
    // Filtered Voices
    val filteredVoices by remember(selectedTextures, allStars) {
        derivedStateOf {
            allStars.filter { voice ->
                voice.texture in selectedTextures
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF050510)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text("Nebula Voice Lab", color = Color.Cyan, style = MaterialTheme.typography.headlineSmall)
            
            // Texture Chips
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val allTextures = listOf("Resonant", "Calm", "Nasal", "Gravelly", "Ethereal", "Dynamic")
                allTextures.forEach { texture ->
                    val isSelected = texture in selectedTextures
                    FilterChip(
                        selected = isSelected,
                        onClick = { 
                            selectedTextures = if (isSelected) selectedTextures - texture else selectedTextures + texture
                        },
                        label = { Text(texture) },
                        leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, null) } } else null
                    )
            }
        }
        
        }
        
        Spacer(Modifier.height(16.dp))

        // 2. Narrator List (Cards)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(filteredVoices) { voice ->
                val isSelected = voice.id == selectedVoiceId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { 
                            selectedVoiceId = voice.id
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color.Cyan.copy(alpha = 0.2f) else Color(0xFF1E1E1E)
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color.Cyan) else null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = voice.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) Color.Cyan else Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${voice.gender} • ${voice.ethnicity} • ${voice.texture}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Traits: ${voice.traits.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
        
        // 3. Controls (Audition / Select)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Probe Text") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.Cyan,
                unfocusedBorderColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Audition (Play Icon)
            FilledIconButton(
                enabled = selectedVoiceId != null,
                onClick = { 
                    selectedVoiceId?.let { id ->
                        val vector = voiceManager.loadVoice("$id.bin")
                        onAudition(inputText, vector)
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFF202020),
                    disabledContainerColor = Color(0xFF101010)
                )
            ) {
                Icon(
                    Icons.Default.PlayArrow, 
                    contentDescription = "Audition", 
                    tint = if (selectedVoiceId != null) Color.Cyan else Color.Gray
                )
            }
            
            // Select/Save (Checkmark)
            Button(
                enabled = selectedVoiceId != null,
                onClick = {
                    selectedVoiceId?.let { id ->
                        val vector = voiceManager.loadVoice("$id.bin")
                        val voiceName = allStars.find { it.id == id }?.name ?: "Unknown"
                        onSpeak(voiceName, vector)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Cyan,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("SELECT VOICE", color = Color.Black)
            }
        }
    }
}

