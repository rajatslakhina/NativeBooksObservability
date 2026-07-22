/// Immutable system configuration assembled at launch.
public struct ObservabilityConfiguration: Sendable {
    public let providers: [any ObservabilityProvider]
    public let isEnabled: Bool

    public init(
        providers: [any ObservabilityProvider] = [],
        isEnabled: Bool = true
    ) {
        self.providers = providers
        self.isEnabled = isEnabled
    }

    public final class Builder {
        private var providers: [any ObservabilityProvider] = []
        private var isEnabled = true

        public init() {}

        @discardableResult
        public func add(_ provider: any ObservabilityProvider) -> Self {
            guard provider.isEnabled else { return self }
            providers.append(provider)
            return self
        }

        @discardableResult
        public func enabled(_ value: Bool) -> Self {
            isEnabled = value
            return self
        }

        public func build() -> ObservabilityConfiguration {
            ObservabilityConfiguration(providers: providers, isEnabled: isEnabled)
        }
    }
}
