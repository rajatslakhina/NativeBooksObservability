protocol EventEnriching: Sendable {
    func enrich(_ event: AnyObservabilityEvent) -> AnyObservabilityEvent
}
