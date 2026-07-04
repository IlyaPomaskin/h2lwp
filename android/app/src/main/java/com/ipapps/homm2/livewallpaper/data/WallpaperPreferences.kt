package com.ipapps.homm2.livewallpaper.data

/**
 * Live wallpaper preferences that are persisted into the fheroes2 config file
 * ("fheroes2.cfg") and read back by the native engine (see
 * Settings::ReadSettingsForLiveWallpaper in the C++ sources).
 *
 * Only the three settings exposed by the settings screen are represented here:
 * scale, map change interval and brightness.
 */

enum class Scale(val value: Int) {
    X1(1),
    X2(2),
    X3(3),
    X4(4),
    X5(5);

    companion object {
        fun fromInt(value: Int?): Scale {
            return values()
                .runCatching { first { it.value == value } }
                .getOrDefault(WallpaperPreferences.defaultScale)
        }
    }
}

enum class MapUpdateInterval(val value: Int) {
    EVERY_SWITCH(0),
    MINUTES_10(1),
    MINUTES_30(2),
    HOURS_2(3),
    HOURS_24(4);

    companion object {
        fun fromInt(value: Int?): MapUpdateInterval {
            return values()
                .runCatching { first { it.value == value } }
                .getOrDefault(WallpaperPreferences.defaultMapUpdateInterval)
        }
    }
}

data class WallpaperPreferences(
    val scale: Scale = defaultScale,
    val mapUpdateInterval: MapUpdateInterval = defaultMapUpdateInterval,
    val brightness: Int = defaultBrightness,
) {
    companion object Defaults {
        val defaultScale = Scale.X5
        val defaultMapUpdateInterval = MapUpdateInterval.MINUTES_10
        const val defaultBrightness = 70

        const val minBrightness = 0
        const val maxBrightness = 100
    }
}
