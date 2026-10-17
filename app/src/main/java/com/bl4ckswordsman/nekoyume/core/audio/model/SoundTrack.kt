package com.bl4ckswordsman.nekoyume.core.audio.model

/**
 * Categorical groupings for ambient sound tracks.
 */
enum class SoundCategory {
    NATURE,
    WEATHER,
    AMBIENCE,
    NOISE,
    SLEEP
}

/**
 * Immutable domain representation of a sound effect cataloged in the application manifest.
 *
 * @property id Unique slug identifier (e.g., "rain", "forest").
 * @property name Human-readable display name (e.g., "Gentle Rain").
 * @property category The sound category grouping.
 * @property assetPath Relative path within application assets (e.g., "sounds/rain.ogg").
 * @property defaultVolume Recommended initial volume multiplier clamped to [0.0f, 1.0f].
 */
data class SoundTrack(
    val id: String,
    val name: String,
    val category: SoundCategory,
    val assetPath: String,
    val defaultVolume: Float = 0.7f
) {
    init {
        require(id.isNotBlank()) { "SoundTrack id cannot be blank" }
        require(name.isNotBlank()) { "SoundTrack name cannot be blank" }
        require(assetPath.isNotBlank()) { "SoundTrack assetPath cannot be blank" }
        require(defaultVolume in 0.0f..1.0f) {
            "defaultVolume must be between 0.0 and 1.0, was: $defaultVolume"
        }
    }
}
