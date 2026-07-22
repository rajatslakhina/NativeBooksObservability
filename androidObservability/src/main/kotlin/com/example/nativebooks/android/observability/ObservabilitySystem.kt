package com.example.nativebooks.android.observability

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.TimeUnit

data class ObservabilityConfiguration(
    val newRelicLicenseKey: String,
    val serviceName: String = "native-books-android",
    val serviceVersion: String = "1.0.0",
    val environment: String = "development",
    val endpoint: String = "https://otlp.nr-data.net/v1/traces",
)

data class ObservabilityStatus(
    val exportsToNewRelic: Boolean,
    val destination: String,
)

/** Native Android owner of OpenTelemetry SDK setup and export. */
object ObservabilitySystem {
    private var tracerProvider: SdkTracerProvider? = null
    private var configuredStatus: ObservabilityStatus? = null

    @Synchronized
    fun configure(configuration: ObservabilityConfiguration): ObservabilityStatus {
        configuredStatus?.let { return it }

        val licenseKey = configuration.newRelicLicenseKey.trim()
        val exporter: SpanExporter
        val status: ObservabilityStatus
        if (licenseKey.isEmpty()) {
            exporter = LoggingSpanExporter.create()
            status = ObservabilityStatus(
                exportsToNewRelic = false,
                destination = "Android Studio Logcat",
            )
        } else {
            exporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(configuration.endpoint)
                .addHeader("api-key", licenseKey)
                .build()
            status = ObservabilityStatus(
                exportsToNewRelic = true,
                destination = "New Relic OTLP/HTTP",
            )
        }

        val resource = Resource.getDefault().merge(
            Resource.create(
                Attributes.builder()
                    .put(AttributeKey.stringKey("service.name"), configuration.serviceName)
                    .put(AttributeKey.stringKey("service.version"), configuration.serviceVersion)
                    .put(AttributeKey.stringKey("deployment.environment.name"), configuration.environment)
                    .put(AttributeKey.stringKey("app.platform"), "Android")
                    .build(),
            ),
        )
        val provider = SdkTracerProvider.builder()
            .setResource(resource)
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build()

        OpenTelemetrySdk.builder()
            .setTracerProvider(provider)
            .buildAndRegisterGlobal()

        tracerProvider = provider
        configuredStatus = status
        return status
    }

    fun forceFlush(timeoutSeconds: Long = 10): Boolean =
        tracerProvider
            ?.forceFlush()
            ?.join(timeoutSeconds, TimeUnit.SECONDS)
            ?.isSuccess
            ?: false

}
