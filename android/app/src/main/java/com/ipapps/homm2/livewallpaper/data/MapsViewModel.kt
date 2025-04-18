package com.ipapps.homm2.livewallpaper.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipapps.homm2.livewallpaper.data.MapReader.Companion.readMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MapsViewModel(
    private val contentResolver: ContentResolver,
    private val mapsFolder: File?
) : ViewModel() {
    companion object {
        const val TAG = "MapsViewModel"
    }

    private val _mapsListState = MutableStateFlow(emptyList<File>())
    private val mapsListFlow = _mapsListState
        .asStateFlow()
        .map { list -> list.mapNotNull(::readMap) }
        .onEach { println("Emit maps $it") }

    fun subscribeToMapsList(callback: (it: List<MapHeader>) -> Unit) {
        mapsListFlow.onEach { callback(it) }.launchIn(viewModelScope)
        updateFilesList()
    }

    init {
        updateFilesList()
    }

    private fun updateFilesList() {
        viewModelScope.launch {
            _mapsListState.emit(mapsFolder?.listFiles()?.toList() ?: emptyList())
        }
    }

    private fun readMap(uri: Uri): MapHeader? {
        contentResolver.openInputStream(uri).use {
            return kotlin.runCatching { readMap(getContentResolverFilename(uri), it) }.getOrNull()
        }
    }

    private fun readMap(file: File): MapHeader? {
        file.inputStream().use {
            return kotlin.runCatching { readMap(file.name, it) }.getOrNull()
        }
    }

    fun uploadMap(uri: Uri?): Boolean {
        if (uri == null) {
            return false
        }

        val map = readMap(uri)
        val fileName = getContentResolverFilename(uri) ?: "${map?.title}.mp2"
        if (map?.isPoL == true) {
            Log.v("MAP", "$fileName is PoL")
            return false
        }

        try {
            copyMapToFile(uri, fileName)
        } catch (e: Exception) {
            Log.v(TAG, "Failed to copy")
            return false
        }

        return true
    }

    private fun getContentResolverFilename(uri: Uri): String {
        val cursor = contentResolver.query(
            uri, null, null, null, null, null
        )

        if (cursor?.moveToFirst() == null) {
            cursor?.close()
            return "Unknown.mp2";
        }

        val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (columnIndex == -1) {
            cursor.close()
            return "Unknown.mp2"
        }

        // Note it's called "Display Name". This is
        // provider-specific, and might not necessarily be the file name.
        val filename = cursor.getString(columnIndex)
        cursor.close()
        if (filename.length > 30 || !filename.endsWith(".mp2")) {
            return "Unknown.mp2";
        }

        return filename
    }

    private fun copyMapToFile(uri: Uri, filename: String) {
        contentResolver.openInputStream(uri).use {
            if (it == null) {
                throw Exception("File not found")
            }

            it.copyTo(FileOutputStream(mapsFolder?.resolve(filename)))
        }

        updateFilesList()
    }

    fun deleteMap(name: String) {
        viewModelScope.launch {
            mapsFolder?.resolve(name)?.delete()

            updateFilesList()
        }
    }
}

