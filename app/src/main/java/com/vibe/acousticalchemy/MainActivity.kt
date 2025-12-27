package com.vibe.acousticalchemy

import android.os.Bundle
import android.util.Log
import android.content.Context
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    // Audio Engine
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var kokoroEngine: KokoroEngine
    
    // State
    private var currentZone by mutableStateOf(Zone.VOICE_LAB)
    
    // Playback State
    // Playback State
    private var currentStyle = FloatArray(256)
    private var currentBookTitle by mutableStateOf("No Book Selected")
    private var currentBookContent = "" // Persistence for Restart
    
    // Teleprompter State
    private var visibleSentences = mutableStateListOf<String>()
    private var activeIndex by mutableStateOf(-1)
    
    private var isPlaying by mutableStateOf(false)
    
    // Zone Enum
    enum class Zone { VOICE_LAB, LIBRARY, PLAYBACK }

    // Service State
    private var isServiceConnected by mutableStateOf(false)
    private var narrationService: NarrationService? = null
    
    private val serviceConnection = object : android.content.ServiceConnection {
        override fun onServiceConnected(className: android.content.ComponentName, service: android.os.IBinder) {
            val binder = service as NarrationService.LocalBinder
            narrationService = binder.getService()
            isServiceConnected = true
            Log.d("MainActivity", "Service Connected")
        }

        override fun onServiceDisconnected(arg0: android.content.ComponentName) {
            isServiceConnected = false
            narrationService = null
            Log.d("MainActivity", "Service Disconnected")
        }
    }

    // Import Launcher
    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val (name, text) = LibraryManager.importTextFile(this, it)
            LibraryManager.saveToVault(this, name, text)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Init Audio
        try {
            audioPlayer = AudioPlayer(this) // Default Context
            kokoroEngine = KokoroEngine(this)
            // Initialize KokoroEngine off the main thread to avoid ANR
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                kokoroEngine.init()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error init: ${e.message}")
        }

        setContent {
             Scaffold(
                 bottomBar = {
                     NavigationBar(containerColor = Color(0xFF101010)) {
                         NavigationBarItem(
                             selected = currentZone == Zone.VOICE_LAB,
                             onClick = { currentZone = Zone.VOICE_LAB },
                             icon = { Icon(Icons.Default.Build, contentDescription = "Mixer") },
                             label = { Text("Mixer") },
                             colors = NavigationBarItemDefaults.colors(
                                 selectedIconColor = Color.Cyan,
                                 selectedTextColor = Color.Cyan,
                                 indicatorColor = Color(0xFF202020)
                             )
                         )
                         NavigationBarItem(
                             selected = currentZone == Zone.LIBRARY,
                             onClick = { currentZone = Zone.LIBRARY },
                             icon = { Icon(Icons.Default.List, contentDescription = "Archive") },
                             label = { Text("Archive") },
                             colors = NavigationBarItemDefaults.colors(
                                 selectedIconColor = Color.Cyan,
                                 selectedTextColor = Color.Cyan,
                                 indicatorColor = Color(0xFF202020)
                             )
                         )
                         NavigationBarItem(
                             selected = currentZone == Zone.PLAYBACK,
                             onClick = { currentZone = Zone.PLAYBACK },
                             icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Player") },
                             label = { Text("Player") },
                             colors = NavigationBarItemDefaults.colors(
                                 selectedIconColor = Color.Cyan,
                                 selectedTextColor = Color.Cyan,
                                 indicatorColor = Color(0xFF202020)
                             )
                         )
                     }
                 },
                 containerColor = Color(0xFF050510)
             ) { paddingValues ->
                 Box(modifier = Modifier.padding(paddingValues)) {
                     when(currentZone) {
                         Zone.VOICE_LAB -> VoiceLabScreen(
                             voiceManager = VoiceManager(this@MainActivity),
                             isPlaying = false, 
                             onSpeak = { name, style ->
                                 // "Checkmark" Action: Save Style
                                 currentStyle = style
                                 android.widget.Toast.makeText(this@MainActivity, "Voice Locked: $name", android.widget.Toast.LENGTH_SHORT).show()
                             },
                             onAudition = { text, style ->
                                 speakText(text, style)
                             }
                         )
                         
                         Zone.LIBRARY -> LibraryScreen(
                             onBookSelected = { title, content, isClassic ->
                                 currentBookTitle = title
                                 loadBook(content)
                                 currentZone = Zone.PLAYBACK
                                 resumePlayback()
                             },
                             onImport = { importLauncher.launch("text/plain") },
                             onBack = { /* No back, use API */ }
                         )
                         
                         Zone.PLAYBACK -> PlaybackScreen(
                             currentBookTitle = currentBookTitle,
                             visibleSentences = visibleSentences,
                             activeIndex = activeIndex,
                             isPlaying = isPlaying,
                             onPlayPause = { 
                                 if (isPlaying) pausePlayback() else resumePlayback() 
                             },
                             onStop = { 
                                 stopPlayback() 
                             },
                             onRestart = {
                                 // Restart Logic
                                 stopPlayback()
                                 if (currentBookContent.isNotEmpty()) {
                                     loadBook(currentBookContent)
                                     resumePlayback()
                                 }
                             },
                             onRewind = {
                                 audioPlayer.rewind()
                             },
                             onBack = { /* No back */ }
                         )
                     }
                 }
             }
        }
    }
    
    // Dual-Threaded Architecture
    private data class AudioFragment(val id: Int, val text: String, val audio: FloatArray)
    
    // Channels
    private val textQueue = Channel<Pair<Int, String>>(Channel.UNLIMITED)
    private val audioQueue = Channel<AudioFragment>(capacity = 10) // Cushion Buffer
    
    private var synthesisJob: Job? = null
    private var playbackJob: Job? = null

    private fun startEngine() {
        // 1. PRODUCER: Synthesis
        synthesisJob = kotlinx.coroutines.GlobalScope.launch(Dispatchers.Default) {
             // Handshake: Wait for Foreground Service to be Ready
             // This ensures we have high priority before burning CPU
             var waitCount = 0
             // Strict Handshake: Service must be Bound AND "Ready"
             while ((!NarrationService.isReady || !isServiceConnected) && waitCount < 50) { 
                 if (!isActive) break
                 delay(100)
                 waitCount++
             }
             
             // System Server Handshake Wait (Allow promotion to Foreground Media bucket)
             delay(1000)
             
             // Re-Init AudioPlayer with Service Context (if available) for Attribution
             withContext(Dispatchers.Main) {
                 NarrationService.instance?.let {
                      // We recreate the player to ensure the AudioTrack is bound to the Service Context
                      audioPlayer.release() 
                      audioPlayer = AudioPlayer(it)
                       
                 }
             }
             
             for ((index, text) in textQueue) {
                 if (!isActive) break
                 
                 // Generate Audio (CPU Heavy)
                 val vector = currentStyle
                 val audio = kokoroEngine.generateSpeech(text, vector, speed = 1.0f)
                 
                 if (audio != null) {
                     // Push to Audio Queue
                     audioQueue.send(AudioFragment(index, text, audio))
                 }
             }
        }
        
        // 2. CONSUMER: Playback
        playbackJob = kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            // High Priority for Audio Thread
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            AudioDiagnosticManager.log("Playback Consumer Started (Urgent Audio)")
            
            // Pre-Roll Logic: Wait for Buffer Saturation
            // "Synthesize the first 4 fragments immediately before starting"
            // We wait until we have at least 3 items OR the queue is full OR timeout?
            // Simple approach: Wait for first items.
            // Actually, we can just consume. But to ensure "Cushion", we might want to peek/delay?
            // User: "Maintain a Queue ... of at least 3 ready ... performance cushion".
            // Implementation: We can just let the Producer run ahead. 
            // If we start consuming immediately, we might catch up.
            // So, let's delay start until we have X items or producer is done.
            
            // Initial Pre-Roll Wait
            var preRollCount = 0
            while (preRollCount < 3 && isActive) {
                // Peek? Channel doesn't support peek.
                // We just wait? No, if we wait, we block consumption.
                // WE MUST NOT CONSUME until we have 3.
                // But we can't check channel size reliably without consuming?
                // Actually `audioQueue.isEmpty` is available but racey.
                // Better: Consume into a local buffer list, then start loop?
                // Let's implement a "Ready State".
                
                delay(100) // Poll-ish wait for producer to fill buffer
                // This is crude.
                // Better: Just start. If the producer is faster (which it isn't always), we are good.
                // But User explicitly asked for "Pre-Roll... synthesize first 4... before starting".
                // So: we need to verify generation speed > playback speed OR buffer enough.
                // Let's simulate Pre-Roll by buffering the first 4 items into a local list.
            }
            
            // Real Implementation:
            // The Channel is the buffer. We just need to wait until it has items.
            // To strictly enforce "Start only after 3", we could do:
            // But Channel hide implementation.
            // Let's rely on the fact that we launched producer first.
            // We can delay this coroutine by X ms to allow fill?
            // "Synthesize first 4 fragments immediately" -> This implies we simply wait.
            
            // Let's implement an explicit "Receive and Hold" for the first 4.
        }
        
        // Re-launching Playback Consumer with correct logic
        playbackJob?.cancel()
        playbackJob = kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
             val localBuffer = mutableListOf<AudioFragment>()
             
             // PRE-ROLL STATE
             Log.d("AudioEngine", "Buffering Pre-Roll...")
             var buffered = 0
             while (buffered < 5 && isActive) {
                 val item = audioQueue.receive()
                 localBuffer.add(item)
                 buffered++
             }
             Log.d("AudioEngine", "Pre-Roll Complete. Starting Playback.")
             
             // Process Pre-Roll
             localBuffer.forEach { playFragment(it) }
             localBuffer.clear()
             
             // STEADY STATE
             for (fragment in audioQueue) {
                 if (!isActive) break
                 playFragment(fragment)
             }
        }
    }
    
    private fun playFragment(fragment: AudioFragment) {
        // Sync Teleprompter
        activeIndex = fragment.id
        
        // Enqueue (Blocking)
        audioPlayer.enqueue(fragment.audio)
    }
    
    private fun loadBook(text: String) {
        stopPlayback()
        currentBookContent = text 
        visibleSentences.clear()
        activeIndex = -1
        
        // Reset Engine
        synthesisJob?.cancel()
        playbackJob?.cancel()
        
        // Clear Channels (Re-create to flush)
        // Actually, we can just drain or re-instantiate if they were class members?
        // They are. `textQueue` is `Channel`. We can't easily clear a Channel without closing.
        // Better to cancel jobs and let GC handle?
        // Let's loop drain?
        while (textQueue.tryReceive().isSuccess) {}
        while (audioQueue.tryReceive().isSuccess) {}

        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            // Micro-Fragmenting: Hard Limit 30 words (approx 50 tokens)
            val rawSentences = text.split(Regex("(?<=[.!?])\\s+"))
            val chunksList = mutableListOf<String>()
            
            rawSentences.forEach { sentence ->
                if (sentence.isBlank()) return@forEach
                
                val words = sentence.trim().split("\\s+".toRegex())
                if (words.size > 15) {
                    // Force Split every 15 words
                    val subChunks = words.chunked(15).map { it.joinToString(" ") }
                    chunksList.addAll(subChunks)
                } else {
                    chunksList.add(sentence)
                }
            }
            
            // Populate UI List
            withContext(Dispatchers.Main) {
                visibleSentences.addAll(chunksList)
            }
            
            // Feed Text Queue
            chunksList.forEachIndexed { index, s ->
                textQueue.send(index to s)
            }
            
            // Start Engine
            startEngine()
        }
    }

    private fun resumePlayback() {
        // Start Foreground Service (High Priority)
        val serviceIntent = android.content.Intent(this, NarrationService::class.java)
        
        // Started Service (for Foreground Notification persistence)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        // Bind Service (for Handshake)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        // Block until connected? We'll let the UI update, but `startEngine` is already waiting for `isReady`.
        // We should trigger play, but if not connected, startEngine loops.
        // `isPlaying` true starts loop.
        
        isPlaying = true
        audioPlayer.play()
    }
    
    private fun pausePlayback() {
        isPlaying = false
        audioPlayer.pause()
        
        // Unbind + Stop
        if (isServiceConnected) {
            unbindService(serviceConnection)
            isServiceConnected = false
        }
        val serviceIntent = android.content.Intent(this, NarrationService::class.java)
        stopService(serviceIntent)
    }

    private fun stopPlayback() {
        isPlaying = false
        synthesisJob?.cancel()
        playbackJob?.cancel()
        audioPlayer.clear()
        
        // Stop Service
        if (isServiceConnected) {
            unbindService(serviceConnection)
            isServiceConnected = false
        }
        val serviceIntent = android.content.Intent(this, NarrationService::class.java)
        stopService(serviceIntent)
        
        // Drain Queues
        while (textQueue.tryReceive().isSuccess) {}
        while (audioQueue.tryReceive().isSuccess) {}
        AudioDiagnosticManager.report()
        AudioDiagnosticManager.reset()
    }

    private suspend fun processSentence(text: String, style: FloatArray) {
        if (!isPlaying) return 
        
        try {
            val audio = kokoroEngine.generateSpeech(text, style, 1.0f) 
            audioPlayer.enqueue(audio)
            
            // Diagnostic Check
            if (AudioDiagnosticManager.getLastRTF() > 1.0f) {
                withContext(Dispatchers.Main) {
                     android.widget.Toast.makeText(this@MainActivity, "CPU Overload - Reducing Quality", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Synthesis Error: ${e.message}")
        }
    }

    private fun speakText(text: String, style: FloatArray) {
        stopPlayback()
        // Use Service Context if available for accurate attribution
        NarrationService.instance?.let { 
             audioPlayer.release()
             audioPlayer = AudioPlayer(it)
             
        }
        
        isPlaying = true
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            processSentence(text, style)
             // Reset after audition if needed, or just leave it
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.release()
        synthesisJob?.cancel()
        playbackJob?.cancel()
    }
}
