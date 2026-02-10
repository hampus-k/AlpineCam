package com.alpinecam.ui.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CameraTopBar(
    state: CameraState,
    onFlashClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Flash button
        IconButton(onClick = onFlashClick) {
            Icon(
                imageVector = when (state.flashMode) {
                    FlashMode.OFF -> Icons.Filled.FlashOff
                    FlashMode.ON -> Icons.Filled.FlashOn
                    FlashMode.AUTO -> Icons.Filled.FlashAuto
                },
                contentDescription = "Flash",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }

        // Recording indicator
        if (state.isRecording) {
            RecordingIndicator(durationSeconds = state.recordingDurationSeconds)
        }

        // Zoom indicator
        if (state.zoomRatio > 1.01f) {
            Text(
                text = "%.1fx".format(state.zoomRatio),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun RecordingIndicator(durationSeconds: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "recording_alpha",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color.Red.copy(alpha = alpha)),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatDuration(durationSeconds),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun CameraBottomControls(
    state: CameraState,
    onCaptureClick: () -> Unit,
    onSwitchCameraClick: () -> Unit,
    onModeChange: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Mode selector
        ModeSelector(
            currentMode = state.cameraMode,
            onModeChange = { onModeChange() },
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Main controls row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Gallery button
            IconButton(
                onClick = onGalleryClick,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoLibrary,
                    contentDescription = "Gallery",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }

            // Capture button
            CaptureButton(
                isVideo = state.cameraMode == CameraMode.VIDEO,
                isRecording = state.isRecording,
                onClick = onCaptureClick,
            )

            // Switch camera button
            IconButton(
                onClick = onSwitchCameraClick,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Cameraswitch,
                    contentDescription = "Switch camera",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
private fun ModeSelector(
    currentMode: CameraMode,
    onModeChange: () -> Unit,
) {
    Row(
        modifier = Modifier
            .background(
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        ModeTab(
            label = "FOTO",
            isSelected = currentMode == CameraMode.PHOTO,
            onClick = { if (currentMode != CameraMode.PHOTO) onModeChange() },
        )
        ModeTab(
            label = "VIDEO",
            isSelected = currentMode == CameraMode.VIDEO,
            onClick = { if (currentMode != CameraMode.VIDEO) onModeChange() },
        )
    }
}

@Composable
private fun ModeTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
        fontSize = 13.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isSelected) {
                    Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun CaptureButton(
    isVideo: Boolean,
    isRecording: Boolean,
    onClick: () -> Unit,
) {
    val innerColor by animateColorAsState(
        targetValue = when {
            isRecording -> Color.Red
            isVideo -> Color.Red
            else -> Color.White
        },
        label = "capture_color",
    )

    val innerSize = if (isRecording) 28.dp else 58.dp
    val innerShape = if (isRecording) RoundedCornerShape(6.dp) else CircleShape

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .border(3.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(innerShape)
                .background(innerColor),
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
