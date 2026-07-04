package com.ipapps.homm2.livewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.ipapps.homm2.livewallpaper.data.WallpaperConfigRepository
import com.ipapps.homm2.livewallpaper.ui.WallpaperSettingsScreen
import org.libsdl.app.SDLActivity
import java.io.File

/**
 * Native settings screen for the live wallpaper. Reads and writes the "lwp *"
 * options (scale, map change interval, brightness) directly in the fheroes2
 * config file consumed by the engine, and offers a shortcut to apply the live
 * wallpaper.
 */
class WallpaperSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The top app bar draws its dark primary color behind the status bar, so
        // use light (white) status bar icons for contrast.
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        val repository = WallpaperConfigRepository(resolveConfigFile())

        setContent {
            WallpaperSettingsScreen(
                repository = repository,
                onSetWallpaper = ::setWallpaper,
                onBack = ::finish,
            )
        }
    }

    /**
     * Returns the config file the engine reads (external files dir /
     * fheroes2.cfg, which maps to Settings::configFileName under
     * System::GetConfigDirectory on Android). Seeds it from the bundled default
     * config if it does not exist yet.
     */
    private fun resolveConfigFile(): File {
        val configDir = getExternalFilesDir(null) ?: filesDir
        val configFile = File(configDir, CONFIG_FILE_NAME)

        if (!configFile.exists()) {
            runCatching {
                assets.open(DEFAULT_CONFIG_ASSET).use { input ->
                    configFile.outputStream().use { output -> input.copyTo(output) }
                }
            }.onFailure { ex ->
                Log.e("fheroes2", "Failed to seed the default config file.", ex)
            }
        }

        return configFile
    }

    private fun setWallpaper() {
        startActivity(
            Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(applicationContext, SDLActivity::class.java),
            )
        )
    }

    private companion object {
        const val CONFIG_FILE_NAME = "fheroes2.cfg"
        const val DEFAULT_CONFIG_ASSET = "files/default_fheroes2.cfg"
    }
}
