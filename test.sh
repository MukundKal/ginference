#!/bin/bash

# TEST CURRENT ADB CONNECTION

echo "================================================"
echo "ADB CONNECTION TEST"
echo "================================================"
echo ""

# Check ADB server
echo "1. Checking ADB server..."
adb version | head -1

echo ""
echo "2. Connected devices:"
adb devices -l

echo ""
echo "3. Device details:"
DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | awk '{print $1}')

if [ -z "$DEVICES" ]; then
    echo "   ✗ NO DEVICES CONNECTED"
    echo ""
    echo "   To connect phone:"
    echo "   ./connect.sh"
    echo ""
    echo "   To start emulator:"
    echo "   ./emulate.sh"
    exit 1
fi

echo "$DEVICES" | while read DEVICE; do
    echo ""
    echo "   Device: $DEVICE"

    # Get model
    MODEL=$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
    if [ ! -z "$MODEL" ]; then
        echo "   Model: $MODEL"
    fi

    # Get Android version
    SDK=$(adb -s "$DEVICE" shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')
    if [ ! -z "$SDK" ]; then
        echo "   SDK: $SDK (Android $(adb -s "$DEVICE" shell getprop ro.build.version.release 2>/dev/null | tr -d '\r'))"
    fi

    # Check if app is installed
    if adb -s "$DEVICE" shell pm list packages 2>/dev/null | grep -q "com.ginference"; then
        echo "   App: ✓ Installed"
    else
        echo "   App: ✗ Not installed"
    fi
done

echo ""
echo "================================================"
echo "✓ TEST COMPLETE"
echo "================================================"
echo ""
echo "Ready to deploy:"
echo "  ./deploy.sh"
echo ""
