package com.ipapps.homm2.livewallpaper.settings.data;

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.lang.Exception

val Context.dataStore by preferencesDataStore(name = "wallpaper_preferences")

fun getIntParam(lines: List<String>, name: String, defaultValue: Int): Int {
    return lines.find { it.startsWith(name) }
        ?.split(" = ")
        ?.get(1)
        ?.toIntOrNull() ?: defaultValue
}

fun setIntParam(config: List<String>, name: String, value: Int): List<String> {
    return config.map {
        if (it.startsWith(name)) {
            "$name = $value";
        } else {
            it
        }
    }
}

fun readConfig(file: File?): List<String> {
    if (file == null || !file.exists()) {
        throw Exception("Can't read config file")
    }

    return file.readText().split("\n")
}

class WallpaperPreferencesRepository(configFile: File?, scope: CoroutineScope) {
    val preferences = MutableStateFlow(readConfig(configFile))

    val preferencesFlow = preferences
        .asStateFlow()
        .map {
            val lines = readConfig(configFile)
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
        preferences.update { list ->
            setIntParam(list, "lwp brightness", value)
        }
    }

    suspend fun toggleUseScroll() {
//        dataStore.edit { preferences ->
//            preferences[USE_SCROLL] = preferences[USE_SCROLL]?.not() ?: false
//        }
    }

    suspend fun setScale(value: Scale) {
//        dataStore.edit { prefs -> prefs[SCALE] = value.value }
    }

    suspend fun setMapUpdateInterval(value: MapUpdateInterval) {
//        dataStore.edit { prefs -> prefs[MAP_UPDATE_INTERVAL] = value.value }
    }

    private fun mapUserPreferences(preferences: Preferences): WallpaperPreferences {
//        val scale = Scale.fromInt(preferences[SCALE])
//        val mapUpdateInterval = MapUpdateInterval.fromInt(preferences[MAP_UPDATE_INTERVAL])
//        val useScroll = preferences[USE_SCROLL] ?: WallpaperPreferences.defaultUseScroll
//        val brightness = preferences[BRIGHTNESS] ?: WallpaperPreferences.defaultBrightness

//        return WallpaperPreferences(scale, mapUpdateInterval, useScroll, brightness)

        return WallpaperPreferences();
    }

}