package com.alpinecam.ui.camera

import android.net.Uri

enum class CameraMode {
    PHOTO,
    VIDEO,
}

enum class FlashMode {
    OFF,
    ON,
    AUTO,
}

data class CameraState(
    val cameraMode: CameraMode = CameraMode.PHOTO,
    val isBackCamera: Boolean = true,
    val flashMode: FlashMode = FlashMode.OFF,
    val isRecording: Boolean = false,
    val recordingDurationSeconds: Int = 0,
    val zoomRatio: Float = 1f,
    val lastCapturedUri: Uri? = null,
)
