package com.example.nativebooks.observability

/** Implemented by Swift and backed by the native New Relic SDK. */
interface IosTracerProvider {
    fun startNativeSpan(
        name: String,
        attributes: Map<String, String>,
        parent: KmpSpanContext?,
    ): KmpSpanContext

    fun endNativeSpan(
        context: KmpSpanContext,
        attributes: Map<String, String>,
        status: KmpSpanStatus,
    ): Boolean
}

actual object NativeTracer : KmpTracer {
    private var delegate: IosTracerProvider? = null

    fun initialize(provider: IosTracerProvider) {
        delegate = provider
    }

    actual override fun startSpan(
        name: String,
        attributes: Map<String, String>,
        parent: KmpSpanContext?,
    ): KmpSpanContext {
        val context = delegate?.startNativeSpan(name, attributes, parent) ?: KmpSpanContext.NO_OP
        println(
            "[KMP][NativeTracer] received native context " +
                "name=$name traceId=${context.traceId} spanId=${context.spanId} " +
                "sampled=${context.sampled} valid=${context.isValid}",
        )
        return context
    }

    actual override fun endSpan(
        context: KmpSpanContext,
        attributes: Map<String, String>,
        status: KmpSpanStatus,
    ): Boolean {
        val acknowledged = delegate?.endNativeSpan(context, attributes, status) ?: false
        println(
            "[KMP][NativeTracer] native end result " +
                "spanId=${context.spanId} status=$status acknowledged=$acknowledged",
        )
        return acknowledged
    }
}
