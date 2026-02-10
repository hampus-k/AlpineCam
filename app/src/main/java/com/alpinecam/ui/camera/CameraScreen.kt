package com.alpinecam.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
@Suppress("DEPRECATION")
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "AlpineCam"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onGalleryClick: () -> Unit,
    onMediaCaptured: (Uri) -> Unit,
    viewModel: CameraViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val permissions = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)
    }

    val permissionState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }

    if (permissionState.permissions.first { it.permission == Manifest.permission.CAMERA }.status.isGranted) {
        CameraContent(
            state = state,
            viewModel = viewModel,
            onGalleryClick = onGalleryClick,
            onMediaCaptured = onMediaCaptured,
        )
    } else {
        PermissionRequest()
    }
}

@Composable
private fun PermissionRequest() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "AlpineCam needs camera permission to work.\nPlease grant camera access in Settings.",
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp),
        )
    }
}

@Composable
private fun CameraContent(
    state: CameraState,
    viewModel: CameraViewModel,
    onGalleryClick: () -> Unit,
    onMediaCaptured: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // Recording timer
    LaunchedEffect(state.isRecording) {
        if (state.isRecording) {
            var seconds = 0
            while (true) {
                delay(1000)
                seconds++
                viewModel.updateRecordingDuration(seconds)
            }
        } else {
            viewModel.updateRecordingDuration(0)
        }
    }

    // Apply zoom
    LaunchedEffect(state.zoomRatio) {
        camera?.cameraControl?.setZoomRatio(state.zoomRatio)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider
                    bindCamera(
                        provider = provider,
                        lifecycleOwner = lifecycleOwner,
                        previewView = previewView,
                        isBackCamera = state.isBackCamera,
                        flashMode = state.flashMode,
                        onImageCaptureReady = { imageCapture = it },
                        onVideoCaptureReady = { videoCapture = it },
                        onCameraReady = { camera = it },
                    )
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val newZoom = state.zoomRatio * zoom
                        viewModel.setZoomRatio(newZoom)
                    }
                },
            update = { previewView ->
                cameraProvider?.let { provider ->
                    bindCamera(
                        provider = provider,
                        lifecycleOwner = lifecycleOwner,
                        previewView = previewView,
                        isBackCamera = state.isBackCamera,
                        flashMode = state.flashMode,
                        onImageCaptureReady = { imageCapture = it },
                        onVideoCaptureReady = { videoCapture = it },
                        onCameraReady = { camera = it },
                    )
                }
            },
        )

        // Top bar
        CameraTopBar(
            state = state,
            onFlashClick = viewModel::cycleFlashMode,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
        )

        // Bottom controls
        CameraBottomControls(
            state = state,
            onCaptureClick = {
                when (state.cameraMode) {
                    CameraMode.PHOTO -> {
                        imageCapture?.let { capture ->
                            takePhoto(
                                context = context,
                                imageCapture = capture,
                                flashMode = state.flashMode,
                                onPhotoSaved = { uri ->
                                    viewModel.setLastCapturedUri(uri)
                                    onMediaCaptured(uri)
                                },
                            )
                        }
                    }
                    CameraMode.VIDEO -> {
                        if (state.isRecording) {
                            activeRecording?.stop()
                            activeRecording = null
                            viewModel.setRecording(false)
                        } else {
                            videoCapture?.let { capture ->
                                activeRecording = startRecording(
                                    context = context,
                                    videoCapture = capture,
                                    onVideoSaved = { uri ->
                                        viewModel.setLastCapturedUri(uri)
                                        viewModel.setRecording(false)
                                        onMediaCaptured(uri)
                                    },
                                    onRecordingStarted = {
                                        viewModel.setRecording(true)
                                    },
                                )
                            }
                        }
                    }
                }
            },
            onSwitchCameraClick = viewModel::toggleCamera,
            onModeChange = { viewModel.toggleCameraMode() },
            onGalleryClick = onGalleryClick,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun bindCamera(
    provider: ProcessCameraProvider,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    isBackCamera: Boolean,
    flashMode: FlashMode,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onVideoCaptureReady: (VideoCapture<Recorder>) -> Unit,
    onCameraReady: (androidx.camera.core.Camera) -> Unit,
) {
    try {
        provider.unbindAll()

        val cameraSelector = if (isBackCamera) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imgCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(
                when (flashMode) {
                    FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                    FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                    FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                },
            )
            .build()

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
        val vidCapture = VideoCapture.withOutput(recorder)

        val camera = provider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imgCapture,
            vidCapture,
        )

        onImageCaptureReady(imgCapture)
        onVideoCaptureReady(vidCapture)
        onCameraReady(camera)
    } catch (e: Exception) {
        Log.e(TAG, "Camera bind failed", e)
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    flashMode: FlashMode,
    onPhotoSaved: (Uri) -> Unit,
) {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "ALPINE_$timestamp")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AlpineCam")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues,
    ).build()

    imageCapture.flashMode = when (flashMode) {
        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        FlashMode.ON -> ImageCapture.FLASH_MODE_ON
        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
    }

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                output.savedUri?.let { onPhotoSaved(it) }
                Log.d(TAG, "Photo saved: ${output.savedUri}")
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
            }
        },
    )
}

@SuppressLint("MissingPermission")
private fun startRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    onVideoSaved: (Uri) -> Unit,
    onRecordingStarted: () -> Unit,
): Recording {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "ALPINE_$timestamp")
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/AlpineCam")
        }
    }

    val outputOptions = MediaStoreOutputOptions.Builder(
        context.contentResolver,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
    ).setContentValues(contentValues).build()

    return videoCapture.output
        .prepareRecording(context, outputOptions)
        .withAudioEnabled()
        .start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    onRecordingStarted()
                    Log.d(TAG, "Recording started")
                }
                is VideoRecordEvent.Finalize -> {
                    if (!event.hasError()) {
                        event.outputResults.outputUri.let { onVideoSaved(it) }
                        Log.d(TAG, "Video saved: ${event.outputResults.outputUri}")
                    } else {
                        Log.e(TAG, "Video recording error: ${event.error}")
                    }
                }
            }
        }
}
