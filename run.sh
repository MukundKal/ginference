#!/bin/bash

# CONFIGURATION
PHONE_IP="192.168.1.11"
PHONE_PORT="43827"
PHONE_ADDR="${PHONE_IP}:${PHONE_PORT}"
PHONE_DEVICE="adb-R5CX13E21XA-ZcoHEF._adb-tls-connect._tcp"
EMULATOR_SERIAL="emulator-5554"
PACKAGE_NAME="com.ginference"
ACTIVITY_NAME="${PACKAGE_NAME}/.MainActivity"

# BUILD COMMANDS
# ==============

# Build debug APK
# ./gradlew assembleDebug

# Clean build
# ./gradlew clean assembleDebug


# PHONE COMMANDS (WiFi ADB)
# ==========================

# Connect to phone
# adb connect ${PHONE_ADDR}

# Install and run on phone (using device name)
# ./gradlew installDebug && adb -s ${PHONE_DEVICE} shell am start -n ${ACTIVITY_NAME}

# Install and run on phone (using IP address)
# ./gradlew installDebug && adb -s ${PHONE_ADDR} shell am start -n ${ACTIVITY_NAME}

# Force stop app on phone
# adb -s ${PHONE_DEVICE} shell am force-stop ${PACKAGE_NAME}

# Uninstall from phone
# adb -s ${PHONE_DEVICE} uninstall ${PACKAGE_NAME}

# View logcat from phone
# adb -s ${PHONE_DEVICE} logcat | grep -i ginference

# Disconnect phone
# adb disconnect ${PHONE_ADDR}


# EMULATOR COMMANDS
# =================

# Install and run on emulator
# export ANDROID_SERIAL=${EMULATOR_SERIAL} && ./gradlew installDebug && adb shell am start -n ${ACTIVITY_NAME}

# Force stop app on emulator
# adb -s ${EMULATOR_SERIAL} shell am force-stop ${PACKAGE_NAME}

# Uninstall from emulator
# adb -s ${EMULATOR_SERIAL} uninstall ${PACKAGE_NAME}

# View logcat from emulator
# adb -s ${EMULATOR_SERIAL} logcat | grep -i ginference


# DEVICE INFO
# ===========

# List connected devices
# adb devices

# Get device properties
# adb shell getprop | grep -i "model\|manufacturer\|version.sdk"

# Check available memory
# adb shell cat /proc/meminfo | grep -i "memtotal\|memfree"

# Check GPU info
# adb shell dumpsys gpu


# QUICK COMMANDS
# ==============

# Phone: build + install + run (using device name)
# ./gradlew assembleDebug && adb -s ${PHONE_DEVICE} install -r app/build/outputs/apk/debug/app-debug.apk && adb -s ${PHONE_DEVICE} shell am start -n ${ACTIVITY_NAME}

# Phone: build + install + run (using IP)
# ./gradlew assembleDebug && adb -s ${PHONE_ADDR} install -r app/build/outputs/apk/debug/app-debug.apk && adb -s ${PHONE_ADDR} shell am start -n ${ACTIVITY_NAME}

# Emulator: build + install + run
# export ANDROID_SERIAL=${EMULATOR_SERIAL} && ./gradlew assembleDebug && ./gradlew installDebug && adb shell am start -n ${ACTIVITY_NAME}


# MOST USED
# =========

# Connect and deploy to phone
adb connect ${PHONE_ADDR} && ./gradlew assembleDebug && adb -s ${PHONE_ADDR} install -r app/build/outputs/apk/debug/app-debug.apk && adb -s ${PHONE_ADDR} shell am start -n ${ACTIVITY_NAME}

# Restart app on phone
# adb -s ${PHONE_ADDR} shell am force-stop ${PACKAGE_NAME} && adb -s ${PHONE_ADDR} shell am start -n ${ACTIVITY_NAME}

# Watch logs from phone
# adb -s ${PHONE_ADDR} logcat -c && adb -s ${PHONE_ADDR} logcat | grep -E "ginference|LLM|MediaPipe|ERROR"
