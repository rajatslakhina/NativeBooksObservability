package com.example.nativebooks.observability

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import java.util.concurrent.ConcurrentHashMap

actual object NativeTracer : KmpTracer {
    private val tracer by lazy {
        GlobalOpenTelemetry.getTracer("com.example.nativebooks.shared")
    }
    private val activeSpans = ConcurrentHashMap<String, Span>()

    actual override fun startSpan(
        name: String,
        attributes: Map<String, String>,
    ): KmpSpanContext {
        val nativeSpan = tracer.spanBuilder(name).startSpan()
        attributes.forEach { (key, value) -> nativeSpan.setAttribute(key, value) }

        val nativeContext = nativeSpan.spanContext
        val result = KmpSpanContext(
            traceId = nativeContext.traceId,
            spanId = nativeContext.spanId,
            sampled = nativeContext.isSampled,
        )
        if (result.isValid) {
            activeSpans[result.spanId] = nativeSpan
        } else {
            nativeSpan.end()
        }
        println(
            "[KMP][NativeTracer] received native context " +
                "name=$name traceId=${result.traceId} spanId=${result.spanId} " +
                "sampled=${result.sampled} valid=${result.isValid}",
        )
        return result
    }

    actual override fun endSpan(
        context: KmpSpanContext,
        attributes: Map<String, String>,
        status: KmpSpanStatus,
    ): Boolean {
        if (!context.isValid) {
            println(
                "[KMP][NativeTracer] native end result " +
                    "spanId=${context.spanId} status=$status acknowledged=false reason=invalid-context",
            )
            return false
        }
        val nativeSpan = activeSpans.remove(context.spanId)
        if (nativeSpan == null) {
            println(
                "[KMP][NativeTracer] native end result " +
                    "spanId=${context.spanId} status=$status acknowledged=false reason=span-not-found",
            )
            return false
        }
        attributes.forEach { (key, value) -> nativeSpan.setAttribute(key, value) }
        when (status) {
            KmpSpanStatus.OK -> nativeSpan.setStatus(StatusCode.OK)
            KmpSpanStatus.ERROR -> nativeSpan.setStatus(StatusCode.ERROR)
            KmpSpanStatus.UNSET -> Unit
        }
        nativeSpan.end()
        println(
            "[KMP][NativeTracer] native end result " +
                "spanId=${context.spanId} status=$status acknowledged=true",
        )
        return true
    }
}
