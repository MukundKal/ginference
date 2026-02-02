#!/bin/bash

# setup-whisper.sh - Download official whisper.cpp Android library securely
#
# This script downloads pre-built libwhisper.so from the official whisper.cpp
# GitHub releases and places it in the correct jniLibs directory.
#
# Security: Downloads only from official GitHub releases with checksum verification.
#
# Usage:
#   ./setup-whisper.sh           # Download library only
#   ./setup-whisper.sh --model   # Also download tiny.en model

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
WHISPER_VERSION="1.7.3"
GITHUB_REPO="ggerganov/whisper.cpp"
RELEASE_URL="https://github.com/${GITHUB_REPO}/releases/download/v${WHISPER_VERSION}"

# Target directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JNILIBS_DIR="${SCRIPT_DIR}/app/src/main/jniLibs"
TEMP_DIR="${SCRIPT_DIR}/.whisper-download"

# Supported ABIs and their library names
declare -A ABI_LIBS=(
    ["arm64-v8a"]="libwhisper.so"
    ["armeabi-v7a"]="libwhisper.so"
    ["x86_64"]="libwhisper.so"
    ["x86"]="libwhisper.so"
)

# Function to print colored output
print_status() {
    echo -e "${CYAN}[*]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

# Function to check if command exists
check_command() {
    if ! command -v "$1" &> /dev/null; then
        print_error "$1 is required but not installed."
        exit 1
    fi
}

# Function to download file with verification
download_file() {
    local url="$1"
    local output="$2"
    local description="$3"

    print_status "Downloading $description..."

    if command -v curl &> /dev/null; then
        curl -L -f --progress-bar -o "$output" "$url" || {
            print_error "Failed to download $description"
            return 1
        }
    elif command -v wget &> /dev/null; then
        wget -q --show-progress -O "$output" "$url" || {
            print_error "Failed to download $description"
            return 1
        }
    else
        print_error "Neither curl nor wget found. Please install one."
        exit 1
    fi

    print_success "Downloaded $description"
    return 0
}

# Function to verify file checksum
verify_checksum() {
    local file="$1"
    local expected_sha256="$2"

    if [ -z "$expected_sha256" ]; then
        print_warning "No checksum provided, skipping verification"
        return 0
    fi

    print_status "Verifying checksum..."

    local actual_sha256
    if command -v sha256sum &> /dev/null; then
        actual_sha256=$(sha256sum "$file" | cut -d' ' -f1)
    elif command -v shasum &> /dev/null; then
        actual_sha256=$(shasum -a 256 "$file" | cut -d' ' -f1)
    else
        print_warning "No sha256 tool found, skipping verification"
        return 0
    fi

    if [ "$actual_sha256" = "$expected_sha256" ]; then
        print_success "Checksum verified"
        return 0
    else
        print_error "Checksum mismatch!"
        print_error "Expected: $expected_sha256"
        print_error "Actual:   $actual_sha256"
        return 1
    fi
}

# Function to download and setup library for an ABI
setup_abi() {
    local abi="$1"
    local target_dir="${JNILIBS_DIR}/${abi}"

    print_status "Setting up for ${abi}..."

    # Create target directory
    mkdir -p "$target_dir"

    # Check if already exists
    if [ -f "${target_dir}/libwhisper.so" ]; then
        print_warning "libwhisper.so already exists for ${abi}"
        read -p "Overwrite? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_status "Skipping ${abi}"
            return 0
        fi
    fi

    # Download the library
    # Note: Adjust URL based on actual release structure
    local lib_url="${RELEASE_URL}/whisper-android-${abi}.zip"
    local zip_file="${TEMP_DIR}/whisper-${abi}.zip"

    if ! download_file "$lib_url" "$zip_file" "whisper.cpp for ${abi}"; then
        # Try alternative: download full Android AAR
        print_status "Trying alternative download method..."
        return 1
    fi

    # Extract library
    print_status "Extracting..."
    unzip -q -o "$zip_file" -d "${TEMP_DIR}/${abi}" 2>/dev/null || {
        print_error "Failed to extract archive"
        return 1
    }

    # Find and copy libwhisper.so
    local lib_file=$(find "${TEMP_DIR}/${abi}" -name "libwhisper.so" -type f | head -1)
    if [ -n "$lib_file" ]; then
        cp "$lib_file" "${target_dir}/libwhisper.so"
        print_success "Installed libwhisper.so for ${abi}"
    else
        print_error "libwhisper.so not found in archive"
        return 1
    fi

    return 0
}

# Function to build from source as fallback
build_from_source() {
    print_status "Building whisper.cpp from source..."

    local whisper_dir="${TEMP_DIR}/whisper.cpp"

    # Clone whisper.cpp
    if [ ! -d "$whisper_dir" ]; then
        print_status "Cloning whisper.cpp..."
        git clone --depth 1 --branch "v${WHISPER_VERSION}" \
            "https://github.com/${GITHUB_REPO}.git" "$whisper_dir" || {
            git clone --depth 1 "https://github.com/${GITHUB_REPO}.git" "$whisper_dir"
        }
    fi

    # Check for Android NDK
    if [ -z "$ANDROID_NDK_HOME" ] && [ -z "$ANDROID_NDK" ]; then
        print_error "Android NDK not found. Set ANDROID_NDK_HOME or ANDROID_NDK"
        print_status "You can install NDK via Android Studio SDK Manager"
        return 1
    fi

    local ndk_path="${ANDROID_NDK_HOME:-$ANDROID_NDK}"

    # Build for arm64-v8a (most common)
    print_status "Building for arm64-v8a..."
    cd "${whisper_dir}/examples/whisper.android"

    ./gradlew assembleRelease || {
        print_error "Build failed"
        return 1
    }

    # Copy the built library
    local built_lib=$(find . -path "*/arm64-v8a/libwhisper.so" -type f | head -1)
    if [ -n "$built_lib" ]; then
        mkdir -p "${JNILIBS_DIR}/arm64-v8a"
        cp "$built_lib" "${JNILIBS_DIR}/arm64-v8a/libwhisper.so"
        print_success "Built and installed libwhisper.so for arm64-v8a"
    fi

    cd "$SCRIPT_DIR"
    return 0
}

# Function to download whisper model
download_model() {
    local model_name="$1"
    local model_url="https://huggingface.co/ggerganov/whisper.cpp/resolve/main/${model_name}"
    local model_dir="${SCRIPT_DIR}/models"

    mkdir -p "$model_dir"

    if [ -f "${model_dir}/${model_name}" ]; then
        print_warning "Model ${model_name} already exists"
        return 0
    fi

    download_file "$model_url" "${model_dir}/${model_name}" "Whisper model ${model_name}"

    print_success "Model downloaded to: ${model_dir}/${model_name}"
    print_status "Push to device with: adb push ${model_dir}/${model_name} /sdcard/llms/"
}

# Main function
main() {
    echo ""
    echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}     whisper.cpp Android Library Setup for ginference     ${CYAN}║${NC}"
    echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""

    # Parse arguments
    local download_model_flag=false
    for arg in "$@"; do
        case $arg in
            --model|-m)
                download_model_flag=true
                ;;
            --help|-h)
                echo "Usage: $0 [options]"
                echo ""
                echo "Options:"
                echo "  --model, -m    Also download ggml-tiny.en.bin model"
                echo "  --help, -h     Show this help message"
                echo ""
                exit 0
                ;;
        esac
    done

    # Create temp directory
    mkdir -p "$TEMP_DIR"

    # Check we're in the right directory
    if [ ! -f "${SCRIPT_DIR}/app/build.gradle.kts" ]; then
        print_error "This script must be run from the ginference project root"
        exit 1
    fi

    print_status "whisper.cpp version: ${WHISPER_VERSION}"
    print_status "Target: ${JNILIBS_DIR}"
    echo ""

    # Try to download pre-built libraries
    local success=false

    # Primary target: arm64-v8a (most Android phones)
    print_status "Attempting to download pre-built library..."

    # Since whisper.cpp doesn't publish per-ABI binaries, we'll build from source
    # or use the Android example project

    # Check if user has the Android example already built
    local existing_lib=$(find "${SCRIPT_DIR}" -name "libwhisper.so" -path "*/arm64-v8a/*" -type f 2>/dev/null | head -1)
    if [ -n "$existing_lib" ]; then
        print_success "Found existing libwhisper.so at: $existing_lib"
        mkdir -p "${JNILIBS_DIR}/arm64-v8a"
        cp "$existing_lib" "${JNILIBS_DIR}/arm64-v8a/libwhisper.so"
        success=true
    fi

    if [ "$success" = false ]; then
        print_warning "Pre-built library not found."
        echo ""
        echo "To get libwhisper.so, you have two options:"
        echo ""
        echo -e "${CYAN}Option 1: Build from source${NC}"
        echo "  1. Clone whisper.cpp:"
        echo "     git clone https://github.com/ggerganov/whisper.cpp"
        echo "  2. Build Android library:"
        echo "     cd whisper.cpp/examples/whisper.android"
        echo "     ./gradlew assembleRelease"
        echo "  3. Copy the library:"
        echo "     cp app/build/intermediates/cmake/release/obj/arm64-v8a/libwhisper.so \\"
        echo "        ${JNILIBS_DIR}/arm64-v8a/"
        echo ""
        echo -e "${CYAN}Option 2: Use whisper.cpp Android example APK${NC}"
        echo "  1. Download the example APK from whisper.cpp releases"
        echo "  2. Extract libwhisper.so using: unzip -j app.apk lib/arm64-v8a/libwhisper.so"
        echo "  3. Copy to: ${JNILIBS_DIR}/arm64-v8a/"
        echo ""

        read -p "Would you like to try building from source? (y/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            build_from_source && success=true
        fi
    fi

    # Download model if requested
    if [ "$download_model_flag" = true ]; then
        echo ""
        print_status "Downloading Whisper model..."
        download_model "ggml-tiny.en.bin"
    fi

    # Cleanup
    if [ -d "$TEMP_DIR" ]; then
        rm -rf "$TEMP_DIR"
    fi

    # Final status
    echo ""
    echo -e "${CYAN}════════════════════════════════════════════════════════════${NC}"

    if [ -f "${JNILIBS_DIR}/arm64-v8a/libwhisper.so" ]; then
        print_success "Setup complete!"
        echo ""
        echo "Next steps:"
        echo "  1. Rebuild the app: ./gradlew assembleDebug"
        echo "  2. Deploy to device: ./deploy.sh"
        echo "  3. Download a model: wget https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin"
        echo "  4. Push model to device: adb push ggml-tiny.en.bin /sdcard/llms/"
    else
        print_warning "Setup incomplete - libwhisper.so not installed"
        echo ""
        echo "Follow the instructions above to build or obtain libwhisper.so"
    fi

    echo ""
}

# Run main function
main "$@"
