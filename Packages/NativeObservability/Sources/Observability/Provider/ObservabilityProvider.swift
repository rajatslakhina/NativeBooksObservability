/// A synchronous destination registered with ``ObservabilitySystem``.
public protocol ObservabilityProvider: AnyObject, Sendable {
    var identifier: ProviderIdentifier { get }
    var capabilities: Set<ObservabilityCapability> { get }
    var isEnabled: Bool { get }
    var configuration: ProviderConfiguration { get }
    var status: ObservabilityStatus { get }

    func initialize() throws
    @discardableResult
    func send(_ event: AnyObservabilityEvent) -> NativeSpanContext?
    func forceFlush()
}

public extension ObservabilityProvider {
    func initialize() throws {}
    func forceFlush() {}
}
