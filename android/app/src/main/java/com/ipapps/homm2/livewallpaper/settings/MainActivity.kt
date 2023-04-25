package com.ipapps.homm2.livewallpaper.settings;

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.ipapps.homm2.livewallpaper.settings.data.MapsViewModel
import com.ipapps.homm2.livewallpaper.settings.data.MapsViewModelFactory
import com.ipapps.homm2.livewallpaper.settings.data.ParsingViewModel
import com.ipapps.homm2.livewallpaper.settings.data.SettingsViewModel
import com.ipapps.homm2.livewallpaper.settings.data.WallpaperPreferencesRepository
import com.ipapps.homm2.livewallpaper.settings.data.dataStore
import com.ipapps.homm2.livewallpaper.settings.ui.components.NavigationHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import java.io.File


class MainActivity() : ComponentActivity() {
    private fun setWallpaper() {

    }

    private fun openIconAuthorUrl() {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("getString(R.string.icon_author_url)")
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mapsViewModel: MapsViewModel by viewModels {
            MapsViewModelFactory(contentResolver, filesDir)
        }

        val config =
            getExternalFilesDir(null)?.resolve("fheroes2.cfg")

        val settingsViewModel = SettingsViewModel(
            WallpaperPreferencesRepository(config, CoroutineScope(SupervisorJob())),
            setWallpaper = ::setWallpaper,
            openIconAuthorUrl = ::openIconAuthorUrl
        )
        val parsingViewModel = ParsingViewModel(application)

        setContent {
            LaunchedEffect(true) {
                if (mapsViewModel.mapsList.first().isEmpty()) {
                    parsingViewModel.copyDefaultMap()
                }
            }

            NavigationHost(
                mapViewModel = mapsViewModel,
                settingsViewModel = settingsViewModel,
                parsingViewModel = parsingViewModel,
            )
        }
    }
}

