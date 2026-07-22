// swift-tools-version: 6.0

import PackageDescription

let package = Package(
    name: "Observability",
    platforms: [
        .iOS(.v17),
        .macOS(.v13),
    ],
    products: [
        // A dynamic product keeps one OTel pipeline when multiple host frameworks
        // depend on this local package.
        .library(
            name: "Observability",
            type: .dynamic,
            targets: ["Observability"]
        ),
    ],
    dependencies: [
        .package(
            url: "https://github.com/open-telemetry/opentelemetry-swift-core.git",
            exact: "2.3.0"
        ),
        .package(
            url: "https://github.com/open-telemetry/opentelemetry-swift.git",
            exact: "2.3.0"
        ),
    ],
    targets: [
        .target(
            name: "Observability",
            dependencies: [
                .product(name: "OpenTelemetryApi", package: "opentelemetry-swift-core"),
                .product(name: "OpenTelemetrySdk", package: "opentelemetry-swift-core"),
                .product(name: "StdoutExporter", package: "opentelemetry-swift-core"),
                .product(name: "OpenTelemetryProtocolExporterHTTP", package: "opentelemetry-swift"),
            ],
            swiftSettings: [.swiftLanguageMode(.v6)]
        ),
        .testTarget(
            name: "ObservabilityTests",
            dependencies: ["Observability"],
            swiftSettings: [.swiftLanguageMode(.v6)]
        ),
    ]
)
