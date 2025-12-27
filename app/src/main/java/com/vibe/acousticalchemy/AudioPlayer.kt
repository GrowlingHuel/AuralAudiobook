package com.vibe.acousticalchemy

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import android.os.Handler
import android.os.HandlerThread

class AudioPlayer(private val context: android.content.Context) {
    // Persistent HandlerThread for audio processing (Android 16 Requirement)
    private val handlerThread = HandlerThread("AudioWorker").apply { start() }
    private val audioHandler = Handler(handlerThread.looper)
    private var audioTrack: AudioTrack? = null
    private val TAG = "AudioPlayer"

    // Circular Buffer for high-pass filter history (DC Offset Removal)
    private val historySize = 8
    private val historyBuffer = FloatArray(historySize)
    private var historyPointer = 0
    
    // Low-Pass / High-Pass State
    private var lastSample = 0f
    private var lastOutput = 0f

    init {
        // Post initialization to the handler thread
        audioHandler.post {
            initTrack()
        }
    }

    private fun initTrack() {
        // User requested dynamic buffer calibration (8x min for stability)
        val minBuf = AudioTrack.getMinBufferSize(24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufferSize = minBuf * 8 // Smoother buffer to eliminate jitter

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
            
        val sampleRate = 24000
        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        try {
            val builder = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // PERFORMANCE_MODE_LOW_LATENCY removed to prevent driver aggression on Android 16
                // builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }
            // Context Injection for Attribution (API 31+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                builder.setContext(context.createAttributionContext("audio_narration"))
            }
            
            audioTrack = builder.build()
            
            // Unmute Hardware
            audioTrack?.setVolume(1.0f)

            // HARDWARE PRIMING: Double Silence Injection (Extened to 24000 samples / 1s)
            // Critical for Android 16 to prevent "Disabled due to previous underrun"
            audioTrack?.play()
            val silence = ShortArray(24000) // 1 second of silence
            audioTrack?.write(silence, 0, silence.size, AudioTrack.WRITE_BLOCKING)
            try { Thread.sleep(100) } catch (e: Exception) {}
            audioTrack?.write(silence, 0, silence.size, AudioTrack.WRITE_BLOCKING)
            Log.d(TAG, "Iron Handshake: Hardware Double-Primed (1s)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack: ${e.message}")
        }
    }

    /**
     * blocking write to the audio stream
     */
    fun enqueue(audioData: FloatArray) {
        // Post audio processing to the persistent handler thread
        audioHandler.post {
            // Elevate thread priority for audio processing
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)

            // Ensure AudioTrack is initialized
            if (audioTrack == null || audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                initTrack()
            }

            // Write to circular history buffer
            for (sample in audioData) {
                historyBuffer[historyPointer] = sample
                historyPointer = (historyPointer + 1) % historySize
            }

            // Ensure playback is started and hardware is awake
            // NEVER flush between chunks - Persistent Stream
            if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                try {
                    audioTrack?.play()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start playback: ${e.message}")
                }
            }

            // Diagnostic: Buffer starvation
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                val underrun = audioTrack?.underrunCount ?: 0
                if (underrun > 0) {
                    Log.w(TAG, "Buffer Starvation! Underrun Count: $underrun")
                }
            }

            // Track audio chunk for diagnostics
            AudioDiagnosticManager.trackAudioChunk(audioData)

            // Convert FloatArray to 16‑bit PCM with pure‑signal pipeline
            val pcm16Data = ShortArray(audioData.size)
            for (i in audioData.indices) {
                var sample = audioData[i]
                // DC offset removal (high‑pass filter)
                val hp = sample - lastSample + (0.995f * lastOutput)
                lastSample = sample
                lastOutput = hp
                sample = hp
                // Apply multiplier and tanh limiter (pure‑signal)
                val limited = kotlin.math.tanh(sample * 1.2f)
                pcm16Data[i] = (limited * 32767).toInt().toShort()
            }

            // Write PCM data to AudioTrack (blocking)
            try {
                val written = audioTrack?.write(pcm16Data, 0, pcm16Data.size, AudioTrack.WRITE_BLOCKING) ?: 0
                if (written < 0) {
                    Log.e(TAG, "AudioTrack write failed: $written")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing to AudioTrack: ${e.message}")
            }
        }
    }
    
    fun play() {
        audioHandler.post {
            audioTrack?.play()
        }
    }

    fun pause() {
        audioHandler.post {
            audioTrack?.pause()
        }
    }
    
    fun clear() {
        audioHandler.post {
            try {
                audioTrack?.pause()
                audioTrack?.flush()
                audioTrack?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun rewind() {
        audioHandler.post {
            // Skip back 240,000 samples (~10 seconds)
            try {
                audioTrack?.pause()
                audioTrack?.flush()
                
                val rewindSamples = 240000
                val replayData = ShortArray(rewindSamples)
                
                var readPtr = (historyPointer - rewindSamples)
                if (readPtr < 0) readPtr += historySize
                
                for (i in 0 until rewindSamples) {
                    val sample = historyBuffer[(readPtr + i) % historySize]
                    
                    val softClipped = kotlin.math.tanh(sample * 0.75f)
                    replayData[i] = (softClipped * 32767).toInt().toShort()
                }
                
                audioTrack?.write(replayData, 0, replayData.size, AudioTrack.WRITE_BLOCKING)
                audioTrack?.play()
            } catch (e: Exception) {
                 Log.e(TAG, "Rewind failed: ${e.message}")
            }
        }
    }

    fun release() {
        audioHandler.post {
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                handlerThread.quitSafely()
            }
        }
    }
}
