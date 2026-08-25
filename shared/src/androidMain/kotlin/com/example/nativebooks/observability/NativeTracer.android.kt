package com.example.nativebooks.observability

import com.example.nativebooks.android.observability.NativeSpan
import com.example.nativebooks.android.observability.ObservabilitySystem

actual object NativeTracer : KmpTracer {
    actual override fun startSpan(
        name: String,
        attributes: Map<String, String>,
        parent: KmpSpanContext?,
    ): KmpSpanContext {
        val nativeContext = ObservabilitySystem.beginSpan(
            name = name,
            attributes = attributes,
            parent = parent?.takeIf { it.isValid }?.let {
                NativeSpan(
                    traceId = it.traceId,
                    spanId = it.spanId,
                    sampled = it.sampled,
                    propagationHeaders = it.propagationHeaders,
                )
            },
        )
        return KmpSpanContext(
            traceId = nativeContext.traceId,
            spanId = nativeContext.spanId,
            sampled = nativeContext.sampled,
            propagationHeaders = nativeContext.propagationHeaders,
        ).also { result ->
            println(
                "[KMP][NativeTracer] received native context " +
                    "name=$name traceId=${result.traceId} spanId=${result.spanId} " +
                    "sampled=${result.sampled} valid=${result.isValid}",
            )
        }
    }

    actual override fun endSpan(
        context: KmpSpanContext,
        attributes: Map<String, String>,
        status: KmpSpanStatus,
    ): Boolean {
        if (!context.isValid) return false
        return ObservabilitySystem.endSpan(
            context = NativeSpan(context.traceId, context.spanId, context.sampled),
            attributes = attributes,
            status = status.name,
        ).also { acknowledged ->
            println(
                "[KMP][NativeTracer] native end result " +
                    "spanId=${context.spanId} status=$status acknowledged=$acknowledged",
            )
        }
    }
}
