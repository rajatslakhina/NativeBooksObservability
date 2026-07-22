/// A closed, Sendable attribute value surface shared by every transport.
/// Keeping this typed avoids `[String: Any]` and `@unchecked Sendable` events.
public enum ObservabilityValue: Sendable, Equatable {
    case string(String)
    case int(Int)
    case double(Double)
    case bool(Bool)
}

public extension Dictionary where Key == String, Value == String {
    var observabilityValues: [String: ObservabilityValue] {
        mapValues(ObservabilityValue.string)
    }
}
