#!/bin/bash

# List available emulators
~/Library/Android/sdk/emulator/emulator -list-avds

echo ""
echo "Starting emulator..."
echo ""

# Run emulator (using available AVD)
~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.1 -gpu host

# Alternative (uncomment to use):
# ~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.1 -memory 4096 -gpu host
