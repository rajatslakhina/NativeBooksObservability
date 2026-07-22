import Foundation

/// Adds process context with caller-wins merge semantics.
struct SchemaEnricher: EventEnriching, Sendable {
    private let context: any AppContextProviding

    init(context: any AppContextProviding) {
        self.context = context
    }

    func enrich(_ event: AnyObservabilityEvent) -> AnyObservabilityEvent {
        var values: [String: ObservabilityValue] = [
            "service.session.id": .string(context.sessionId),
            "device.model.name": .string(context.deviceModel),
            "os.version": .string(context.osVersion),
            "app.version": .string(context.appVersion),
            "app.build": .string(context.appBuildNumber),
            "deployment.environment.name": .string(context.environment),
            "locale": .string(Locale.current.identifier),
            "event.timestamp": .string(Date().ISO8601Format()),
            "app.platform": .string("iOS"),
            "telemetry.source": .string("app"),
        ]

        // Caller wins unless it explicitly supplied an empty string and the
        // system already has a non-empty value.
        for (key, value) in event.attributes {
            if case .string(let supplied) = value,
               supplied.isEmpty,
               case .string(let existing) = values[key],
               !existing.isEmpty {
                continue
            }
            values[key] = value
        }

        return AnyObservabilityEvent(type: event.type, attributes: values)
    }
}
