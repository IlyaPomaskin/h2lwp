package com.ipapps.homm2.livewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.ipapps.homm2.livewallpaper.data.SettingsViewModel
import com.ipapps.homm2.livewallpaper.data.WallpaperPreferencesRepository
import com.ipapps.homm2.livewallpaper.data.WebViewSettingsEvent
import com.ipapps.homm2.livewallpaper.data.sendWebViewEvent
import de.andycandy.android.bridge.Bridge
import org.libsdl.app.SDLActivity
import java.io.InputStream

class WebViewActivity : AppCompatActivity() {
    private fun setWallpaper() {
        startActivity(
            Intent().setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(
                    applicationContext, SDLActivity::class.java
                )
            )
        )
    }

    private fun copyFile(input: InputStream, to: String, path: String?) {
        val output = getExternalFilesDir(path)?.resolve(to);

        if (output != null && !output.exists() && output.createNewFile()) {
            output.outputStream().use {
                input.copyTo(it)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val webView = findViewById<WebView>(R.id.activity_web_view)

        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        assets.open("files/data/resurrection.h2d").use {
            copyFile(it, "resurrection.h2d", "files/data")
        }

        assets.open("files/default_fheroes2.cfg").use {
            copyFile(it, "fheroes2.cfg", null)
        }

        val config = getExternalFilesDir(null)?.resolve("fheroes2.cfg")
        val prefsRepository = WallpaperPreferencesRepository(config)
        val settingsViewModel = SettingsViewModel(prefsRepository, ::setWallpaper);

        val webView = findViewById<WebView>(R.id.activity_web_view)
        val bridge = Bridge(applicationContext, webView)
        bridge.addJSInterface(AndroidNativeInterface(settingsViewModel))
        bridge.addAfterInitializeListener {
            settingsViewModel.subscribeToPreferences {
                sendWebViewEvent(WebViewSettingsEvent(it), webView)
            }
        }

        webView.loadUrl("file:///android_asset/www/index.html")
    }
}
