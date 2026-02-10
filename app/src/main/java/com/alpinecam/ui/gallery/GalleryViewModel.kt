package com.alpinecam.ui.gallery

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MediaItem(
    val uri: Uri,
    val name: String,
    val dateAdded: Long,
    val isVideo: Boolean,
    val duration: Long = 0,
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val items = mutableListOf<MediaItem>()
            items.addAll(loadImages())
            items.addAll(loadVideos())
            items.sortByDescending { it.dateAdded }
            _mediaItems.value = items
            _isLoading.value = false
        }
    }

    private fun loadImages(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
        )

        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%AlpineCam%")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        getApplication<Application>().contentResolver.query(
            collection, projection, selection, selectionArgs, sortOrder,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val dateAdded = cursor.getLong(dateColumn)
                val uri = ContentUris.withAppendedId(collection, id)

                items.add(MediaItem(uri = uri, name = name, dateAdded = dateAdded, isVideo = false))
            }
        }

        return items
    }

    private fun loadVideos(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
        )

        val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%AlpineCam%")
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        getApplication<Application>().contentResolver.query(
            collection, projection, selection, selectionArgs, sortOrder,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val dateAdded = cursor.getLong(dateColumn)
                val duration = cursor.getLong(durationColumn)
                val uri = ContentUris.withAppendedId(collection, id)

                items.add(
                    MediaItem(
                        uri = uri,
                        name = name,
                        dateAdded = dateAdded,
                        isVideo = true,
                        duration = duration,
                    ),
                )
            }
        }

        return items
    }
}
