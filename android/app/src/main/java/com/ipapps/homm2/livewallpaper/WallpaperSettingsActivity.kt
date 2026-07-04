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

        // The engine needs the bundled assets (e.g. files/data/resurrection.h2d)
        // extracted to the external files dir before the wallpaper starts. Do it
        // here since this screen is the entry point for applying the wallpaper.
        extractBundledAssetsIfChanged()

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

    /**
     * Copies the bundled assets ("files" and "maps") to the external files dir
     * consumed by the engine, but only when the bundled assets digest differs
     * from the last extracted one. The local digest is updated only after every
     * asset has been extracted successfully.
     */
    private fun extractBundledAssetsIfChanged() {
        val externalFilesDir = getExternalFilesDir(null) ?: filesDir

        if (!isAssetsDigestChanged(ASSETS_DIGEST, File(filesDir, ASSETS_DIGEST))) {
            return
        }

        try {
            extractAssets("files", externalFilesDir)
            extractAssets("maps", externalFilesDir)
            extractAssets(ASSETS_DIGEST, filesDir)
        } catch (ex: Exception) {
            Log.e("fheroes2", "Failed to extract bundled assets.", ex)
        }
    }

    private fun isAssetsDigestChanged(assetPath: String, localDigestFile: File): Boolean {
        return try {
            assets.open(assetPath).use { assetDigestStream ->
                val bundledDigest = assetDigestStream.readBytes()

                try {
                    localDigestFile.inputStream().use { localDigestStream ->
                        !bundledDigest.contentEquals(localDigestStream.readBytes())
                    }
                } catch (ex: Exception) {
                    Log.i("fheroes2", "Failed to read the local assets digest. Considering it as changed.", ex)
                    true
                }
            }
        } catch (ex: Exception) {
            Log.e("fheroes2", "Failed to read the bundled assets digest. Considering it as changed.", ex)
            true
        }
    }

    private fun extractAssets(srcPath: String, dstDir: File) {
        for (path in assetPaths(srcPath)) {
            assets.open(path).use { input ->
                val outFile = File(dstDir, path)
                outFile.parentFile?.mkdirs()
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    /**
     * Recursively lists every asset file under the given path. A leaf (a file)
     * has no children; directories are expanded into their contents.
     */
    private fun assetPaths(path: String): List<String> {
        val children = assets.list(path) ?: return emptyList()

        if (children.isEmpty()) {
            return listOf(path)
        }

        return children.flatMap { assetPaths("$path/$it") }
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
        const val ASSETS_DIGEST = "assets.digest"
    }
}
