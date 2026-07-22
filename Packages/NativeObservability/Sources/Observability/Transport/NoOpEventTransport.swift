public struct NoOpEventTransport: EventTransportProtocol, Sendable {
    public init() {}

    public func send(_ event: AnyObservabilityEvent) -> NativeSpanContext? {
        nil
    }
}
