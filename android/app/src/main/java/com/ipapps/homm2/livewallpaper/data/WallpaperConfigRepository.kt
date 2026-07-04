package com.ipapps.homm2.livewallpaper.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Reads and writes the live wallpaper settings stored inside the fheroes2
 * config file. The file is a plain list of "key = value" lines; this repository
 * only touches the "lwp *" keys and leaves every other line untouched so the
 * rest of the game configuration is preserved.
 */
class WallpaperConfigRepository(private val configFile: File) {
    private var configLines: List<String> = readConfig(configFile)

    private val _preferences = MutableStateFlow(parsePreferences(configLines))
    val preferences: StateFlow<WallpaperPreferences> = _preferences.asStateFlow()

    fun setScale(scale: Scale) {
        updateParam(KEY_SCALE, scale.value.toString())
    }

    fun setMapUpdateInterval(interval: MapUpdateInterval) {
        updateParam(KEY_MAP_UPDATE_INTERVAL, interval.value.toString())
    }

    fun setBrightness(value: Int) {
        val clamped = value.coerceIn(WallpaperPreferences.minBrightness, WallpaperPreferences.maxBrightness)
        updateParam(KEY_BRIGHTNESS, clamped.toString())
    }

    private fun updateParam(name: String, value: String) {
        configLines = setParam(configLines, name, value)
        writeConfig(configFile, configLines)
        _preferences.value = parsePreferences(configLines)
    }

    private fun parsePreferences(lines: List<String>): WallpaperPreferences {
        return WallpaperPreferences(
            scale = Scale.fromInt(getIntParam(lines, KEY_SCALE, WallpaperPreferences.defaultScale.value)),
            mapUpdateInterval = MapUpdateInterval.fromInt(
                getIntParam(lines, KEY_MAP_UPDATE_INTERVAL, WallpaperPreferences.defaultMapUpdateInterval.value)
            ),
            brightness = getIntParam(lines, KEY_BRIGHTNESS, WallpaperPreferences.defaultBrightness),
        )
    }

    companion object {
        private const val KEY_SCALE = "lwp scale"
        private const val KEY_MAP_UPDATE_INTERVAL = "lwp map update interval"
        private const val KEY_BRIGHTNESS = "lwp brightness"
    }
}

private fun getParam(lines: List<String>, name: String): String? {
    return lines.find { it.startsWith("$name = ") }
        ?.split(" = ")
        ?.getOrNull(1)
        ?.trim()
}

private fun getIntParam(lines: List<String>, name: String, defaultValue: Int): Int {
    return getParam(lines, name)?.toIntOrNull() ?: defaultValue
}

private fun setParam(config: List<String>, name: String, value: String): List<String> {
    val exists = config.any { it.startsWith("$name = ") }

    return if (!exists) {
        config.plus("$name = $value")
    } else {
        config.map { if (it.startsWith("$name = ")) "$name = $value" else it }
    }
}

private fun readConfig(file: File): List<String> {
    return if (file.exists()) file.readText().split("\n") else emptyList()
}

private fun writeConfig(file: File, lines: List<String>) {
    file.writeText(lines.joinToString("\n"))
}
