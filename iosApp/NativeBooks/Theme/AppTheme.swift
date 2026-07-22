import SwiftUI

enum AppTheme {
    static let accent = Color(red: 0.21, green: 0.38, blue: 0.30)
    static let ink = Color(red: 0.10, green: 0.12, blue: 0.11)
    static let secondaryInk = Color(red: 0.38, green: 0.41, blue: 0.39)
    static let paper = Color(red: 0.96, green: 0.95, blue: 0.91)
    static let card = Color(red: 1.00, green: 0.99, blue: 0.96)
    static let line = Color.black.opacity(0.08)
}

extension View {
    func cardStyle() -> some View {
        self
            .background(AppTheme.card)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(AppTheme.line, lineWidth: 1)
            }
            .shadow(color: Color.black.opacity(0.05), radius: 14, y: 7)
    }
}
