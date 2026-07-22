import SharedKit
import SwiftUI

struct LoginView: View {
    @EnvironmentObject private var appState: AppState
    @State private var email = "reader@example.com"
    @State private var password = "books123"
    @State private var validationMessage: String?
    @FocusState private var focusedField: Field?

    private enum Field {
        case email
        case password
    }

    var body: some View {
        ZStack {
            AppTheme.paper.ignoresSafeArea()

            Circle()
                .fill(AppTheme.accent.opacity(0.12))
                .frame(width: 330, height: 330)
                .blur(radius: 2)
                .offset(x: 150, y: -330)

            ScrollView {
                VStack(alignment: .leading, spacing: 30) {
                    brand
                    welcomeCopy
                    form
                    observabilityNote
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 34)
                .frame(maxWidth: 560)
                .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.interactively)
        }
    }

    private var brand: some View {
        HStack(spacing: 12) {
            Image(systemName: "books.vertical.fill")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: 44, height: 44)
                .background(AppTheme.accent)
                .clipShape(RoundedRectangle(cornerRadius: 13, style: .continuous))

            Text("NATIVE BOOKS")
                .font(.system(.subheadline, design: .rounded, weight: .bold))
                .tracking(1.8)
                .foregroundStyle(AppTheme.ink)
        }
    }

    private var welcomeCopy: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Your next good idea\nstarts here.")
                .font(.system(size: 43, weight: .bold, design: .rounded))
                .foregroundStyle(AppTheme.ink)
                .minimumScaleFactor(0.8)

            Text("Sign in to open your small, thoughtfully curated library.")
                .font(.system(.body, design: .rounded))
                .foregroundStyle(AppTheme.secondaryInk)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var form: some View {
        VStack(spacing: 16) {
            inputField(
                title: "Email",
                icon: "envelope",
                text: $email,
                field: .email,
                contentType: .emailAddress
            )

            VStack(alignment: .leading, spacing: 8) {
                Label("Password", systemImage: "lock")
                    .font(.system(.caption, design: .rounded, weight: .semibold))
                    .foregroundStyle(AppTheme.secondaryInk)

                SecureField("At least 6 characters", text: $password)
                    .textContentType(.password)
                    .focused($focusedField, equals: .password)
                    .submitLabel(.go)
                    .onSubmit(submit)
                    .padding(15)
                    .background(Color.white.opacity(0.72))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .overlay {
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .stroke(focusedField == .password ? AppTheme.accent : AppTheme.line, lineWidth: 1.2)
                    }
            }

            if let validationMessage {
                Text(validationMessage)
                    .font(.system(.footnote, design: .rounded, weight: .medium))
                    .foregroundStyle(.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            Button(action: submit) {
                HStack {
                    Text("Open my library")
                    Spacer()
                    Image(systemName: "arrow.right")
                }
                .font(.system(.headline, design: .rounded, weight: .semibold))
                .foregroundStyle(.white)
                .padding(.horizontal, 18)
                .frame(height: 56)
                .background(AppTheme.accent)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("login.button")
        }
        .padding(20)
        .cardStyle()
    }

    private func inputField(
        title: String,
        icon: String,
        text: Binding<String>,
        field: Field,
        contentType: UITextContentType
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Label(title, systemImage: icon)
                .font(.system(.caption, design: .rounded, weight: .semibold))
                .foregroundStyle(AppTheme.secondaryInk)

            TextField("reader@example.com", text: text)
                .textContentType(contentType)
                .textInputAutocapitalization(.never)
                .keyboardType(.emailAddress)
                .autocorrectionDisabled()
                .focused($focusedField, equals: field)
                .submitLabel(.next)
                .onSubmit { focusedField = .password }
                .padding(15)
                .background(Color.white.opacity(0.72))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .stroke(focusedField == field ? AppTheme.accent : AppTheme.line, lineWidth: 1.2)
                }
        }
    }

    private var observabilityNote: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: appState.observabilityStatus.exportsToNewRelic ? "waveform.badge.checkmark" : "waveform")
                .foregroundStyle(AppTheme.accent)
            Text("OpenTelemetry is active. \(appState.observabilityStatus.destination).")
                .font(.system(.caption, design: .rounded))
                .foregroundStyle(AppTheme.secondaryInk)
        }
    }

    private func submit() {
        let validation = LoginValidator().validate(email: email, password: password)
        let context = NativeTracer.shared.startSpan(
            name: "login.submit",
            attributes: [
                "app.feature": "login",
                "login.valid": String(validation.isValid),
            ]
        )
        _ = NativeTracer.shared.endSpan(
            context: context,
            attributes: ["kmp.context.received": String(context.isValid)],
            status: validation.isValid ? .ok : .error
        )

        if validation.isValid {
            validationMessage = nil
            focusedField = nil
            appState.logIn(email: email)
        } else {
            validationMessage = validation.message
        }
    }
}
