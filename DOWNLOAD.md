# MODEL INSTALLATION

## MANUAL MODEL SETUP

Models are NOT downloaded by the app. You must place them manually.

---

## STORAGE LOCATION

**Internal App Storage:**
```
/data/data/com.ginference/files/models/
```

**Access via ADB:**
```bash
adb shell run-as com.ginference ls /data/data/com.ginference/files/models/
```

---

## SUPPORTED FORMATS

- `.task` files (LiteRT Task Bundle)
- `.litertlm` files (LiteRT-LM format)

Place ANY `.task` or `.litertlm` file in the storage path - app will scan and show it.

---

## RECOMMENDED MODELS

### For Your Snapdragon 8 Gen 3:

**1. Gemma 3 1B IT (Optimized for SD8G3)** ⭐ RECOMMENDED
- Size: 689MB
- File: `Gemma3-1B-IT_q4_ekv1280_sm8750.litertlm`
- Download: https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_q4_ekv1280_sm8750.litertlm
- Note: Requires accepting Gemma license on HuggingFace first

**2. SmolLM 135M (Smallest, No License)** ⭐ GOOD FOR TESTING
- Size: 167MB
- File: `SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task`
- Download: https://huggingface.co/litert-community/SmolLM-135M-Instruct/resolve/main/SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task
- License: Apache 2.0 (open)

**3. Gemma 3 1B IT (Generic)**
- Size: 584MB
- File: `gemma3-1b-it-int4.litertlm`
- Download: https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma3-1b-it-int4.litertlm

---

## INSTALLATION STEPS

### Method 1: Via ADB (Direct)

```bash
# 1. Download model to your computer
wget https://huggingface.co/litert-community/SmolLM-135M-Instruct/resolve/main/SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task

# 2. Connect phone
adb connect 192.168.1.11:33705

# 3. Push to temp location
adb -s 192.168.1.11:33705 push SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task /sdcard/Download/

# 4. Move to app storage (requires run-as)
adb -s 192.168.1.11:33705 shell
run-as com.ginference
mkdir -p /data/data/com.ginference/files/models/
cp /sdcard/Download/SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task /data/data/com.ginference/files/models/
exit
exit

# 5. Verify
adb -s 192.168.1.11:33705 shell run-as com.ginference ls -lh /data/data/com.ginference/files/models/

# 6. Restart app
adb -s 192.168.1.11:33705 shell am force-stop com.ginference
adb -s 192.168.1.11:33705 shell am start -n com.ginference/.MainActivity
```

### Method 2: Via ADB (Script)

Create `install_model.sh`:
```bash
#!/bin/bash
MODEL_FILE=$1
PHONE_ADDR="192.168.1.11:33705"

if [ -z "$MODEL_FILE" ]; then
    echo "Usage: ./install_model.sh <model-file>"
    exit 1
fi

echo "Pushing model to phone..."
adb -s $PHONE_ADDR push "$MODEL_FILE" /sdcard/Download/

echo "Moving to app storage..."
adb -s $PHONE_ADDR shell "run-as com.ginference mkdir -p /data/data/com.ginference/files/models/ && run-as com.ginference cp /sdcard/Download/$(basename $MODEL_FILE) /data/data/com.ginference/files/models/"

echo "Cleaning up..."
adb -s $PHONE_ADDR shell rm /sdcard/Download/$(basename $MODEL_FILE)

echo "Model installed! Restarting app..."
adb -s $PHONE_ADDR shell am force-stop com.ginference
adb -s $PHONE_ADDR shell am start -n com.ginference/.MainActivity

echo "Done!"
```

Usage:
```bash
chmod +x install_model.sh
./install_model.sh SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task
```

---

## BROWSE MORE MODELS

**LiteRT Community (All Models):**
https://huggingface.co/litert-community

**Filter by:**
- Text Generation
- Sort by: Most downloads
- Look for `.task` or `.litertlm` files

**Requirements:**
- Must be LiteRT format (not PyTorch, not GGUF)
- Smaller models work better on mobile (<2GB recommended)
- GPU-optimized models preferred (int4/int8 quantized)

---

## VERIFY INSTALLATION

In the app:
1. Tap `[MODEL]` button
2. See list of available models
3. Tap a model to load it
4. Header shows model name when loaded
5. Input field turns green (ready for prompts)

---

## TROUBLESHOOTING

**No models showing?**
```bash
# Check storage path exists
adb shell run-as com.ginference ls -la /data/data/com.ginference/files/models/

# Check permissions
adb shell run-as com.ginference stat /data/data/com.ginference/files/models/
```

**Model won't load?**
- Check file is complete (compare size with HuggingFace)
- Check file extension (.task or .litertlm)
- Check logcat for errors: `adb logcat | grep LLMEngine`

**Storage full?**
```bash
# Check free space
adb shell df -h /data

# Delete old models
adb shell run-as com.ginference rm /data/data/com.ginference/files/models/OLD_MODEL.task
```

---

## NOTES

- Models persist across app updates/reinstalls
- Only deleted on app uninstall
- Each model loads into VRAM (watch temp/throttling)
- Snapdragon 8 Gen 3 recommended: 1-2GB models
- Larger models (>3GB) may cause OOM on some devices