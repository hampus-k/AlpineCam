package com.alpinecam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.alpinecam.ui.navigation.AlpineCamNavHost
import com.alpinecam.ui.theme.AlpineCamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlpineCamTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlpineCamNavHost()
                }
            }
        }
    }
}
