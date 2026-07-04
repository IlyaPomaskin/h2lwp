package com.ipapps.homm2.livewallpaper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ipapps.homm2.livewallpaper.R
import com.ipapps.homm2.livewallpaper.data.MapUpdateInterval
import com.ipapps.homm2.livewallpaper.data.Scale
import com.ipapps.homm2.livewallpaper.data.WallpaperConfigRepository
import com.ipapps.homm2.livewallpaper.data.WallpaperPreferences

@Composable
fun WallpaperSettingsScreen(
    repository: WallpaperConfigRepository,
    onSetWallpaper: () -> Unit,
    onBack: () -> Unit,
) {
    val prefs by repository.preferences.collectAsState()

    val scaleOptions = listOf(
        SettingsDropdownItem(Scale.X1, "x1"),
        SettingsDropdownItem(Scale.X2, "x2"),
        SettingsDropdownItem(Scale.X3, "x3"),
        SettingsDropdownItem(Scale.X4, "x4"),
        SettingsDropdownItem(Scale.X5, "x5"),
    )
    val mapUpdateIntervalOptions = listOf(
        SettingsDropdownItem(MapUpdateInterval.EVERY_SWITCH, stringResource(R.string.update_timeout_every_switch)),
        SettingsDropdownItem(MapUpdateInterval.MINUTES_10, stringResource(R.string.update_timeout_10minutes)),
        SettingsDropdownItem(MapUpdateInterval.MINUTES_30, stringResource(R.string.update_timeout_30minutes)),
        SettingsDropdownItem(MapUpdateInterval.HOURS_2, stringResource(R.string.update_timeout_2hour)),
        SettingsDropdownItem(MapUpdateInterval.HOURS_24, stringResource(R.string.update_timeout_24hours)),
    )

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    modifier = Modifier
                        .background(MaterialTheme.colors.primary)
                        .statusBarsPadding(),
                    elevation = 0.dp,
                    title = { Text(stringResource(R.string.settings_screen_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Text(
                                text = "←",
                                style = MaterialTheme.typography.h6,
                            )
                        }
                    },
                )
            },
            floatingActionButton = {
                SetWallpaperFab(
                    modifier = Modifier.navigationBarsPadding(),
                    onClick = onSetWallpaper,
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = innerPadding,
            ) {
                item { SettingsCategory(text = stringResource(R.string.preferences_header)) }
                item {
                    SettingsDropdown(
                        title = stringResource(R.string.scale_title),
                        subtitle = scaleOptions.find { it.value == prefs.scale }?.title.orEmpty(),
                        items = scaleOptions,
                        selectedItemValue = prefs.scale,
                        onItemSelected = { repository.setScale(it.value) },
                    )
                }
                item {
                    SettingsDropdown(
                        title = stringResource(R.string.update_time_title),
                        subtitle = mapUpdateIntervalOptions.find { it.value == prefs.mapUpdateInterval }?.title.orEmpty(),
                        items = mapUpdateIntervalOptions,
                        selectedItemValue = prefs.mapUpdateInterval,
                        onItemSelected = { repository.setMapUpdateInterval(it.value) },
                    )
                }
                item {
                    BrightnessSetting(
                        brightness = prefs.brightness,
                        onBrightnessChangeFinished = { repository.setBrightness(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BrightnessSetting(
    brightness: Int,
    onBrightnessChangeFinished: (Int) -> Unit,
) {
    // Keep a local, continuously-updated value while dragging; commit it to the
    // config file only when the user releases the slider. Re-seed it whenever
    // the persisted value changes (keyed on [brightness]).
    var sliderValue by remember(brightness) { mutableStateOf(brightness.toFloat()) }

    SettingsItem(
        title = stringResource(R.string.brightness_title),
        subtitle = sliderValue.toInt().toString(),
        nextLine = true,
    ) {
        Slider(
            value = sliderValue,
            valueRange = WallpaperPreferences.minBrightness.toFloat()..WallpaperPreferences.maxBrightness.toFloat(),
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onBrightnessChangeFinished(sliderValue.toInt()) },
        )
    }
}

@Composable
private fun SetWallpaperFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isLimitationVisible by remember { mutableStateOf(false) }

    if (isLimitationVisible) {
        AlertDialog(
            title = { Text(stringResource(R.string.wallpaper_change_limitation_title)) },
            text = { Text(stringResource(R.string.wallpaper_change_limitation_text)) },
            confirmButton = {
                Button(onClick = {
                    isLimitationVisible = false
                    onClick()
                }) { Text(stringResource(R.string.wallpaper_change_limitation_button)) }
            },
            onDismissRequest = { isLimitationVisible = false },
        )
    }

    FloatingActionButton(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.primary,
        onClick = { isLimitationVisible = true },
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(R.string.wallpaper_change_button),
        )
    }
}
