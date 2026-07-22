import Observability
import SharedKit
import SwiftUI

@main
struct NativeBooksApp: App {
    @StateObject private var appState: AppState
    private let tracerProvider: IosTracerProviderImpl

    init() {
        let licenseKey = Bundle.main.object(forInfoDictionaryKey: "NewRelicLicenseKey") as? String
        let status = ObservabilitySystem.configure(
            context: DefaultAppContextProvider(environment: "development")
        ) { builder in
            builder.add(
                TelemetryProvider(
                    dependencies: .init(
                        newRelic: NewRelicConfiguration(
                            licenseKey: licenseKey,
                            serviceName: "native-books-ios",
                            serviceVersion: "1.0.0"
                        )
                    )
                )
            )
        }

        let provider = IosTracerProviderImpl()
        NativeTracer.shared.initialize(provider: provider)
        tracerProvider = provider
        _appState = StateObject(wrappedValue: AppState(observabilityStatus: status))
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(appState)
                .tint(AppTheme.accent)
        }
    }
}
