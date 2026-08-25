/// Sole owner of the native New Relic mobile-agent transport.
public final class TelemetryProvider: ObservabilityProvider, @unchecked Sendable {
    public struct Dependencies: Sendable {
        public let newRelic: NewRelicConfiguration

        public init(newRelic: NewRelicConfiguration) {
            self.newRelic = newRelic
        }
    }

    public let identifier: ProviderIdentifier = .telemetry
    public let capabilities: Set<ObservabilityCapability> = [.telemetry]
    public let configuration: ProviderConfiguration
    public let status: ObservabilityStatus
    public var isEnabled: Bool { configuration.isEnabled }

    private let transport: any EventTransportProtocol

    public init(
        isEnabled: Bool = true,
        dependencies: Dependencies
    ) {
        configuration = ProviderConfiguration(isEnabled: isEnabled)

        if isEnabled,
           dependencies.newRelic.exportsToNewRelic || dependencies.newRelic.useConsoleWhenUnconfigured {
            transport = NewRelicSpanTransport(configuration: dependencies.newRelic)
            status = ObservabilityStatus(
                isConfigured: true,
                exportsToNewRelic: dependencies.newRelic.exportsToNewRelic,
                destination: dependencies.newRelic.exportsToNewRelic
                    ? "New Relic Mobile"
                    : "Xcode console (add a New Relic app token to export)"
            )
        } else {
            transport = NoOpEventTransport()
            status = ObservabilityStatus(
                isConfigured: true,
                exportsToNewRelic: false,
                destination: "Disabled"
            )
        }
    }

    public func initialize() throws {}

    @discardableResult
    public func send(_ event: AnyObservabilityEvent) -> NativeSpanContext? {
        guard isEnabled else { return nil }
        return transport.send(event)
    }

    public func forceFlush() {
        transport.forceFlush()
    }
}

public extension ObservabilityProvider where Self == TelemetryProvider {
    static func telemetryProvider(
        dependencies: TelemetryProvider.Dependencies
    ) -> TelemetryProvider {
        TelemetryProvider(dependencies: dependencies)
    }
}
