struct ContextEnricher: Sendable {
    private let enrichers: [any EventEnriching]

    init(enrichers: [any EventEnriching]) {
        self.enrichers = enrichers
    }

    func enrich(_ event: AnyObservabilityEvent) -> AnyObservabilityEvent {
        enrichers.reduce(event) { current, enricher in
            enricher.enrich(current)
        }
    }
}
