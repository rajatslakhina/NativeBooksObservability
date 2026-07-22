/// Type-erased envelope routed to registered providers.
public struct AnyObservabilityEvent: Sendable, Equatable {
    public let type: EventType
    public let attributes: [String: ObservabilityValue]

    public init(
        type: EventType,
        attributes: [String: ObservabilityValue] = [:]
    ) {
        self.type = type
        self.attributes = attributes
    }
}
