// swift-tools-version: 6.0

import PackageDescription

let package = Package(
    name: "Observability",
    platforms: [
        .iOS(.v17),
        .macOS(.v13),
    ],
    products: [
        // A dynamic product keeps one native monitoring pipeline when multiple
        // host frameworks depend on this local package.
        .library(
            name: "Observability",
            type: .dynamic,
            targets: ["Observability"]
        ),
    ],
    dependencies: [
        .package(
            url: "https://github.com/newrelic/newrelic-ios-agent-spm.git",
            exact: "7.7.6"
        ),
    ],
    targets: [
        .target(
            name: "Observability",
            dependencies: [
                .product(
                    name: "NewRelic",
                    package: "newrelic-ios-agent-spm",
                    condition: .when(platforms: [.iOS])
                ),
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
