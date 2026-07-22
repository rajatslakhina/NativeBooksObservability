/// Synchronous fan-out useful for migrations and test spies.
public struct CompositeEventTransport: EventTransportProtocol, Sendable {
    private let transports: [any EventTransportProtocol]

    public init(_ transports: [any EventTransportProtocol]) {
        self.transports = transports
    }

    public func send(_ event: AnyObservabilityEvent) -> NativeSpanContext? {
        var primary: NativeSpanContext?
        for transport in transports {
            let context = transport.send(event)
            if primary == nil { primary = context }
        }
        return primary
    }

    public func forceFlush() {
        transports.forEach { $0.forceFlush() }
    }
}
