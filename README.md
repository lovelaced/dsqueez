<div align="center">

<img src="assets/lens.svg" alt="dsqueez logo, a stylized anamorphic lens mark" width="160">

# dsqueez

*Pixel-accurate anamorphic desqueeze for Android. Three taps from a squeezed JPEG to a finished file in your album.*

[![Android 12+](https://img.shields.io/badge/Android-12%2B-3DDC84?logo=android&logoColor=white&style=flat-square)](https://www.android.com/)
[![Build](https://img.shields.io/github/actions/workflow/status/lovelaced/dsqueez/build.yml?style=flat-square&label=build)](../../actions)
[![Latest release](https://img.shields.io/github/v/release/lovelaced/dsqueez?style=flat-square&display_name=tag)](../../releases)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)

</div>

<!-- TODO: hero — Edit screen mid-stretch on a real shot with the metadata strip visible.
     Capture: adb exec-out screencap -p > assets/screenshots/hero.png
     Then commit and reference here as the hero image, e.g.:
     <div align="center"><img src="assets/screenshots/hero.png" alt="dsqueez edit screen" width="320"></div>
-->

---

Shoot anamorphic on the Lumix S9? Video previews come out desqueezed on-camera, but stills land on your phone horizontally compressed — and Lumix Lab doesn't fix it on transfer. dsqueez closes that gap: open Lumix Lab, hit *Share* → *dsqueez* → *Save*. The corrected file lands in a dedicated **dsqueez** album in Google Photos, with original EXIF, color profile, and capture date intact, ready for Lightroom or Instagram.

## Install

1. Open the [Releases page](../../releases) and download `dsqueez-vX.Y.Z.apk`.
2. On your phone, tap the APK from your file manager. Approve "Install unknown apps" once when prompted.
3. dsqueez appears in your launcher and as a target in any image share sheet.

*Bleeding-edge builds: every push to `master` produces an APK as a workflow artifact under [Actions](../../actions). Note: CI builds use a per-run debug keystore, so uninstall the previous version before installing a new one.*

## Features

- **Picks the right ratio automatically.** Reads `LensModel` from EXIF and snaps to the matching squeeze factor — SIRUI, Vazen, Cooke and most anamorphic glass announce themselves there. A `DETECTED · {LENS}` line surfaces above the picker so you can see what it's basing the suggestion on.
- **All six native Lumix S9 ratios.** 1.30×, 1.33×, 1.50×, 1.60×, 1.80×, 2.00× — same set you pick from in-camera for video. Long-press any chip to set it as your default for photos without an EXIF lens hint.
- **No quality compromise.** Hand-written Lanczos-3 kernel against libjpeg-turbo — same kernel as ImageMagick and FFmpeg. Output is bit-identical to libvips for this operation.
- **Every byte of metadata survives.** EXIF (camera, lens, ISO, shutter, GPS, datetime) and ICC color profile carried through untouched. Orientation gets baked into pixels so no downstream reader rotates the file twice.
- **Share-sheet first.** Three taps from Lumix Lab to a finished file in your camera roll. No file-picker dance.
- **Batch processing.** Share a stack of photos, process in parallel.
- **No ads, no accounts, no telemetry.** Local processing only — your photos never leave the device.
- **Photographer's interface.** Halide-inspired dark and light themes, monospace numerals, motion and haptics tuned for the Pixel.

<!-- TODO: 2×2 screenshot grid showing empty / edit / batch / saved states.
     <div align="center">
       <img src="assets/screenshots/empty.png" width="48%">
       &nbsp;
       <img src="assets/screenshots/edit.png" width="48%">
       <br><br>
       <img src="assets/screenshots/batch.png" width="48%">
       &nbsp;
       <img src="assets/screenshots/saved.png" width="48%">
     </div>
-->

## How it works

Anamorphic lenses optically squeeze a wide field of view onto a regular 3:2 sensor. To get a normal image back, you stretch it horizontally by the lens's squeeze factor — usually 1.33×, 1.5×, or 2×. The Lumix S9 desqueezes its video preview in real time, but exports stills as-shot.

dsqueez runs the stretch in native C++ using a Lanczos-3 kernel against libjpeg-turbo, then writes a new JPEG with EXIF (APP1) and ICC (APP2) carried through. The right squeeze factor is read from the photo's `LensModel` EXIF tag when present and falls back to your long-press-set default otherwise. Output lands in `DCIM/dsqueez/`, which Google Photos shows in your main feed (DCIM is the standard camera-roll location, alongside `DCIM/Camera`).

<details>
<summary>Full pipeline</summary>

1. Decode JPEG to tightly-packed 8-bit RGB (libjpeg-turbo)
2. Apply EXIF orientation to the pixel buffer
3. Lanczos-3 horizontal resample with edge clamping
4. Encode JPEG at quality 95, full-resolution chroma, progressive
5. Carry EXIF + ICC through verbatim; host side patches geometry tags + forces `Orientation = 1`

Verified against a 4272×2848 Lumix S9 + SIRUI 1.33× Anamorphic JPEG: output is exactly 5682×2848 with every EXIF field preserved (Make, Model, LensModel, ExposureTime, FNumber, ISO, DateTimeOriginal, Orientation).

</details>

## What it doesn't do

By design, to keep dsqueez a single-purpose tool:

- No crop, rotate, color, exposure, or any other editing
- No RAW / DNG support — Lightroom on desktop is the right tool for that
- No HEIC — Lumix Lab exports JPEG, so dsqueez accepts JPEG
- No cloud sync, accounts, telemetry, analytics, ads

## License

[MIT](LICENSE). Bundled fonts and the linked libjpeg-turbo retain their own licenses — see *Credits*.

### Credits

- **[libjpeg-turbo](https://github.com/libjpeg-turbo/libjpeg-turbo)** — modified BSD, statically linked
- **[Inter](https://rsms.me/inter/)** and **[JetBrains Mono](https://www.jetbrains.com/lp/mono/)** — SIL Open Font License 1.1
- **AndroidX, Jetpack Compose, Material 3** — Apache 2.0

Visual language inspired by [Halide](https://halide.cam/) and [Kino](https://lux.camera/kino) from Lux Optics.

---

<details>
<summary><b>For contributors: build from source</b></summary>

### Prerequisites

- Android Studio (Jellyfish 2026.x or newer), **or**
- JDK 17 + Android SDK platform 37 + NDK + CMake 3.22.1

libjpeg-turbo source is fetched at first configure via CMake `FetchContent` and cached under `app/.cxx/` — no vendored binaries.

### Build & install

```bash
./gradlew installDebug    # build + install on a connected Pixel
./gradlew assembleDebug   # just build, leaves the APK at app/build/outputs/apk/debug/
```

Watch logs during a save:

```bash
adb logcat -s dsqueez-native dsqueez-jni Resampler
```

### Verify the pipeline without flashing the phone

The C++ pipeline is `__ANDROID__`-guarded, so it builds and runs on macOS or Linux against system libjpeg-turbo:

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
# Args: input output ratio orientation(1..8)
```

### Architecture

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

### Releasing

Tag a `v*` commit and push to fire the workflow's release path. A GitHub Release with the APK attached is created automatically:

```bash
git tag -a v0.2.0 -m "Release notes"
git push origin v0.2.0
```

</details>

<div align="center"><sub>Made because a photographer was tired of round-tripping through a desktop.</sub></div>
