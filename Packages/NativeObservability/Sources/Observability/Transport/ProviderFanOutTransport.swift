struct ProviderFanOutTransport: EventTransportProtocol, Sendable {
    private let providers: [any ObservabilityProvider]

    init(providers: [any ObservabilityProvider]) {
        self.providers = providers
    }

    func send(_ event: AnyObservabilityEvent) -> NativeSpanContext? {
        var primary: NativeSpanContext?
        for provider in providers where provider.isEnabled && provider.capabilities.contains(event.capability) {
            let context = provider.send(event)
            if primary == nil { primary = context }
        }
        return primary
    }

    func forceFlush() {
        providers.forEach { $0.forceFlush() }
    }
}

private extension AnyObservabilityEvent {
    var capability: ObservabilityCapability {
        switch type {
        case .telemetry: return .telemetry
        }
    }
}
