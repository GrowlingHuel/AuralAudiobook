package com.vibe.acousticalchemy

import android.util.Log

object AudioDiagnosticManager {
    private const val TAG = "AcousticDiagnostics"
    
    // Stats
    private var totalSamples = 0L
    private var clippedSamples = 0L
    private var bufferUnderruns = 0
    private var totalInferenceTimeMs = 0L
    private var totalAudioDurationMs = 0L
    
    // Config
    private const val PEAK_THRESHOLD = 0.95f
    
    fun log(message: String) {
        Log.d(TAG, message)
    }

    fun trackAudioChunk(samples: FloatArray) {
        var peaks = 0
        samples.forEach { 
            if (kotlin.math.abs(it) > PEAK_THRESHOLD) peaks++ 
        }
        
        totalSamples += samples.size
        clippedSamples += peaks
        
        if (peaks > 0) {
            Log.w(TAG, "Peak Detection: $peaks samples exceeded $PEAK_THRESHOLD in chunk of ${samples.size}")
        }
    }
    
    fun logUnderrun() {
        bufferUnderruns++
        Log.e(TAG, "Buffer Underrun Detected! Stream starving.")
    }
    
    private var lastRTF = 0f

    fun getLastRTF() = lastRTF
    
    fun trackPerformance(inferenceMs: Long, audioDurationMs: Long) {
        totalInferenceTimeMs += inferenceMs
        totalAudioDurationMs += audioDurationMs
        
        val rtf = inferenceMs.toFloat() / audioDurationMs.toFloat()
        lastRTF = rtf
        Log.d(TAG, "RTF (Real-Time Factor): %.2f (Inf: ${inferenceMs}ms / Aud: ${audioDurationMs}ms)".format(rtf))
        
        if (rtf > 1.0f) {
            Log.e(TAG, "CRITICAL: System is slower than real-time! Audio will stutter.")
        }
    }
    
    fun report() {
        Log.i(TAG, """
            === Acoustic Diagnostic Report ===
            Total Samples: $totalSamples
            Peaks/Clips: $clippedSamples
            Buffer Underruns: $bufferUnderruns
            Avg RTF: %.2f
            ==================================
        """.trimIndent().format(if (totalAudioDurationMs > 0) totalInferenceTimeMs.toFloat() / totalAudioDurationMs else 0f))
    }
    
    fun reset() {
        totalSamples = 0
        clippedSamples = 0
        bufferUnderruns = 0
        totalInferenceTimeMs = 0
        totalAudioDurationMs = 0
    }
}
