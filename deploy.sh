#!/bin/bash

# GINFERENCE DEPLOY SCRIPT
# Auto-detect device and deploy

set -e

PACKAGE="com.ginference"
ACTIVITY="${PACKAGE}/.MainActivity"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

echo "================================================"
echo "GINFERENCE DEPLOY"
echo "================================================"

# Check for connected devices
DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | awk '{print $1}')
DEVICE_COUNT=$(echo "$DEVICES" | grep -v '^$' | wc -l | xargs)

if [ "$DEVICE_COUNT" -eq "0" ]; then
    echo "ERROR: No devices connected"
    echo ""
    echo "Options:"
    echo "  1. Connect phone via WiFi ADB:"
    echo "     adb connect 192.168.1.11:43827"
    echo ""
    echo "  2. Start emulator:"
    echo "     ~/Library/Android/sdk/emulator/emulator -avd Pixel_8_API_34"
    echo ""
    exit 1
fi

# Select device
if [ "$DEVICE_COUNT" -eq "1" ]; then
    DEVICE=$(echo "$DEVICES" | head -1)
    echo "Found 1 device: $DEVICE"
else
    echo "Found $DEVICE_COUNT devices:"
    echo "$DEVICES" | nl
    echo ""
    echo -n "Select device number [1]: "
    read -t 3 SELECTION || SELECTION=1
    SELECTION=${SELECTION:-1}
    DEVICE=$(echo "$DEVICES" | sed -n "${SELECTION}p")
    echo "Selected device: $DEVICE"
fi

echo "Using device: $DEVICE"
echo ""

# Build
echo "Building APK..."
./gradlew assembleDebug --quiet

if [ ! -f "$APK_PATH" ]; then
    echo "ERROR: Build failed, APK not found"
    exit 1
fi

APK_SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
echo "Built APK: $APK_SIZE"
echo ""

# Install
echo "Installing on $DEVICE..."
adb -s "$DEVICE" install -r "$APK_PATH"

# Launch
echo "Launching app..."
adb -s "$DEVICE" shell am start -n "$ACTIVITY"

echo ""
echo "================================================"
echo "DEPLOYED SUCCESSFULLY"
echo "================================================"
echo ""
echo "Useful commands:"
echo "  Watch logs: adb -s $DEVICE logcat | grep -E 'ginference|ERROR'"
echo "  Restart app: adb -s $DEVICE shell am force-stop $PACKAGE && adb -s $DEVICE shell am start -n $ACTIVITY"
echo "  Stop app: adb -s $DEVICE shell am force-stop $PACKAGE"
echo ""
