# GINFERENCE COMMANDS SCRATCHPAD

Quick reference for all development commands. Copy-paste as needed.

**Configuration:**
- Phone IP: `192.168.1.11:43827`
- Phone Device: `adb-R5CX13E21XA-ZcoHEF._adb-tls-connect._tcp`
- Package: `com.ginference`
- Emulator: `emulator-5554`

---

## VARIABLES (Set These First)

```bash
export PHONE_IP="192.168.1.11"
export PHONE_PORT="43827"
export PHONE_ADDR="${PHONE_IP}:${PHONE_PORT}"
export PHONE_DEVICE="adb-R5CX13E21XA-ZcoHEF._adb-tls-connect._tcp"
export EMULATOR_SERIAL="emulator-5554"
export PACKAGE="com.ginference"
export ACTIVITY="${PACKAGE}/.MainActivity"
export APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
```

---

## BUILD

```bash
# Build debug APK
./gradlew assembleDebug

# Clean build
./gradlew clean assembleDebug

# Build and show APK location
./gradlew assembleDebug && ls -lh ${APK_PATH}
```

---

## PHONE (WiFi ADB)

### Connect
```bash
# Connect to phone
adb connect ${PHONE_ADDR}

# Check connection
adb devices
```

### Deploy to Phone (Using IP Address)
```bash
# Quick: Connect + Build + Install + Run
adb connect ${PHONE_ADDR} && ./gradlew assembleDebug && adb -s ${PHONE_ADDR} install -r ${APK_PATH} && adb -s ${PHONE_ADDR} shell am start -n ${ACTIVITY}

# Or step by step:
adb connect ${PHONE_ADDR}
./gradlew installDebug
adb -s ${PHONE_ADDR} shell am start -n ${ACTIVITY}
```

### Deploy to Phone (Using Device Name)
```bash
# Quick: Build + Install + Run
./gradlew assembleDebug && adb -s ${PHONE_DEVICE} install -r ${APK_PATH} && adb -s ${PHONE_DEVICE} shell am start -n ${ACTIVITY}

# Or with gradle:
./gradlew installDebug && adb -s ${PHONE_DEVICE} shell am start -n ${ACTIVITY}
```

### Phone Control
```bash
# Force stop app
adb -s ${PHONE_ADDR} shell am force-stop ${PACKAGE}

# Restart app
adb -s ${PHONE_ADDR} shell am force-stop ${PACKAGE} && adb -s ${PHONE_ADDR} shell am start -n ${ACTIVITY}

# Uninstall
adb -s ${PHONE_ADDR} uninstall ${PACKAGE}

# Disconnect
adb disconnect ${PHONE_ADDR}
```

### Phone Debugging
```bash
# View logcat (filtered)
adb -s ${PHONE_ADDR} logcat | grep -E "ginference|LLM|MediaPipe"

# Clear logcat and watch
adb -s ${PHONE_ADDR} logcat -c && adb -s ${PHONE_ADDR} logcat | grep -i ginference

# Watch for errors
adb -s ${PHONE_ADDR} logcat | grep -E "ERROR|Exception|FATAL"

# View app data directory
adb -s ${PHONE_ADDR} shell ls -la /data/data/${PACKAGE}/

# View app cache (where models are stored)
adb -s ${PHONE_ADDR} shell ls -lh /data/data/${PACKAGE}/cache/

# Check app memory usage
adb -s ${PHONE_ADDR} shell dumpsys meminfo ${PACKAGE}

# Check app GPU usage
adb -s ${PHONE_ADDR} shell dumpsys gfxinfo ${PACKAGE}
```

---

## EMULATOR

### Start Emulator
```bash
# List available AVDs
~/Library/Android/sdk/emulator/emulator -list-avds

# Start Pixel 8 (recommended)
~/Library/Android/sdk/emulator/emulator -avd Pixel_8_API_34

# Start with GPU acceleration
~/Library/Android/sdk/emulator/emulator -avd Pixel_8_API_34 -gpu host

# Start with more RAM
~/Library/Android/sdk/emulator/emulator -avd Pixel_8_API_34 -memory 4096
```

### Deploy to Emulator
```bash
# Quick: Build + Install + Run
export ANDROID_SERIAL=${EMULATOR_SERIAL} && ./gradlew assembleDebug && ./gradlew installDebug && adb shell am start -n ${ACTIVITY}

# Or step by step:
export ANDROID_SERIAL=${EMULATOR_SERIAL}
./gradlew installDebug
adb -s ${EMULATOR_SERIAL} shell am start -n ${ACTIVITY}
```

### Emulator Control
```bash
# Force stop
adb -s ${EMULATOR_SERIAL} shell am force-stop ${PACKAGE}

# Restart app
adb -s ${EMULATOR_SERIAL} shell am force-stop ${PACKAGE} && adb -s ${EMULATOR_SERIAL} shell am start -n ${ACTIVITY}

# Uninstall
adb -s ${EMULATOR_SERIAL} uninstall ${PACKAGE}
```

### Emulator Debugging
```bash
# View logcat
adb -s ${EMULATOR_SERIAL} logcat | grep -E "ginference|LLM|MediaPipe"

# Check memory
adb -s ${EMULATOR_SERIAL} shell dumpsys meminfo ${PACKAGE}
```

---

## DEVICE INFO

```bash
# List all connected devices
adb devices -l

# Get device model and SDK
adb shell getprop | grep -E "model|manufacturer|version.sdk|version.release"

# Get specific device info
adb -s ${PHONE_ADDR} shell getprop ro.product.model
adb -s ${PHONE_ADDR} shell getprop ro.build.version.sdk

# Check RAM
adb shell cat /proc/meminfo | head -5

# Check GPU
adb shell dumpsys gpu

# Check CPU
adb shell cat /proc/cpuinfo | grep -E "processor|Hardware|model name"

# Check storage
adb shell df -h /data

# Check thermal state
adb shell dumpsys thermalservice

# Check OpenGL ES version
adb shell dumpsys SurfaceFlinger | grep GLES
```

---

## SYSTEM METRICS (While App Running)

```bash
# Phone GPU usage
adb -s ${PHONE_ADDR} shell dumpsys gpu --gpuwork

# Phone memory pressure
adb -s ${PHONE_ADDR} shell dumpsys meminfo | grep -A 10 "Total PSS by process"

# Phone CPU freq
adb -s ${PHONE_ADDR} shell cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq

# Phone temperature
adb -s ${PHONE_ADDR} shell cat /sys/class/thermal/thermal_zone*/temp

# Detailed app stats
adb -s ${PHONE_ADDR} shell dumpsys activity ${PACKAGE}
```

---

## FILE OPERATIONS

```bash
# Push model file to phone
adb -s ${PHONE_ADDR} push model.litertlm /sdcard/Download/

# Push to app cache
adb -s ${PHONE_ADDR} push model.litertlm /data/local/tmp/
adb -s ${PHONE_ADDR} shell run-as ${PACKAGE} cp /data/local/tmp/model.litertlm /data/data/${PACKAGE}/cache/

# Pull model from phone
adb -s ${PHONE_ADDR} pull /data/data/${PACKAGE}/cache/model.litertlm .

# Clear app cache
adb -s ${PHONE_ADDR} shell pm clear ${PACKAGE}

# List app files
adb -s ${PHONE_ADDR} shell run-as ${PACKAGE} ls -la /data/data/${PACKAGE}/cache/
```

---

## GRADLE TASKS

```bash
# List all tasks
./gradlew tasks

# Lint check
./gradlew lint

# Run tests
./gradlew test

# Check dependencies
./gradlew dependencies

# Clean build cache
./gradlew clean
```

---

## TROUBLESHOOTING

```bash
# Kill ADB server and restart
adb kill-server && adb start-server

# Reconnect to phone
adb disconnect ${PHONE_ADDR} && sleep 2 && adb connect ${PHONE_ADDR}

# Check if app is installed
adb shell pm list packages | grep ginference

# View app permissions
adb shell dumpsys package ${PACKAGE} | grep permission

# Force GPU rendering
adb shell setprop debug.hwui.renderer opengl

# Check app installation path
adb shell pm path ${PACKAGE}

# Clear app data and cache
adb shell pm clear ${PACKAGE}
```

---

## QUICK COPY-PASTE COMMANDS

### Phone - Full Deploy (IP Address)
```bash
export PHONE_ADDR="192.168.1.11:43827" PACKAGE="com.ginference" ACTIVITY="${PACKAGE}/.MainActivity" APK_PATH="app/build/outputs/apk/debug/app-debug.apk" && adb connect ${PHONE_ADDR} && ./gradlew assembleDebug && adb -s ${PHONE_ADDR} install -r ${APK_PATH} && adb -s ${PHONE_ADDR} shell am start -n ${ACTIVITY}
```

### Phone - Full Deploy (Device Name)
```bash
export PHONE_DEVICE="adb-R5CX13E21XA-ZcoHEF._adb-tls-connect._tcp" PACKAGE="com.ginference" ACTIVITY="${PACKAGE}/.MainActivity" APK_PATH="app/build/outputs/apk/debug/app-debug.apk" && ./gradlew assembleDebug && adb -s ${PHONE_DEVICE} install -r ${APK_PATH} && adb -s ${PHONE_DEVICE} shell am start -n ${ACTIVITY}
```

### Phone - Quick Deploy (Already Built)
```bash
export PHONE_ADDR="192.168.1.11:43827" && ./gradlew installDebug && adb -s ${PHONE_ADDR} shell am start -n com.ginference/.MainActivity
```

### Phone - Restart App
```bash
export PHONE_ADDR="192.168.1.11:43827" && adb -s ${PHONE_ADDR} shell am force-stop com.ginference && adb -s ${PHONE_ADDR} shell am start -n com.ginference/.MainActivity
```

### Phone - Watch Logs
```bash
export PHONE_ADDR="192.168.1.11:43827" && adb -s ${PHONE_ADDR} logcat -c && adb -s ${PHONE_ADDR} logcat | grep -E "ginference|LLM|MediaPipe|ERROR"
```

### Emulator - Full Deploy
```bash
export EMULATOR_SERIAL="emulator-5554" && export ANDROID_SERIAL=${EMULATOR_SERIAL} && ./gradlew clean assembleDebug installDebug && adb shell am start -n com.ginference/.MainActivity
```

### Emulator - Watch Logs
```bash
adb -s emulator-5554 logcat -c && adb -s emulator-5554 logcat | grep -E "ginference|LLM|MediaPipe|ERROR"
```

---

## SCRATCHPAD

```bash
# Your quick notes here





```

---

## NOTES

- Phone connects via WiFi ADB (already paired)
- Use IP address `192.168.1.11:43827` OR device name `adb-R5CX13E21XA-ZcoHEF._adb-tls-connect._tcp`
- Both methods work, IP is shorter to type
- Port may change after phone reboot, check with `adb devices`
- Models will be stored in `/data/data/com.ginference/cache/`
- Min SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)
- GPU backend required (Adreno on Snapdragon 8 Gen 3)