package com.tecsup.nexusmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tecsup.nexusmobile.presentation.ui.NexusApp
import com.tecsup.nexusmobile.ui.theme.Nexus_mobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Nexus_mobileTheme {
                NexusApp()
            }
        }
    }
}