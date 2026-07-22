public struct ObservabilityStatus: Sendable, Equatable {
    public let isConfigured: Bool
    public let exportsToNewRelic: Bool
    public let destination: String

    public init(
        isConfigured: Bool,
        exportsToNewRelic: Bool,
        destination: String
    ) {
        self.isConfigured = isConfigured
        self.exportsToNewRelic = exportsToNewRelic
        self.destination = destination
    }

    public static let notConfigured = ObservabilityStatus(
        isConfigured: false,
        exportsToNewRelic: false,
        destination: "Not configured"
    )
}
