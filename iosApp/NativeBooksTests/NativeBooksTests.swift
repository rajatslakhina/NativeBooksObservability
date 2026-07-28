import SharedKit
import XCTest
@testable import NativeBooks

final class NativeBooksTests: XCTestCase {
    func testSharedLoginValidation() {
        let validation = LoginValidator().validate(
            email: "reader@example.com",
            password: "books123"
        )

        XCTAssertTrue(validation.isValid)
    }

    func testDummyApiReturnsBookDetails() {
        let nativeTracerProvider = IosTracerProviderImpl()
        NativeTracer.shared.initialize(provider: nativeTracerProvider)
        let repository = BooksRepository()
        let loaded = expectation(description: "KMP books loaded")

        repository.loadBooks { books, error in
            XCTAssertNil(error)
            XCTAssertEqual(books?.count, 5)
            XCTAssertTrue(repository.lastSpanEndAcknowledged)
            XCTAssertEqual(repository.book(id: "atomic-habits")?.author, "James Clear")
            loaded.fulfill()
        }

        wait(for: [loaded], timeout: 2)
    }

    func testNativeBridgeReturnsContextAndAcknowledgesEnd() {
        let provider = IosTracerProviderImpl()
        let context = provider.startNativeSpan(
            name: "test.kmp.lifecycle",
            attributes: ["test.source": "ios-host"]
        )

        XCTAssertTrue(context.isValid)
        XCTAssertTrue(
            provider.endNativeSpan(
                context: context,
                attributes: ["kmp.context.received": "true"],
                status: .ok
            )
        )
        XCTAssertFalse(
            provider.endNativeSpan(
                context: context,
                attributes: [:],
                status: .ok
            ),
            "A second end must fail because the native registry already removed the span"
        )
    }

    func testSharedSduiViewModelPublishesRemoteSearchResultToSwift() {
        let viewModel = SduiBooksViewModel(
            configuration: SduiScreenConfiguration(
                titleOverride: nil,
                showReadingTime: true,
                searchDebounceMillis: 20,
                initialFavoriteIds: ["atomic-habits"]
            )
        )
        let loaded = expectation(description: "KMP search result observed in Swift")
        var requestedSearch = false

        let observation = viewModel.observe { state in
            guard let success = state.status as? SduiSearchStatus.Success else { return }

            if !requestedSearch {
                requestedSearch = true
                viewModel.setSearchQuery(query: "leadership")
            } else if success.result.query == "leadership" {
                XCTAssertEqual(success.result.components.count, 1)
                XCTAssertEqual(success.result.components.first?.title, "Staff Engineer")
                loaded.fulfill()
            }
        }

        viewModel.start()
        wait(for: [loaded], timeout: 3)
        observation.cancel()
        viewModel.clear()
    }
}
