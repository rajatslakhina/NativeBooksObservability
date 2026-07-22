import Foundation

/// Synchronous, thread-safe façade for the native observability pipeline.
public enum ObservabilitySystem {
    private static let state = ObservabilitySystemState()

    /// Builds and installs the provider graph. Repeated calls return the status
    /// of the first completed configuration instead of registering OTel twice.
    @discardableResult
    public static func configure(
        context: some AppContextProviding,
        _ build: (ObservabilityConfiguration.Builder) -> Void
    ) -> ObservabilityStatus {
        let builder = ObservabilityConfiguration.Builder()
        build(builder)
        return state.configure(context: context, configuration: builder.build())
    }

    @discardableResult
    public static func configure(
        context: some AppContextProviding,
        configuration: ObservabilityConfiguration
    ) -> ObservabilityStatus {
        state.configure(context: context, configuration: configuration)
    }

    public static var status: ObservabilityStatus { state.status }

    @discardableResult
    public static func track(
        type: EventType,
        attributes: [String: ObservabilityValue] = [:]
    ) -> NativeSpanContext? {
        state.track(AnyObservabilityEvent(type: type, attributes: attributes))
    }

    /// Opens a native span and keeps it in the package registry until `endSpan`.
    @discardableResult
    public static func beginSpan(
        name: String,
        attributes: [String: String] = [:],
        parent: NativeSpanContext? = nil
    ) -> NativeSpanContext {
        track(
            type: .telemetry(.span(.start(name: name, parentSpanId: parent?.spanId))),
            attributes: attributes.observabilityValues
        ) ?? .invalid
    }

    @discardableResult
    public static func endSpan(
        _ context: NativeSpanContext,
        attributes: [String: String] = [:],
        status: SpanStatus = .unset
    ) -> Bool {
        guard context.isValid else { return false }
        let endedContext = track(
            type: .telemetry(
                .span(
                    .end(
                        spanId: context.spanId,
                        attributes: attributes.observabilityValues,
                        status: status
                    )
                )
            )
        )
        return endedContext == context
    }

    /// KMP compatibility API. Span creation and completion are synchronous.
    /// Only native trace identifiers are returned; network propagation is a KMP concern.
    @discardableResult
    public static func startSpan(
        name: String,
        attributes: [String: String] = [:]
    ) -> NativeSpanContext {
        let context = beginSpan(name: name, attributes: attributes)
        endSpan(context)
        return context
    }

    public static func forceFlush() {
        state.forceFlush()
    }

    #if DEBUG
    public static func resetForTesting() {
        state.reset()
    }
    #endif
}

private final class ObservabilitySystemState: @unchecked Sendable {
    private let lock = NSLock()
    private var tracker: EventTracker?
    private var currentStatus = ObservabilityStatus.notConfigured

    var status: ObservabilityStatus {
        lock.withLock { currentStatus }
    }

    func configure(
        context: any AppContextProviding,
        configuration: ObservabilityConfiguration
    ) -> ObservabilityStatus {
        lock.withLock {
            guard tracker == nil else { return currentStatus }

            let providers = configuration.isEnabled ? configuration.providers : []
            providers.forEach { provider in
                try? provider.initialize()
            }

            let transport: any EventTransportProtocol = providers.isEmpty
                ? NoOpEventTransport()
                : ProviderFanOutTransport(providers: providers)
            tracker = EventTracker(
                transport: transport,
                enricher: ContextEnricher(enrichers: [SchemaEnricher(context: context)])
            )

            currentStatus = providers.first?.status ?? ObservabilityStatus(
                isConfigured: true,
                exportsToNewRelic: false,
                destination: configuration.isEnabled ? "No providers registered" : "Disabled"
            )
            return currentStatus
        }
    }

    func track(_ event: AnyObservabilityEvent) -> NativeSpanContext? {
        let activeTracker = lock.withLock { tracker }
        return activeTracker?.track(event)
    }

    func forceFlush() {
        let activeTracker = lock.withLock { tracker }
        activeTracker?.forceFlush()
    }

    #if DEBUG
    func reset() {
        let previous = lock.withLock { () -> EventTracker? in
            let value = tracker
            tracker = nil
            currentStatus = .notConfigured
            return value
        }
        previous?.forceFlush()
    }
    #endif
}
