package com.vibe.acousticalchemy

import android.content.Context
import org.json.JSONObject

class JsonTokenizer(context: Context) {
    private val vocab = mutableMapOf<String, Int>()

    init {
        try {
            // Load verified 2tokenizer.json
            val jsonString = context.assets.open("2tokenizer.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            
            // Navigate to model -> vocab
            val modelObj = jsonObject.optJSONObject("model")
            val vocabObj = modelObj?.optJSONObject("vocab") ?: jsonObject.optJSONObject("vocab") ?: jsonObject
            
            vocabObj.keys().forEach { key ->
                vocab[key] = vocabObj.getInt(key)
            }
            android.util.Log.d("Kokoro", "Tokenizer loaded ${vocab.size} tokens")
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("Kokoro", "Failed to load tokenizer: ${e.message}")
        }
    }

    fun getTokensFor(text: String): LongArray {
        // Step 1: Phonemize (Heuristic Regex)
        val ipaString = textToPhonemes(text)
        android.util.Log.d("Kokoro", "Phonemes: $ipaString")
        
        // Step 2: Map to IDs
        val ids = mutableListOf<Long>()
        ids.add(0L) // Start Token

        for (char in ipaString) {
            val key = char.toString()
            if (vocab.containsKey(key)) {
                ids.add(vocab[key]!!.toLong())
            } else {
                // Fallback for unknown chars? Skip or space.
            }
        }
        
        ids.add(0L) // End Token
        return ids.toLongArray()
    }

    private fun textToPhonemes(text: String): String {
        var t = text.lowercase()

        // 1. Digraphs & Trigraphs (Order matters!)
        t = t.replace(Regex("qu"), "kw")
        t = t.replace(Regex("x"), "ks")
        t = t.replace(Regex("th"), "\u03b8") // θ
        t = t.replace(Regex("sh"), "\u0283") // ʃ
        t = t.replace(Regex("ch"), "t\u0283") // tʃ
        t = t.replace(Regex("ph"), "f")
        t = t.replace(Regex("ng"), "\u014b") // ŋ
        t = t.replace(Regex("ck"), "k")
        t = t.replace(Regex("ee"), "i")
        t = t.replace(Regex("oo"), "u")
        t = t.replace(Regex("ai"), "e")
        t = t.replace(Regex("ay"), "e")

        // 2. Letters to simplified IPA (Approximate)
        // Adjust these to match available vocab keys from 2tokenizer.json
        // a -> a (43), e -> e (47), i -> i (51), o -> o (57), u -> u (63)
        // c -> k (53) usually
        t = t.replace(Regex("c(?=[eiy])"), "s") // Soft c
        t = t.replace(Regex("c"), "k")          // Hard c
        t = t.replace(Regex("j"), "d\u0292")    // dʒ (if \u0292 exists in vocab? Yes: 147)
        t = t.replace(Regex("y"), "i")          // simplified
        
        // Keep punctuation map?
        // Tokenizer has: ; : , . ! ? (IDs 1-6)
        // \u2026 (...) -> 10
        // " -> 11
        // ( ) -> 12 13
        
        // Ensure spaces are preserved (ID 16)
        // Return string of characters that exist in vocab
        return t
    }
}
