#if canImport(Foundation)
import Foundation
#endif
#if canImport(UIKit)
import UIKit
#endif
#if canImport(NewRelic)
@preconcurrency import NewRelic
#endif

/// Starts the New Relic mobile agent from the host application's launch callback.
public enum NewRelicAgentBootstrap {
    public static func start(applicationToken: String?) {
        guard let token = NewRelicConfiguration(
            applicationToken: applicationToken,
            serviceName: "bootstrap",
            serviceVersion: "bootstrap"
        ).applicationToken else { return }

        #if canImport(NewRelic)
        // Feature flags must be configured before the agent starts. Keep every stable
        // collection feature explicit so local defaults cannot silently reduce coverage.
        NewRelic.enableFeatures(NRMAFeatureFlags.NRFeatureFlag_InteractionTracing)
        NewRelic.enableFeatures(NRMAFeatureFlags.NRFeatureFlag_DefaultInteractions)
        NewRelic.enableFeatures(NRMAFeatureFlags.NRFeatureFlag_CrashReporting)
        NewRelic.enableFeatures(NRMAFeatureFlags.NRFeatureFlag_HandledExceptionEvents)
        NewRelic.enableFeatures(NRMAFeatureFlags.NRFeatureFlag_JSErrorEvents)
        NewRelic.enableFeatures(NRMAFeatureFlags.NRFeatureFlag_NSURLSessionInstrumentation)
        NewRelic.enableFeatures(NRMAFeatureFlags.NRFeatureFlag_SwiftAsyncURLSessionSupport)
        NewRelic.enableFeatures(NRMAFeatureFlags.NRFeatureFlag_HttpResponseBodyCapture)
        NewRelic.enableFeatures(NRMAFeatureFlags.NRFeatureFlag_WebViewInstrumentation)
        NewRelic.enableFeatures(NRMAFeatureFlags.NRFeatureFlag_DistributedTracing)
        NewRelic.enableFeatures(NRMAFeatureFlags.NRFeatureFlag_NetworkRequestEvents)
        NewRelic.enableFeatures(NRMAFeatureFlags.NRFeatureFlag_RequestErrorEvents)
        NewRelic.enableFeatures(NRMAFeatureFlags.NRFeatureFlag_AppStartMetrics)
        NewRelic.enableFeatures(NRMAFeatureFlags.NRFeatureFlag_LogReporting)
        NewRelic.enableFeatures(NRMAFeatureFlags.NRFeatureFlag_OfflineStorage)
        NewRelic.enableFeatures(NRMAFeatureFlags.NRFeatureFlag_BackgroundReporting)
        NewRelic.enableFeatures(NRMAFeatureFlags.NRFeatureFlag_AutoCollectLogs)
        NewRelic.start(withApplicationToken: token)
        #endif
    }

    /// Records lifecycle transitions with the device signals used by the session explorer.
    @MainActor
    public static func recordSessionState(_ state: String, reason: String) {
        #if canImport(NewRelic) && canImport(UIKit)
        var attributes = sessionAttributes
        attributes["lifecycle.state"] = state
        attributes["lifecycle.reason"] = reason
        attributes["app.platform"] = "iOS"
        attributes.merge(deviceAttributes) { _, latest in latest }
        NewRelic.recordCustomEvent("MobileSessionState", name: state, attributes: attributes)
        #endif
    }

    /// Records connection changes separately so Wi-Fi/cellular/offline transitions are visible.
    public static func recordConnectivity(
        type: String,
        isExpensive: Bool,
        isConstrained: Bool
    ) {
        #if canImport(NewRelic)
        var attributes = sessionAttributes
        attributes["connection.type"] = type
        attributes["connection.expensive"] = isExpensive
        attributes["connection.constrained"] = isConstrained
        attributes["app.platform"] = "iOS"
        NewRelic.recordCustomEvent("MobileDeviceState", name: "connectivity_changed", attributes: attributes)
        #endif
    }

    #if canImport(NewRelic)
    private static var sessionAttributes: [String: Any] {
        let sessionId = NewRelic.currentSessionId().trimmingCharacters(in: .whitespacesAndNewlines)
        guard !sessionId.isEmpty else { return [:] }
        return [
            "service.session.id": sessionId,
            "newrelic.session.id": sessionId,
        ]
    }
    #endif

    #if canImport(UIKit)
    @MainActor
    private static var deviceAttributes: [String: Any] {
        let device = UIDevice.current
        device.isBatteryMonitoringEnabled = true
        var attributes: [String: Any] = [
            "battery.state": batteryStateName(device.batteryState),
            "power.lowMode": ProcessInfo.processInfo.isLowPowerModeEnabled,
            "thermal.state": thermalStateName(ProcessInfo.processInfo.thermalState),
            "device.orientation": orientationName(device.orientation),
        ]
        if device.batteryLevel >= 0 {
            attributes["battery.level.percent"] = Double(device.batteryLevel * 100)
        }
        return attributes
    }

    private static func batteryStateName(_ state: UIDevice.BatteryState) -> String {
        switch state {
        case .charging: "charging"
        case .full: "full"
        case .unplugged: "discharging"
        case .unknown: "unknown"
        @unknown default: "unknown"
        }
    }

    private static func thermalStateName(_ state: ProcessInfo.ThermalState) -> String {
        switch state {
        case .nominal: "nominal"
        case .fair: "fair"
        case .serious: "serious"
        case .critical: "critical"
        @unknown default: "unknown"
        }
    }

    private static func orientationName(_ orientation: UIDeviceOrientation) -> String {
        switch orientation {
        case .portrait, .portraitUpsideDown: "portrait"
        case .landscapeLeft, .landscapeRight: "landscape"
        case .faceUp: "face_up"
        case .faceDown: "face_down"
        default: "unknown"
        }
    }
    #endif
}
