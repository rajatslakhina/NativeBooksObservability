package com.example.nativebooks.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.nativebooks.android.observability.ObservabilitySystem
import com.example.nativebooks.android.ui.NativeBooksApp
import com.example.nativebooks.android.ui.NativeBooksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ObservabilitySystem.start(this)
        enableEdgeToEdge()

        val status = (application as NativeBooksApplication).observabilityStatus
        setContent {
            NativeBooksTheme {
                NativeBooksApp(observabilityStatus = status)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ObservabilitySystem.recordSessionState(this, state = "foreground", reason = "activity_started")
    }

    override fun onResume() {
        super.onResume()
        ObservabilitySystem.recordSessionState(this, state = "active", reason = "activity_resumed")
    }

    override fun onPause() {
        ObservabilitySystem.recordSessionState(this, state = "inactive", reason = "activity_paused")
        super.onPause()
    }

    override fun onStop() {
        ObservabilitySystem.recordSessionState(this, state = "background", reason = "activity_stopped")
        super.onStop()
    }
}
