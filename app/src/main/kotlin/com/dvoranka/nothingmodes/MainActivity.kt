package com.dvoranka.nothingmodes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dvoranka.nothingmodes.nav.NothingModesNavHost
import com.dvoranka.nothingmodes.ui.theme.NothingModesTheme
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
