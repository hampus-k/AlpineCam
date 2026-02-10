package com.alpinecam.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alpinecam.ui.camera.CameraScreen
import com.alpinecam.ui.gallery.GalleryScreen
import com.alpinecam.ui.gallery.MediaPreviewScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val CAMERA = "camera"
    const val GALLERY = "gallery"
    const val MEDIA_PREVIEW = "media_preview/{uri}"

    fun mediaPreview(uri: String): String {
        val encoded = URLEncoder.encode(uri, "UTF-8")
        return "media_preview/$encoded"
    }
}

@Composable
fun AlpineCamNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.CAMERA,
    ) {
        composable(Routes.CAMERA) {
            CameraScreen(
                onGalleryClick = { navController.navigate(Routes.GALLERY) },
                onMediaCaptured = { uri ->
                    navController.navigate(Routes.mediaPreview(uri.toString()))
                },
            )
        }

        composable(Routes.GALLERY) {
            GalleryScreen(
                onBackClick = { navController.popBackStack() },
                onMediaClick = { uri ->
                    navController.navigate(Routes.mediaPreview(uri.toString()))
                },
            )
        }

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
