package com.vibe.acousticalchemy

import java.util.Random

data class VoiceArchetype(
    val id: String,
    val name: String,
    val description: String,
    val position: Vector3, // Normalized [-1, 1]
    val gender: String,
    val ethnicity: String,
    val traits: List<String>,
    val texture: String
)

data class Vector3(val x: Float, val y: Float, val z: Float)

object VoiceRegistry {
    // Specific Archetype Map
    private val specificMap = mapOf(
        "am_fenrir.bin" to Vector3(-0.9f, 0.4f, -0.8f),
        "am_adam.bin" to Vector3(0.0f, 0.0f, 0.0f),
        "bf_alice.bin" to Vector3(0.7f, -0.4f, 0.2f),
        "af_sky.bin" to Vector3(-0.3f, 0.8f, 0.5f),
        "am_onyx.bin" to Vector3(-0.6f, -0.8f, -0.7f),
        "af_bella.bin" to Vector3(0.8f, -0.7f, 0.6f),
        "zm_yunxi.bin" to Vector3(0.9f, -0.9f, -0.5f)
    )

    private val titles = mapOf(
        "am_fenrir.bin" to "The Viking",
        "am_adam.bin" to "The Narrator",
        "bf_alice.bin" to "The Storyteller",
        "af_sky.bin" to "The Protagonist",
        "am_onyx.bin" to "The Noir Detective",
        "af_bella.bin" to "The Muse",
        "zm_yunxi.bin" to "The Scholar"
    )

    private val specificTraits = mapOf(
        "am_fenrir.bin" to listOf("Gritty", "Deep"),
        "am_adam.bin" to listOf("Authoritative", "Neutral"),
        "bf_alice.bin" to listOf("Calm", "Precise"),
        "af_sky.bin" to listOf("Dynamic", "Expressive"),
        "am_onyx.bin" to listOf("Dark", "Mysterious"),
        "af_bella.bin" to listOf("Soft", "High-Pitch"),
        "zm_yunxi.bin" to listOf("Calm", "Spiritual")
    )

    private val textures = mapOf(
        "Resonant" to listOf("am_adam", "zm_yunjian", "bm_george", "hm_omega", "im_nicola"),
        "Calm" to listOf("af_bella", "bf_alice", "af_sarah", "zf_xiaoxiao", "if_sara"),
        "Nasal" to listOf("af_alloy", "af_jessica", "pf_dora", "zf_xiaoni", "am_echo"),
        "Gravelly" to listOf("am_fenrir", "am_onyx", "bm_fable", "am_puck", "bm_lewis"),
        "Ethereal" to listOf("af_heart", "af_aoede", "af_kore", "hf_alpha", "ff_siwis"),
        "Dynamic" to listOf("af_sky", "am_michael", "af_nova", "af_nicole", "bf_lily")
    )

    // Full list of assets
    private val allFiles = listOf(
        "af_alloy.bin", "af_aoede.bin", "af_bella.bin", "af_heart.bin", "af_jessica.bin",
        "af_kore.bin", "af_nicole.bin", "af_nova.bin", "af_river.bin", "af_sarah.bin",
        "af_sky.bin", "am_adam.bin", "am_echo.bin", "am_eric.bin", "am_fenrir.bin",
        "am_liam.bin", "am_michael.bin", "am_onyx.bin", "am_puck.bin", "am_santa.bin",
        "bf_alice.bin", "bf_emma.bin", "bf_isabella.bin", "bf_lily.bin", "bm_daniel.bin",
        "bm_fable.bin", "bm_george.bin", "bm_lewis.bin", "ef_dora.bin", "em_alex.bin",
        "em_santa.bin", "ff_siwis.bin", "hf_alpha.bin", "hf_beta.bin", "hm_omega.bin",
        "hm_psi.bin", "if_sara.bin", "im_nicola.bin", "jf_alpha.bin", "jf_gongitsune.bin",
        "jf_nezumi.bin", "jf_tebukuro.bin", "jm_kumo.bin", "pf_dora.bin", "pm_alex.bin",
        "pm_santa.bin", "zf_xiaobei.bin", "zf_xiaoni.bin", "zf_xiaoxiao.bin", "zf_xiaoyi.bin",
        "zm_yunjian.bin", "zm_yunxi.bin", "zm_yunxia.bin", "zm_yunyang.bin"
    )

    private val voiceTextureMap: Map<String, String> by lazy {
        val map = mutableMapOf<String, String>()
        textures.forEach { (texture, ids) ->
            ids.forEach { id -> map[id] = texture }
        }
        map
    }

    fun getVoices(context: android.content.Context): List<VoiceArchetype> {
        return allFiles.mapNotNull { fileName ->
            // Asset Guard: Check if file exists to prevent Garble
            try {
                context.assets.open("voices/$fileName").close()
                // If effective, parse it
                val id = fileName.removeSuffix(".bin")
                val pos = specificMap[fileName] ?: randomPos()
                val title = titles[fileName] ?: generateGenericTitle(fileName)
                val (gender, ethnicity) = parseMetadata(fileName)
                val traits = specificTraits[fileName] ?: listOf(gender, ethnicity)
                val texture = voiceTextureMap[id] ?: "Standard"
                
                VoiceArchetype(
                    id = id,
                    name = title,
                    description = "Voice Asset",
                    position = pos,
                    gender = gender,
                    ethnicity = ethnicity,
                    traits = traits,
                    texture = texture
                )
            } catch (e: Exception) {
                // Asset missing, skip to prevent crash/garble
                null
            }
        }
    }

    private fun parseMetadata(fileName: String): Pair<String, String> {
        val prefix = fileName.take(2)
        val gender = if (prefix.endsWith("f")) "Female" else "Male"
        val ethnicity = when(prefix.first()) {
            'a' -> "American"
            'b' -> "British"
            'z' -> "Chinese"
            'j' -> "Japanese"
            'e' -> "Spanish" // e for Espanol
            'f' -> "French"
            'h' -> "Hindi"
            'i' -> "Italian"
            'p' -> "Portuguese"
            else -> "Other"
        }
        return gender to ethnicity
    }

    private fun generateGenericTitle(fileName: String): String {
        // e.g. af_alloy.bin -> American Female (Alloy)
        val parts = fileName.removeSuffix(".bin").split("_")
        if (parts.size < 2) return fileName
        val name = parts[1].replaceFirstChar { it.uppercase() }
        val (gender, ethnicity) = parseMetadata(fileName)
        return "$ethnicity $gender ($name)"
    }

    private fun randomPos(): Vector3 {
        val r = Random()
        return Vector3(
            (r.nextFloat() * 2 - 1), // -1 to 1
            (r.nextFloat() * 2 - 1),
            (r.nextFloat() * 2 - 1)
        )
    }

    fun filterVoices(context: android.content.Context, gender: String? = null, accent: String? = null, trait: String? = null, texture: String? = null): List<VoiceArchetype> {
        return getVoices(context).filter { voice ->
            val matchGender = gender == null || voice.gender.equals(gender, ignoreCase = true)
            val matchAccent = accent == null || voice.ethnicity.equals(accent, ignoreCase = true)
            val matchTrait = trait == null || voice.traits.any { it.equals(trait, ignoreCase = true) }
            val matchTexture = texture == null || voice.texture.equals(texture, ignoreCase = true)
            matchGender && matchAccent && matchTrait && matchTexture
        }
    }
}
