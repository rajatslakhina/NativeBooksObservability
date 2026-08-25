/// Native trace identifiers returned to Kotlin Multiplatform.
/// Header construction and network propagation belong to the KMP network layer.
public struct NativeSpanContext: Sendable, Equatable {
    public let traceId: String
    public let spanId: String
    public let sampled: Bool
    public let propagationHeaders: [String: String]

    public init(
        traceId: String,
        spanId: String,
        sampled: Bool,
        propagationHeaders: [String: String] = [:]
    ) {
        self.traceId = traceId
        self.spanId = spanId
        self.sampled = sampled
        self.propagationHeaders = propagationHeaders
    }

    public var isValid: Bool {
        traceId.isValidW3CHex(length: 32) && spanId.isValidW3CHex(length: 16)
    }

    public static let invalid = NativeSpanContext(
        traceId: "",
        spanId: "",
        sampled: false,
        propagationHeaders: [:]
    )
}

private extension String {
    func isValidW3CHex(length: Int) -> Bool {
        let bytes = Array(utf8)
        return bytes.count == length
            && bytes.allSatisfy { (48...57).contains($0) || (97...102).contains($0) }
            && bytes.contains(where: { $0 != 48 })
    }
}
