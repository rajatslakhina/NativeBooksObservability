import Foundation
#if canImport(UIKit)
import UIKit
#endif

/// Process-level context added to every event.
public protocol AppContextProviding: Sendable {
    var appVersion: String { get }
    var appBuildNumber: String { get }
    var environment: String { get }
    var deviceModel: String { get }
    var osVersion: String { get }
    var sessionId: String { get }
}

public struct DefaultAppContextProvider: AppContextProviding, Sendable {
    public let appVersion: String
    public let appBuildNumber: String
    public let environment: String
    public let deviceModel: String
    public let osVersion: String
    public let sessionId: String

    public init(
        appVersion: String,
        appBuildNumber: String = "",
        environment: String,
        deviceModel: String = "",
        osVersion: String = "",
        sessionId: String = UUID().uuidString
    ) {
        self.appVersion = appVersion
        self.appBuildNumber = appBuildNumber
        self.environment = environment
        self.deviceModel = deviceModel
        self.osVersion = osVersion
        self.sessionId = sessionId
    }

    @MainActor
    public init(environment: String) {
        appVersion = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""
        appBuildNumber = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? ""
        self.environment = environment
        sessionId = UUID().uuidString
        #if canImport(UIKit)
        deviceModel = UIDevice.current.model
        osVersion = UIDevice.current.systemVersion
        #else
        deviceModel = "Mac"
        osVersion = ProcessInfo.processInfo.operatingSystemVersionString
        #endif
    }
}
