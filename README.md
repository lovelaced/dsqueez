# dsqueez

A single-purpose Android tool that does one thing: horizontally desqueezes 1.33× anamorphic still photos. Built for the Lumix S9 + anamorphic adapter → Lumix Lab → phone workflow, where stills arrive squeezed and there's no native fix on the camera or in Lab.

- Share-sheet target: from Lumix Lab (or any gallery), Share → dsqueez → tap Save.
- Pure desqueeze: pixel-accurate horizontal Lanczos-3 stretch via libvips. EXIF, ICC profile, capture date — all preserved.
- Saves to a "dsqueez" album in Google Photos (`Pictures/dsqueez/`).
- Light/dark following system. Halide-inspired aesthetic.
- Built for Android 16 / Pixel 6 Pro (`minSdk 31`, `arm64-v8a` only).

## Prerequisites

Before you can build:

1. **Android Studio** (Jellyfish 2026.x or newer). Bundles the JDK, Android SDK, and the gradle wrapper bootstraps the rest.
2. **NDK and CMake** — install from Studio's *SDK Manager → SDK Tools*:
   - NDK (Side by side), latest stable (~28.0+)
   - CMake 3.22.1 (Studio's default is fine)
3. **Pixel 6 Pro** with USB debugging on, or any Android 12+ arm64 device.

That's the whole baseline. Open the project root in Studio → it will sync gradle, fetch dependencies, prompt to install any missing SDK components.

## Native processing (libvips)

The pixel work runs in native code via libvips with a Lanczos-3 resampler, called through a small JNI bridge under `app/src/main/cpp/`. The prebuilt `.so`s are **not committed** to this repo — you need to vendor them once before the first build that touches native code:

```
app/src/main/jniLibs/arm64-v8a/
├── libvips.so
├── libgobject-2.0.so
├── libglib-2.0.so
├── libgio-2.0.so
├── libheif.so
├── libjpeg.so          # mozjpeg or libjpeg-turbo
├── libexpat.so
└── libz.so             # may already be on-device; vendor for self-contained
```

You also need libvips' public headers under `app/src/main/cpp/vips_headers/` so CMake can compile against them.

### Where to get the prebuilts

Two paths:

**(A) Vendored prebuilt (fastest).** Use one of the community libvips Android build projects (search GitHub for "libvips android" — the healthiest forks track current libvips releases). Drop the `arm64-v8a/*.so` files plus the `include/` tree into the locations above.

**(B) Build from source (slower, reproducible).** A `tools/build-libvips.sh` script will be added that runs an NDK Docker container, configures libvips against a static glib + libheif + mozjpeg, and outputs the arm64-v8a artifacts. Until that script exists, follow the upstream libvips build instructions and pass `--host=aarch64-linux-android` with NDK toolchain vars.

The app gracefully degrades if the prebuilts aren't present: the UI loads, you can pick photos and see the desqueeze preview, but tapping Save reports "Processing engine not installed." This lets you iterate on the UI before the native pipeline is in place.

## Building

### In CI (easiest — no local toolchain needed)

A GitHub Action at `.github/workflows/build.yml` builds a debug APK on every push and PR. To grab a build:

1. Push to GitHub → wait for the **Build APK** workflow to finish.
2. Open the run from the *Actions* tab → scroll to *Artifacts* → download `dsqueez-<sha>.apk`.
3. On your phone, open the APK from your file manager (Drive, Chrome). Approve "Install unknown apps" if Android asks.
4. **When updating: uninstall the previous version first.** Debug-signed APKs use a per-CI keystore so signatures don't match between runs.

To cut a versioned release with a stable downloadable URL, push a tag:

```bash
git tag v0.1.0 && git push --tags
```

That triggers the same workflow, which additionally creates a GitHub Release with the APK attached.

### Locally

If you'd rather build on your machine:

```bash
gradle wrapper --gradle-version 9.0   # one-time, only if you didn't open the project in Studio
./gradlew assembleDebug               # build debug APK
./gradlew installDebug                # install on a connected device
```

Opening in Android Studio does the wrapper bootstrap automatically on first sync.

To watch logs during a save:
```bash
adb logcat -s dsqueez-native dsqueez-jni Vips
```

## Verifying quality (the whole point)

After installing and running a test save:

1. `adb pull /sdcard/Pictures/dsqueez/IMG_xxx_dsq.jpg`
2. Open the file in Lightroom or `exiftool -a IMG_xxx_dsq.jpg`. Confirm:
   - **Dimensions** = source width × 1.33 (rounded) × source height
   - **Camera / Lens / ISO / Shutter / GPS / DateTimeOriginal** — all intact, copied from source
   - **ICC Profile** = Display P3 (if the source was P3, which Lumix S9 typically is)
   - **Orientation** = 1 (rotation baked into pixels, not relied on by the EXIF tag)
3. At 100% zoom in Lightroom, compare against Lightroom's own "Enhance → Super Resolution" upscale on the unmodified squeezed source. Lanczos-3 should match or beat it on visible detail. No visible ringing on hard edges (sky/buildings).

## Architecture

```
app/
├── src/main/kotlin/app/dsqueez/
│   ├── DsqueezApp.kt          — Application; loads libdsqueez.so
│   ├── MainActivity.kt        — single activity, intent routing
│   ├── ui/
│   │   ├── theme/             — Halide-inspired design system
│   │   ├── components/        — PhotoFrame, MetadataStrip, RatioControl, …
│   │   ├── screens/           — Empty, Edit, Batch, OptionsSheet
│   │   ├── anim/              — DesqueezeStretch
│   │   └── DsqueezApp.kt      — screen router based on URI count
│   ├── photo/
│   │   ├── PhotoSource.kt     — URI → preview bitmap + metadata
│   │   ├── PhotoMetadata.kt   — value type
│   │   ├── Pipeline.kt        — orchestration (decode → desqueeze → save)
│   │   ├── MediaStoreSaver.kt — IS_PENDING flow, DATE_TAKEN
│   │   └── ExifBridge        — folded into MediaStoreSaver
│   ├── nativebridge/Vips.kt   — JNI surface
│   ├── settings/UserPrefs.kt  — DataStore (export_as_jpeg, last_ratio)
│   └── share/ShareIntent.kt   — parse SEND / SEND_MULTIPLE / VIEW
└── src/main/cpp/
    ├── CMakeLists.txt
    ├── dsqueez.{h,cpp}        — libvips pipeline
    └── jni_bridge.cpp
```

The full implementation plan, including phase-by-phase verification, lives at
`~/.claude/plans/nested-drifting-narwhal.md`.

## What this app does NOT do

By design — to keep the experience a true single-purpose tool:

- No crop, rotate, color, exposure, or any other editing
- No multiple anamorphic ratios in the UI (the model supports them, but the picker shows 1.33× only — change `SUPPORTED_RATIOS` in `RatioControl.kt` if you want to add more)
- No RAW / DNG support
- No before/after toggle in preview — we trust your eye for the desqueezed result
- No cloud sync, accounts, telemetry, analytics, ads

## Attribution & licenses

- **libvips** — LGPL 2.1 (dynamic linking)
- **libheif** — LGPL 3.0
- **mozjpeg / libjpeg-turbo** — BSD-derived
- **glib** — LGPL 2.1
- **Inter** — SIL Open Font License 1.1
- **JetBrains Mono** — SIL Open Font License 1.1
- **AndroidX / Jetpack Compose / Material 3** — Apache 2.0

A consolidated `LICENSES.txt` will be shipped in the APK. Source links live in the gradle dependency tree and the CMakeLists.

## Status

This is a green-field project; the scaffold is in place, the UI is wired end-to-end, and the native bridge compiles cleanly. The remaining piece is vendoring (or building) the libvips arm64-v8a artifacts. Once those are in place the pipeline is live.
