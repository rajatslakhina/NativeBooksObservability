public enum ObservabilityError: Error, Sendable {
    case notConfigured
    case configurationInvalid
    case providerInitializationFailed(identifier: ProviderIdentifier)
}
