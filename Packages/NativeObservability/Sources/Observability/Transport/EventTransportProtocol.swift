/// Synchronous transport abstraction. Implementations must provide their own
/// thread-safety because callers may enter from Swift or Kotlin worker threads.
public protocol EventTransportProtocol: Sendable {
    @discardableResult
    func send(_ event: AnyObservabilityEvent) -> NativeSpanContext?
    func forceFlush()
}

public extension EventTransportProtocol {
    func forceFlush() {}
}
