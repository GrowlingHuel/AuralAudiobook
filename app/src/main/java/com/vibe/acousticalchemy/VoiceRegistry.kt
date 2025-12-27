package com.vibe.acousticalchemy

import java.util.Random

data class VoiceArchetype(
    val id: String,
    val name: String,
    val description: String,
    val position: Vector3, // Normalized [-1, 1]
    val gender: String,
    val ethnicity: String,
    val traits: List<String>
)

data class Vector3(val x: Float, val y: Float, val z: Float)

object VoiceRegistry {
    // Specific Archetype Map
    private val specificMap = mapOf(
        "am_fenrir.bin" to Vector3(-0.9f, 0.4f, -0.8f),
        "am_adam.bin" to Vector3(0.2f, -0.6f, -0.3f),
        "bf_alice.bin" to Vector3(0.7f, -0.4f, 0.2f),
        "af_sky.bin" to Vector3(-0.3f, 0.8f, 0.5f),
        "am_onyx.bin" to Vector3(-0.6f, -0.8f, -0.7f),
        "af_bella.bin" to Vector3(0.8f, -0.7f, 0.6f),
        "zm_yunxi.bin" to Vector3(0.9f, -0.9f, -0.5f)
    )

    private val titles = mapOf(
        "am_fenrir.bin" to "The Viking",
        "am_adam.bin" to "The Professor",
        "bf_alice.bin" to "The Librarian",
        "af_sky.bin" to "The Rogue",
        "am_onyx.bin" to "The Noir Detective",
        "af_bella.bin" to "The Oracle",
        "zm_yunxi.bin" to "The Eastern Monk"
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

    val voices: List<VoiceArchetype> = allFiles.map { fileName ->
        val pos = specificMap[fileName] ?: randomPos()
        val title = titles[fileName] ?: generateGenericTitle(fileName)
        val (gender, ethnicity) = parseMetadata(fileName)
        val traits = specificTraits[fileName] ?: listOf(gender, ethnicity)
        
        VoiceArchetype(
            id = fileName,
            name = title,
            description = "Voice Asset",
            position = pos,
            gender = gender,
            ethnicity = ethnicity,
            traits = traits
        )
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

    fun filterVoices(gender: String? = null, accent: String? = null, trait: String? = null): List<VoiceArchetype> {
        return voices.filter { voice ->
            val matchGender = gender == null || voice.gender.equals(gender, ignoreCase = true)
            val matchAccent = accent == null || voice.ethnicity.equals(accent, ignoreCase = true)
            val matchTrait = trait == null || voice.traits.any { it.equals(trait, ignoreCase = true) }
            matchGender && matchAccent && matchTrait
        }
    }
}
