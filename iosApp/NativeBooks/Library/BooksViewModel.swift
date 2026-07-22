import SharedKit
import SwiftUI

@MainActor
final class LibraryViewModel: ObservableObject {
    @Published private(set) var books: [Book] = []
    @Published private(set) var isLoading = false
    @Published var errorMessage: String?
    @Published private(set) var lastSpanEndAcknowledged = false

    private let sharedViewModel = BooksViewModel()
    private var hasLoaded = false

    func load() {
        guard !hasLoaded else { return }
        hasLoaded = true
        loadBooks(forceReload: false)
    }

    func reload() {
        hasLoaded = true
        loadBooks(forceReload: true)
    }

    private func loadBooks(forceReload: Bool) {
        isLoading = true
        errorMessage = nil

        sharedViewModel.loadBooks(forceReload: forceReload) { [weak self] books, error in
            Task { @MainActor in
                guard let self else { return }
                self.books = books ?? []
                self.errorMessage = error?.localizedDescription ?? self.sharedViewModel.errorMessage
                self.lastSpanEndAcknowledged = self.sharedViewModel.lastSpanEndAcknowledged
                self.isLoading = false
            }
        }
    }
}
