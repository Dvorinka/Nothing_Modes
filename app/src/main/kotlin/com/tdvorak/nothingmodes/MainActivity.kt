package com.tdvorak.nothingmodes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tdvorak.nothingmodes.nav.NothingModesNavHost
import com.tdvorak.nothingmodes.ui.theme.NothingModesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NothingModesTheme {
                NothingModesNavHost()
            }
        }
    }
}
