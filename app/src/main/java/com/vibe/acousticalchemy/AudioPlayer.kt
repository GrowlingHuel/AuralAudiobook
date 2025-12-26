package com.vibe.acousticalchemy

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

class AudioPlayer {
    private val sampleRate = 24000
    private var audioTrack: AudioTrack? = null

    fun play(audioData: FloatArray) {
        // Stop any previous playback
        audioTrack?.release()

        // Convert Float (-1.0 to 1.0) to PCM 16-bit Short (-32768 to 32767)
        val pcm16Data = ShortArray(audioData.size)
        for (i in audioData.indices) {
            val sample = audioData[i]
            val clamped = sample.coerceIn(-1.0f, 1.0f)
            pcm16Data[i] = (clamped * 32767).toInt().toShort()
        }

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        // Buffer must hold all data (static mode) but ensuring it respects min*2 logic for safety/jitter
        val bufferSize = (minBufferSize * 2).coerceAtLeast(pcm16Data.size * 2)

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack?.write(pcm16Data, 0, pcm16Data.size, AudioTrack.WRITE_BLOCKING)
        
        // Log first 10 values to check for silence (all zeros detected means inference succeeded but produced silence)
        val snippet = pcm16Data.take(10).joinToString(", ")
        android.util.Log.d("AudioPlayer", "Playing audio. Size: ${pcm16Data.size}. First 10 samples (PCM16): [$snippet]")
        
        audioTrack?.play()
    }

    fun release() {
        audioTrack?.release()
    }
}
