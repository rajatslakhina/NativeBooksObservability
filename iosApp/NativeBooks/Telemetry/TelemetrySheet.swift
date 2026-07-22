import Observability
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
                    detail: "OpenTelemetry Swift 2.3.0"
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

                Button {
                    let context = NativeTracer.shared.startSpan(
                        name: "debug.manual-span",
                        attributes: ["debug.trigger": "telemetry-sheet"]
                    )
                    appState.lastSpan = NativeSpanContext(
                        traceId: context.traceId,
                        spanId: context.spanId,
                        sampled: context.sampled
                    )
                    appState.lastSpanEndAcknowledged = NativeTracer.shared.endSpan(
                        context: context,
                        attributes: [
                            "debug.completed": "true",
                            "kmp.context.received": String(context.isValid),
                        ],
                        status: .ok
                    )
                    ObservabilitySystem.forceFlush()
                } label: {
                    Label("Create test span", systemImage: "waveform.badge.plus")
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                }
                .buttonStyle(.borderedProminent)
                .tint(AppTheme.accent)

                if let span = appState.lastSpan {
                    VStack(alignment: .leading, spacing: 7) {
                        Text("LAST TEST CONTEXT")
                            .font(.caption2.bold())
                            .tracking(1)
                        Text("traceId  \(span.traceId)")
                        Text("spanId   \(span.spanId)")
                        Text("sampled  \(span.sampled ? "true" : "false")")
                        Text("ended   \(appState.lastSpanEndAcknowledged == true ? "acknowledged" : "not acknowledged")")
                    }
                    .font(.system(.caption, design: .monospaced))
                    .foregroundStyle(AppTheme.secondaryInk)
                    .textSelection(.enabled)
                }

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
