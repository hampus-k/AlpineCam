package com.alpinecam.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alpinecam.ui.admin.AdminScreen
import com.alpinecam.ui.admin.GroupsScreen
import com.alpinecam.ui.admin.SkiersScreen
import com.alpinecam.ui.camera.CameraScreen
import com.alpinecam.ui.gallery.GalleryScreen
import com.alpinecam.ui.gallery.MediaPreviewScreen
import com.alpinecam.ui.home.HomeScreen
import com.alpinecam.ui.training.RecordingFlowScreen
import com.alpinecam.ui.training.TrainingSessionsScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val CAMERA = "camera"
    const val CAMERA_TRAINING = "camera_training/{sessionId}/{skierId}/{skierName}"
    const val GALLERY = "gallery"
    const val MEDIA_PREVIEW = "media_preview/{uri}"
    const val ADMIN = "admin"
    const val GROUPS = "admin/groups"
    const val SKIERS = "admin/skiers"
    const val TRAINING_SESSIONS = "training_sessions"
    const val RECORDING_FLOW = "recording_flow?sessionId={sessionId}"

    fun mediaPreview(uri: String): String {
        val encoded = URLEncoder.encode(uri, "UTF-8")
        return "media_preview/$encoded"
    }

    fun cameraTraining(sessionId: Long, skierId: Long, skierName: String): String {
        val encodedName = URLEncoder.encode(skierName, "UTF-8")
        return "camera_training/$sessionId/$skierId/$encodedName"
    }

    fun recordingFlow(sessionId: Long? = null): String {
        return if (sessionId != null) "recording_flow?sessionId=$sessionId" else "recording_flow"
    }
}

@Composable
fun AlpineCamNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        // Home
        composable(Routes.HOME) {
            HomeScreen(
                onQuickRecord = {
                    navController.navigate(Routes.recordingFlow())
                },
                onTrainingSessions = {
                    navController.navigate(Routes.TRAINING_SESSIONS)
                },
                onAdmin = {
                    navController.navigate(Routes.ADMIN)
                },
            )
        }

        // Admin
        composable(Routes.ADMIN) {
            AdminScreen(
                onGroupsClick = { navController.navigate(Routes.GROUPS) },
                onSkiersClick = { navController.navigate(Routes.SKIERS) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.GROUPS) {
            GroupsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.SKIERS) {
            SkiersScreen(onBackClick = { navController.popBackStack() })
        }

        // Training Sessions
        composable(Routes.TRAINING_SESSIONS) {
            TrainingSessionsScreen(
                onBackClick = { navController.popBackStack() },
                onSessionClick = { sessionId ->
                    navController.navigate(Routes.recordingFlow(sessionId))
                },
                onNewSession = { sessionId ->
                    navController.navigate(Routes.recordingFlow(sessionId))
                },
            )
        }

        // Recording Flow (quick record or from existing session)
        composable(
            route = Routes.RECORDING_FLOW,
            arguments = listOf(
                navArgument("sessionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val sessionIdStr = backStackEntry.arguments?.getString("sessionId")
            val sessionId = sessionIdStr?.toLongOrNull()

            RecordingFlowScreen(
                sessionId = sessionId,
                onOpenCamera = { sid, skierId, skierName ->
                    navController.navigate(Routes.cameraTraining(sid, skierId, skierName))
                },
                onBackClick = { navController.popBackStack() },
            )
        }

        // Camera in training mode (with skier overlay)
        composable(
            route = Routes.CAMERA_TRAINING,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.LongType },
                navArgument("skierId") { type = NavType.LongType },
                navArgument("skierName") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val skierName = URLDecoder.decode(
                backStackEntry.arguments?.getString("skierName") ?: "", "UTF-8"
            )

            CameraScreen(
                onGalleryClick = { navController.navigate(Routes.GALLERY) },
                onMediaCaptured = { navController.popBackStack() },
                trainingSkierName = skierName,
            )
        }

        // Standalone camera (legacy)
        composable(Routes.CAMERA) {
            CameraScreen(
                onGalleryClick = { navController.navigate(Routes.GALLERY) },
                onMediaCaptured = { uri ->
                    navController.navigate(Routes.mediaPreview(uri.toString()))
                },
            )
        }

        // Gallery
        composable(Routes.GALLERY) {
            GalleryScreen(
                onBackClick = { navController.popBackStack() },
                onMediaClick = { uri ->
                    navController.navigate(Routes.mediaPreview(uri.toString()))
                },
            )
        }

        // Media Preview
        composable(
            route = Routes.MEDIA_PREVIEW,
            arguments = listOf(navArgument("uri") { type = NavType.StringType }),
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("uri") ?: return@composable
            val decodedUri = URLDecoder.decode(encodedUri, "UTF-8")
            MediaPreviewScreen(
                uri = decodedUri,
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}
