package com.jjrapps.aquihaytomate

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.jjrapps.aquihaytomate.ui.navigation.AquiHayTomateNavGraph
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.BackgroundVoid
import dagger.hilt.android.AndroidEntryPoint

/**
 * Extends [AppCompatActivity] rather than ComponentActivity because runtime locale switching
 * goes through `AppCompatDelegate.setApplicationLocales`.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AquiHayTomateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundVoid,
                ) {
                    AquiHayTomateNavGraph()
                }
            }
        }
    }
}
