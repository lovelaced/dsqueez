<div align="center">

<img src="assets/lens.svg" alt="dsqueez logo, a stylized anamorphic lens mark" width="120">

# dsqueez

*Desqueeze anamorphic stills on Android. One tap, from the share sheet.*

[![Android 12+](https://img.shields.io/badge/Android-12%2B-3DDC84?logo=android&logoColor=white&style=flat-square)](https://www.android.com/)
[![Build APK](https://img.shields.io/github/actions/workflow/status/lovelaced/dsqueez/build.yml?style=flat-square&label=build)](../../actions)
[![Latest release](https://img.shields.io/github/v/release/lovelaced/dsqueez?style=flat-square&display_name=tag)](../../releases)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)

</div>

<!-- TODO: hero screenshot — Edit screen mid-stretch, photo visible, metadata strip showing 1.33×. Recommended capture: adb exec-out screencap -p > assets/screenshots/hero.png -->

---

Shoot anamorphic on the Lumix S9? Video previews come out desqueezed on-camera, but stills land on your SD card horizontally compressed — and Lumix Lab doesn't fix it on the way to your phone. Every keeper has to round-trip through Lightroom on a desktop before it's usable for anything else.

dsqueez closes that gap. Open Lumix Lab, hit *Share* → *dsqueez* → *Save*. Done. The corrected file lands in a dedicated **dsqueez** album in Google Photos, with the original EXIF, color profile, and capture date intact — ready for Lightroom, Instagram, or the next photo in your camera roll.

## Features

- **Pixel-accurate desqueeze.** Custom Lanczos-3 resampler running on libjpeg-turbo. Same kernel as ImageMagick and FFmpeg, written from scratch for this app — no oversized dependency stack, ~1 MB of native code.
- **Metadata stays intact.** EXIF (camera, lens, ISO, shutter, GPS) and ICC color profile carried through end-to-end via libjpeg-turbo's APP-segment helpers. Capture date propagates so the desqueezed copy sorts chronologically next to the source.
- **Orientation-aware.** EXIF orientation is baked into pixels before resampling, then the output is marked Orientation=1 so no downstream reader applies the rotation twice.
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

dsqueez runs the stretch in native C++ (`app/src/main/cpp/`) using a hand-written [Lanczos-3](https://en.wikipedia.org/wiki/Lanczos_resampling) kernel against libjpeg-turbo for the codec. The full pipeline:

1. Decode JPEG → tightly packed 8-bit RGB
2. Apply EXIF orientation to the pixel buffer (so portrait shots come out portrait)
3. Lanczos-3 horizontal resample by the chosen ratio
4. Encode JPEG at quality 95, full-resolution chroma, progressive
5. Carry EXIF (APP1) + ICC profile (APP2) through verbatim, with the host side patching geometry tags and forcing Orientation=1

Output lands in `Pictures/dsqueez/`, which Google Photos surfaces as a dedicated album.

## Build it yourself

Prereqs: Android Studio (Jellyfish 2026.x or newer), or JDK 17 + Android SDK platform 37 + NDK + CMake 3.22.1 if you build from the command line. libjpeg-turbo source is fetched at first configure via CMake `FetchContent` and cached under `app/.cxx/` — no vendored binaries.

```bash
./gradlew installDebug    # build + install on a connected Pixel
./gradlew assembleDebug   # just build, leaves the APK at app/build/outputs/apk/debug/
```

### Host smoke test

The C++ pipeline is `__ANDROID__`-guarded, so it builds and runs on macOS or Linux too — handy for verifying the kernel against a sample JPEG without flashing the phone:

```bash
xcrun clang++ -std=c++17 -O2 \
  -I/opt/homebrew/include -Iapp/src/main/cpp \
  test/dsq_test.cpp \
  app/src/main/cpp/lanczos.cpp \
  app/src/main/cpp/jpeg_pipeline.cpp \
  app/src/main/cpp/dsqueez.cpp \
  -L/opt/homebrew/lib -ljpeg \
  -o /tmp/dsq_test
/tmp/dsq_test input.jpg output.jpg 1.33 1
```

## Architecture

```
app/src/main/
├── kotlin/app/dsqueez/
│   ├── DsqueezApp.kt              Application — loads libdsqueez.so
│   ├── MainActivity.kt            Single activity; routes SEND / SEND_MULTIPLE / VIEW intents
│   ├── ui/
│   │   ├── theme/                 Color, Type, Motion, Spacing, Haptics
│   │   ├── components/            PhotoFrame, MetadataStrip, RatioControl, …
│   │   ├── screens/               Empty / Edit / Batch
│   │   └── anim/DesqueezeStretch  The signature stretch animation
│   ├── photo/                     Pipeline, MediaStoreSaver, PhotoSource
│   ├── nativebridge/Resampler.kt  JNI surface
│   ├── settings/UserPrefs.kt      DataStore
│   └── share/ShareIntent.kt       SEND / SEND_MULTIPLE / VIEW parser
└── cpp/
    ├── CMakeLists.txt             FetchContent libjpeg-turbo, single .so output
    ├── dsqueez.{h,cpp}            Orchestrator: decode → orient → resample → encode
    ├── lanczos.{h,cpp}            Lanczos-3 kernel + horizontal resample
    ├── jpeg_pipeline.{h,cpp}      libjpeg-turbo decode/encode + EXIF/ICC passthrough
    └── jni_bridge.cpp             JNI surface, Android-only
```

## What it doesn't do

By design, to keep dsqueez a true single-purpose tool:

- No crop, rotate, color, exposure, or any other editing
- No multi-ratio picker in v1 (the data model supports 1.5×, 2×, etc. — flip them on in `RatioControl.kt`)
- No RAW / DNG support
- No before/after toggle in the preview
- No cloud sync, accounts, telemetry, analytics, ads

## License

[MIT](LICENSE). Bundled fonts and the linked libjpeg-turbo retain their own licenses — see *Credits*.

### Credits

- **[libjpeg-turbo](https://github.com/libjpeg-turbo/libjpeg-turbo)** — modified BSD, statically linked
- **[Inter](https://rsms.me/inter/)** and **[JetBrains Mono](https://www.jetbrains.com/lp/mono/)** — SIL Open Font License 1.1
- **AndroidX, Jetpack Compose, Material 3** — Apache 2.0

Visual language inspired by [Halide](https://halide.cam/) and [Kino](https://lux.camera/kino) from Lux Optics.

---

<div align="center"><sub>Made because a photographer was tired of round-tripping through a desktop.</sub></div>
