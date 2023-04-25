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

class WallpaperPreferencesRepository(configFile: File?) {
    val preferences = MutableStateFlow(readConfig(configFile))

    val preferencesFlow = preferences
        .asStateFlow()
        .map {
            val lines = readConfig(configFile)
            val brightness = getIntParam(lines, "lwp brightness", 100)
            val scale = getIntParam(lines, "lwp scale", 0)
            val mapUpdateInterval = getIntParam(lines, "lwp map update interval", 0)

            WallpaperPreferences(
                scale = Scale.fromInt(scale),
                brightness = brightness.toFloat() / 100,
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

    init {
        CoroutineScope(Job() + Dispatchers.Main).launch {
            preferencesFlow
                .onEach {
                    println("onEach")
                }
                .collect {
                    println("collect")
                }
        }
    }

    suspend fun setBrightness(value: Float) {
        preferences.update {
                list -> setIntParam(list, "lwp brightness", (value * 100).toInt())
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

//    suspend fun setBrightness(value: Float) {
////        dataStore.edit { prefs -> prefs[BRIGHTNESS] = value }
//        preferences.update {
//            list -> setIntParam(list, "lwp brightness", (value * 100).toInt())
//        }
//        preferences.emit(preferences.value)
//    }

    private fun mapUserPreferences(preferences: Preferences): WallpaperPreferences {
//        val scale = Scale.fromInt(preferences[SCALE])
//        val mapUpdateInterval = MapUpdateInterval.fromInt(preferences[MAP_UPDATE_INTERVAL])
//        val useScroll = preferences[USE_SCROLL] ?: WallpaperPreferences.defaultUseScroll
//        val brightness = preferences[BRIGHTNESS] ?: WallpaperPreferences.defaultBrightness

//        return WallpaperPreferences(scale, mapUpdateInterval, useScroll, brightness)

        return WallpaperPreferences();
    }

}