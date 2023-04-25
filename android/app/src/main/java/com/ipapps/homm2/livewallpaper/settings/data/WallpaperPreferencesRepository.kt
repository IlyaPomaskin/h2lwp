package com.ipapps.homm2.livewallpaper.settings.data;

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.IOException
import java.lang.Exception

fun getIntParam(lines: List<String>, name: String, defaultValue: Int): Int {
    return lines.find { it.startsWith(name) }
        ?.split(" = ")
        ?.get(1)
        ?.toIntOrNull() ?: defaultValue
}

fun setIntParam(config: List<String>, name: String, value: Int): List<String> {
    val exists = config.find { it.startsWith(name) }

    if (exists == null) {
        return config.plus("$name = $value")
    } else {
        return config.map {
            if (it.startsWith(name)) {
                "$name = $value";
            } else {
                it
            }
        }
    }
}

fun readConfig(file: File?): List<String> {
    if (file == null || !file.exists()) {
        throw Exception("Can't read config file")
    }

    return file.readText().split("\n")
}

fun writeConfig(file: File?, lines: List<String>) {
    if (file == null) {
        throw Exception("No access to file")
    }

    return file.writeText(lines.joinToString("\n"))
}

class WallpaperPreferencesRepository(configFile: File?) {
    val preferences = MutableStateFlow(readConfig(configFile))

    val preferencesFlow = preferences
        .asStateFlow()
        .onEach { writeConfig(configFile, it) }
        .map { lines ->
            val brightness =
                getIntParam(lines, "lwp brightness", WallpaperPreferences.defaultBrightness)
            val scale = getIntParam(lines, "lwp scale", WallpaperPreferences.defaultScale.value)
            val mapUpdateInterval = getIntParam(
                lines,
                "lwp map update interval",
                WallpaperPreferences.defaultMapUpdateInterval.value
            )

            WallpaperPreferences(
                scale = Scale.fromInt(scale),
                brightness = brightness,
                mapUpdateInterval = MapUpdateInterval.fromInt(mapUpdateInterval)
            )
        }
        .catch { exception ->
            if (exception is IOException) {
                emit(WallpaperPreferences())
            } else {
                throw exception
            }
        }

    fun setBrightness(value: Int) {
        preferences.update { setIntParam(it, "lwp brightness", value) }
    }

    fun toggleUseScroll() {
    }

    fun setScale(scale: Scale) {
        preferences.update { setIntParam(it, "lwp scale", scale.value) }
    }

    fun setMapUpdateInterval(interval: MapUpdateInterval) {
        preferences.update { setIntParam(it, "lwp map update interval", interval.value) }
    }
}