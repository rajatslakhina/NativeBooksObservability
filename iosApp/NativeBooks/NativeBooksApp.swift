import Observability
import Network
import SharedKit
import SwiftUI
import UIKit

final class AppDelegate: NSObject, UIApplicationDelegate {
    private let connectivityMonitor = SessionConnectivityMonitor()

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        NewRelicAgentBootstrap.start(
            applicationToken: Bundle.main.object(forInfoDictionaryKey: "NewRelicApplicationToken") as? String
        )
        NewRelicAgentBootstrap.recordSessionState("launch", reason: "application_launched")
        connectivityMonitor.start()
        return true
    }
}

private final class SessionConnectivityMonitor: @unchecked Sendable {
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "com.example.nativebooks.connectivity")

    func start() {
        monitor.pathUpdateHandler = { path in
            let type: String
            if path.status != .satisfied {
                type = "offline"
            } else if path.usesInterfaceType(.wifi) {
                type = "wifi"
            } else if path.usesInterfaceType(.cellular) {
                type = "cellular"
            } else if path.usesInterfaceType(.wiredEthernet) {
                type = "ethernet"
            } else {
                type = "other"
            }
            NewRelicAgentBootstrap.recordConnectivity(
                type: type,
                isExpensive: path.isExpensive,
                isConstrained: path.isConstrained
            )
        }
        monitor.start(queue: queue)
    }
}

@main
struct NativeBooksApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var appState: AppState
    private let tracerProvider: IosTracerProviderImpl

    init() {
        let applicationToken = Bundle.main.object(forInfoDictionaryKey: "NewRelicApplicationToken") as? String
        let status = ObservabilitySystem.configure(
            context: DefaultAppContextProvider(environment: "development")
        ) { builder in
            builder.add(
                TelemetryProvider(
                    dependencies: .init(
                        newRelic: NewRelicConfiguration(
                            applicationToken: applicationToken,
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
                .onChange(of: scenePhase) { _, phase in
                    NewRelicAgentBootstrap.recordSessionState(
                        phase.newRelicState,
                        reason: "scene_phase_changed"
                    )
                }
        }
    }
}

private extension ScenePhase {
    var newRelicState: String {
        switch self {
        case .active: "active"
        case .inactive: "inactive"
        case .background: "background"
        @unknown default: "unknown"
        }
    }
}
