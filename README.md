# PrismCut — Lite Android Video Editor

A lightweight, hardware-accelerated CapCut-style Android video editor built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Jetpack Media3 (`Transformer`, `CompositionPlayer`, `Effect` & OpenGL ES shaders)**.

## Features
- **Multi-Clip Timeline**: Import multiple videos via the zero-permission Android Photo Picker, reorder, duplicate, or merge clips sequentially.
- **Trim & Split**: Precision range-slider trimming and frame-accurate split at the playhead.
- **Transitions**: Cut, Fade Black, Flash White, Zoom Push, and Slide In between clips.
- **10 Visual Filters & Color Grading**: Original, B&W Noir, Sepia, Vintage, Teal & Gold, Vivid Pop, Golden Hour, Arctic Cool, Matte Fade, Neon Cyber + Brightness, Contrast, and Saturation sliders.
- **GPU Special Effects (FX)**: Soft Blur (9-tap GL blur), RGB Glitch (animated chromatic split + scanline tear), Vignette, Retro Pixelate, and Beat Zoom.
- **Speed, Audio & Text**: Speed presets (`0.5x`–`3.0x`), mute/unmute, 90° rotation, aspect ratio presets (`9:16`, `16:9`, `1:1`, `4:5`), and text caption overlays.
- **High-Resolution Export**: Hardware-encoded MP4 export in **720p**, **1080p Full HD**, **4K Ultra HD**, and **8K Master** (H.264 / H.265 HEVC) saved directly to `Movies/PrismCut`.

## Project Structure
```text
app/src/main/java/com/example/
├── MainActivity.kt                  # Edge-to-edge host & Photo Picker launchers
├── data/
│   └── ProjectStorage.kt            # Lightweight local JSON project draft persistence
├── engine/
│   ├── CodecCapabilityChecker.kt    # Queries device MediaCodecList for 1080p/4K/8K support
│   ├── CompositionBuilder.kt        # Shared Media3 Composition builder (preview & export)
│   ├── GlFilterShaders.kt           # OpenGL ES 2.0 shaders for filters, blur, glitch, transitions
│   ├── MediaInspector.kt            # Metadata & timeline thumbnail strip extractor
│   └── VideoExportManager.kt        # Media3 Transformer + MediaStore gallery saver
├── model/
│   └── VideoModels.kt               # Clips, Filters, Special FX, Transitions, Export configs
└── ui/
    ├── PrismCutViewModel.kt         # StateFlow timeline, player lifecycle & export state
    ├── components/
    │   ├── EditorToolPanels.kt      # Edit, Filters, Adjust, Effects, Transition, Text panels
    │   └── TimelineTrack.kt         # Scrubbable multi-clip filmstrip & transition diamonds
    ├── screens/
    │   ├── EditorScreen.kt          # Live CompositionPlayer preview + timeline + tool dock
    │   ├── ExportSheet.kt           # 720p/1080p/4K/8K export studio & share/play actions
    │   └── HomeScreen.kt            # Studio hub, hardware encoder badge & saved drafts
    └── theme/                       # Dark Cinema Studio Material 3 theme & custom fonts
```

## Push to GitHub & Install on Your Mobile Device

### 1. Push to GitHub
- **From Google AI Studio**: Click the **GitHub** icon / **Export -> Push to GitHub** in the top bar to create a new repository directly from this workspace.
- **Or via Git CLI**:
  ```bash
  git init
  git add .
  git commit -m "Initial commit: PrismCut Android video editor"
  git branch -M main
  git remote add origin https://github.com/<your-username>/PrismCut.git
  git push -u origin main
  ```

### 2. Automated APK Build on GitHub (Included)
This repository includes `.github/workflows/android-apk.yml`. Every push to `main` automatically builds a downloadable **`PrismCut-debug.apk`** in the GitHub **Actions -> Artifacts** tab so you can download and install it directly on your Android phone without needing Android Studio locally!

### 3. Clone & Build Locally (Android Studio / CLI)
```bash
git clone https://github.com/<your-username>/PrismCut.git
cd PrismCut
```
- **Option A — Android Studio**: Open the cloned folder in Android Studio, connect your Android phone with **USB Debugging** enabled, and click **Run (▶)**.
- **Option B — Command Line (`adb`)**:
  ```bash
  keytool -genkeypair -noprompt -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
  gradle :app:assembleDebug
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```
- **Minimum Requirements**: Android 7.0 (API 24) or higher. Hardware 4K/8K export depends on your phone's SoC encoder capabilities (automatically detected on launch).
