import Foundation
import Observability
import XCTest

final class ObservabilityTests: XCTestCase {
    override func setUp() {
        super.setUp()
        ObservabilitySystem.resetForTesting()
    }

    override func tearDown() {
        ObservabilitySystem.resetForTesting()
        super.tearDown()
    }

    func testSynchronousSpanReturnsValidW3CContext() {
        let status = configureConsolePipeline()

        let context = ObservabilitySystem.startSpan(
            name: "test.kmp.bridge",
            attributes: ["test.kind": "synchronous"]
        )
        ObservabilitySystem.forceFlush()

        XCTAssertTrue(status.isConfigured)
        XCTAssertFalse(status.exportsToNewRelic)
        XCTAssertTrue(context.isValid)
        XCTAssertTrue(context.sampled)
        XCTAssertEqual(context.traceId.count, 32)
        XCTAssertEqual(context.spanId.count, 16)
        XCTAssertEqual(context.traceparent?.count, 55)
    }

    func testExplicitSynchronousSpanLifecycle() {
        _ = configureConsolePipeline()

        let context = ObservabilitySystem.beginSpan(
            name: "test.explicit-lifecycle",
            attributes: ["phase": "begin"]
        )
        let acknowledged = ObservabilitySystem.endSpan(
            context,
            attributes: ["phase": "end"],
            status: .ok
        )

        XCTAssertTrue(context.isValid)
        XCTAssertTrue(acknowledged)
        XCTAssertFalse(ObservabilitySystem.endSpan(context))
    }

    func testSchemaEnricherUsesCallerWinsSemantics() {
        let provider = SpyProvider()
        let context = DefaultAppContextProvider(
            appVersion: "1.0.0",
            environment: "test",
            sessionId: "session-1"
        )
        ObservabilitySystem.configure(context: context) { builder in
            builder.add(provider)
        }

        ObservabilitySystem.track(
            type: .telemetry(.log(name: "test.log", severity: .info)),
            attributes: ["app.version": .string("caller-version")]
        )

        XCTAssertEqual(provider.lastEvent?.attributes["app.version"], .string("caller-version"))
        XCTAssertEqual(provider.lastEvent?.attributes["service.session.id"], .string("session-1"))
        XCTAssertEqual(provider.lastEvent?.attributes["deployment.environment.name"], .string("test"))
    }

    private func configureConsolePipeline() -> ObservabilityStatus {
        ObservabilitySystem.configure(
            context: DefaultAppContextProvider(
                appVersion: "1.0.0",
                environment: "test"
            )
        ) { builder in
            builder.add(
                TelemetryProvider(
                    dependencies: .init(
                        newRelic: NewRelicConfiguration(
                            licenseKey: nil,
                            serviceName: "native-books-tests",
                            serviceVersion: "1.0"
                        )
                    )
                )
            )
        }
    }
}

private final class SpyProvider: ObservabilityProvider, @unchecked Sendable {
    let identifier: ProviderIdentifier = .telemetry
    let capabilities: Set<ObservabilityCapability> = [.telemetry]
    let configuration = ProviderConfiguration()
    let status = ObservabilityStatus(
        isConfigured: true,
        exportsToNewRelic: false,
        destination: "Spy"
    )
    var isEnabled: Bool { true }

    private let lock = NSLock()
    private var events: [AnyObservabilityEvent] = []

    var lastEvent: AnyObservabilityEvent? {
        lock.withLock { events.last }
    }

    func initialize() throws {}

    func send(_ event: AnyObservabilityEvent) -> NativeSpanContext? {
        lock.withLock { events.append(event) }
        return nil
    }

    func forceFlush() {}
}
