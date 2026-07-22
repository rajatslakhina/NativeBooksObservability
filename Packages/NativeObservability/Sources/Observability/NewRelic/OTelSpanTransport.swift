import Foundation
import OSLog
@preconcurrency import OpenTelemetryApi
@preconcurrency import OpenTelemetrySdk
@preconcurrency import OpenTelemetryProtocolExporterCommon
@preconcurrency import OpenTelemetryProtocolExporterHttp
@preconcurrency import StdoutExporter

/// Thread-safe synchronous OpenTelemetry transport for traces and logs.
/// The provider is intentionally private and is never registered globally.
public final class OTelSpanTransport: EventTransportProtocol, @unchecked Sendable {
    private struct Pipeline {
        let spanProcessor: any SpanProcessor
        let logProcessor: any LogRecordProcessor
        let tracerProvider: any TracerProvider
        let loggerProvider: LoggerProviderSdk
    }

    private struct SpanEntry {
        let context: NativeSpanContext
        let span: any Span
    }

    private let lock = NSLock()
    private let tracer: any Tracer
    private let otelLogger: any OpenTelemetryApi.Logger
    private let pipeline: Pipeline
    private var spans: [String: SpanEntry] = [:]

    private static let logger = Logger(
        subsystem: "com.example.nativebooks",
        category: "Observability.OTel"
    )

    public static func make(
        configuration: NewRelicConfiguration,
        providerConfiguration: ProviderConfiguration
    ) -> OTelSpanTransport {
        let resource = Resource(attributes: [
            "service.name": .string(configuration.serviceName),
            "service.version": .string(configuration.serviceVersion),
            "telemetry.distro.name": .string("native-books-observability"),
        ])

        let spanProcessor: any SpanProcessor
        let logProcessor: any LogRecordProcessor

        if let key = configuration.licenseKey {
            let otlpConfiguration = OtlpConfiguration(
                timeout: 10,
                headers: [("api-key", key)]
            )
            spanProcessor = BatchSpanProcessor(
                spanExporter: OtlpHttpTraceExporter(
                    endpoint: configuration.tracesEndpoint,
                    config: otlpConfiguration
                ),
                scheduleDelay: providerConfiguration.flushInterval,
                exportTimeout: 10,
                maxQueueSize: 256,
                maxExportBatchSize: 32
            )
            logProcessor = BatchLogRecordProcessor(
                logRecordExporter: OtlpHttpLogExporter(
                    endpoint: configuration.logsEndpoint,
                    config: otlpConfiguration
                ),
                scheduleDelay: providerConfiguration.flushInterval,
                exportTimeout: 10,
                maxQueueSize: 256,
                maxExportBatchSize: 32
            )
        } else {
            spanProcessor = SimpleSpanProcessor(spanExporter: StdoutSpanExporter())
            logProcessor = SimpleLogRecordProcessor(logRecordExporter: StdoutLogExporter())
        }

        let tracerProvider = TracerProviderBuilder()
            .with(resource: resource)
            .add(spanProcessor: spanProcessor)
            .build()
        let loggerProvider = LoggerProviderBuilder()
            .with(resource: resource)
            .with(processors: [logProcessor])
            .build()

        return OTelSpanTransport(
            tracer: tracerProvider.get(
                instrumentationName: "com.example.nativebooks.observability",
                instrumentationVersion: configuration.serviceVersion
            ),
            logger: loggerProvider.get(
                instrumentationScopeName: "com.example.nativebooks.observability"
            ),
            pipeline: Pipeline(
                spanProcessor: spanProcessor,
                logProcessor: logProcessor,
                tracerProvider: tracerProvider,
                loggerProvider: loggerProvider
            )
        )
    }

    private init(
        tracer: any Tracer,
        logger: any OpenTelemetryApi.Logger,
        pipeline: Pipeline
    ) {
        self.tracer = tracer
        otelLogger = logger
        self.pipeline = pipeline
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
                emitLog(name: name, severity: .info, attributes: event.attributes)
                return nil
            case .span(let operation):
                return handle(operation, envelopeAttributes: event.attributes)
            }
        }
    }

    public func forceFlush() {
        pipeline.spanProcessor.forceFlush(timeout: nil)
        _ = pipeline.logProcessor.forceFlush()
    }

    private func handle(
        _ operation: SpanOperation,
        envelopeAttributes: [String: ObservabilityValue]
    ) -> NativeSpanContext? {
        switch operation {
        case .start(let name, let parentSpanId):
            return start(name: name, parentSpanId: parentSpanId, attributes: envelopeAttributes)

        case .addAttributes(let spanId, let attributes):
            guard let span = spans[spanId]?.span else { return nil }
            merged(envelopeAttributes, attributes).forEach {
                span.setAttribute(key: $0.key, value: $0.value.otelValue)
            }
            return nil

        case .addEvent(let spanId, let name, let attributes):
            guard let span = spans[spanId]?.span else { return nil }
            span.addEvent(
                name: name,
                attributes: merged(envelopeAttributes, attributes).otelAttributes
            )
            return nil

        case .setStatus(let spanId, let status):
            spans[spanId]?.span.apply(status)
            return nil

        case .recordError(let spanId, let message, let domain):
            guard let span = spans[spanId]?.span else { return nil }
            span.status = .error(description: message)
            span.addEvent(name: "exception", attributes: [
                "exception.message": .string(message),
                "exception.type": .string(domain ?? "error"),
            ])
            return nil

        case .end(let spanId, let attributes, let status):
            guard let entry = spans.removeValue(forKey: spanId) else { return nil }
            merged(envelopeAttributes, attributes).forEach {
                entry.span.setAttribute(key: $0.key, value: $0.value.otelValue)
            }
            entry.span.apply(status)
            entry.span.end()
            return entry.context
        }
    }

    private func start(
        name: String,
        parentSpanId: String?,
        attributes: [String: ObservabilityValue]
    ) -> NativeSpanContext {
        let builder = tracer.spanBuilder(spanName: name)
        if let parentSpanId, let parent = spans[parentSpanId]?.span {
            _ = builder.setParent(parent.context)
        }
        let span = builder.startSpan()
        attributes.forEach {
            span.setAttribute(key: $0.key, value: $0.value.otelValue)
        }

        let context = NativeSpanContext(
            traceId: span.context.traceId.hexString,
            spanId: span.context.spanId.hexString,
            sampled: span.context.isSampled
        )
        guard context.isValid else {
            span.end()
            Self.logger.error("OpenTelemetry returned an invalid span context for '\(name)'")
            return .invalid
        }
        spans[context.spanId] = SpanEntry(context: context, span: span)
        return context
    }

    private func emitLog(
        name: String,
        severity: LogSeverity,
        attributes: [String: ObservabilityValue]
    ) {
        otelLogger.logRecordBuilder()
            .setSeverity(severity.otelSeverity)
            .setBody(.string(name))
            .setAttributes(attributes.otelAttributes)
            .emit()
    }

    private func merged(
        _ first: [String: ObservabilityValue],
        _ second: [String: ObservabilityValue]
    ) -> [String: ObservabilityValue] {
        first.merging(second) { _, caller in caller }
    }
}
