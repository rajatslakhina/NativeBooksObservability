# Native Books: native iOS and Android hosts + shared KMP logic

Two standalone native host applications with:

- email/password login
- a five-book library
- a book description screen opened by tapping a row
- a native server-driven book explorer with favourites and shared search
- KMP-owned business logic, repository, dummy Ktor API, and view-model state
- native SwiftUI on iOS and native Jetpack Compose on Android
- platform-owned OpenTelemetry tracing exported to New Relic

```text
NativeBooksObservability
├── iosApp/                       native SwiftUI application
├── Packages/NativeObservability local iOS source SPM
├── Packages/SharedKMP           local iOS binary SPM + XCFramework
├── androidApp/                  native Jetpack Compose application
├── androidObservability/        local native Android library
└── shared/                      KMP business logic and platform tracer actuals
```

The iOS target owns all SwiftUI screens, navigation, app lifecycle, and native SDK setup. It consumes two independent local Swift packages; the KMP package is an integrated library, not the application shell:

```text
NativeBooks.xcworkspace
├── Packages/NativeObservability       local source SPM package
│   └── Observability dynamic library + OpenTelemetry/New Relic
└── Packages/SharedKMP                 local binary SPM package
    └── SharedKit.xcframework          built from shared/ Kotlin sources
```

## Server-driven native screen

After login, use the heart/document button in the library toolbar to open the new SDUI screen. Its
Ktor screen and search endpoints, JSON models, search debounce, pagination, favourites, section
projection, and state lifecycle are shared in KMP. Android collects the KMP `StateFlow` directly
and iOS observes the same KMP ViewModel through a cancellable callback; neither platform adds a
native ViewModel for this feature. Compose and SwiftUI still render fully native rows and search
controls.

The fake API loads three books per page. Search for `architecture` to see a successful debounced
request, search for `error` to exercise the typed HTTP 503 error state, or use **Load more** to see
the in-flight pagination state preserve the existing rows.

The shared state transitions are request-driven:

```text
Initial load:  Idle -> Loading -> Success(result)
Search:        query -> debounce -> Loading -> Success(result) / Error(apiError)
Pagination:    Success(existing, Loading) -> Success(existing + next page, Settled)
```

`Success` owns the result and pagination data that belong together. The feature intentionally
publishes one atomic `SduiBooksUiState` instead of combining independent status, result, and
pagination streams. `StateFlow` may conflate intermediate render states, so correctness depends on
the latest complete snapshot rather than observing every transition.

Rows use section-scoped native identities such as `favorites.atomic-habits` and
`all.atomic-habits`. This is required because the same book can appear in both sections. It avoids
duplicate identity warnings and undefined layout reuse in SwiftUI `LazyVStack`; Compose uses the
same `sectionId.bookId` rule for lazy-list keys.

See [the SDUI architecture and scale analysis](docs/SDUI_ARCHITECTURE.md) for the reactive flow,
`combine`/conflation guidance, platform configuration example, performance characteristics, and
memory-leak safeguards.

KMP does not embed an iOS observability SDK. It asks the host application to create each native span through the delegate exported from `iosMain`:

```text
KMP repository / view model
        │ NativeTracer.startSpan(...)
        ▼
iosMain NativeTracer actual
        │ IosTracerProvider.startNativeSpan(...)
        ▼
Swift IosTracerProviderImpl
        │ ObservabilitySystem.beginSpan(...)
        ▼
OpenTelemetry Swift SDK creates and registers the active span
        │ traceId + spanId + sampled
        ▼
KmpSpanContext returned to shared Kotlin
        │ W3C traceparent request header
        ▼
Ktor dummy HTTP request
        │ response / error
        ▼
NativeTracer.endSpan(context, status)
        │ synchronous Swift delegate call
        ▼
ObservabilitySystem.endSpan(...) → acknowledged Bool
```

Android follows the same `commonMain` contract. `NativeBooksApplication` configures the SDK from the local `androidObservability` module before `androidMain` obtains its tracer from `GlobalOpenTelemetry`.

## Run the iOS app

Requirements: Xcode 16+, an iOS 17+ simulator, XcodeGen, and JDK 21. Internet access is needed once to resolve Gradle and Swift package dependencies.

1. Open `iosApp/NativeBooks.xcworkspace`.
2. Select the `NativeBooks` scheme and an Apple Silicon iOS simulator.
3. Run the app.

The local `SharedKMP` package already contains `SharedKit.xcframework`. After changing Kotlin code, rebuild that binary package before reopening or rebuilding in Xcode:

```sh
./scripts/build-shared-kmp-package.sh
```

To regenerate the checked-in Xcode project:

```sh
cd iosApp
xcodegen generate --spec project.yml
```

The login form is prefilled. Any syntactically valid email and password of at least six characters works.

## Run the Android app

Requirements: JDK 21, Android SDK 36.1, and an Android 24+ device or emulator.

1. Open this repository root in Android Studio.
2. Select the `androidApp` run configuration.
3. Select an emulator or connected device and run.

The Android app is a normal `com.android.application` Compose host. It depends on `:shared` and `:androidObservability` as local Gradle modules; the shared module does not own the Activity, Compose UI, navigation, or application lifecycle.

Command-line build:

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
./gradlew :androidApp:assembleDebug
```

## Send iOS traces to New Relic

The app remains usable without a credential; the native package uses its console span exporter in that case.

1. Copy `iosApp/Configuration/NewRelic.xcconfig.example` to `iosApp/Configuration/NewRelic.local.xcconfig` if the local file does not exist.
2. Set `NEW_RELIC_LICENSE_KEY` to a New Relic ingest/license key. Do not use a user or browser key.
3. Rebuild and run the app.
4. Sign in, load the library, open a book, and use the waveform button to create and flush a test span.

The native package sends OTLP/HTTP traces to `https://otlp.nr-data.net/v1/traces` with the `api-key` header. The key is never included in this repository.

Open the supplied [New Relic account link](https://one.newrelic.com/catalogs/software?account=7864060&state=976c7445-fbf8-c320-b387-7d543d234957) after signing in. A useful first query is:

```sql
FROM Span SELECT *
WHERE service.name = 'native-books-ios'
SINCE 30 minutes ago
```

## Send Android traces to New Relic

Provide the same New Relic ingest/license key without storing it in the repository:

```sh
export NEW_RELIC_LICENSE_KEY='your-ingest-license-key'
./gradlew :androidApp:installDebug
```

You can alternatively add `NEW_RELIC_LICENSE_KEY=...` to `~/.gradle/gradle.properties`. With no key, `androidObservability` uses the OpenTelemetry logging exporter and the app remains fully usable. Android spans use `service.name = 'native-books-android'`.

## Native package architecture

The local package follows the same separation of concerns as the reference consumer app while remaining focused on OpenTelemetry/New Relic:

```text
ObservabilitySystem (public synchronous façade)
        │ builder installs providers once
        ▼
EventTracker → enrichment chain → capability fan-out
        │ typed, Sendable event attributes
        ▼
TelemetryProvider → OTelSpanTransport
        │ private tracer/logger providers + locked span registry
        ▼
OTLP/HTTP traces and logs → New Relic
```

- All public operations are synchronous: there are no actors, detached tasks, or fire-and-forget sends.
- Shared mutable state and the active-span registry are protected with `NSLock`; event dispatch and flushing occur outside the system-state lock.
- The package owns private OpenTelemetry tracer and logger providers. It does not mutate OpenTelemetry's global registry, avoiding conflicts with another SDK in the host app.
- Events use typed `ObservabilityValue` attributes and a caller-wins enrichment policy for predictable metadata.
- `beginSpan`/`endSpan` support real span lifecycles. The KMP bridge maps its separate start/end calls to these native operations and receives a synchronous end acknowledgement.
- Missing credentials select the stdout exporter. Credentials are injected by the host through xcconfig and are never stored in the package.

Adobe and Embrace providers from the reference package are intentionally not copied because this sample's provider boundary is the native OpenTelemetry/New Relic implementation.

The Android equivalent remains native as a separate local Gradle library:

```text
NativeBooksApplication
        │ configure synchronously at process launch
        ▼
androidObservability.ObservabilitySystem
        │ installs SdkTracerProvider + SimpleSpanProcessor
        ▼
GlobalOpenTelemetry
        │ androidMain NativeTracer starts/ends native spans
        ▼
OTLP/HTTP exporter → New Relic, or Logcat when no key is supplied
```

## KMP bridge

The shared contract is platform-agnostic:

```kotlin
data class KmpSpanContext(
    val traceId: String,
    val spanId: String,
    val sampled: Boolean,
)

enum class KmpSpanStatus { UNSET, OK, ERROR }

interface KmpTracer {
    fun startSpan(
        name: String,
        attributes: Map<String, String> = emptyMap(),
    ): KmpSpanContext

    fun endSpan(
        context: KmpSpanContext,
        attributes: Map<String, String> = emptyMap(),
        status: KmpSpanStatus = KmpSpanStatus.UNSET,
    ): Boolean
}

expect object NativeTracer : KmpTracer
```

`iosMain` exposes `IosTracerProvider` and delegates synchronously. The app retains its Swift provider for the full launch lifecycle and registers it before SwiftUI creates the first screen:

```swift
let provider = IosTracerProviderImpl()
NativeTracer.shared.initialize(provider: provider)
```

`BooksRepository` then converts the returned context to a W3C header such as:

```text
traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
```

The sample uses Ktor `MockEngine` so it is deterministic and offline while still executing the request/header pipeline. Replace the engine and dummy URL to exercise a real backend.

The native span remains in a thread-safe registry while Ktor performs the request. KMP ends that exact span in `finally`, supplying success/error status and response attributes. `endSpan` returns `true` only if the native registry found and removed the matching span; a duplicate end returns `false`.

The `traceparent` value stays private to KMP's network layer and is not exposed to either native frontend or generated by either native Observability implementation. The telemetry UI shows only context IDs and whether native span end was acknowledged. The KMP fake-tracer test independently verifies that the repository receives the exact native context, constructs and attaches the header to Ktor, and ends the same context.

The same round trip is visible in Xcode's debug console or Android Studio Logcat without inspecting the UI:

```text
[KMP][NativeTracer] received native context ... valid=true
[KMP][DummyBooksApi] received BFF response status=200 traceparentAttached=true
[KMP][BooksRepository] received BFF books count=5 ids=...
[KMP][NativeTracer] native end result ... acknowledged=true
```

These logs deliberately report metadata and identifiers rather than response bodies or credentials. Replace `DummyBooksApi` with the real BFF client while retaining the same repository and tracing boundary.

## Tests and builds

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
./gradlew :shared:allTests \
  :androidApp:assembleDebug \
  :androidApp:assembleDebugAndroidTest

swift test --package-path Packages/NativeObservability
./scripts/build-shared-kmp-package.sh

cd iosApp
xcodebuild -workspace NativeBooks.xcworkspace \
  -scheme NativeBooks \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO build
```

KMP tests verify login validation, dummy book data, repository caching, the exact `traceparent`
header, start/use/end lifecycle behavior, SDUI state transitions, debounced API search, pagination,
error handling, and observation cancellation. Swift package and iOS tests verify W3C context
validity, synchronous native lifecycle acknowledgement, duplicate-end rejection, enrichment
precedence, and delivery of shared SDUI search results to Swift. The Android Compose instrumentation
test verifies login, native SDUI rendering, and a debounced KMP API search result.

## Dependency versions

- Kotlin Multiplatform `2.3.21`
- Ktor `3.5.1`
- Android OpenTelemetry SDK/exporter `1.62.0`
- Jetpack Compose BOM `2026.06.00`
- Android Activity Compose `1.13.0`
- OpenTelemetry Swift/Core `2.3.0`
- Android Gradle Plugin `8.13.2`
- Android compile SDK `36.1`, min SDK `24`
- iOS deployment target `17.0`
