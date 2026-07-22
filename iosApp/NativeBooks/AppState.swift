import Foundation
import Observability
import SharedKit

@MainActor
final class AppState: ObservableObject {
    @Published private(set) var isAuthenticated = false
    @Published private(set) var signedInEmail = ""
    @Published var lastSpan: NativeSpanContext?
    @Published var lastSpanEndAcknowledged: Bool?

    let observabilityStatus: ObservabilityStatus

    init(observabilityStatus: ObservabilityStatus) {
        self.observabilityStatus = observabilityStatus
    }

    func logIn(email: String) {
        signedInEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        isAuthenticated = true
    }

    func logOut() {
        signedInEmail = ""
        isAuthenticated = false
        lastSpan = nil
        lastSpanEndAcknowledged = nil
    }
}
