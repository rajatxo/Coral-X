# Coral X — Master Handover Document

> **Purpose**: This document captures EVERYTHING about the Coral X project.
> If the AI session resets, show this file and work continues seamlessly.
> Last updated: 2026-10-11

---

## 1. WHAT IS CORAL X?

Coral X is a **fresh-start Android music player app** built from scratch.
It is NOT an update of the old "Coral" app. It is a completely new codebase.

- **Old app**: "Coral" (github.com/rajatxo/Coral) — had layout fitting issues,
  gesture conflicts, system bar overlap on different phones
- **New app**: "Coral X" (github.com/rajatxo/Coral-X) — clean slate, built
  with universal layout patterns from day one
- **Final name**: When the app is finalized, the name changes from "Coral X"
  to "Coral" (replacing the old app entirely)
- **The "X" is temporary** — used only to avoid confusion between old and new

### Developer
- Name: Rajat
- Telegram: https://t.me/rajat_xo
- Support group: https://t.me/coralmusicsupport
- GitHub: rajatxo
- GitHub token: `[YOUR_GITHUB_TOKEN]`

### Target
- Play Store release (eventually)
- Telegram distribution (immediate, for sharing)
- Must work on EVERY Android phone — no fitting issues, no gesture conflicts,
  no system bar overlap, no floating window problems

---

## 2. REPOSITORY

```
Repo:     https://github.com/rajatxo/Coral-X.git
Branch:   main
Local:    /home/z/my-project/coral-x
```

### Current state (commit 26ed0e8)
Clean slate — 11 files total. Zero Coral code. Zero features.
Just a blank black screen with "Coral X" text.

```
app/src/main/
├── AndroidManifest.xml          ← universal flags
├── java/com/rajatxo/coral/
│   ├── CoralApplication.kt      ← empty Application class
│   └── MainActivity.kt         ← edge-to-edge + blank screen
└── res/
    ├── drawable/
    │   ├── ic_launcher_background.xml
    │   ├── ic_launcher_foreground.xml
    │   ├── ic_launcher_monochrome.xml
    │   └── splash_background.xml  ← pure black
    ├── mipmap-anydpi-v26/
    │   ├── ic_launcher.xml
    │   └── ic_launcher_round.xml
    └── values/
        ├── strings.xml           ← app_name = "Coral X"
        └── themes.xml            ← universal theme
```

### Version
```
versionCode = 4
versionName = "0.1.0"
applicationId = "com.rajatxo.coral"
minSdk = 24
targetSdk = 35
compileSdk = 35
```

---

## 3. SIGNING KEYSTORE

```
Keystore file:  coral-x-release.jks
Alias:           coralx
Store password:  The Weeknd 199
Key password:    The Weeknd 199
Validity:        9125 days (25 years)
Algorithm:       RSA 2048-bit
```

### Keystore base64 backup (decode to restore .jks file):
```
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
```

To restore the keystore from base64:
```bash
echo "<base64 string above>" | base64 -d > coral-x-release.jks
```

---

## 4. GITHUB SECRETS (already set)

These 4 secrets are configured in github.com/rajatxo/Coral-X → Settings →
Secrets and variables → Actions:

```
CORAL_X_KEYSTORE_BASE64   = <the base64 string from section 3>
CORAL_X_STORE_PASSWORD    = The Weeknd 199
CORAL_X_KEY_ALIAS         = coralx
CORAL_X_KEY_PASSWORD      = The Weeknd 199
```

---

## 5. BUILD ENVIRONMENT

```
JDK:          /home/z/jdk-17.0.20.1+1 (Temurin 17)
Android SDK:  /home/z/android-sdk (platforms;android-35, build-tools;35.0.0)
Gradle:       /home/z/my-project/gradle-9.6.1/bin/gradle
JAVA_HOME:    /home/z/jdk-17.0.20.1+1
```

### Build commands
```bash
cd /home/z/my-project/coral-x
export JAVA_HOME=/home/z/jdk-17.0.20.1+1
export PATH=$JAVA_HOME/bin:$PATH

# Compile only
/home/z/my-project/gradle-9.6.1/bin/gradle :app:compileDebugKotlin

# Build debug APK
/home/z/my-project/gradle-9.6.1/bin/gradle :app:assembleDebug

# Build release APK (needs keystore.properties)
/home/z/my-project/gradle-9.6.1/bin/gradle :app:assembleRelease
```

### keystore.properties (gitignored, NOT in repo)
```properties
storeFile=coral-x-release.jks
storePassword=The Weeknd 199
keyAlias=coralx
keyPassword=The Weeknd 199
```

### GitHub Actions
- Workflow: `.github/workflows/build.yml`
- Triggers on: push to main, PR to main, manual dispatch
- Builds: debug APK (always) + release APK (if secrets configured)
- Artifacts: `coral-x-debug-apk` + `coral-x-release-apk` (30-day retention)

---

## 6. THE 10 UNIVERSAL LAYOUT LAWS

These are the patterns that 6+ music player repos all use. Coral X MUST
follow these to work on every phone. They are already baked into the
current foundation.

### Law 1: ONE CompositionLocal for ALL insets
```kotlin
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> {
    error("No WindowInsets provided")
}
// Computed ONCE at root, consumed EVERYWHERE
// Includes: system bars + nav bar + mini player height + IME
```

### Law 2: Manual edge-to-edge (NOT enableEdgeToEdge)
```kotlin
WindowCompat.setDecorFitsSystemWindows(window, false)
// NOT: enableEdgeToEdge() — that applies its own bar colors we can't control
```

### Law 3: Theme XML with enforceNavigationBarContrast=false
```xml
<style name="Theme.Coral" parent="android:Theme.Material.Light.NoActionBar">
    <item name="android:windowDrawsSystemBarBackgrounds">true</item>
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
    <item name="android:enforceStatusBarContrast" tools:targetApi="29">false</item>
    <item name="android:enforceNavigationBarContrast" tools:targetApi="29">false</item>
    <item name="android:windowContentOverlay">@null</item>
    <item name="android:windowMinWidthMinor">100%</item>
    <item name="android:windowMinWidthMajor">100%</item>
    <item name="android:windowLayoutInDisplayCutoutMode">shortEdges</item>
</style>
```

### Law 4: Custom BottomSheet (NOT Material3 ModalBottomSheet)
- Position via `graphicsLayer.translationY` (draw phase, zero recomposition)
- `collapsedBound = bottomInset + navBarHeight + miniPlayerHeight`
- `expandedBound = maxHeight` (from BoxWithConstraints)

### Law 5: preUpPostDownNestedScrollConnection
- Lets inner LazyColumn scroll normally
- Hands gesture to sheet when list reaches top
- Pattern: onPreScroll + onPostScroll + onPreFling + onPostFling

### Law 6: System gesture exclusion zone
- Query `WindowInsetsCompat.Type.systemGestures()` insets
- Or: `maxOf(20.dp, navBarInset + 8.dp)` exclusion at bottom edge
- Bail out of gesture handler if touch starts in exclusion zone

### Law 7: BoxWithConstraints at root (NEVER screenHeightDp)
```kotlin
BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val containerHeight = this.maxHeight  // actual measured height
    // Works in multi-window, split-screen, floating windows, foldables
}
```

### Law 8: Manifest flags
```xml
android:enableOnBackInvokedCallback="true"
android:windowSoftInputMode="adjustResize"
android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|keyboard|smallestScreenSize|uiMode|locale|layoutDirection|fontScale|density"
android:launchMode="singleTask"
```

### Law 9: Status bar icon appearance flips per-screen-state
```kotlin
WindowCompat.getInsetsController(window, decorView).apply {
    isAppearanceLightStatusBars = !isDark
    isAppearanceLightNavigationBars = !isDark
}
```

### Law 10: BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE for immersive mode
```kotlin
controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
// The ONLY mode that doesn't steal gestures
```

---

## 7. REPOS ANALYZED (the 10-repo study)

These repos were cloned and deeply analyzed for universal layout patterns:

| Repo | Path | Key takeaway |
|------|------|--------------|
| ViTune | /home/z/my-project/ViTune | LocalPlayerAwareWindowInsets + custom BottomSheet |
| Metrolist | /home/z/my-project/Metrolist | Surgical WindowInsetsSides + dynamic density scaling |
| ArchiveTune | /home/z/my-project/ArchiveTune | BottomSheetGestureRegion (system gesture exclusion) |
| SimpMusic | /home/z/my-project/SimpMusic | ModalBottomSheet with contentWindowInsets=0 + manual insets |
| vivi-music | /tmp/repos/vivi-music | graphicsLayer for position + bottomInset filler Box |
| PixelPlay | /tmp/repos/PixelPlay | Sanitize insets (clamp for floating windows) + unit tests |
| harmber | /home/z/my-project/harmber | Same as ArchiveTune (fork of ViMusic family) |
| looper | /home/z/my-project/looper | Flutter app — pure-function geometry classes + touch resampling |
| SpatialFlow | /home/z/my-project/SpatialFlow | Modifier.layout for zero-recomposition animated dims |
| Convx | /home/z/my-project/Convx | appTopBarWindowInsets() helper + rememberScreenCornerRadius() |

---

## 8. ARCHITECTURE BLUEPRINT (what to build)

### Foundation (DONE — already in place)
1. ✅ Theme XML (Law 3)
2. ✅ Manifest flags (Law 8)
3. ✅ MainActivity with edge-to-edge (Law 2)
4. ✅ setSystemBarAppearance() (Law 9)

### Next to build (in order)
5. `LocalPlayerAwareWindowInsets` CompositionLocal (Law 1)
6. `Dimensions.kt` constants file (navBarHeight, miniPlayerHeight, etc.)
7. Custom `BottomSheet.kt` (Law 4) + `preUpPostDownNestedScrollConnection` (Law 5)
8. `BottomSheetGestureRegion` (Law 6) — system gesture exclusion
9. `BoxWithConstraints` root layout (Law 7)
10. `CoralScaffold` — the root composable that provides everything

### Then features (in order)
11. Media3 playback service (foregroundServiceType=mediaPlayback)
12. Song scanner (MediaStore.Audio.Media query)
13. Home screen (LazyColumn with LocalPlayerAwareWindowInsets contentPadding)
14. Mini player (collapsed state of BottomSheet)
15. Full player (expanded state of BottomSheet)
16. Search screen
17. Playlists
18. Settings
19. Lyrics
20. Cover art management

---

## 9. PLAY STORE COMPLIANCE

- ❌ NO `MANAGE_EXTERNAL_STORAGE` — Play Store rejects music apps with this
- ✅ Use SAF (Storage Access Framework) for metadata embedding if needed
- ✅ `READ_MEDIA_AUDIO` (Android 13+) / `READ_EXTERNAL_STORAGE` (≤12)
- ✅ `POST_NOTIFICATIONS` (Android 13+) — for playback notification
- ✅ `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- ✅ `WAKE_LOCK` — for background playback

---

## 10. BEYOND LAYOUT — OTHER THINGS TO HANDLE

### Audio robustness
- Audio focus (duck/pause when other apps need audio)
- ACTION_AUDIO_BECOMING_NOISY (pause on headphone unplug)
- Bluetooth disconnect handling
- Media button handling via MediaSession
- Phone call pause
- Corrupt file handling (Player.Listener.onPlayerError)

### Performance
- Stable LazyColumn keys (key = { it.id })
- Bitmap memory management (Coil3 size + cache)
- Cold start < 1.5s
- No main thread I/O

### Memory
- ExoPlayer released in service onDestroy
- CoroutineScope cancelled properly
- DisposableEffect for cleanup

### Battery
- WAKE_LOCK only when playing
- Batch MediaScanner calls
- Foreground service stops when playback ends

### Accessibility
- contentDescription on every Icon
- 48dp minimum touch targets
- sp for all text (never dp)
- WCAG AA color contrast (4.5:1)

### Internationalization
- All strings in strings.xml
- RTL support (start/end padding, not left/right)
- Plurals for "1 song" vs "5 songs"

---

## 11. OLD CORAL REPO (reference, not to be copied directly)

```
Repo:     https://github.com/rajatxo/Coral.git
Local:    /home/z/coral-push (if still exists)
```

The old Coral repo has working features that CAN be ported to Coral X
(but should be rewritten to follow the universal patterns):
- Spiral2Player (the main player UI)
- PlaylistDetailScreen, PlaylistsScreen (wheel picker)
- SearchScreen
- SettingsScreen (with dev info card + Telegram links)
- SongCoverManager (custom covers)
- MetadataEmbedder (TagLib-based)
- CrossfadeVisualState + SimpleCrossfadeController
- CoralIcons (custom Lucide-style icon set)
- TextColorStyle (22 color extraction algorithms)
- PermissionScreen (glass cards + iOS toggles)

### Features the user specifically wants (from old Coral):
- Playlist wheel (Canvas-based rotary picker)
- Kyant glass morphism (io.github.kyant0:backdrop:1.0.0)
- Spiral2 player design (the main player the user likes)
- Custom album cover art (app-only + metadata embedding)
- Today's Top section
- Speed dial cards
- Draggable search FAB + nav bar
- Sleep timer
- Equalizer
- Lyrics (embedded + online)
- Crossfade playback
- Dev info card with Telegram links + "Made in India" badge

---

## 12. CI/CD SETUP

### GitHub Actions workflow
File: `.github/workflows/build.yml`

```yaml
name: Build Coral-X APK
on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    env:
      CORAL_X_KEYSTORE_BASE64: ${{ secrets.CORAL_X_KEYSTORE_BASE64 }}
      CORAL_X_STORE_PASSWORD: ${{ secrets.CORAL_X_STORE_PASSWORD }}
      CORAL_X_KEY_ALIAS: ${{ secrets.CORAL_X_KEY_ALIAS }}
      CORAL_X_KEY_PASSWORD: ${{ secrets.CORAL_X_KEY_PASSWORD }}

    steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    - uses: android-actions/setup-android@v3
      with:
        packages: 'platform-tools platforms;android-35 build-tools;35.0.0'
    - uses: gradle/actions/setup-gradle@v3

    - name: Decode signing keystore
      run: |
        if [ -n "$CORAL_X_KEYSTORE_BASE64" ]; then
          echo "$CORAL_X_KEYSTORE_BASE64" | base64 -d > coral-x-release.jks
          cat > keystore.properties << 'PROPS_EOF'
        storeFile=coral-x-release.jks
        storePassword=PLACEHOLDER_STORE_PASS
        keyAlias=PLACEHOLDER_KEY_ALIAS
        keyPassword=PLACEHOLDER_KEY_PASS
        PROPS_EOF
          sed -i "s|PLACEHOLDER_STORE_PASS|$CORAL_X_STORE_PASSWORD|g" keystore.properties
          sed -i "s|PLACEHOLDER_KEY_ALIAS|$CORAL_X_KEY_ALIAS|g" keystore.properties
          sed -i "s|PLACEHOLDER_KEY_PASS|$CORAL_X_KEY_PASSWORD|g" keystore.properties
        fi

    - run: chmod +x gradlew
    - run: ./gradlew :app:assembleDebug --no-daemon --max-workers=2
    - if: env.CORAL_X_KEYSTORE_BASE64 != ''
      run: ./gradlew :app:assembleRelease --no-daemon --max-workers=2

    - uses: actions/upload-artifact@v4
      with:
        name: coral-x-debug-apk
        path: app/build/outputs/apk/debug/*.apk
        retention-days: 30
    - if: env.CORAL_X_KEYSTORE_BASE64 != ''
      uses: actions/upload-artifact@v4
      with:
        name: coral-x-release-apk
        path: app/build/outputs/apk/release/*.apk
        retention-days: 30
```

---

## 13. HOW TO CONTINUE WORK

When resuming work on Coral X:

1. Clone the repo:
   ```bash
   git clone https://rajatxo:[YOUR_GITHUB_TOKEN]@github.com/rajatxo/Coral-X.git coral-x
   ```

2. Restore build environment:
   ```bash
   export JAVA_HOME=/home/z/jdk-17.0.20.1+1
   export PATH=$JAVA_HOME/bin:$PATH
   echo "sdk.dir=/home/z/android-sdk" > coral-x/local.properties
   ```

3. Restore keystore (if needed):
   ```bash
   echo "<base64 from section 3>" | base64 -d > coral-x/coral-x-release.jks
   cat > coral-x/keystore.properties << 'EOF'
   storeFile=coral-x-release.jks
   storePassword=The Weeknd 199
   keyAlias=coralx
   keyPassword=The Weeknd 199
   EOF
   ```

4. Build:
   ```bash
   cd coral-x
   /home/z/my-project/gradle-9.6.1/bin/gradle :app:assembleDebug
   ```

5. Push:
   ```bash
   git add -A && git commit -m "feat: <description>" && git push origin main
   ```

---

## 14. USER PREFERENCES & CONTEXT

- The user has ZERO coding knowledge — everything must be done by the AI
- The user is preparing for SSC CHSL exam — work may pause/resume
- The user wants the app to be "absolutely perfect like all other famous apps"
- The user wants it on Play Store eventually
- The user is sharing the app on Telegram for now
- The old Coral app is at github.com/rajatxo/Coral (reference only)
- The user's GitHub token: [YOUR_GITHUB_TOKEN]

### Communication style
- Be direct, no fluff
- Explain what was done in simple terms
- Push to GitHub after each change
- Provide APK download links
- Don't ask questions unless absolutely necessary — make decisions and go

### Design preferences (from old Coral)
- Dark theme (pure black for AMOLED)
- Glass morphism (kyant backdrop library)
- Cal Sans font for headings
- Lucide-style icons (custom drawn, no Material Icons)
- Coral red accent (#FF6B6B)
- "Made with love in India" badge
- Telegram links in settings

---

## END OF DOCUMENT

This document is the single source of truth for Coral X.
Show it to the AI to resume work at any time.
