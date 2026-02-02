# GINFERENCE - Local LLM Inference on Android
## Minimal Cyberpunk Retro Tech Stack

---

## PROJECT STATUS

**Current Phase:** Phase 6 (Polish) - PENDING TESTING
**Completed:** Phases 0-5 ✓
**Next:** Test with .task model files, then settings/error handling

### ⚠️ KNOWN ISSUE: RTLM Format Not Supported

MediaPipe tasks-genai (0.10.27) does NOT support `.litertlm` (RTLM format) files.
- Error: `Model provided has model identifier 'RTLM', should be 'TFL3'`
- **Solution:** Use `.task` files instead of `.litertlm` files
- LiteRT GenAI SDK (which supports RTLM) is not yet publicly available on Maven

**Compatible Models:** Must use `.task` format from HuggingFace litert-community

---

## SPECS

**Target:** Android Phone (SDK 26+, tested on Snapdragon 8 Gen 3 Adreno GPU)
**Language:** Kotlin
**UI:** Jetpack Compose (Cyberpunk/80s terminal aesthetic)
**Backend:** GPU via MediaPipe LLM Inference API
**Models:** Manual installation (user picks folder with SAF)

---

## COMPATIBLE MODELS (Not Just Gemma)

### LiteRT-Compatible Models on Android:

**From https://huggingface.co/litert-community:**

1. **Gemma Family** (Google)
   - Gemma3-1B-IT (689MB) - Optimized for SD8G3 ⭐ RECOMMENDED
   - Gemma 2B variants (500-1500MB)
   - Gemma 1.1 2B/7B variants

2. **SmolLM** (HuggingFace)
   - SmolLM-135M (167MB) - Ultra-light, no license ⭐ GOOD FOR TESTING
   - SmolLM-360M (360MB)
   - SmolLM-1.7B (1.7GB)

3. **Phi Models** (Microsoft)
   - Phi-2 (2.7B)
   - Phi-3-mini (3.8B)

4. **Qwen Models** (Alibaba)
   - Qwen2-0.5B/1.5B variants

⚠️ **IMPORTANT:** Models MUST be in `.task` format. The `.litertlm` (RTLM) format is NOT supported by MediaPipe.

**Working Download Links (.task format only):**
- Gemma3-1B-IT (555MB): https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.task
- Gemma3-1B-IT q8 (1GB): https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q8_ekv2048.task
- SmolLM-135M: https://huggingface.co/litert-community/SmolLM-135M-Instruct/resolve/main/SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task

**DO NOT USE:** `.litertlm` files (RTLM format not supported)

---

## MODEL INSTALLATION

### How It Works:
1. On first app launch → Storage Access Framework folder picker appears
2. User selects folder containing models (e.g., `/llms/`)
3. App scans folder for `.task` and `.litertlm` files
4. Models appear in [MODEL] selector
5. Tap model to load into memory
6. Start inferencing

### Storage:
- User-selected folder (persists across app updates/reinstalls)
- Models stay where user places them
- App only needs READ permission to folder
- No copying/moving - direct access via DocumentFile API

### Manual Installation:
See `DOWNLOAD.md` for detailed instructions and ADB commands.

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
- Monospace font: Roboto Mono
- Terminal-style text rendering
- Blinking cursor on text generation

**UI Elements:**
- Sharp edges (no rounded corners)
- 1px neon borders
- Revolving spinner (⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏) during generation
- Animated cursor (█▓▒░) on streaming output
- ASCII-style metrics readouts
- Auto-hide keyboard on Execute

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
- `androidx.documentfile:documentfile:1.0.1` (Storage Access Framework)
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`

**System APIs:**
- `ActivityManager` - RAM metrics
- `Debug.MemoryInfo` - VRAM estimation
- `PowerManager` - thermal state
- `/proc/stat` - CPU usage (requires permission)
- `/sys/class/kgsl/kgsl-3d0/*` - Adreno GPU metrics
- `DocumentFile` - Storage Access Framework for model folder

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
│   │   │   │       └── ModelSelector.kt
│   │   │   ├── inference/
│   │   │   │   ├── LLMEngine.kt (MediaPipe wrapper)
│   │   │   │   └── ModelManager.kt (folder scanning)
│   │   │   ├── metrics/
│   │   │   │   ├── SystemMetrics.kt
│   │   │   │   └── InferenceMetrics.kt
│   │   │   └── viewmodels/
│   │   │       └── InferenceViewModel.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── PLAN.md (this file)
├── DOWNLOAD.md (model installation guide)
├── COMMANDS.md (ADB commands reference)
├── deploy.sh (smart deployment)
├── run.sh (manual commands)
├── connect.sh (WiFi ADB helper)
├── test.sh (connection test)
└── emulate.sh (emulator launcher)
```

---

## IMPLEMENTATION PROGRESS

### PHASE 0: Setup (Step 0.1 - 0.3) ✓ COMPLETE
- [x] 0.1: Create project scaffold + gradle config
- [x] 0.2: Add dependencies
- [x] 0.3: Setup cyberpunk theme + color system
**Checkpoint:** Empty app builds, theme colors defined
**Git Commit:** `e8fb2c8` - Phase 0 Complete
**Status:** Deployed to phone, shows "GINFERENCE INITIALIZING..."

### PHASE 1: Core Inference (Step 1.1 - 1.4) ✓ COMPLETE
- [x] 1.1: Implement `LLMEngine.kt` (MediaPipe wrapper)
- [x] 1.2: Implement `ModelManager.kt` (local storage)
- [x] 1.3: Add basic model loading logic
- [x] 1.4: Test with hardcoded model path
**Checkpoint:** Can load model and generate text (backend ready)
**Git Commit:** `41e9285` - Phase 1 Complete
**Status:** Backend infrastructure working, no UI yet

### PHASE 2: System Metrics (Step 2.1 - 2.6) ✓ COMPLETE
- [x] 2.1: Implement RAM tracking
- [x] 2.2: Implement VRAM estimation
- [x] 2.3: Implement CPU usage monitoring
- [x] 2.4: Implement GPU usage (Adreno kgsl sysfs)
- [x] 2.5: Implement thermal monitoring
- [x] 2.6: Implement inference metrics (TTFT, tokens/sec)
**Checkpoint:** All metrics readable in logcat
**Git Commit:** `6013406` - Phase 2 Complete
**Status:** System metrics collecting every 1s

### PHASE 3: Basic UI (Step 3.1 - 3.4) ✓ COMPLETE
- [x] 3.1: Create `InferenceScreen.kt` main layout
- [x] 3.2: Implement `PromptInput.kt` (terminal-style input)
- [x] 3.3: Implement `OutputDisplay.kt` (streaming text)
- [x] 3.4: Connect ViewModel to UI
**Checkpoint:** Can type prompt and see UI
**Git Commit:** `32aad10` - Phase 3 Complete
**Status:** Full chat interface with metrics bar, message history

### PHASE 4: Metrics UI (Step 4.1 - 4.3) ✓ SKIPPED
- [x] 4.1: Implement `MetricsPanel.kt` (already in Phase 3)
- [x] 4.2: Real-time metric updates (already working)
- [x] 4.3: CRT effects (minimal approach)
**Checkpoint:** Live metrics visible during inference
**Status:** Metrics bar shows TTFT, tokens/sec, RAM, GPU, temp in real-time

### PHASE 5: Model Management (Step 5.1 - 5.4) ✓ COMPLETE
- [x] 5.1: Implement `ModelSelector.kt` UI
- [x] 5.2: Add folder picker with Storage Access Framework
- [x] 5.3: Implement folder scanning (DocumentFile API)
- [x] 5.4: Add model loading on selection
**Checkpoint:** Can select folder and load models
**Git Commit:** `eba65ff` - Phase 5 Complete
**Status:** [MODEL] button opens selector, scans user folder, shows available models

**Additional Features Added:**
- Keyboard auto-hide on Execute
- Revolving spinner (⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏) when generating
- Animated blinking cursor (█▓▒░) on streaming text
- Input disabled until model loaded (red border warning)
- Model name displayed in header when loaded

### PHASE 6: Polish (Step 6.1 - 6.3) - PENDING TESTING
- [ ] 6.0: **TEST WITH .task MODEL FILE** ⚠️ BLOCKED - Need to test with proper format
- [ ] 6.1: Add settings (temperature, top_k, max_tokens)
- [ ] 6.2: Error handling improvements
- [ ] 6.3: Performance optimizations
**Checkpoint:** Production-ready v1
**Status:** PENDING - Need to test model loading with .task file (not .litertlm)

**Latest Changes (2024-02-02):**
- Updated MediaPipe to 0.10.27
- Added RTLM format detection with clear error message
- Documented that .litertlm files are NOT supported
- User must download .task files from HuggingFace

---

## CURRENT FEATURES (v0.9)

### ✓ Working:
- Cyberpunk terminal UI with matrix green/cyan neon colors
- Message history (user/assistant bubbles)
- Input field with terminal placeholder
- Execute/Abort buttons
- [MODEL] selector with folder scanning
- Storage Access Framework folder picker on first launch
- Live metrics bar: TTFT, tokens/sec, RAM, GPU, temperature
- System metrics collection (1s interval)
- Model loading with MediaPipe
- Streaming text generation (ready when model loaded)
- Keyboard auto-hide
- Animated loading states
- Input disabled without model

### ✗ Not Yet:
- Settings panel
- Conversation save/load
- Model switching during generation
- Advanced error recovery

---

## EXECUTION PROTOCOL

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

---

## DEPLOYMENT WORKFLOW

**Phone Connection:**
- WiFi ADB (port changes on each wireless debug session)
- Current: `adb connect 192.168.1.11:PORT`
- Check Wireless Debugging screen for current port

**Quick Deploy:**
```bash
adb connect 192.168.1.11:PORT
adb -s 192.168.1.11:PORT install -r app/build/outputs/apk/debug/app-debug.apk
adb -s 192.168.1.11:PORT shell am start -n com.ginference/.MainActivity
```

**Or use:**
```bash
./deploy.sh
```

---

## MODEL INSTALLATION (MANUAL APPROACH)

**Why Manual?**
- HuggingFace models require auth/licenses
- Large files (500MB-4GB)
- User controls where models stored
- Persists across app updates/reinstalls
- No app-managed downloads

**How It Works:**
1. User downloads `.task` or `.litertlm` from HuggingFace
2. Places in folder on phone (e.g., `/llms/`)
3. App asks for folder permission on first launch (SAF picker)
4. App scans folder for compatible models
5. Models appear in [MODEL] selector
6. User taps to load into memory

**Storage Location:**
- User-chosen (via Android folder picker)
- Recommended: `/storage/emulated/0/llms/`
- Or: `/sdcard/llms/`, `/sdcard/Download/`, etc.

**See DOWNLOAD.md for:**
- Recommended models with direct links
- ADB commands for file transfer
- Troubleshooting guide

---

## TECH STACK DETAILS

### Android LLM Inference Stack:
```
Your App (ginference)
    ↓
MediaPipe LLM Inference API
    ↓
LiteRT Runtime (.task/.litertlm execution)
    ↓
GPU Delegate (Vulkan/OpenCL)
    ↓
Android NNAPI
    ↓
Adreno GPU HAL
    ↓
Snapdragon 8 Gen 3 Adreno GPU
```

### PC vs Android Counterparts:
| PC (vllm) | Android (ginference) |
|-----------|---------------------|
| vllm | MediaPipe LLM Inference API |
| PyTorch | LiteRT Runtime |
| CUDA | GPU Delegate (Vulkan) |
| .safetensors | .task/.litertlm |
| nvidia-smi | Adreno kgsl sysfs |

---

## PERMISSIONS

**Required:**
- `INTERNET` - For future updates
- `ACCESS_NETWORK_STATE` - Network checks
- `READ_EXTERNAL_STORAGE` - Legacy (SDK < 33)
- `WRITE_EXTERNAL_STORAGE` - Legacy (SDK < 33)
- `MANAGE_EXTERNAL_STORAGE` - Android 11+ (for broader access)

**Runtime Permissions:**
- Storage Access Framework folder picker (no dangerous permissions)
- Persistable URI permissions granted on folder selection

---

## GIT HISTORY

```
ccda89a - Update PLAN.md: Mark Phase 5 complete
eba65ff - Phase 5 Complete: Model management + UI improvements
d8795d9 - Update PLAN.md: Mark Phase 3 complete
32aad10 - Phase 3 Complete: Basic UI implementation
8367413 - Update PLAN.md: Mark Phase 2 complete
6013406 - Phase 2 Complete: System metrics implementation
a14fa96 - Update PLAN.md: Mark Phase 1 complete
41e9285 - Phase 1 Complete: Core inference backend
d7c41d2 - Update PLAN.md: Mark Phase 0 complete, add git protocol
e8fb2c8 - Phase 0 Complete: Project setup, dependencies, cyberpunk theme
```

---

## PHASE 6: POLISH (REMAINING WORK)

### 6.1: Settings Panel
- Temperature slider (0.0 - 2.0)
- Top K slider (1 - 100)
- Max tokens slider (128 - 4096)
- Random seed input
- Save/load preferences

### 6.2: Error Handling
- Model load failures → Show error message
- Generation errors → Graceful recovery
- Storage permission denied → Retry prompt
- Low memory warnings → Suggest smaller model
- Thermal throttling → Pause generation option

### 6.3: Performance Optimizations
- Unload model on app background
- Clear KV cache between sessions
- Memory pressure monitoring
- Thermal throttling response
- 60fps UI maintenance

---

## SUCCESS CRITERIA FOR V1

- [x] App launches with cyberpunk UI
- [x] Folder picker on first launch
- [x] Scan folder for models
- [x] Model selector shows available models
- [x] Can load model into memory
- [ ] Can generate text with streaming output (needs model loaded)
- [x] Metrics update in real-time (TTFT, tokens/sec, GPU%, RAM, temp)
- [x] Input disabled without model loaded
- [x] Keyboard auto-hides on Execute
- [ ] No crashes during normal operation
- [ ] Settings panel functional

---

## TESTING CHECKLIST

**Before each phase commit:**
- [ ] Build succeeds without warnings
- [ ] Deploy to phone successful
- [ ] UI renders correctly (check phone screen)
- [ ] No crashes in logcat
- [ ] Feature works as intended
- [ ] Git commit with descriptive message

**Current Test Device:**
- Model: Galaxy (Snapdragon 8 Gen 3)
- WiFi ADB: 192.168.1.11:PORT (changes frequently)
- Android SDK: 34+
- RAM: 10.83GB total
- Storage: 3.5GB+ free

---

## KNOWN ISSUES

1. **CPU metrics:** `/proc/stat` permission denied (Android security)
   - Status: Expected, working around it

2. **WiFi ADB port changes:** Requires reconnect after wireless debug toggle
   - Solution: Check phone for current port

3. **Storage location display:** Shows URI path not human-readable
   - Status: Acceptable for v1

---

## DEPLOYMENT SCRIPTS

**Available Commands:**
- `./deploy.sh` - Auto-detect device and deploy
- `./run.sh` - All commands with variables
- `./connect.sh` - WiFi ADB connection helper
- `./test.sh` - Verify device connection
- `./emulate.sh` - Launch Android emulator

**See COMMANDS.md for complete reference**

---

## NEXT STEPS

1. **Download a .task model file** (NOT .litertlm)
   - Recommended: `gemma3-1b-it-int4.task` (555MB)
   - From: https://huggingface.co/litert-community/Gemma3-1B-IT
   - Requires accepting Gemma license on HuggingFace
2. **Push model to device:** `adb push gemma3-1b-it-int4.task /sdcard/llms/`
3. **Deploy app and test folder picker**
4. **Verify model loads and generates text**
5. **Complete Phase 6 (settings, error handling)**
6. **Final commit for v1.0**

## GITHUB REPOSITORY

**Repository:** https://github.com/MukundKal/ginference
**Releases:** https://github.com/MukundKal/ginference/releases
**Latest APK:** https://github.com/MukundKal/ginference/releases/tag/v0.9.0-beta

---

## NOTES

- All code uses minimal comments (self-explanatory)
- No loading spinners (terminal-style animations only)
- All text in monospace font
- Target 60fps UI
- Test on Snapdragon 8 Gen 3 device
- Models tested: Gemma3-1B, SmolLM-135M