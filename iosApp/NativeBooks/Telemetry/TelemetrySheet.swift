import SharedKit
import SwiftUI

struct TelemetrySheet: View {
    let kmpSpanEndAcknowledged: Bool

    @EnvironmentObject private var appState: AppState
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 20) {
                statusRow(
                    icon: "shippingbox.fill",
                    title: "Native package",
                    detail: "New Relic iOS 7.7.6"
                )
                statusRow(
                    icon: "arrow.triangle.branch",
                    title: "KMP round trip",
                    detail: kmpRoundTripDetail
                )
                statusRow(
                    icon: appState.observabilityStatus.exportsToNewRelic ? "cloud.fill" : "terminal.fill",
                    title: "Trace destination",
                    detail: appState.observabilityStatus.destination
                )

                Spacer()
            }
            .padding(22)
            .navigationTitle("Observability")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    private var kmpRoundTripDetail: String {
        kmpSpanEndAcknowledged
            ? "Native span end acknowledged"
            : "Waiting for a KMP request"
    }

    private func statusRow(icon: String, title: String, detail: String) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .foregroundStyle(AppTheme.accent)
                .frame(width: 30)
            VStack(alignment: .leading, spacing: 3) {
                Text(title).font(.headline)
                Text(detail)
                    .font(.caption)
                    .foregroundStyle(AppTheme.secondaryInk)
            }
        }
    }
}
