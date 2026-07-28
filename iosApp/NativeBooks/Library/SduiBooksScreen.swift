import SharedKit
import SwiftUI

/// A native SwiftUI renderer that owns the KMP view model directly.
///
/// There is intentionally no ObservableObject adapter. The cancellable KMP observation only
/// copies immutable snapshots into SwiftUI's local rendering state.
struct SduiBooksScreen: View {
    @State private var viewModel: SduiBooksViewModel
    @State private var uiState: SduiBooksUiState?
    @State private var observation: SduiObservation?

    init() {
        _viewModel = State(
            initialValue: SduiBooksViewModel(
                configuration: SduiScreenConfiguration(
                    titleOverride: "Book explorer",
                    showReadingTime: true,
                    searchDebounceMillis: 220,
                    initialFavoriteIds: ["atomic-habits"]
                )
            )
        )
    }

    var body: some View {
        ZStack {
            AppTheme.paper.ignoresSafeArea()

            if let state = uiState {
                content(state)
            } else {
                ProgressView()
                    .controlSize(.large)
                    .tint(AppTheme.accent)
            }
        }
        .navigationTitle(uiState?.screenTitle ?? "Book explorer")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear(perform: beginObservation)
        .onDisappear(perform: endObservation)
    }

    @ViewBuilder
    private func content(_ state: SduiBooksUiState) -> some View {
        if state.status is SduiSearchStatus.Loading, state.sections.isEmpty {
            ProgressView()
                .controlSize(.large)
                .tint(AppTheme.accent)
        } else if
            let failure = state.status as? SduiSearchStatus.Error,
            state.sections.isEmpty
        {
            ContentUnavailableView {
                Label("Couldn’t load screen", systemImage: "wifi.exclamationmark")
            } description: {
                Text(failure.apiError.message)
            } actions: {
                Button("Try again", action: viewModel.reload)
            }
        } else {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 12) {
                    searchField(state)
                    statusBanner(state.status)

                    ForEach(state.sections, id: \.definition.id) { section in
                        sectionView(section, state: state)
                    }

                    if let success = state.status as? SduiSearchStatus.Success {
                        paginationView(success.result)
                    }
                }
                .padding(.horizontal, 18)
                .padding(.vertical, 12)
            }
            .accessibilityIdentifier("sdui.list")
            .refreshable {
                viewModel.reload()
            }
        }
    }

    @ViewBuilder
    private func statusBanner(_ status: SduiSearchStatus) -> some View {
        if status is SduiSearchStatus.Loading {
            VStack(alignment: .leading, spacing: 6) {
                ProgressView()
                    .tint(AppTheme.accent)
                Text("Calling fake KMP search API…")
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundStyle(AppTheme.secondaryInk)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        } else if let failure = status as? SduiSearchStatus.Error {
            HStack(spacing: 12) {
                Image(systemName: "exclamationmark.triangle.fill")
                Text(failure.apiError.message)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Button("Retry", action: viewModel.reload)
            }
            .font(.system(.subheadline, design: .rounded))
            .foregroundStyle(Color.red)
            .padding(14)
            .background(Color.red.opacity(0.09))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
    }

    private func searchField(_ state: SduiBooksUiState) -> some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(AppTheme.secondaryInk)

            TextField(
                state.searchPlaceholder,
                text: Binding(
                    get: { uiState?.query ?? "" },
                    set: { viewModel.setSearchQuery(query: $0) }
                )
            )
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .accessibilityIdentifier("sdui.search")

            if !state.query.isEmpty {
                Button {
                    viewModel.setSearchQuery(query: "")
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(AppTheme.secondaryInk)
                }
                .accessibilityLabel("Clear search")
            }
        }
        .padding(.horizontal, 14)
        .frame(minHeight: 50)
        .background(AppTheme.card)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(AppTheme.line, lineWidth: 1)
        }
    }

    @ViewBuilder
    private func sectionView(
        _ section: SduiRenderedSection,
        state: SduiBooksUiState
    ) -> some View {
        Text(section.definition.title)
            .font(.system(.title3, design: .rounded, weight: .bold))
            .foregroundStyle(AppTheme.ink)
            .padding(.top, 10)

        if section.items.isEmpty {
            Text(section.definition.emptyMessage)
                .font(.system(.subheadline, design: .rounded))
                .foregroundStyle(AppTheme.secondaryInk)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.vertical, 14)
        } else {
            switch section.definition.componentType {
            case .bookRow:
                ForEach(
                    section.items.map {
                        ScopedSduiBookItem(
                            sectionID: section.definition.id,
                            item: $0
                        )
                    }
                ) { scopedItem in
                    SduiBookRow(
                        item: scopedItem.item,
                        showReadingTime: state.showReadingTime,
                        onToggleFavorite: {
                            viewModel.toggleFavorite(bookId: scopedItem.item.book.id)
                        }
                    )
                }
            default:
                EmptyView()
            }
        }
    }

    private func paginationView(_ result: SduiSearchResult) -> some View {
        let pagination = result.pagination
        return VStack(spacing: 7) {
            if pagination.totalPages > 0 {
                Text(
                    "Page \(pagination.currentPage) of \(pagination.totalPages) · " +
                        "\(result.components.count) loaded"
                )
                .font(.system(.footnote, design: .rounded))
                .foregroundStyle(AppTheme.secondaryInk)
            }

            if pagination.event == .loading {
                ProgressView("Loading next page from KMP…")
                    .tint(AppTheme.accent)
            } else if pagination.currentPage < pagination.totalPages {
                Button("Load more", action: viewModel.loadNextPage)
                    .buttonStyle(.bordered)
                    .tint(AppTheme.accent)
                    .accessibilityIdentifier("sdui.loadMore")
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
    }

    private func beginObservation() {
        guard observation == nil else { return }
        uiState = viewModel.currentState
        observation = viewModel.observe { state in
            uiState = state
        }
        viewModel.start()
    }

    private func endObservation() {
        observation?.cancel()
        observation = nil
        viewModel.clear()
    }
}

/// SwiftUI flattens nested `ForEach` children inside a `LazyVStack`, so a book that appears in
/// Favourites and All books must have a section-scoped identity.
private struct ScopedSduiBookItem: Identifiable {
    let sectionID: String
    let item: SduiBookItemState

    var id: String {
        "\(sectionID).\(item.book.id)"
    }
}

private struct SduiBookRow: View {
    let item: SduiBookItemState
    let showReadingTime: Bool
    let onToggleFavorite: () -> Void

    var body: some View {
        HStack(spacing: 14) {
            BookCover(category: item.book.category, title: item.book.title, compact: true)

            VStack(alignment: .leading, spacing: 5) {
                Text(item.book.category.uppercased())
                    .font(.system(.caption2, design: .rounded, weight: .bold))
                    .tracking(0.8)
                    .foregroundStyle(AppTheme.accent)
                Text(item.book.title)
                    .font(.system(.headline, design: .rounded, weight: .bold))
                    .foregroundStyle(AppTheme.ink)
                    .lineLimit(2)
                Text(subtitle)
                    .font(.system(.subheadline, design: .rounded))
                    .foregroundStyle(AppTheme.secondaryInk)
                    .lineLimit(2)
            }

            Spacer(minLength: 4)

            Button(action: onToggleFavorite) {
                Image(systemName: item.isFavorite ? "heart.fill" : "heart")
                    .font(.title3)
                    .foregroundStyle(AppTheme.accent)
                    .frame(width: 44, height: 44)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(
                item.isFavorite
                    ? "Remove \(item.book.title) from favourites"
                    : "Add \(item.book.title) to favourites"
            )
            .accessibilityIdentifier("favorite.\(item.book.id)")
        }
        .padding(14)
        .cardStyle()
    }

    private var subtitle: String {
        guard showReadingTime else { return item.book.author }
        return "\(item.book.author) · \(item.book.readingMinutes) min"
    }
}
