package com.alpinecam.ui.camera

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CameraViewModel : ViewModel() {

    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    fun toggleCameraMode() {
        _state.update {
            it.copy(
                cameraMode = when (it.cameraMode) {
                    CameraMode.PHOTO -> CameraMode.VIDEO
                    CameraMode.VIDEO -> CameraMode.PHOTO
                },
                isRecording = false,
                recordingDurationSeconds = 0,
            )
        }
    }

    fun toggleCamera() {
        _state.update { it.copy(isBackCamera = !it.isBackCamera) }
    }

    fun cycleFlashMode() {
        _state.update {
            it.copy(
                flashMode = when (it.flashMode) {
                    FlashMode.OFF -> FlashMode.ON
                    FlashMode.ON -> FlashMode.AUTO
                    FlashMode.AUTO -> FlashMode.OFF
                },
            )
        }
    }

    fun setRecording(recording: Boolean) {
        _state.update { it.copy(isRecording = recording) }
    }

    fun updateRecordingDuration(seconds: Int) {
        _state.update { it.copy(recordingDurationSeconds = seconds) }
    }

    fun setZoomRatio(ratio: Float) {
        _state.update { it.copy(zoomRatio = ratio.coerceIn(1f, 10f)) }
    }

    fun setLastCapturedUri(uri: Uri?) {
        _state.update { it.copy(lastCapturedUri = uri) }
    }
}
