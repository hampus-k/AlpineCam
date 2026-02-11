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
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "AlpineCam"
private const val BUILD_ID = "v5-0211"  // Change this to verify builds

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onGalleryClick: () -> Unit,
    onMediaCaptured: (Uri) -> Unit,
    viewModel: CameraViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

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
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Show build ID on startup to verify correct version
    LaunchedEffect(Unit) {
        Log.d(TAG, "=== AlpineCam $BUILD_ID started ===")
        Toast.makeText(context, "AlpineCam $BUILD_ID", Toast.LENGTH_SHORT).show()
    }

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

    // Rebind camera when camera direction, flash, or mode changes — never during recording
    LaunchedEffect(state.isBackCamera, state.flashMode, state.cameraMode) {
        if (state.isRecording) return@LaunchedEffect
        val provider = cameraProvider ?: return@LaunchedEffect
        val pv = previewView ?: return@LaunchedEffect
        bindCamera(
            provider = provider,
            lifecycleOwner = lifecycleOwner,
            previewView = pv,
            isBackCamera = state.isBackCamera,
            flashMode = state.flashMode,
            cameraMode = state.cameraMode,
            onImageCaptureReady = { imageCapture = it },
            onVideoCaptureReady = { videoCapture = it },
            onCameraReady = { camera = it },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }.also { pv ->
                    previewView = pv
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        cameraProvider = provider
                        bindCamera(
                            provider = provider,
                            lifecycleOwner = lifecycleOwner,
                            previewView = pv,
                            isBackCamera = state.isBackCamera,
                            flashMode = state.flashMode,
                            cameraMode = state.cameraMode,
                            onImageCaptureReady = { imageCapture = it },
                            onVideoCaptureReady = { videoCapture = it },
                            onCameraReady = { camera = it },
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val newZoom = state.zoomRatio * zoom
                        viewModel.setZoomRatio(newZoom)
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

        // Build version label (bottom-left, small)
        Text(
            text = BUILD_ID,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 8.dp, bottom = 8.dp),
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
                            Log.d(TAG, "Stopping recording... activeRecording=$activeRecording")
                            activeRecording?.stop()
                            activeRecording = null
                        } else {
                            Log.d(TAG, "Starting recording... videoCapture=$videoCapture")
                            videoCapture?.let { capture ->
                                activeRecording = startRecording(
                                    context = context,
                                    videoCapture = capture,
                                    onVideoSaved = { uri ->
                                        Log.d(TAG, "onVideoSaved callback: $uri")
                                        viewModel.setLastCapturedUri(uri)
                                        viewModel.setRecording(false)
                                        onMediaCaptured(uri)
                                    },
                                    onRecordingStarted = {
                                        Log.d(TAG, "onRecordingStarted callback")
                                        viewModel.setRecording(true)
                                    },
                                    onRecordingError = { errorMsg ->
                                        Log.e(TAG, "onRecordingError callback: $errorMsg")
                                        viewModel.setRecording(false)
                                        Toast.makeText(context, "Recording failed: $errorMsg", Toast.LENGTH_LONG).show()
                                    },
                                )
                                Log.d(TAG, "startRecording returned: $activeRecording")
                            } ?: run {
                                Log.e(TAG, "videoCapture is NULL! Cannot record.")
                                Toast.makeText(context, "Camera not ready, try again", Toast.LENGTH_SHORT).show()
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
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    isBackCamera: Boolean,
    flashMode: FlashMode,
    cameraMode: CameraMode,
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

        val camera = when (cameraMode) {
            CameraMode.PHOTO -> {
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

                onImageCaptureReady(imgCapture)
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imgCapture)
            }
            CameraMode.VIDEO -> {
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.FHD))
                    .build()
                val vidCapture = VideoCapture.withOutput(recorder)

                onVideoCaptureReady(vidCapture)
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, vidCapture)
            }
        }

        onCameraReady(camera)
        Log.d(TAG, "Camera bound successfully in $cameraMode mode")
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
                Toast.makeText(context, "Photo failed: ${exception.message}", Toast.LENGTH_SHORT).show()
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
    onRecordingError: (String) -> Unit,
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

    Log.d(TAG, "Preparing recording...")

    return videoCapture.output
        .prepareRecording(context, outputOptions)
        .withAudioEnabled()
        .start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    onRecordingStarted()
                    Log.d(TAG, "Recording started")
                }
                is VideoRecordEvent.Status -> {
                    Log.d(TAG, "Recording status: ${event.recordingStats.numBytesRecorded} bytes")
                }
                is VideoRecordEvent.Finalize -> {
                    if (!event.hasError()) {
                        val uri = event.outputResults.outputUri
                        Log.d(TAG, "Video saved: $uri")
                        Toast.makeText(context, "Video saved!", Toast.LENGTH_SHORT).show()
                        onVideoSaved(uri)
                    } else {
                        val errorMsg = "Error code: ${event.error}, cause: ${event.cause?.message}"
                        Log.e(TAG, "Video recording error: $errorMsg")
                        onRecordingError(errorMsg)
                    }
                }
            }
        }
}
