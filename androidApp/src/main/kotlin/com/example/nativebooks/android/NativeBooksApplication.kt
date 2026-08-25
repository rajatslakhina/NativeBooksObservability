package com.example.nativebooks.android

import android.app.Application
import android.util.Log
import com.example.nativebooks.android.observability.ObservabilityConfiguration
import com.example.nativebooks.android.observability.ObservabilityStatus
import com.example.nativebooks.android.observability.ObservabilitySystem

class NativeBooksApplication : Application() {
    lateinit var observabilityStatus: ObservabilityStatus
        private set

    override fun onCreate() {
        super.onCreate()
        observabilityStatus = ObservabilitySystem.configure(
            ObservabilityConfiguration(
                newRelicApplicationToken = BuildConfig.NEW_RELIC_APP_TOKEN,
                serviceName = "native-books-android",
                serviceVersion = BuildConfig.VERSION_NAME,
                environment = if (BuildConfig.DEBUG) "development" else "production",
            ),
        )
        Log.i("NativeBooks", "Observability destination=${observabilityStatus.destination}")
    }
}
