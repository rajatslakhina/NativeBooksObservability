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
}
