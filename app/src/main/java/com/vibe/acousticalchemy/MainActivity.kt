package com.vibe.acousticalchemy

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var voiceManager: VoiceManager
    private lateinit var kokoroEngine: KokoroEngine
    private lateinit var audioPlayer: AudioPlayer

    private var isSpeaking by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        voiceManager = VoiceManager(this)
        kokoroEngine = KokoroEngine(this)
        audioPlayer = AudioPlayer()

        // Initialize Engine
        kokoroEngine.init()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    VoiceLabScreen(
                        voiceManager = voiceManager,
                        isPlaying = isSpeaking,
                        onSpeak = { text, style ->
                            speakText(text, style)
                        }
                    )
                }
            }
        }
    }

    private fun speakText(text: String, style: FloatArray) {
        if (isSpeaking) return // Prevent overlap
        isSpeaking = true

        // Run on IO thread
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                // Force Max Volume
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, maxVolume, 0)

                // Synthesize (Speed 1.0f)
                val audio = kokoroEngine.generateSpeech(text, style, speed = 1.0f)
                
                if (audio.isNotEmpty()) {
                    audioPlayer.play(audio)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    isSpeaking = false
                }
            }
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        kokoroEngine.close()
        audioPlayer.release()
    }
}
