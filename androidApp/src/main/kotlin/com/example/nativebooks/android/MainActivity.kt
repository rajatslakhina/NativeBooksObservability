package com.example.nativebooks.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.nativebooks.android.ui.NativeBooksApp
import com.example.nativebooks.android.ui.NativeBooksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val status = (application as NativeBooksApplication).observabilityStatus
        setContent {
            NativeBooksTheme {
                NativeBooksApp(observabilityStatus = status)
            }
        }
    }
}
