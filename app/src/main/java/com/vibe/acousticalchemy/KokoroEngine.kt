package com.vibe.acousticalchemy

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.Collections

class KokoroEngine(private val context: Context) {
    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    private val tokenizer = JsonTokenizer(context)

    fun init() {
        try {
            ortEnv = OrtEnvironment.getEnvironment()
            val modelBytes = context.assets.open("kokoro.onnx").readBytes()
            
            // Performance Optimization: Multi-threading
            val options = OrtSession.SessionOptions()
            options.setIntraOpNumThreads(4)
            options.setInterOpNumThreads(4)
            
            ortSession = ortEnv?.createSession(modelBytes, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Naive tokenizer removed in favor of JsonTokenizer

    fun generateSpeech(text: String, styleVector: FloatArray, speed: Float = 1.0f): FloatArray {
        val session = ortSession ?: return FloatArray(0)
        val env = ortEnv ?: return FloatArray(0)

        // 1. Prepare Inputs
        // Tokens: Shape [1, sequence_length]
        val fullTokens = tokenizer.getTokensFor(text)
        
        // Truncate to 512 tokens to prevent "Invalid Expand Shape" or model context limit errors
        val tokens = if (fullTokens.size > 512) {
             fullTokens.copyOfRange(0, 512)
        } else {
             fullTokens
        }
        
        val shapeTokens = longArrayOf(1, tokens.size.toLong())
        
        // Style inputs
        // Ensure styleVector is exactly 256. 
        // VoiceManager should have handled this, but we slice again to be safe if larger.
        val styleInput = if (styleVector.size > 256) {
             styleVector.sliceArray(0 until 256)
        } else {
             styleVector
        }
        val shapeStyle = longArrayOf(1, styleInput.size.toLong())

        // Speed: Shape [1]
        val shapeSpeed = longArrayOf(1)
        val speedBuffer = FloatBuffer.wrap(floatArrayOf(speed))
        
        // 2. Create Tensors
        // NOTE: Kokoro quantized requires Int64 for input_ids (tokens)
        // Ensure tokens are passed as LongBuffer/Long (OnnxTensor via wrap)
        val tokenTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(tokens), shapeTokens)
        val styleTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(styleInput), shapeStyle)
        val speedTensor = OnnxTensor.createTensor(env, speedBuffer, shapeSpeed)

        // 3. Map Inputs (Renamed 'tokens' -> 'input_ids')
        val inputs = mapOf(
            "input_ids" to tokenTensor,
            "style" to styleTensor,
            "speed" to speedTensor
        )

        // 4. Run Inference
        return try {
            android.util.Log.d("KokoroEngine", "Starting inference with input_ids size: ${tokens.size}, style size: ${styleVector.size}, speed: $speed")
            val startTime = System.currentTimeMillis()
            val results = session.run(inputs)
            val endTime = System.currentTimeMillis()
            val inferenceTimeStr = "Took ${endTime - startTime}ms"
            
            // Log RTF
            val outputTensor = results[0] as OnnxTensor
            val floatBuffer = outputTensor.floatBuffer
            val floatArray = FloatArray(floatBuffer.remaining())
            floatBuffer.get(floatArray)
            
            val audioDurationMs = (floatArray.size.toFloat() / 24000f * 1000f).toLong()
            AudioDiagnosticManager.trackPerformance(endTime - startTime, audioDurationMs)
            
            android.util.Log.d("KokoroEngine", "Inference Complete. $inferenceTimeStr")
            android.util.Log.d("KokoroEngine", "Output generated, size: ${floatArray.size}")
            floatArray
        } catch (e: Exception) {
            android.util.Log.e("KokoroEngine", "Inference Failed", e)
            e.printStackTrace()
            FloatArray(0)
        } finally {
            tokenTensor.close()
            styleTensor.close()
            speedTensor.close()
        }
    }

    fun close() {
        ortSession?.close()
        ortEnv?.close()
    }
}
