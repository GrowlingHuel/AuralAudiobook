package com.vibe.acousticalchemy

import android.content.Context
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VoiceManager(private val context: Context) {

    fun loadVoice(assetName: String): FloatArray {
        return try {
            val inputStream: InputStream = context.assets.open("voices/$assetName")
            val bytes = inputStream.readBytes()
            val floatBuffer = ByteBuffer.wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asFloatBuffer()
            val floatArray = FloatArray(floatBuffer.remaining())
            floatBuffer.get(floatArray)
            
            // Extract the first 256 elements (Style Vector) if larger
            if (floatArray.size >= 256) {
                floatArray.sliceArray(0 until 256)
            } else {
                floatArray
            }
        } catch (e: Exception) {
            e.printStackTrace()
            FloatArray(256) // Return empty 256-dim vector on failure
        }
    }

    fun blendVoices(voiceA: FloatArray, voiceB: FloatArray, ratio: Float): FloatArray {
        return weightedBlend(listOf(voiceA, voiceB), listOf(1f - ratio, ratio))
    }

    fun weightedBlend(voices: List<FloatArray>, weights: List<Float>): FloatArray {
        if (voices.isEmpty() || voices.size != weights.size) {
            return FloatArray(256)
        }
        
        // Pure Vector Logic: Optimization for Single Voice
        if (voices.size == 1) {
            return voices[0].clone() 
        }

        val size = voices[0].size
        val blended = FloatArray(size)
        // Normalize weights
        val totalWeight = weights.sum()
        val normalizedWeights = if (totalWeight > 0) weights.map { it / totalWeight } else weights

        for (i in 0 until size) {
            var sum = 0f
            for (j in voices.indices) {
                sum += voices[j][i] * normalizedWeights[j]
            }
            blended[i] = sum
        }
        return blended
    }
}
