package com.ipapps.homm2.livewallpaper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/** Small section header, e.g. "Preferences". */
@Composable
fun SettingsCategory(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp, end = 16.dp),
        color = MaterialTheme.colors.primary,
        style = MaterialTheme.typography.subtitle2,
    )
}

/**
 * A single settings row with a title, an optional subtitle and an optional
 * trailing (or, when [nextLine] is set, below-the-title) action such as a
 * slider.
 */
@Composable
fun SettingsItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    onClick: (() -> Unit)? = null,
    nextLine: Boolean = false,
    action: @Composable () -> Unit = {},
) {
    var rowModifier = modifier.fillMaxWidth()
    if (onClick != null) {
        rowModifier = rowModifier.clickable(onClick = onClick)
    }

    Surface(modifier = rowModifier) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.body1,
                )

                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                    )
                }

                if (nextLine) {
                    action()
                }
            }

            if (!nextLine) {
                action()
            }
        }
    }
}

data class SettingsDropdownItem<T>(
    val value: T,
    val title: String,
)

/** A settings row that opens a dropdown menu to pick one of [items]. */
@Composable
fun <T> SettingsDropdown(
    title: String,
    subtitle: String,
    items: List<SettingsDropdownItem<T>>,
    selectedItemValue: T,
    onItemSelected: (SettingsDropdownItem<T>) -> Unit,
) {
    var dropDownExpanded by remember { mutableStateOf(false) }

    Box {
        SettingsItem(
            title = title,
            subtitle = subtitle,
            onClick = { dropDownExpanded = !dropDownExpanded },
        )
        DropdownMenu(
            expanded = dropDownExpanded,
            offset = DpOffset(24.dp, 0.dp),
            onDismissRequest = { dropDownExpanded = false },
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        dropDownExpanded = false
                        onItemSelected(item)
                    },
                    modifier = Modifier.background(
                        if (selectedItemValue == item.value) {
                            MaterialTheme.colors.primary.copy(alpha = 0.3f)
                        } else {
                            Color.Unspecified
                        }
                    ),
                ) {
                    Text(item.title)
                }
            }
        }
    }
}
