package com.ipapps.homm2.livewallpaper.settings.data;

import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val wallpaperPreferencesRepository: WallpaperPreferencesRepository,
    private val setWallpaper: () -> Unit,
    private val openIconAuthorUrl: () -> Unit
) : ViewModel() {
    val settingsUiModel = wallpaperPreferencesRepository.preferencesFlow.asLiveData()

    fun toggleUseScroll() {
        viewModelScope.launch {
            wallpaperPreferencesRepository.toggleUseScroll()
        }
    }

    fun setScale(value: Scale) {
        viewModelScope.launch {
            wallpaperPreferencesRepository.setScale(value)
        }
    }

    fun setMapUpdateInterval(value: MapUpdateInterval) {
        viewModelScope.launch {
            wallpaperPreferencesRepository.setMapUpdateInterval(value)
        }
    }

    fun setBrightness(value: Int) {
        viewModelScope.launch {
            wallpaperPreferencesRepository.setBrightness(value)
        }
    }

    fun onSetWallpaper() {
        setWallpaper()
    }

    fun onOpenIconAuthorUrl() {
        openIconAuthorUrl()
    }
}
