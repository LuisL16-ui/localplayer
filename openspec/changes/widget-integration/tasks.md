# Tasks: Widget Integration

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~350–450 (deps config ~10, MusicService ~20, widget files ~150, manifest/xml ~15, tests ~150–200) |
| 400-line budget risk | Medium |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 → PR 4 |
| Delivery strategy | ask-on-risk |
| Chain strategy | feature-branch-chain |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Add Glance deps + version catalog + `glance-testing` | PR 1 | `./gradlew dependencies --configuration debugRuntimeClasspath` | Gradle sync | Remove glance deps from build files |
| 2 | Add seek constants + `when` branch in `onStartCommand` + manifest receiver + widget XML | PR 2 | `./gradlew test` | Emulator with widget placed | Revert MusicService + manifest + res/xml |
| 3 | Create `PlayerWidget.kt` + `PlayerWidgetActions.kt` | PR 3 | `./gradlew test` | Widget rendering on emulator | Delete `widget/` package |
| 4 | Wire `updateAll` + write strict TDD tests | PR 4 | `./gradlew test` | Full widget lifecycle on emulator | Remove `updateAll` call + test files |

## Open Questions Addressed

1. **`ACTION_SEEK_BACKWARD`/`ACTION_SEEK_FORWARD` constants** → Added to `MusicService.companion` in T2
2. **`onStartCommand` seek handling** → New `when` branch in T2
3. **`ImageProvider(Bitmap)` API** → Verified task in T4; confirmed `fun ImageProvider(bitmap: Bitmap): ImageProvider` matches design
4. **`ArtworkLoader.loadThumbnail` signature** → Verified `suspend fun loadThumbnail(context, uri, filePath?, targetSizePx)` — confirmed suspend, callable directly in `provideGlance`

---

## Phase 1: Dependencies and Config

- [ ] 1.1 Add `glance-appwidget:1.1.1`, `glance-material3:1.1.1`, and `kotlinCompilerExtensionVersion = "1.5.15"` to `gradle/libs.versions.toml` under `[libraries]` and `[versions]` sections
- [ ] 1.2 Add `implementation(libs.glance.appwidget)`, `implementation(libs.glance.material3)`, `glancePlugin(libs.glance.compiler)` to `app/build.gradle.kts` under `dependencies {}` and `plugins {}`
- [ ] 1.3 Add `androidTestImplementation("androidx.glance:glance-testing:1.1.1")` to `app/build.gradle.kts` under `dependencies {}`
- [ ] 1.4 Sync Gradle and verify `./gradlew dependencies --configuration debugRuntimeClasspath` resolves all Glance artifacts without conflict

## Phase 2: MusicService Seek Constants and Handlers (parallel with Phase 3)

- [ ] 2.1 Add `const val ACTION_SEEK_BACKWARD = "com.cvc953.localplayer.ACTION_SEEK_BACKWARD"` and `const val ACTION_SEEK_FORWARD = "com.cvc953.localplayer.ACTION_SEEK_FORWARD"` to `MusicService.companion` in `Services/MusicService.kt`
- [ ] 2.2 Add `ACTION_SEEK_BACKWARD` and `ACTION_SEEK_FORWARD` `when` branches in `MusicService.onStartCommand` → `playerController.seekTo(max(0, positionMs - 10_000))` and `playerController.seekTo(min(durationMs, positionMs + 10_000))` respectively, both returning `START_STICKY`
- [ ] 2.3 Verify seek clamping: position=30_000 → seekTo(20_000); position=5_000 → seekTo(0); position=175_000,duration=180_000 → seekTo(180_000)
- [ ] 2.4 Unit test: `MusicServiceSeekTest` — verify each `when` branch invokes `playerController.seekTo` with correct clamped values

## Phase 3: Widget Manifest and Metadata (parallel with Phase 2)

- [ ] 3.1 Create `app/src/main/res/xml/player_widget_info.xml` with `<appwidget-provider>` — `minWidth="180dp"`, `minHeight="110dp"`, `updatePeriodMillis="1800000"`, `initialLayout="@layout/player_widget"`, `resizeMode="horizontal|vertical"`, `widgetCategory="home_screen"`
- [ ] 3.2 Add `<receiver android:name="androidx.glance.appwidget.GlanceAppWidgetReceiver" android:exported="true">` to `AndroidManifest.xml` with `<intent-filter><action android:name="android.appwidget.action.APPWIDGET_UPDATE"/></intent-filter>` and `<meta-data android:name="android.appwidget.provider" android:resource="@xml/player_widget_info"/>`
- [ ] 3.3 Verify manifest compiles — `./gradlew :app:compileDebugAndroidTestSources` succeeds

## Phase 4: Core Widget Implementation

- [ ] 4.1 Create `widget/PlayerWidget.kt`: `class PlayerWidget : GlanceAppWidget()` with `override suspend fun provideGlance(context, appWidgetManager, appWidgetId)` — reads `AppPrefs(context)` for `loadLastSongUri()`, `loadPlaybackPosition()`, `loadIsPlaying()`, calls `withContext(Dispatchers.IO) { ArtworkLoader.loadThumbnail(context, Uri.parse(uri), targetSizePx=256) }` for artwork, invokes `MyContent()`
- [ ] 4.2 Create `widget/PlayerWidget.kt`: `@Composable fun MyContent(isPlaying, position, duration, artwork: Bitmap?)` — `GlanceTheme(darkTheme = true, primaryColor = Color(0xFF2196F3))`, `Image(provider = artwork?.let { ImageProvider(it) } ?: ImageProvider(R.drawable.ic_launcher_foreground), ...)`, `Text`, `LinearProgressIndicator(progress = position.toFloat() / duration.toFloat())`, transport `Button(actionRunCallback<PlayPauseAction>)`, `Button(actionRunCallback<NextAction>)`, `Button(actionRunCallback<PrevAction>)`, seek `-10`/`+10` buttons
- [ ] 4.3 Verify `ImageProvider(Bitmap)` API: confirm `fun ImageProvider(bitmap: Bitmap): ImageProvider` accepts the `albumArt: Bitmap?` from `ArtworkLoader.loadThumbnail` — write a compile-check assertion in task notes; if API differs, adjust `ImageProvider` call
- [ ] 4.4 Verify `ArtworkLoader.loadThumbnail` is `suspend` and can be called directly inside `provideGlance` without `withContext(Dispatchers.IO)` wrapper — design doc confirms it is suspend; add task check that `withContext(Dispatchers.IO)` wrapping is optional

## Phase 5: Widget Action Callbacks

- [ ] 5.1 Create `widget/PlayerWidgetActions.kt`: `abstract class MusicServiceAction(private val action: String) : ActionCallback()` with `override suspend fun onAction(context, glanceId, parameters)` → `context.startService(Intent(context, MusicService::class.java).apply { this.action = action })`
- [ ] 5.2 Create `class PlayPauseAction : MusicServiceAction(MusicService.ACTION_PLAY_PAUSE)`, `class NextAction : MusicServiceAction(MusicService.ACTION_NEXT)`, `class PrevAction : MusicServiceAction(MusicService.ACTION_PREV)`
- [ ] 5.3 Create `class SeekBackward10Action : MusicServiceAction(MusicService.ACTION_SEEK_BACKWARD)` and `class SeekForward10Action : MusicServiceAction(MusicService.ACTION_SEEK_FORWARD)`
- [ ] 5.4 Verify `GlanceActionCallback` + `actionRunCallback<T>` API works with `Button` composable — compile-check that `Button(actionRunCallback<PlayPauseAction>)` resolves correctly
- [ ] 5.5 Unit test: `PlayerWidgetActionsTest` — for each of the 5 action subclasses, mock `Context`, verify `startService` intent action equals the correct `MusicService` constant

## Phase 6: State Push Wiring

- [ ] 6.1 Add `PlayerWidget().updateAll(this)` inside the `playerController.state.collect` block in `MusicService.onCreate()`, inside the existing throttle logic (1s interval), after `updateNotification()` calls
- [ ] 6.2 Verify `updateAll` works from `Service` context — confirm `Context` passed to `MusicService` (via `this`) is valid for `GlanceAppWidgetManager` — add compile-check assertion
- [ ] 6.3 Instrumented test: `WidgetStateUpdateTest` — place widget on emulator, trigger `playerController.state` change via `ACTION_PLAY_PAUSE`, verify `PlayerWidget().updateAll(this)` fires within 1s and widget reflects new `isPlaying` state

## Phase 7: Strict TDD Testing

- [ ] 7.1 Unit test: `WidgetColdStartTest` — mock `AppPrefs`, verify `provideGlance` reads `loadLastSongUri()`, `loadPlaybackPosition()`, `loadIsPlaying()` on cold-start; test fallback when `AppPrefs` has no saved data
- [ ] 7.2 Unit test: `WidgetArtworkRecoveryTest` — mock `ArtworkLoader.loadThumbnail`, verify it is called with `Uri.parse(lastSongUri)` and `targetSizePx=256` when artwork exists; verify fallback `ImageProvider(R.drawable.ic_launcher_foreground)` when `lastSongUri` is null
- [ ] 7.3 Unit test: `SeekClampTest` — test `SeekBackward10Action` computes `max(0, position - 10_000)` for boundary cases (position=30_000 → 20_000, position=5_000 → 0); test `SeekForward10Action` computes `min(duration, position + 10_000)` (pos=30_000,dur=180_000 → 40_000, pos=175_000,dur=180_000 → 180_000)
- [ ] 7.4 Instrumented test: `WidgetActionClickTest` — use `glance-testing` `GlanceTestRule`, place widget, click `SeekBackward10Action`/`SeekForward10Action` buttons, verify `MusicService` receives correct `ACTION_SEEK_BACKWARD`/`ACTION_SEEK_FORWARD` intents
- [ ] 7.5 Instrumented test: `WidgetArtworkDeathTest` — kill app process, re-render widget, verify `ArtworkLoader.loadThumbnail` produces correct `Bitmap` from `loadLastSongUri()` and widget displays recovered artwork
- [ ] 7.6 Run `./gradlew test` and confirm all tests green (strict TDD gate)

---

## Ordering Constraints

- T1 (Phase 1) must complete before T3–T6 (any Glance code compiles)
- T2 (Phase 2) and T3 (Phase 3) run in parallel — service changes and manifest/xml are independent
- T4–T5 (Phase 4–5) depend on T1 (Glance deps resolved)
- T6 (Phase 6) depends on T4 (needs `PlayerWidget` class)
- T7 (Phase 7) depends on T2–T6 (all code must exist before tests)

## PR Chain Plan (feature-branch-chain)

- **PR 1** (base = feature/tracker): T1 — Dependencies and config
- **PR 2** (base = PR 1): T2 + T3 — Seek constants/handlers + manifest/xml metadata
- **PR 3** (base = PR 2): T4 + T5 — Widget UI composable + action callbacks
- **PR 4** (base = PR 3): T6 + T7 — State push wiring + strict TDD tests

Each child PR diff stays focused; the tracker branch accumulates the final integration. Only the tracker merges to main.
