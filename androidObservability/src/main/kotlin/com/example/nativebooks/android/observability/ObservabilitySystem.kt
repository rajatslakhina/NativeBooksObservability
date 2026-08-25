package com.example.nativebooks.android.observability

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.newrelic.agent.android.FeatureFlag
import com.newrelic.agent.android.NewRelic
import com.newrelic.agent.android.TaskQueue
import com.newrelic.agent.android.api.common.TransactionData
import com.newrelic.agent.android.distributedtracing.DistributedTracing
import com.newrelic.agent.android.distributedtracing.TraceContext
import com.newrelic.agent.android.distributedtracing.TraceParent
import com.newrelic.agent.android.distributedtracing.TraceState
import com.newrelic.agent.android.instrumentation.TransactionState
import com.newrelic.agent.android.measurement.HttpTransactionMeasurement
import java.util.concurrent.ConcurrentHashMap

data class ObservabilityConfiguration(
    val newRelicApplicationToken: String,
    val serviceName: String = "native-books-android",
    val serviceVersion: String = "1.0.0",
    val environment: String = "development",
)

data class ObservabilityStatus(
    val exportsToNewRelic: Boolean,
    val destination: String,
)

data class NativeSpan(
    val traceId: String,
    val spanId: String,
    val sampled: Boolean,
    val propagationHeaders: Map<String, String> = emptyMap(),
) {
    val isValid: Boolean
        get() = traceId.isValidW3cId(32) && spanId.isValidW3cId(16)

    companion object {
        val Invalid = NativeSpan(
            traceId = "",
            spanId = "",
            sampled = false,
            propagationHeaders = emptyMap(),
        )
    }
}

private data class ActiveSpan(
    val context: NativeSpan,
    val name: String,
    val interactionId: String?,
    val startedAtNanos: Long,
    val transactionState: TransactionState,
    val traceContext: TraceContext,
    val attributes: MutableMap<String, String>,
)

private data class GeneratedTraceContext(
    val context: NativeSpan,
    val transactionState: TransactionState,
    val traceContext: TraceContext,
)

/** Native Android owner of New Relic Mobile setup and custom interactions. */
object ObservabilitySystem {
    private val activeSpans = ConcurrentHashMap<String, ActiveSpan>()

    @Volatile
    private var configuration: ObservabilityConfiguration? = null

    @Volatile
    private var configuredStatus: ObservabilityStatus? = null

    @Synchronized
    fun configure(configuration: ObservabilityConfiguration): ObservabilityStatus {
        configuredStatus?.let { return it }
        val exports = configuration.newRelicApplicationToken.normalizedToken() != null
        this.configuration = configuration
        return ObservabilityStatus(
            exportsToNewRelic = exports,
            destination = if (exports) {
                "New Relic Mobile"
            } else {
                "Android Studio Logcat (add a New Relic app token to export)"
            },
        ).also { configuredStatus = it }
    }

    /** Must be called from the launch Activity so the agent can instrument its lifecycle. */
    @Synchronized
    fun start(activity: Activity) {
        if (NewRelic.isStarted()) return
        val settings = configuration ?: return
        val token = settings.newRelicApplicationToken.normalizedToken() ?: return

        // Feature flags must be configured before start. Keep every stable collection
        // feature explicit so local defaults cannot silently reduce coverage.
        NewRelic.enableFeature(FeatureFlag.CrashReporting)
        NewRelic.enableFeature(FeatureFlag.NativeReporting)
        NewRelic.enableFeature(FeatureFlag.HandledExceptions)
        NewRelic.enableFeature(FeatureFlag.JSError)
        NewRelic.enableFeature(FeatureFlag.AnalyticsEvents)
        NewRelic.enableFeature(FeatureFlag.InteractionTracing)
        NewRelic.enableFeature(FeatureFlag.DefaultInteractions)
        NewRelic.enableFeature(FeatureFlag.NetworkRequests)
        NewRelic.enableFeature(FeatureFlag.NetworkErrorRequests)
        NewRelic.enableFeature(FeatureFlag.HttpResponseBodyCapture)
        NewRelic.enableFeature(FeatureFlag.DistributedTracing)
        NewRelic.enableFeature(FeatureFlag.AppStartMetrics)
        NewRelic.enableFeature(FeatureFlag.ApplicationExitReporting)
        NewRelic.enableFeature(FeatureFlag.Jetpack)
        NewRelic.enableFeature(FeatureFlag.LogReporting)
        NewRelic.enableFeature(FeatureFlag.OfflineStorage)
        NewRelic.enableFeature(FeatureFlag.EventPersistence)
        NewRelic.enableFeature(FeatureFlag.BackgroundReporting)
        NewRelic.withApplicationToken(token)
            .withApplicationVersion(settings.serviceVersion)
            .withCrashReportingEnabled(true)
            .withLoggingEnabled(true)
            .start(activity.applicationContext)
        NewRelic.setAttribute("service.name", settings.serviceName)
        NewRelic.setAttribute("deployment.environment.name", settings.environment)
        NewRelic.setAttribute("app.platform", "Android")
        recordSessionState(activity, state = "launch", reason = "agent_started")
    }

    /** Emits Embrace-style session lifecycle and device context keyed by New Relic session ID. */
    fun recordSessionState(activity: Activity, state: String, reason: String) {
        if (!NewRelic.isStarted()) return
        val attributes = mutableMapOf<String, Any>(
            "lifecycle.state" to state,
            "lifecycle.reason" to reason,
            "app.platform" to "Android",
            "connection.type" to connectionType(activity),
            "device.orientation" to orientation(activity),
        )
        attributes.putAll(batteryAttributes(activity))
        attributes.putAll(systemHealthAttributes(activity))
        attributes.putAll(currentSessionAttributes())
        NewRelic.recordCustomEvent("MobileSessionState", state, attributes)
    }

    fun beginSpan(
        name: String,
        attributes: Map<String, String>,
        parent: NativeSpan? = null,
    ): NativeSpan {
        if (!NewRelic.isStarted()) return NativeSpan.Invalid

        val interactionId = NewRelic.startInteraction(name)
        val generatedContext = newRelicSpanContext(name, parent?.takeIf { it.isValid })
        if (generatedContext == null || !generatedContext.context.isValid) {
            NewRelic.endInteraction(interactionId)
            return NativeSpan.Invalid
        }
        val context = generatedContext.context
        activeSpans[context.spanId] = ActiveSpan(
            context = context,
            name = name,
            interactionId = interactionId,
            startedAtNanos = System.nanoTime(),
            transactionState = generatedContext.transactionState,
            traceContext = generatedContext.traceContext,
            attributes = attributes.toMutableMap().apply {
                parent?.takeIf { it.isValid }?.let { put("parent.span.id", it.spanId) }
            },
        )
        return context
    }

    /**
     * Ask the New Relic agent to create the distributed trace context. The mobile
     * interaction API returns only an interaction ID, so it cannot be treated as
     * either a trace ID or a span ID.
     */
    private fun newRelicSpanContext(
        name: String,
        parent: NativeSpan?,
    ): GeneratedTraceContext? = runCatching {
        val transactionState = TransactionState().apply {
            url = standardSpanUrl(name)
            httpMethod = "POST"
        }
        val traceContext = DistributedTracing.getInstance().startTrace(transactionState)
        transactionState.trace = traceContext
        val sdkSpanId = traceContext.tracePayload.spanId.lowercase()
        val traceId = parent?.traceId ?: traceContext.traceId.lowercase()
        val sampled = parent?.sampled ?: (
            traceContext.sampled.equals("true", ignoreCase = true) ||
                traceContext.sampled == "1" || traceContext.sampled == "01"
            )
        val propagationHeaders = if (parent == null) {
            buildMap {
                traceContext.headers.forEach { put(it.headerName, it.headerValue) }
                val traceParent = TraceParent.createTraceParent(traceContext)
                put(traceParent.headerName, traceParent.headerValue)
                val traceState = TraceState.createTraceState(traceContext)
                put(traceState.headerName, traceState.headerValue)
            }
        } else {
            mapOf("traceparent" to w3cTraceParent(traceId, sdkSpanId, sampled))
        }
        GeneratedTraceContext(
            context = NativeSpan(
                traceId = traceId,
                spanId = sdkSpanId,
                sampled = sampled,
                propagationHeaders = propagationHeaders,
            ),
            transactionState = transactionState,
            traceContext = traceContext,
        )
    }.getOrElse {
        NewRelic.recordHandledException(
            IllegalStateException("New Relic could not create a distributed trace context", it),
        )
        null
    }

    fun endSpan(
        context: NativeSpan,
        attributes: Map<String, String>,
        status: String,
    ): Boolean {
        val span = activeSpans.remove(context.spanId) ?: return false
        val effectiveStatus = if (
            status.equals("unset", ignoreCase = true) &&
            span.attributes["span.status"].equals("error", ignoreCase = true)
        ) {
            "error"
        } else {
            status.lowercase()
        }
        span.attributes.putAll(attributes)
        span.attributes["trace.id"] = context.traceId
        span.attributes["span.id"] = context.spanId
        span.attributes["span.status"] = effectiveStatus
        span.attributes["duration.ms"] =
            ((System.nanoTime() - span.startedAtNanos) / 1_000_000.0).toString()
        span.attributes.putAll(currentSessionAttributes())
        if (span.interactionId != null && NewRelic.isStarted()) {
            recordStandardSpan(span, effectiveStatus)
            NewRelic.endInteraction(span.interactionId)
            NewRelic.recordCustomEvent(
                "NativeSpan",
                span.name,
                span.attributes.mapValues { it.value as Any },
            )
        } else {
            println("[NativeBooks][NewRelic] Completed span ${span.name} ${context.spanId}")
        }
        return true
    }

    fun addSpanAttributes(context: NativeSpan, attributes: Map<String, String>): Boolean {
        val span = activeSpans[context.spanId] ?: return false
        synchronized(span) { span.attributes.putAll(attributes) }
        return true
    }

    fun addSpanEvent(
        context: NativeSpan,
        name: String,
        attributes: Map<String, String> = emptyMap(),
    ): Boolean {
        val span = activeSpans[context.spanId] ?: return false
        val eventAttributes = synchronized(span) {
            span.attributes.toMutableMap().apply {
                putAll(attributes)
                put("trace.id", context.traceId)
                put("span.id", context.spanId)
                putAll(currentSessionAttributes())
            }
        }
        return NewRelic.recordCustomEvent(
            "NativeSpanEvent",
            name,
            eventAttributes.mapValues { it.value as Any },
        )
    }

    fun setSpanStatus(context: NativeSpan, status: String): Boolean {
        val span = activeSpans[context.spanId] ?: return false
        synchronized(span) { span.attributes["span.status"] = status.lowercase() }
        return true
    }

    fun recordSpanError(context: NativeSpan, message: String, domain: String? = null): Boolean {
        val span = activeSpans[context.spanId] ?: return false
        val errorAttributes = synchronized(span) {
            span.attributes["span.status"] = "error"
            span.attributes["error.message"] = message
            if (domain != null) span.attributes["error.domain"] = domain
            span.attributes.toMutableMap().apply {
                put("trace.id", context.traceId)
                put("span.id", context.spanId)
                putAll(currentSessionAttributes())
            }
        }
        NewRelic.recordHandledException(
            IllegalStateException(message),
            errorAttributes.mapValues { it.value as Any },
        )
        return true
    }

    fun recordBreadcrumb(name: String, attributes: Map<String, String> = emptyMap()): Boolean =
        NewRelic.recordBreadcrumb(
            name,
            (attributes + currentSessionAttributes()).mapValues { it.value as Any },
        )

    fun recordLog(
        message: String,
        severity: String = "info",
        attributes: Map<String, String> = emptyMap(),
    ) {
        NewRelic.logAttributes(
            (attributes + currentSessionAttributes() + mapOf("message" to message))
                .mapValues { it.value as Any },
        )
        when (severity.lowercase()) {
            "debug" -> NewRelic.logDebug(message)
            "warning", "warn" -> NewRelic.logWarning(message)
            "error" -> NewRelic.logError(message)
            else -> NewRelic.logInfo(message)
        }
    }

    /**
     * New Relic Mobile produces standard Span events from transactional network
     * measurements. This records an explicitly tagged HTTP-like transaction with
     * the New Relic-generated trace attributes, without performing a network call.
     */
    private fun recordStandardSpan(span: ActiveSpan, status: String) {
        val traceAttributes = span.traceContext.asTraceAttributes().toMutableMap().apply {
            put("trace.id", span.context.traceId)
            put("id", span.context.spanId)
            put("guid", span.context.spanId)
            put("span.id", span.context.spanId)
            span.attributes["parent.span.id"]?.let { put("parent.id", it) }
            put("native.span.name", span.name)
            put("native.span.source", "new-relic-mobile-sdk")
            put("native.span.synthetic_transaction", true)
        }
        val transactionData: TransactionData = span.transactionState.apply {
            statusCode = if (status.equals("error", ignoreCase = true)) 500 else 200
            bytesSent = 0L
            bytesReceived = 0L
            params = span.attributes
        }.end() ?: return
        transactionData.traceAttributes = traceAttributes
        TaskQueue.queue(HttpTransactionMeasurement(transactionData))
    }

    private fun standardSpanUrl(name: String): String =
        "https://native-span.invalid/" +
            java.net.URLEncoder.encode(name, Charsets.UTF_8.name()).replace("+", "%20")

    private fun w3cTraceParent(traceId: String, spanId: String, sampled: Boolean): String =
        "00-$traceId-$spanId-${if (sampled) "01" else "00"}"

    fun forceFlush(): Boolean {
        // New Relic Mobile owns its harvest schedule and has no public flush API.
        return NewRelic.isStarted() || configuredStatus != null
    }

    private fun String.normalizedToken(): String? =
        trim().takeIf { it.isNotEmpty() && it != "YOUR_NEW_RELIC_APP_TOKEN" }

    private fun currentSessionAttributes(): Map<String, String> {
        if (!NewRelic.isStarted()) return emptyMap()
        val sessionId = NewRelic.currentSessionId().trim()
        if (sessionId.isEmpty()) return emptyMap()
        return mapOf(
            "service.session.id" to sessionId,
            "newrelic.session.id" to sessionId,
        )
    }

    private fun connectionType(context: Context): String {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return "offline"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
            else -> "other"
        }
    }

    private fun orientation(context: Context): String = when (
        context.resources.configuration.orientation
    ) {
        Configuration.ORIENTATION_LANDSCAPE -> "landscape"
        Configuration.ORIENTATION_PORTRAIT -> "portrait"
        else -> "unknown"
    }

    private fun batteryAttributes(context: Context): Map<String, Any> {
        val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return emptyMap()
        val level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryStatus = status.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = status.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        return buildMap {
            if (level >= 0 && scale > 0) put("battery.level.percent", level * 100.0 / scale)
            put(
                "battery.state",
                when (batteryStatus) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
                    BatteryManager.BATTERY_STATUS_FULL -> "full"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
                    else -> "unknown"
                },
            )
            put("battery.plugged", plugged != 0)
        }
    }

    private fun systemHealthAttributes(context: Context): Map<String, Any> {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memory = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val powerManager = context.getSystemService(PowerManager::class.java)
        return buildMap {
            put("memory.available.mb", memory.availMem / 1_048_576.0)
            put("memory.low", memory.lowMemory)
            put("power.saveMode", powerManager.isPowerSaveMode)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put("thermal.status", powerManager.currentThermalStatus)
            }
        }
    }

}

private fun String.isValidW3cId(length: Int): Boolean =
    this.length == length &&
        any { it != '0' } &&
        all { it in '0'..'9' || it in 'a'..'f' }
