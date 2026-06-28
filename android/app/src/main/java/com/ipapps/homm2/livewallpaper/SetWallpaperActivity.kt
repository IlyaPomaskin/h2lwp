package com.ipapps.homm2.livewallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import org.libsdl.app.SDLActivity

/**
 * Headless helper activity, invokable from adb, that opens the system live
 * wallpaper preview for this wallpaper:
 *
 *   adb shell am start -n com.ipapps.homm2.livewallpaper/.SetWallpaperActivity
 *
 * The preview is launched from a real app context (unlike a bare adb
 * CHANGE_LIVE_WALLPAPER intent), so it resolves reliably regardless of the
 * active launcher.
 */
class SetWallpaperActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(applicationContext, SDLActivity::class.java)
            )
        )
        finish()
    }
}
