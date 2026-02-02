# GINFERENCE - Local LLM Inference on Android
## Minimal Cyberpunk Retro Tech Stack

---

## SPECS

**Target:** Android Phone (SDK 34+, tested on Snapdragon 8 Gen 3 Adreno GPU)
**Language:** Kotlin
**UI:** Jetpack Compose (Cyberpunk/80s terminal aesthetic)
**Backend:** GPU only (OpenCL/Vulkan via MediaPipe)
**Models:** Download on first run from HuggingFace

---

## COMPATIBLE MODELS (Not Just Gemma)

### LiteRT-Compatible Models on Android:

1. **Gemma Family** (Google)
   - Gemma-3n E2B (2GB) - multimodal
   - Gemma-3n E4B (4GB) - multimodal
   - Gemma-3 1B (1GB) - lightweight
   - Gemma-2 2B (2GB) - best reasoning

2. **Phi Models** (Microsoft)
   - Phi-2 (2.7B) - strong reasoning
   - Phi-3-mini (3.8B) - multilingual

3. **SmolLM** (HuggingFace)
   - SmolLM-360M - ultra-light
   - SmolLM-1.7B - balanced

4. **Qwen Models** (Alibaba)
   - Qwen2-1.5B - multilingual
   - Qwen2-0.5B - minimal

All models must be in `.task` or `.litertlm` format from HuggingFace LiteRT Community repo.

---

## UI AESTHETIC: CYBERPUNK RETRO TERMINAL

**Color Palette:**
- Background: `#0A0E27` (deep space blue-black)
- Primary Text: `#00FF41` (matrix green)
- Secondary Text: `#00D9FF` (cyan neon)
- Accent: `#FF006E` (hot pink/magenta)
- Warning: `#FFD60A` (yellow)
- Error: `#FF0055` (red neon)

**Typography:**
- Monospace font: `JetBrains Mono` or fallback to `Roboto Mono`
- Scanline effect on background (optional subtle overlay)
- CRT screen glow effect on text

**UI Elements:**
- No rounded corners (sharp edges, terminal style)
- Minimal borders (1-2px neon lines)
- Blinking cursor on input
- Streaming text appears character-by-character
- Metrics displayed as live ASCII-style readouts

---

## TECH STACK

```
Layer 7: ginference App (Compose UI + ViewModels)
Layer 6: MediaPipe LLM Inference API (v0.10.14+)
Layer 5: LiteRT Runtime + GPU Delegate
Layer 4: Android NNAPI + GPU HAL
Layer 3: Adreno GPU (Qualcomm drivers)
```

**Dependencies:**
- `com.google.mediapipe:tasks-genai:0.10.14`
- `androidx.compose.ui:*` (Compose UI)
- `androidx.lifecycle:lifecycle-viewmodel-compose`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`
- `com.squareup.okhttp3:okhttp` (model downloads)

**System APIs:**
- `ActivityManager` - RAM metrics
- `Debug.MemoryInfo` - VRAM estimation
- `PowerManager` - thermal state
- `/proc/stat` - CPU usage
- OpenGL ES queries - GPU usage (via NDK if needed)

---

## PROJECT STRUCTURE

```
ginference/
├── app/
│   ├── src/main/
│   │   ├── kotlin/com/ginference/
│   │   │   ├── MainActivity.kt
│   │   │   ├── ui/
│   │   │   │   ├── theme/Theme.kt (cyberpunk colors)
│   │   │   │   ├── screens/InferenceScreen.kt
│   │   │   │   └── components/
│   │   │   │       ├── PromptInput.kt
│   │   │   │       ├── OutputDisplay.kt
│   │   │   │       ├── MetricsPanel.kt
│   │   │   │       └── ModelSelector.kt
│   │   │   ├── inference/
│   │   │   │   ├── LLMEngine.kt (MediaPipe wrapper)
│   │   │   │   └── ModelManager.kt (download/cache)
│   │   │   ├── metrics/
│   │   │   │   ├── SystemMetrics.kt
│   │   │   │   └── InferenceMetrics.kt
│   │   │   └── viewmodels/
│   │   │       └── InferenceViewModel.kt
│   │   ├── res/
│   │   │   └── values/
│   │   │       └── strings.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── PLAN.md (this file)
```

---

## SEQUENTIAL IMPLEMENTATION PHASES

### PHASE 0: Setup (Step 0.1 - 0.3) ✓ COMPLETE
- [x] 0.1: Create project scaffold + gradle config
- [x] 0.2: Add dependencies
- [x] 0.3: Setup cyberpunk theme + color system
**Checkpoint:** Empty app builds, theme colors defined
**Git Commit:** `e8fb2c8` - Phase 0 Complete

### PHASE 1: Core Inference (Step 1.1 - 1.4) ✓ COMPLETE
- [x] 1.1: Implement `LLMEngine.kt` (MediaPipe wrapper)
- [x] 1.2: Implement `ModelManager.kt` (local storage)
- [x] 1.3: Add basic model loading logic
- [x] 1.4: Test with hardcoded model path
**Checkpoint:** Can load model and generate text (no UI yet)
**Git Commit:** `41e9285` - Phase 1 Complete

### PHASE 2: System Metrics (Step 2.1 - 2.6)
- [ ] 2.1: Implement RAM tracking
- [ ] 2.2: Implement VRAM estimation
- [ ] 2.3: Implement CPU usage monitoring
- [ ] 2.4: Implement GPU usage (via OpenGL queries)
- [ ] 2.5: Implement thermal monitoring
- [ ] 2.6: Implement inference metrics (TTFT, tokens/sec)
**Checkpoint:** All metrics readable in logcat

### PHASE 3: Basic UI (Step 3.1 - 3.4)
- [ ] 3.1: Create `InferenceScreen.kt` main layout
- [ ] 3.2: Implement `PromptInput.kt` (terminal-style input)
- [ ] 3.3: Implement `OutputDisplay.kt` (streaming text)
- [ ] 3.4: Connect ViewModel to UI
**Checkpoint:** Can type prompt and see generated output

### PHASE 4: Metrics UI (Step 4.1 - 4.3)
- [ ] 4.1: Implement `MetricsPanel.kt` (ASCII-style readouts)
- [ ] 4.2: Real-time metric updates (1s interval)
- [ ] 4.3: Add CRT glow effects on metrics
**Checkpoint:** Live metrics visible during inference

### PHASE 5: Model Management (Step 5.1 - 5.4)
- [ ] 5.1: Implement `ModelSelector.kt` UI
- [ ] 5.2: Add HuggingFace model list (hardcoded initially)
- [ ] 5.3: Implement model downloader (OkHttp)
- [ ] 5.4: Add download progress indicator
**Checkpoint:** Can download and switch between models

### PHASE 6: Polish (Step 6.1 - 6.3)
- [ ] 6.1: Add settings (temperature, top_k, max_tokens)
- [ ] 6.2: Add error handling + retry logic
- [ ] 6.3: Performance optimization (memory cleanup)
**Checkpoint:** Production-ready v1

---

## STEP-BY-STEP EXECUTION PROTOCOL

**For each step:**
1. Announce step number and what's being built
2. Show code additions (minimal comments)
3. Ask user confirmation if uncertain about implementation detail
4. Run/test after each step
5. Report success/failure in plaintext
6. Move to next step only after user acknowledgment

**After each phase completion:**
1. Verify all phase checkpoints passed
2. User confirms phase complete
3. Git commit with message: `Phase X Complete: [description]`
4. Update PLAN.md to mark phase complete with ✓
5. Proceed to next phase only after git commit

**Example step output:**
```
STEP 1.1: Implementing LLMEngine.kt

[code shown]

Status: ✓ Builds successfully
Next: Step 1.2 (ModelManager.kt)
Ready to proceed? (y/n)
```

---

## MODEL LIST FOR V1 (Downloadable)

**Starter Pack:**
1. Gemma-3 1B (1GB) - fast, lightweight
2. Gemma-2 2B (2GB) - best quality
3. Phi-2 (2.7GB) - good reasoning
4. SmolLM-1.7B (1.7GB) - efficient

**HuggingFace URLs:**
- Base: `https://huggingface.co/google/[model-name]/resolve/main/[model-file].litertlm`
- List maintained in: `ModelManager.kt` as `sealed class AvailableModel`

---

## GPU OPTIMIZATION NOTES

**Adreno-Specific:**
- Use GPU delegate (default in MediaPipe)
- Vulkan backend preferred over OpenCL (if available)
- Monitor thermal throttling (Snapdragon 8 Gen 3 runs hot)
- Batch size: 1 (streaming generation)
- FP16 quantization (built into .litertlm models)

**Memory Management:**
- Unload model when app backgrounds
- Keep max 1 model in memory at a time
- Clear KV cache between generations if needed

---

## PERMISSIONS REQUIRED

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

---

## SUCCESS CRITERIA FOR V1

- [ ] App launches with cyberpunk UI
- [ ] Can download model from HuggingFace on first run
- [ ] Can generate text with streaming output
- [ ] Metrics update in real-time (TTFT, tokens/sec, GPU%, RAM, VRAM, CPU, temp)
- [ ] Can switch between 4 models
- [ ] No crashes during normal operation
- [ ] Runs entirely on GPU (no CPU fallback)

---

## NOTES

- Keep all code in single commits per step
- No excessive comments (code should be self-explanatory)
- No loading spinners (use terminal-style "..." animations)
- All text in monospace font
- Target 60fps UI (Compose performance matters)
- Test on Snapdragon 8 Gen 3 device

---

**Phase 0: ✓ COMPLETE**

**Ready to start Phase 1: Core Inference**