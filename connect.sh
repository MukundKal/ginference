#!/bin/bash

# WIFI ADB CONNECTION HELPER

echo "================================================"
echo "WIFI ADB CONNECTION HELPER"
echo "================================================"
echo ""
echo "IMPORTANT: Pairing Port ≠ Connection Port"
echo ""
echo "On your phone:"
echo "  Settings → Developer Options → Wireless Debugging"
echo ""
echo "You'll see TWO different port numbers:"
echo ""
echo "  1. TOP OF SCREEN:"
echo "     'IP address & Port: 192.168.1.11:XXXXX'"
echo "     ↑ THIS IS YOUR CONNECTION PORT (use this!)"
echo ""
echo "  2. IN PAIRING POPUP:"
echo "     'Pairing code: 123456'"
echo "     'IP address & Port: 192.168.1.11:YYYYY'"
echo "     ↑ This is ONLY for pairing (changes each time)"
echo ""
echo "================================================"
echo ""

PHONE_IP="192.168.1.11"

# Check current devices
CURRENT_DEVICES=$(adb devices | grep "${PHONE_IP}" | grep "device$" | awk '{print $1}')

if [ ! -z "$CURRENT_DEVICES" ]; then
    echo "✓ Already connected to: $CURRENT_DEVICES"
    exit 0
fi

echo "Enter the CONNECTION port (from TOP of Wireless Debugging screen)"
echo -n "Port [press Enter to try common ports]: "
read USER_PORT

if [ ! -z "$USER_PORT" ]; then
    # User provided port
    CONNECT_ADDR="${PHONE_IP}:${USER_PORT}"
    echo ""
    echo "Attempting connection to ${CONNECT_ADDR}..."
    if adb connect "$CONNECT_ADDR"; then
        echo ""
        echo "✓ SUCCESS! Connected to ${CONNECT_ADDR}"
        echo ""
        adb devices
        exit 0
    else
        echo ""
        echo "✗ Failed to connect to ${CONNECT_ADDR}"
        exit 1
    fi
fi

# Try common ports
echo ""
echo "Trying common connection ports..."
echo ""

COMMON_PORTS=(
    "37429"
    "37871"
    "38103"
    "39547"
    "40725"
    "41853"
    "42587"
    "43827"
    "44531"
    "45678"
)

for PORT in "${COMMON_PORTS[@]}"; do
    CONNECT_ADDR="${PHONE_IP}:${PORT}"
    echo -n "Trying ${CONNECT_ADDR}... "

    if timeout 2 adb connect "$CONNECT_ADDR" 2>&1 | grep -q "connected"; then
        echo "✓ SUCCESS!"
        echo ""
        echo "Connected to: ${CONNECT_ADDR}"
        echo ""
        adb devices
        exit 0
    else
        echo "✗ failed"
    fi
done

echo ""
echo "================================================"
echo "Could not auto-connect"
echo "================================================"
echo ""
echo "Please check your phone's Wireless Debugging screen"
echo "and run this script again with the correct port:"
echo ""
echo "  ./connect.sh"
echo ""
echo "Or manually connect:"
echo "  adb connect 192.168.1.11:PORT"
echo ""
echo "Troubleshooting:"
echo "  1. Make sure Wireless Debugging is ON"
echo "  2. Make sure you're on the same WiFi network"
echo "  3. Try disabling and re-enabling Wireless Debugging"
echo "  4. Check if phone shows any authorization prompts"
echo ""
