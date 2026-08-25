import Foundation
import OSLog
#if canImport(NewRelic)
@preconcurrency import NewRelic
#endif

/// Thread-safe adapter from the app's span-shaped contract to New Relic Mobile.
public final class NewRelicSpanTransport: EventTransportProtocol, @unchecked Sendable {
    private struct SpanEntry {
        let context: NativeSpanContext
        let name: String
        let interactionId: String?
        let traceHeaders: [String: String]
        let startedAt: Date
        var attributes: [String: ObservabilityValue]
    }

    private struct GeneratedTraceContext {
        let context: NativeSpanContext
        let headers: [String: String]
    }

    private let lock = NSLock()
    private let configuration: NewRelicConfiguration
    private var spans: [String: SpanEntry] = [:]

    private static let logger = Logger(
        subsystem: "com.example.nativebooks",
        category: "Observability.NewRelic"
    )

    public init(configuration: NewRelicConfiguration) {
        self.configuration = configuration
    }

    @discardableResult
    public func send(_ event: AnyObservabilityEvent) -> NativeSpanContext? {
        lock.withLock {
            guard case .telemetry(let telemetry) = event.type else { return nil }
            switch telemetry {
            case .log(let name, let severity):
                emitLog(name: name, severity: severity, attributes: event.attributes)
                return nil
            case .breadcrumb(let name):
                emitBreadcrumb(name: name, attributes: event.attributes)
                return nil
            case .span(let operation):
                return handle(operation, envelopeAttributes: event.attributes)
            }
        }
    }

    public func forceFlush() {
        // New Relic Mobile owns its harvest schedule and has no public flush API.
    }

    private func handle(
        _ operation: SpanOperation,
        envelopeAttributes: [String: ObservabilityValue]
    ) -> NativeSpanContext? {
        switch operation {
        case .start(let name, let parentSpanId):
            var attributes = envelopeAttributes
            if let parentSpanId {
                attributes["parent.span.id"] = .string(parentSpanId)
                attributes["parent.id"] = .string(parentSpanId)
            }
            return start(name: name, attributes: attributes, parentSpanId: parentSpanId)

        case .addAttributes(let spanId, let attributes):
            update(spanId: spanId) { $0.attributes.merge(attributes) { _, caller in caller } }
            return nil

        case .addEvent(let spanId, let name, let attributes):
            guard let entry = spans[spanId] else { return nil }
            var values = merged(entry.attributes, merged(envelopeAttributes, attributes))
            values["trace.id"] = .string(entry.context.traceId)
            values["span.id"] = .string(entry.context.spanId)
            recordEvent(type: "NativeSpanEvent", name: name, attributes: values)
            return nil

        case .setStatus(let spanId, let status):
            update(spanId: spanId) { $0.attributes["span.status"] = .string(status.name) }
            return nil

        case .recordError(let spanId, let message, let domain):
            update(spanId: spanId) {
                $0.attributes["span.status"] = .string("error")
                $0.attributes["error.message"] = .string(message)
                if let domain { $0.attributes["error.domain"] = .string(domain) }
            }
            return nil

        case .end(let spanId, let attributes, let status):
            guard var entry = spans.removeValue(forKey: spanId) else { return nil }
            let endedAt = Date()
            let effectiveStatus = Self.effectiveStatus(requested: status, attributes: entry.attributes)
            entry.attributes = merged(entry.attributes, merged(envelopeAttributes, attributes))
            entry.attributes["span.status"] = .string(effectiveStatus.name)
            entry.attributes["trace.id"] = .string(entry.context.traceId)
            entry.attributes["span.id"] = .string(entry.context.spanId)
            entry.attributes["duration.ms"] = .double(endedAt.timeIntervalSince(entry.startedAt) * 1_000)

            if let interactionId = entry.interactionId {
                recordStandardSpan(entry, status: effectiveStatus, endedAt: endedAt)
                stopInteraction(interactionId)
                recordEvent(type: "NativeSpan", name: entry.name, attributes: entry.attributes)
            } else {
                Self.logger.info("Completed span '\(entry.name, privacy: .public)' \(entry.context.spanId, privacy: .public)")
            }
            return entry.context
        }
    }

    private func start(
        name: String,
        attributes: [String: ObservabilityValue],
        parentSpanId: String?
    ) -> NativeSpanContext {
        guard let interactionId = startInteraction(name: name) else {
            return .invalid
        }
        let parent = parentSpanId.flatMap { spans[$0] }
        guard let generatedContext = newRelicSpanContext(parent: parent) else {
            stopInteraction(interactionId)
            Self.logger.error("New Relic did not generate a valid distributed trace context for '\(name, privacy: .public)'")
            return .invalid
        }
        let context = generatedContext.context
        spans[context.spanId] = SpanEntry(
            context: context,
            name: name,
            interactionId: interactionId,
            traceHeaders: generatedContext.headers,
            startedAt: Date(),
            attributes: attributes
        )
        return context
    }

    /// New Relic does not return trace identifiers from `startInteraction`.
    /// Generate its distributed-tracing headers and use the W3C context owned by
    /// the agent instead of inventing unrelated application UUIDs.
    private func newRelicSpanContext(parent: SpanEntry?) -> GeneratedTraceContext? {
        #if canImport(NewRelic)
        guard configuration.exportsToNewRelic else { return nil }
        let generatedHeaders = NewRelic.generateDistributedTracingHeaders()
        guard let generatedContext = NewRelicTraceContextParser.context(from: generatedHeaders) else {
            return nil
        }

        guard let parent else {
            let context = NativeSpanContext(
                traceId: generatedContext.traceId,
                spanId: generatedContext.spanId,
                sampled: generatedContext.sampled,
                propagationHeaders: generatedHeaders
            )
            return GeneratedTraceContext(context: context, headers: generatedHeaders)
        }

        let context = NativeSpanContext(
            traceId: parent.context.traceId,
            spanId: generatedContext.spanId,
            sampled: parent.context.sampled,
            propagationHeaders: NewRelicTraceContextParser.w3cHeaders(
                traceId: parent.context.traceId,
                spanId: generatedContext.spanId,
                sampled: parent.context.sampled
            )
        )
        return GeneratedTraceContext(context: context, headers: context.propagationHeaders)
        #else
        return nil
        #endif
    }

    /// The mobile SDK creates standard `Span` events only through its
    /// transactional network pipeline. Record this internal operation as a
    /// clearly tagged HTTP-like transaction and attach the context generated by
    /// New Relic so it is queryable from `FROM Span` and the Traces UI.
    private func recordStandardSpan(
        _ entry: SpanEntry,
        status: SpanStatus,
        endedAt: Date
    ) {
        #if canImport(NewRelic)
        guard configuration.exportsToNewRelic,
              let url = Self.standardSpanURL(name: entry.name) else { return }

        // The iOS agent's manual network API expects string-valued params even
        // though its Swift declaration accepts `Any`. Passing a Bool here causes
        // the agent to send `UTF8String` to NSNumber and crash the application.
        var params = entry.attributes.withCurrentNewRelicSession.newRelicStringAttributes
        params["native.span.name"] = entry.name
        params["native.span.source"] = "new-relic-mobile-sdk"
        params["native.span.synthetic_transaction"] = "true"

        NewRelic.noticeNetworkRequest(
            for: url,
            httpMethod: "POST",
            startTime: entry.startedAt.timeIntervalSince1970,
            endTime: endedAt.timeIntervalSince1970,
            responseHeaders: [:],
            statusCode: status.httpStatusCode,
            bytesSent: 0,
            bytesReceived: 0,
            responseData: nil,
            traceHeaders: entry.traceHeaders,
            andParams: params
        )
        #endif
    }

    private static func standardSpanURL(name: String) -> URL? {
        let allowed = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "-._~"))
        guard let encodedName = name.addingPercentEncoding(withAllowedCharacters: allowed) else {
            return nil
        }
        return URL(string: "https://native-span.invalid/\(encodedName)")
    }

    private static func effectiveStatus(
        requested: SpanStatus,
        attributes: [String: ObservabilityValue]
    ) -> SpanStatus {
        guard case .unset = requested,
              let recordedStatus = attributes["span.status"],
              case .string("error") = recordedStatus else {
            return requested
        }
        let description: String
        if let errorMessage = attributes["error.message"],
           case .string(let message) = errorMessage {
            description = message
        } else {
            description = "Recorded error"
        }
        return .error(description: description)
    }

    private func update(spanId: String, _ body: (inout SpanEntry) -> Void) {
        guard var entry = spans[spanId] else { return }
        body(&entry)
        spans[spanId] = entry
    }

    private func emitLog(
        name: String,
        severity: LogSeverity,
        attributes: [String: ObservabilityValue]
    ) {
        var values = attributes
        values["severity"] = .string(severity.rawValue)
        if configuration.exportsToNewRelic {
            recordEvent(type: "NativeLog", name: name, attributes: values)
        } else {
            Self.logger.log(level: severity.osLogType, "\(name, privacy: .public)")
        }
    }

    private func emitBreadcrumb(
        name: String,
        attributes: [String: ObservabilityValue]
    ) {
        if configuration.exportsToNewRelic {
            recordBreadcrumb(name: name, attributes: attributes)
        } else {
            Self.logger.info("Breadcrumb: \(name, privacy: .public)")
        }
    }

    private func recordEvent(
        type: String,
        name: String,
        attributes: [String: ObservabilityValue]
    ) {
        #if canImport(NewRelic)
        NewRelic.recordCustomEvent(
            type,
            name: name,
            attributes: attributes.withCurrentNewRelicSession.newRelicAttributes
        )
        #endif
    }

    private func startInteraction(name: String) -> String? {
        #if canImport(NewRelic)
        guard configuration.exportsToNewRelic else { return nil }
        return NewRelic.startInteraction(withName: name)
        #else
        return nil
        #endif
    }

    private func stopInteraction(_ identifier: String) {
        #if canImport(NewRelic)
        NewRelic.stopCurrentInteraction(identifier)
        #endif
    }

    private func recordBreadcrumb(
        name: String,
        attributes: [String: ObservabilityValue]
    ) {
        #if canImport(NewRelic)
        NewRelic.recordBreadcrumb(
            name,
            attributes: attributes.withCurrentNewRelicSession.newRelicAttributes
        )
        #endif
    }

    private func merged(
        _ first: [String: ObservabilityValue],
        _ second: [String: ObservabilityValue]
    ) -> [String: ObservabilityValue] {
        first.merging(second) { _, caller in caller }
    }
}

enum NewRelicTraceContextParser {
    static func context(from headers: [String: String]) -> NativeSpanContext? {
        guard let traceparent = headers.first(where: {
            $0.key.caseInsensitiveCompare("traceparent") == .orderedSame
        })?.value else {
            return nil
        }

        let fields = traceparent.split(separator: "-", omittingEmptySubsequences: false)
        guard fields.count == 4,
              fields[0].count == 2,
              fields[3].count == 2,
              let flags = UInt8(fields[3], radix: 16) else {
            return nil
        }

        let context = NativeSpanContext(
            traceId: String(fields[1]).lowercased(),
            spanId: String(fields[2]).lowercased(),
            sampled: flags & 0x01 == 0x01
        )
        return context.isValid ? context : nil
    }

    static func w3cHeaders(traceId: String, spanId: String, sampled: Bool) -> [String: String] {
        let flags = sampled ? "01" : "00"
        return ["traceparent": "00-\(traceId)-\(spanId)-\(flags)"]
    }
}

private extension Dictionary where Key == String, Value == ObservabilityValue {
    var withCurrentNewRelicSession: Self {
        #if canImport(NewRelic)
        let sessionId = NewRelic.currentSessionId().trimmingCharacters(in: .whitespacesAndNewlines)
        guard !sessionId.isEmpty else { return self }
        var values = self
        values["service.session.id"] = .string(sessionId)
        values["newrelic.session.id"] = .string(sessionId)
        return values
        #else
        return self
        #endif
    }

    var newRelicAttributes: [String: Any] {
        mapValues(\.newRelicValue)
    }

    var newRelicStringAttributes: [String: Any] {
        mapValues(\.newRelicStringValue)
    }
}

private extension ObservabilityValue {
    var newRelicValue: Any {
        switch self {
        case .string(let value): value
        case .int(let value): value
        case .double(let value): value
        case .bool(let value): value
        }
    }

    var newRelicStringValue: String {
        switch self {
        case .string(let value): value
        case .int(let value): String(value)
        case .double(let value): String(value)
        case .bool(let value): String(value)
        }
    }
}

private extension SpanStatus {
    var name: String {
        switch self {
        case .ok: "ok"
        case .error: "error"
        case .unset: "unset"
        }
    }

    var httpStatusCode: Int {
        switch self {
        case .error: 500
        case .ok, .unset: 200
        }
    }
}

private extension LogSeverity {
    var osLogType: OSLogType {
        switch self {
        case .debug: .debug
        case .info: .info
        case .warning: .default
        case .error: .error
        }
    }
}
