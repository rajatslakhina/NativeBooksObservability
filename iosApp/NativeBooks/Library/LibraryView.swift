import SharedKit
import SwiftUI

struct LibraryView: View {
    @EnvironmentObject private var appState: AppState
    @StateObject private var viewModel = LibraryViewModel()
    @State private var showTelemetry = false
    @State private var traceContext: KmpSpanContext?

    var body: some View {
        NavigationStack {
            ZStack {
                AppTheme.paper.ignoresSafeArea()

                if viewModel.isLoading {
                    loadingView
                } else {
                    booksList
                }
            }
            .navigationTitle("My library")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        showTelemetry = true
                    } label: {
                        Image(systemName: "waveform.path.ecg")
                    }
                    .accessibilityLabel("Telemetry details")
                }

                ToolbarItemGroup(placement: .topBarTrailing) {
                    NavigationLink {
                        SduiBooksScreen()
                    } label: {
                        Image(systemName: "heart.text.square")
                    }
                    .accessibilityLabel("Open server-driven books")

                    Menu {
                        Text(appState.signedInEmail)
                        Button("Reload books", systemImage: "arrow.clockwise", action: viewModel.reload)
                        Button("Sign out", systemImage: "rectangle.portrait.and.arrow.right", role: .destructive) {
                            let context = NativeTracer.shared.startSpan(name: "login.signout", attributes: [:])
                            _ = NativeTracer.shared.endSpan(context: context, attributes: [:], status: .ok)
                            appState.logOut()
                        }
                    } label: {
                        Image(systemName: "person.crop.circle.fill")
                            .font(.title3)
                    }
                }
            }
            .sheet(isPresented: $showTelemetry) {
                TelemetrySheet(
                    kmpSpanEndAcknowledged: viewModel.lastSpanEndAcknowledged
                )
                    .environmentObject(appState)
                    .presentationDetents([.medium])
            }
        }
        .onAppear {
            if traceContext == nil {
                traceContext = NativeTracer.shared.startSpan(
                    name: "screen.library.visible",
                    attributes: ["screen.name": "library"]
                )
            }
            viewModel.load()
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

    private var loadingView: some View {
        VStack(spacing: 14) {
            ProgressView()
                .controlSize(.large)
                .tint(AppTheme.accent)
            Text("Finding good books…")
                .font(.system(.subheadline, design: .rounded, weight: .medium))
                .foregroundStyle(AppTheme.secondaryInk)
        }
    }

    private var booksList: some View {
        ScrollView {
            LazyVStack(spacing: 14) {
                introCard

                ForEach(viewModel.books, id: \.id) { book in
                    NavigationLink {
                        BookDetailView(book: book)
                    } label: {
                        BookRow(book: book)
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("book.\(book.id)")
                }
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 14)
        }
        .refreshable {
            viewModel.reload()
        }
    }

    private var introCard: some View {
        HStack(spacing: 14) {
            VStack(alignment: .leading, spacing: 7) {
                Text("CURATED FOR YOU")
                    .font(.system(.caption2, design: .rounded, weight: .bold))
                    .tracking(1.2)
                    .foregroundStyle(AppTheme.accent)
                Text("Five books worth\nyour attention")
                    .font(.system(.title2, design: .rounded, weight: .bold))
                    .foregroundStyle(AppTheme.ink)
            }
            Spacer()
            Image(systemName: "bookmark.fill")
                .font(.system(size: 36))
                .foregroundStyle(AppTheme.accent.opacity(0.75))
        }
        .padding(20)
        .cardStyle()
    }
}

private struct BookRow: View {
    let book: Book

    var body: some View {
        HStack(spacing: 16) {
            BookCover(category: book.category, title: book.title, compact: true)

            VStack(alignment: .leading, spacing: 7) {
                Text(book.category.uppercased())
                    .font(.system(.caption2, design: .rounded, weight: .bold))
                    .tracking(0.8)
                    .foregroundStyle(AppTheme.accent)
                Text(book.title)
                    .font(.system(.headline, design: .rounded, weight: .bold))
                    .foregroundStyle(AppTheme.ink)
                    .multilineTextAlignment(.leading)
                Text(book.author)
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundStyle(AppTheme.secondaryInk)
                    .lineLimit(2)
            }

            Spacer(minLength: 4)
            Image(systemName: "chevron.right")
                .font(.footnote.weight(.bold))
                .foregroundStyle(AppTheme.secondaryInk.opacity(0.55))
        }
        .padding(14)
        .cardStyle()
    }
}
