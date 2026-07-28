# Shared KMP ViewModel + native SDUI renderers

## What is shared

`SduiBooksViewModel` is the only state owner for this feature on both platforms. It owns:

- the Ktor requests and JSON decoding of the SDUI document and search results;
- `Idle`, `Loading`, `Success(result)`, and `Error(apiError)` state;
- debounced, cancellable remote search and pagination;
- the set of favourite IDs;
- projection of the server's `FAVORITES` and `ALL` sources into rendered sections;
- cancellation of requests, debounce jobs, and observers.

Android does not add an Android `ViewModel`. Compose collects the KMP `StateFlow` directly.
iOS does not add an `ObservableObject` view model. SwiftUI retains the KMP object and copies its
immutable state snapshots into local `@State` through a cancellable observation.

The native views still own rendering, accessibility, navigation, and platform interaction details.
The server sends a versioned screen definition and component identifiers; each platform maps
`BOOK_ROW` to a real Compose or SwiftUI view.

```text
GET /v1/screens/discover-books       GET /v1/search?q=...&page=...
                \                         /
                 \-> SduiScreenRepository
                              |
                              v
                    SduiBooksViewModel
          intent -> request/reducer -> StateFlow<SduiBooksUiState>
                              |                    |
                              |                    +-> callback -> @State -> SwiftUI
                              +-----------------------> collectAsState -> Compose
```

“The same ViewModel” means the same KMP class and behavior. Android and iOS run separate instances
in their own processes; an object cannot literally be shared between devices.

## Reactivity

The UI sends intents (`setSearchQuery`, `toggleFavorite`, `reload`) to KMP. KMP publishes an
immutable state snapshot. `StateFlow` is conflated, so a slow collector receives the latest state
rather than an unbounded queue of obsolete states.

The fake API is a Ktor `MockEngine`, so this sample stays offline while exercising real request,
HTTP-status, JSON serialization, and cancellation paths:

```text
start
  Idle -> Loading -> Success(result: first 3 books, page 1 of 3)

type "architecture"
  query changes immediately
  -> debounce
  -> Loading
  -> GET /v1/search?q=architecture&page=1
  -> Success(result: Clean Architecture)

type "error"
  -> Loading
  -> GET /v1/search?q=error&page=1
  -> HTTP 503
  -> Error(apiError)

tap "Load more"
  Success(existing result, pagination.event = Loading)
  -> GET /v1/search?q=...&page=2
  -> Success(existing + new components, pagination.event = Settled)
```

Search updates the query immediately so typing stays responsive and cancels the preceding debounce
or pagination job. Only the settled query calls the API. During a fresh search, the last rendered
rows stay available under `Loading`; the native renderers show an inline progress indicator.
Pagination remains a `Success` with a `Loading` pagination event so existing rows do not disappear.
Toggling a favourite projects both sections immediately, keeping duplicate rows consistent.

### `combine`, conflation, and “lost” data

`combine` does not provide queue semantics. It retains the latest value from each upstream and
emits a new tuple when an upstream changes. With hot/conflated inputs or a slow downstream,
intermediate combinations may never be rendered. `StateFlow` has the same intentional property:
collectors are guaranteed the latest state, not every historical state.

That is not data loss when the values describe **current UI state**. It becomes a bug when the code
models a must-process event as state, or splits one atomic transition over independent flows such
as `statusFlow`, `resultsFlow`, and `paginationFlow`. A collector could then observe a logically
mixed snapshot, for example `Success` paired with results from the preceding query.

This sample therefore publishes one `SduiBooksUiState`, and `Success` contains its matching result.
The UI must remain correct if it observes `Loading -> Success` or only the final `Success`. In
production:

- use one atomic state object for values that must be consistent together;
- use `map`/`distinctUntilChanged` only for derived rendering concerns;
- do not rely on observing `Loading` to unlock pagination—the settled state is authoritative;
- use a buffered `Channel`/`SharedFlow`, an acknowledgement, or a direct suspending call for
  commands/effects that must be handled exactly once;
- carry a request ID/query in results and reject stale responses when cancellation alone cannot
  guarantee ordering.

Configuration is injected when the KMP ViewModel is constructed. The sample intentionally uses:

| Setting | Android | iOS |
|---|---:|---:|
| Screen title | Server value | `Book explorer` override |
| Reading time | Hidden | Shown |
| Search debounce | 180 ms | 220 ms |
| Initial favourite | `atomic-habits` | `atomic-habits` |

This is preferable to separate ViewModels when the behavior is the same. Keep configuration small
and typed. If platform behavior starts to branch substantially, use injected strategy interfaces
or separate features instead of accumulating platform conditionals.

## Performance

For this screen the pattern is inexpensive:

- one hot, conflated state stream per visible screen;
- stable item IDs in both lazy lists;
- cancellation of obsolete search work;
- immutable snapshots that reuse `Book` references while constructing small item wrappers;
- no polling and no permanent background collector.

The current projection is `O(number of books + number of visible sections)` and runs on the main
dispatcher. That is appropriate for the small page size in this sample. In a large enterprise
screen, do not send thousands of records in one result. Keep server-side search, prefer cursor
pagination when the data can change, cap page caches, and move genuinely expensive transforms to
`Dispatchers.Default`. Measure request cancellation, state size, projection duration,
recomposition/body evaluations, and scroll frame time.

SDUI itself can hurt performance when documents are deeply nested, identifiers are unstable,
component payloads are very generic, or every small event rebuilds the whole tree. Prefer a finite
component registry, flattened screen state, stable IDs, and feature-level state streams rather
than one application-wide “god” ViewModel.

## Memory and lifecycle

This pattern is not inherently leaky, but an uncancelled collector can be:

- Compose stops collecting when the screen leaves composition. Its `DisposableEffect` calls
  `clear()`, cancelling the KMP `SupervisorJob`.
- Swift retains the returned `SduiObservation`. On disappearance it calls `cancel()` to release
  the callback and then `clear()` to cancel all KMP jobs.
- `clear()` is idempotent and makes that ViewModel instance terminal.

The Swift callback captures the view's `@State`; retaining that callback after navigation would
retain native state. The explicit observation token prevents that. A `StateFlow` also retains its
latest value while its ViewModel is alive. That is expected memory, not a leak, but a singleton
ViewModel holding large documents would make it effectively process-lifetime memory.

For an enterprise app:

1. Scope each ViewModel to a route or feature, not the application singleton.
2. Make collection lifecycle-aware and make cancellation mandatory in the iOS bridge.
3. Avoid callbacks stored in global repositories or SDKs.
4. Use leak tests (LeakCanary on Android and Xcode Memory Graph/Instruments on iOS) that repeatedly
   push and pop the screen.
5. Persist favourites in a repository/database when product requirements need them across screen
   instances; keep persistence out of the native renderers.

## Production SDUI safeguards

Before a backend can independently roll out this schema, add:

- schema compatibility checks and a fallback screen for unsupported versions/components;
- tolerant decoding for additive fields, plus contract tests for breaking changes;
- payload size/depth limits and URL/action allowlists;
- component analytics and render-failure metrics;
- cached last-known-good documents and explicit offline behavior;
- staged rollout/kill switches;
- pagination and remote search for large collections.

The sample uses a Ktor `MockEngine`, but both endpoints return and decode JSON through the same
content-negotiation path as a real backend. Replacing the engine and base URL is therefore a
transport change, not a presentation rewrite.
