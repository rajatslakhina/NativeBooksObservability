// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "SharedKMP",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "SharedKMP", targets: ["SharedKit"]),
    ],
    targets: [
        .binaryTarget(
            name: "SharedKit",
            path: "SharedKit.xcframework"
        ),
    ]
)
