final class EventTracker: Sendable {
    private let transport: any EventTransportProtocol
    private let enricher: ContextEnricher

    init(
        transport: any EventTransportProtocol,
        enricher: ContextEnricher
    ) {
        self.transport = transport
        self.enricher = enricher
    }

    @discardableResult
    func track(_ event: AnyObservabilityEvent) -> NativeSpanContext? {
        transport.send(enricher.enrich(event))
    }

    func forceFlush() {
        transport.forceFlush()
    }
}
