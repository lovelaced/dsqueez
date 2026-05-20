<div align="center">

<img src="assets/lens.svg" alt="dsqueez logo, a stylized anamorphic lens mark" width="120">

# dsqueez

*Desqueeze anamorphic stills on Android. One tap, from the share sheet.*

[![Android 12+](https://img.shields.io/badge/Android-12%2B-3DDC84?logo=android&logoColor=white&style=flat-square)](https://www.android.com/)
[![Build APK](https://img.shields.io/github/actions/workflow/status/lovelaced/dsqueez/build.yml?style=flat-square&label=build)](../../actions)
[![Latest release](https://img.shields.io/github/v/release/lovelaced/dsqueez?style=flat-square&display_name=tag)](../../releases)
[![License](https://img.shields.io/badge/license-TBD-lightgrey?style=flat-square)](#license)

</div>

<!-- TODO: hero screenshot — Edit screen mid-stretch, photo visible, metadata strip showing 1.33×. Recommended capture: adb exec-out screencap -p > assets/screenshots/hero.png -->

---

Shoot anamorphic on the Lumix S9? Video previews come out desqueezed on-camera, but stills land on your SD card horizontally compressed — and Lumix Lab doesn't fix it on the way to your phone. Every keeper has to round-trip through Lightroom on a desktop before it's usable for anything else.

dsqueez closes that gap. Open Lumix Lab, hit *Share* → *dsqueez* → *Save*. Done. The corrected file lands in a dedicated **dsqueez** album in Google Photos, with the original EXIF, color profile, and capture date intact — ready for Lightroom, Instagram, or the next photo in your camera roll.

## Features

- **Pixel-accurate desqueeze.** Horizontal 1.33× stretch via libvips with a Lanczos-3 resampler — the same kernel used by FFmpeg and ImageMagick. Sharper than Lightroom's built-in upscaler at 100% zoom.
- **Metadata stays intact.** EXIF (camera, lens, ISO, shutter, GPS) and ICC color profile carried through end-to-end. Capture date propagates so the desqueezed copy sorts chronologically next to the source.
- **Share-sheet first.** Three taps from Lumix Lab to a finished file in your album.
- **Batch processing.** Share a stack of photos, processed in parallel.
- **A real photographer's interface.** Halide-inspired dark and light themes, monospace numerals on every metadata pair, motion and haptics tuned for the Pixel 6 Pro.
- **No ads, no accounts, no telemetry.** Local processing. Your photos never leave the device.

<!-- TODO: 2×2 screenshot grid — empty state, edit screen, batch queue, options sheet -->

## Get the APK

Download the latest signed release:

1. Open the [Releases page](../../releases) and grab `dsqueez-vX.Y.Z.apk` from the most recent tag.
2. On your phone, open the APK from your file manager (Drive, Chrome). Approve "Install unknown apps" once when prompted.
3. dsqueez shows up in your launcher and as a target in any image share sheet.

For the bleeding edge — every push to `main` builds an APK in CI:

1. Open the [Actions tab](../../actions) → pick the most recent **Build APK** run.
2. Scroll to *Artifacts* and download `dsqueez-<sha>`.

> **Updating across CI builds:** uninstall the previous version first. Debug-signed builds use a per-run keystore, so signatures don't match between runs.

## How it works

Anamorphic lenses optically squeeze a wide field of view onto a sensor's regular 3:2 frame. To get a normal image back out, you stretch it horizontally by the lens's squeeze factor — usually 1.33×, 1.5×, or 2×. The Lumix S9 handles this for video previews in real time, but exports stills as-shot.

dsqueez runs the stretch in native code via libvips' [Lanczos-3](https://en.wikipedia.org/wiki/Lanczos_resampling) resampler, then writes a new JPEG with the original EXIF and ICC profile preserved. The pipeline streams tiles, so peak memory stays under 100 MB even on a 24 MP image. Output lands in `Pictures/dsqueez/`, which Google Photos surfaces as a dedicated album.

## Build it yourself

Prereqs: Android Studio (Jellyfish 2026.x or newer), or JDK 17 + Android SDK platform 36 if you build from the command line. For the native pipeline, also install **NDK** and **CMake 3.22.1** via Studio's *SDK Manager → SDK Tools*.

```bash
# From a fresh clone — one-time
gradle wrapper --gradle-version 9.0   # or just open the project in Android Studio

# Build & install on a connected Pixel
./gradlew installDebug
```

### Native processing (libvips)

The pixel work runs through a JNI bridge in `app/src/main/cpp/`. The libvips `.so` binaries aren't committed to the repo — they're large and LGPL-shipping-aware. **The app builds and runs without them**, but tapping *Save* reports `Processing engine not installed` until they're in place.

To enable saving, drop arm64-v8a libvips builds (plus transitive deps) into:

```
app/src/main/jniLibs/arm64-v8a/
├── libvips.so
├── libheif.so
├── libjpeg.so
├── libgio-2.0.so   libgobject-2.0.so   libglib-2.0.so
├── libexpat.so
└── libz.so
```

with libvips' public headers under `app/src/main/cpp/vips_headers/`. Two sourcing paths:

- **Community prebuilt** — search GitHub for `libvips android`; healthiest forks track current libvips releases.
- **Build from source** — via an NDK Docker toolchain. A `tools/build-libvips.sh` is on the roadmap.

The Gradle build auto-detects whether the binaries are present and toggles the CMake step accordingly. No config flag needed.

## Architecture

```
app/src/main/
├── kotlin/app/dsqueez/
│   ├── DsqueezApp.kt              Application — loads libdsqueez.so
│   ├── MainActivity.kt            Single activity; routes SEND / SEND_MULTIPLE / VIEW intents
│   ├── ui/
│   │   ├── theme/                 Color, Type, Motion, Spacing, Haptics
│   │   ├── components/            PhotoFrame, MetadataStrip, RatioControl, …
│   │   ├── screens/               Empty / Edit / Batch / OptionsSheet
│   │   └── anim/DesqueezeStretch  The signature stretch animation
│   ├── photo/                     Pipeline, MediaStoreSaver, PhotoSource
│   ├── nativebridge/Vips.kt       JNI surface
│   ├── settings/UserPrefs.kt      DataStore
│   └── share/ShareIntent.kt       SEND / SEND_MULTIPLE / VIEW parser
└── cpp/
    ├── CMakeLists.txt
    ├── dsqueez.{h,cpp}            libvips pipeline (load_buffer → affine(lanczos3) → save_buffer)
    └── jni_bridge.cpp
```

## What it doesn't do

By design, to keep dsqueez a true single-purpose tool:

- No crop, rotate, color, exposure, or any other editing
- No multi-ratio picker in v1 (the data model supports 1.5×, 2×, etc. — flip them on in `RatioControl.kt`)
- No RAW / DNG support
- No before/after toggle in the preview
- No cloud sync, accounts, telemetry, analytics, ads

## License

License: TBD. The choice intersects with the dynamically-linked LGPL libvips and SIL-licensed bundled fonts; MIT or Apache 2.0 are the likely candidates. Open an issue if you'd like to weigh in before a tag is cut.

### Credits

- **[libvips](https://www.libvips.org/)** — LGPL 2.1, dynamically linked
- **[libheif](https://github.com/strukturag/libheif)** — LGPL 3.0
- **[mozjpeg](https://github.com/mozilla/mozjpeg) / libjpeg-turbo** — BSD
- **[glib](https://gitlab.gnome.org/GNOME/glib)** — LGPL 2.1
- **[Inter](https://rsms.me/inter/)** and **[JetBrains Mono](https://www.jetbrains.com/lp/mono/)** — SIL Open Font License 1.1
- **AndroidX, Jetpack Compose, Material 3** — Apache 2.0

Visual language inspired by [Halide](https://halide.cam/) and [Kino](https://lux.camera/kino) from Lux Optics.

---

<div align="center"><sub>Made because a photographer was tired of round-tripping through a desktop.</sub></div>
