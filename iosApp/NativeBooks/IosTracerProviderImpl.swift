import Foundation
import Observability
import SharedKit

/// Native implementation of the protocol generated from iosMain Kotlin.
final class IosTracerProviderImpl: NSObject, IosTracerProvider {
    func startNativeSpan(
        name: String,
        attributes: [String: String],
        parent: KmpSpanContext?
    ) -> KmpSpanContext {
        let nativeParent = parent.map {
            NativeSpanContext(
                traceId: $0.traceId,
                spanId: $0.spanId,
                sampled: $0.sampled,
                propagationHeaders: $0.propagationHeaders
            )
        }
        let nativeContext = ObservabilitySystem.beginSpan(
            name: name,
            attributes: attributes,
            parent: nativeParent
        )

        return KmpSpanContext(
            traceId: nativeContext.traceId,
            spanId: nativeContext.spanId,
            sampled: nativeContext.sampled,
            propagationHeaders: nativeContext.propagationHeaders
        )
    }

    func endNativeSpan(
        context: KmpSpanContext,
        attributes: [String: String],
        status: KmpSpanStatus
    ) -> Bool {
        let nativeStatus: SpanStatus
        if status === KmpSpanStatus.ok {
            nativeStatus = .ok
        } else if status === KmpSpanStatus.error {
            nativeStatus = .error(description: attributes["error.message"] ?? "KMP operation failed")
        } else {
            nativeStatus = .unset
        }

        return ObservabilitySystem.endSpan(
            NativeSpanContext(
                traceId: context.traceId,
                spanId: context.spanId,
                sampled: context.sampled,
                propagationHeaders: context.propagationHeaders
            ),
            attributes: attributes,
            status: nativeStatus
        )
    }
}
