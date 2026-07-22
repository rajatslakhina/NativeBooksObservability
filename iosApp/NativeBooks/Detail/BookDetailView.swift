import SharedKit
import SwiftUI

struct BookDetailView: View {
    let book: Book
    @State private var traceContext: KmpSpanContext?

    var body: some View {
        ZStack {
            AppTheme.paper.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 28) {
                    BookCover(category: book.category, title: book.title)
                        .padding(.top, 18)

                    VStack(alignment: .leading, spacing: 20) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(book.category.uppercased())
                                .font(.system(.caption, design: .rounded, weight: .bold))
                                .tracking(1.2)
                                .foregroundStyle(AppTheme.accent)
                            Text(book.title)
                                .font(.system(.largeTitle, design: .rounded, weight: .bold))
                                .foregroundStyle(AppTheme.ink)
                            Text("by \(book.author)")
                                .font(.system(.body, design: .rounded))
                                .foregroundStyle(AppTheme.secondaryInk)
                        }

                        HStack(spacing: 0) {
                            stat(value: String(book.year), label: "Published")
                            Divider().frame(height: 42)
                            stat(value: "\(book.readingMinutes) min", label: "Reading time")
                        }
                        .padding(.vertical, 12)
                        .cardStyle()

                        VStack(alignment: .leading, spacing: 10) {
                            Text("About this book")
                                .font(.system(.title3, design: .rounded, weight: .bold))
                                .foregroundStyle(AppTheme.ink)
                            Text(book.summary)
                                .font(.system(.body, design: .rounded))
                                .foregroundStyle(AppTheme.secondaryInk)
                                .lineSpacing(5)
                        }

                        if let traceContext, traceContext.isValid {
                            VStack(alignment: .leading, spacing: 5) {
                                Label("KMP → native span stitched", systemImage: "checkmark.seal.fill")
                                    .foregroundStyle(AppTheme.accent)
                                Text("Trace \(traceContext.traceId.prefix(12))… · Span \(traceContext.spanId.prefix(8))…")
                                    .font(.system(.caption2, design: .monospaced))
                                    .foregroundStyle(AppTheme.secondaryInk)
                            }
                            .font(.system(.caption, design: .rounded, weight: .semibold))
                            .padding(14)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(AppTheme.accent.opacity(0.09))
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                        }
                    }
                    .padding(.horizontal, 22)
                    .padding(.bottom, 36)
                }
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            if traceContext == nil {
                traceContext = NativeTracer.shared.startSpan(
                    name: "screen.book.detail",
                    attributes: [
                        "screen.name": "book-detail",
                        "book.id": book.id,
                        "book.category": book.category,
                    ]
                )
            }
        }
        .onDisappear {
            guard let traceContext else { return }
            _ = NativeTracer.shared.endSpan(
                context: traceContext,
                attributes: ["screen.disappeared": "true"],
                status: .ok
            )
            self.traceContext = nil
        }
    }

    private func stat(value: String, label: String) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.system(.subheadline, design: .rounded, weight: .bold))
                .foregroundStyle(AppTheme.ink)
            Text(label)
                .font(.system(.caption2, design: .rounded))
                .foregroundStyle(AppTheme.secondaryInk)
        }
        .frame(maxWidth: .infinity)
    }
}
