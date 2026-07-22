@preconcurrency import OpenTelemetryApi

extension ObservabilityValue {
    var otelValue: AttributeValue {
        switch self {
        case .string(let value): return .string(value)
        case .int(let value): return .int(value)
        case .double(let value): return .double(value)
        case .bool(let value): return .bool(value)
        }
    }
}

extension Dictionary where Key == String, Value == ObservabilityValue {
    var otelAttributes: [String: AttributeValue] {
        mapValues(\.otelValue)
    }
}

extension LogSeverity {
    var otelSeverity: Severity {
        switch self {
        case .debug: return .debug
        case .info: return .info
        case .warning: return .warn
        case .error: return .error
        }
    }
}

extension Span {
    func apply(_ value: SpanStatus) {
        switch value {
        case .ok: status = .ok
        case .error(let description): status = .error(description: description)
        case .unset: status = .unset
        }
    }
}
