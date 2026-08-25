import Foundation

/// Common provider controls. Credentials are supplied by the host at runtime.
public struct ProviderConfiguration: Sendable, Equatable {
    public let isEnabled: Bool
    public let flushInterval: TimeInterval

    public init(
        isEnabled: Bool = true,
        flushInterval: TimeInterval = 5
    ) {
        self.isEnabled = isEnabled
        self.flushInterval = max(1, flushInterval)
    }
}

/// New Relic mobile-agent settings owned by the native Swift layer.
public struct NewRelicConfiguration: Sendable, Equatable {
    public let applicationToken: String?
    public let serviceName: String
    public let serviceVersion: String
    public let useConsoleWhenUnconfigured: Bool

    public init(
        applicationToken: String?,
        serviceName: String,
        serviceVersion: String,
        useConsoleWhenUnconfigured: Bool = true
    ) {
        self.applicationToken = Self.normalized(applicationToken)
        self.serviceName = serviceName
        self.serviceVersion = serviceVersion
        self.useConsoleWhenUnconfigured = useConsoleWhenUnconfigured
    }

    public var exportsToNewRelic: Bool { applicationToken != nil }

    private static func normalized(_ key: String?) -> String? {
        guard let value = key?.trimmingCharacters(in: .whitespacesAndNewlines),
              !value.isEmpty,
              value != "YOUR_NEW_RELIC_APP_TOKEN"
        else { return nil }
        return value
    }
}
