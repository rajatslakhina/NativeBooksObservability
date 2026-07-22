package com.example.nativebooks.observability

data class KmpSpanContext(
    val traceId: String,
    val spanId: String,
    val sampled: Boolean,
) {
    val isValid: Boolean
        get() = traceId.isValidW3cId(32) && spanId.isValidW3cId(16)

    internal fun traceparent(): String? {
        if (!isValid) return null
        val flags = if (sampled) "01" else "00"
        return "00-$traceId-$spanId-$flags"
    }

    companion object {
        val NO_OP = KmpSpanContext(traceId = "", spanId = "", sampled = false)
    }
}

enum class KmpSpanStatus {
    UNSET,
    OK,
    ERROR,
}

/** Platform-independent contract used by repositories and view models. */
interface KmpTracer {
    fun startSpan(
        name: String,
        attributes: Map<String, String> = emptyMap(),
    ): KmpSpanContext

    /** Returns true only when the native layer found and ended this exact span. */
    fun endSpan(
        context: KmpSpanContext,
        attributes: Map<String, String> = emptyMap(),
        status: KmpSpanStatus = KmpSpanStatus.UNSET,
    ): Boolean
}

expect object NativeTracer : KmpTracer {
    override fun startSpan(
        name: String,
        attributes: Map<String, String>,
    ): KmpSpanContext

    override fun endSpan(
        context: KmpSpanContext,
        attributes: Map<String, String>,
        status: KmpSpanStatus,
    ): Boolean
}

private fun String.isValidW3cId(expectedLength: Int): Boolean =
    length == expectedLength &&
        all { it in '0'..'9' || it in 'a'..'f' } &&
        any { it != '0' }
