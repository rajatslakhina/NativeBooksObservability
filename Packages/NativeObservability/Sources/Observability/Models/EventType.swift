/// Events accepted by the synchronous observability pipeline.
public enum EventType: Sendable, Equatable {
    case telemetry(TelemetryEvent)
}

public enum TelemetryEvent: Sendable, Equatable {
    case log(name: String, severity: LogSeverity)
    case breadcrumb(name: String)
    case span(SpanOperation)
}

public enum LogSeverity: String, Sendable, Equatable, CaseIterable {
    case debug
    case info
    case warning
    case error
}

/// Span operations use the native span id as their stable registry key.
/// This avoids name collisions when the same operation runs concurrently.
public enum SpanOperation: Sendable, Equatable {
    case start(name: String, parentSpanId: String?)
    case addAttributes(spanId: String, attributes: [String: ObservabilityValue])
    case addEvent(spanId: String, name: String, attributes: [String: ObservabilityValue])
    case setStatus(spanId: String, status: SpanStatus)
    case recordError(spanId: String, message: String, domain: String?)
    case end(spanId: String, attributes: [String: ObservabilityValue], status: SpanStatus)
}

public enum SpanStatus: Sendable, Equatable {
    case ok
    case error(description: String)
    case unset
}
