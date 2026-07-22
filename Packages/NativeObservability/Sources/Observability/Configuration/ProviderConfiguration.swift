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

/// New Relic OTLP/HTTP settings owned by the native Swift layer.
public struct NewRelicConfiguration: Sendable, Equatable {
    public let licenseKey: String?
    public let serviceName: String
    public let serviceVersion: String
    public let tracesEndpoint: URL
    public let logsEndpoint: URL
    public let useConsoleWhenUnconfigured: Bool

    public init(
        licenseKey: String?,
        serviceName: String,
        serviceVersion: String,
        tracesEndpoint: URL? = nil,
        logsEndpoint: URL? = nil,
        useConsoleWhenUnconfigured: Bool = true
    ) {
        self.licenseKey = Self.normalized(licenseKey)
        self.serviceName = serviceName
        self.serviceVersion = serviceVersion
        self.tracesEndpoint = tracesEndpoint ?? Self.defaultEndpoint(path: "/v1/traces")
        self.logsEndpoint = logsEndpoint ?? Self.defaultEndpoint(path: "/v1/logs")
        self.useConsoleWhenUnconfigured = useConsoleWhenUnconfigured
    }

    public var exportsToNewRelic: Bool { licenseKey != nil }

    private static func normalized(_ key: String?) -> String? {
        guard let value = key?.trimmingCharacters(in: .whitespacesAndNewlines),
              !value.isEmpty,
              value != "YOUR_NEW_RELIC_LICENSE_KEY"
        else { return nil }
        return value
    }

    private static func defaultEndpoint(path: String) -> URL {
        var components = URLComponents()
        components.scheme = "https"
        components.host = "otlp.nr-data.net"
        components.path = path
        guard let url = components.url else {
            preconditionFailure("Unable to construct the fixed New Relic OTLP endpoint")
        }
        return url
    }
}
