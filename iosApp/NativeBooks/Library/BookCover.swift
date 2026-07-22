import SwiftUI

struct BookCover: View {
    let category: String
    let title: String
    var compact = false

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            LinearGradient(
                colors: palette,
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            Circle()
                .fill(.white.opacity(0.14))
                .frame(width: compact ? 54 : 150)
                .offset(x: compact ? 25 : 90, y: compact ? -35 : -120)

            VStack(alignment: .leading, spacing: compact ? 4 : 10) {
                Image(systemName: symbol)
                    .font(.system(size: compact ? 15 : 27, weight: .semibold))
                if !compact {
                    Text(title)
                        .font(.system(.title3, design: .serif, weight: .bold))
                        .lineLimit(4)
                }
            }
            .foregroundStyle(.white)
            .padding(compact ? 10 : 20)
        }
        .frame(width: compact ? 70 : 210, height: compact ? 94 : 292)
        .clipShape(RoundedRectangle(cornerRadius: compact ? 10 : 18, style: .continuous))
        .overlay(alignment: .leading) {
            Rectangle()
                .fill(.black.opacity(0.13))
                .frame(width: compact ? 4 : 7)
        }
        .shadow(color: palette[1].opacity(0.22), radius: compact ? 5 : 16, x: 3, y: compact ? 3 : 10)
    }

    private var palette: [Color] {
        switch category {
        case "Software Design": return [Color(red: 0.16, green: 0.42, blue: 0.34), Color(red: 0.07, green: 0.20, blue: 0.17)]
        case "Engineering": return [Color(red: 0.65, green: 0.28, blue: 0.18), Color(red: 0.30, green: 0.11, blue: 0.08)]
        case "Distributed Systems": return [Color(red: 0.17, green: 0.36, blue: 0.58), Color(red: 0.08, green: 0.16, blue: 0.30)]
        case "Productivity": return [Color(red: 0.75, green: 0.53, blue: 0.17), Color(red: 0.43, green: 0.24, blue: 0.07)]
        default: return [Color(red: 0.47, green: 0.28, blue: 0.56), Color(red: 0.21, green: 0.10, blue: 0.29)]
        }
    }

    private var symbol: String {
        switch category {
        case "Software Design": return "square.3.layers.3d"
        case "Engineering": return "wrench.and.screwdriver"
        case "Distributed Systems": return "point.3.connected.trianglepath.dotted"
        case "Productivity": return "sparkles"
        default: return "paintbrush.pointed"
        }
    }
}
