#!/usr/bin/env bash
set -e

SHIELD_IP="${1:-192.168.1.17}"
SDK_DIR="$HOME/android-sdk"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APK_OUT="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE="com.g0xre.vpnindicator"
SDK_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
BUILD_TOOLS_VERSION="34.0.0"
PLATFORM_VERSION="android-34"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'
info()    { echo -e "${CYAN}[*]${NC} $*"; }
success() { echo -e "${GREEN}[+]${NC} $*"; }
warn()    { echo -e "${YELLOW}[!]${NC} $*"; }
die()     { echo -e "${RED}[✗]${NC} $*"; exit 1; }

echo -e "${BOLD}"
echo "  ╔══════════════════════════════════════╗"
echo "  ║   VPN Indicator — Build & Deploy     ║"
echo "  ║   G0XRE                              ║"
echo "  ╚══════════════════════════════════════╝"
echo -e "${NC}"

info "Checking dependencies..."
MISSING=()
for cmd in java wget unzip adb; do
    command -v "$cmd" &>/dev/null || MISSING+=("$cmd")
done
if [[ ${#MISSING[@]} -gt 0 ]]; then
    warn "Installing: ${MISSING[*]}"
    sudo apt-get update -qq && sudo apt-get install -y -qq "${MISSING[@]}" openjdk-17-jdk-headless
fi
success "Java: $(java -version 2>&1 | head -1)"

if [[ ! -f "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]]; then
    info "Downloading Android SDK command-line tools..."
    mkdir -p "$SDK_DIR/cmdline-tools"
    wget -q --show-progress "$SDK_TOOLS_URL" -O /tmp/cmdline-tools.zip
    unzip -q /tmp/cmdline-tools.zip -d "$SDK_DIR/cmdline-tools"
    [[ -d "$SDK_DIR/cmdline-tools/cmdline-tools" ]] && mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
    rm -f /tmp/cmdline-tools.zip
    success "SDK tools downloaded"
else
    success "SDK tools already present"
fi

export ANDROID_HOME="$SDK_DIR"
export PATH="$SDK_DIR/cmdline-tools/latest/bin:$SDK_DIR/platform-tools:$SDK_DIR/build-tools/$BUILD_TOOLS_VERSION:$PATH"

if [[ ! -d "$SDK_DIR/platforms/$PLATFORM_VERSION" ]] || [[ ! -d "$SDK_DIR/build-tools/$BUILD_TOOLS_VERSION" ]]; then
    info "Installing platform and build tools (first time — few minutes)..."
    yes | sdkmanager --licenses --sdk_root="$SDK_DIR" > /dev/null 2>&1 || true
    sdkmanager "platforms;$PLATFORM_VERSION" "build-tools;$BUILD_TOOLS_VERSION" "platform-tools" --sdk_root="$SDK_DIR"
    success "SDK components installed"
else
    success "SDK components already present"
fi

info "Building APK..."
cd "$PROJECT_DIR"
echo "sdk.dir=$SDK_DIR" > local.properties
chmod +x gradlew 2>/dev/null || true
./gradlew clean assembleDebug --no-daemon 2>&1 | tail -20

[[ -f "$APK_OUT" ]] || die "Build failed — APK not found"
success "APK built — $(du -sh "$APK_OUT" | cut -f1)"

info "Connecting to Shield at $SHIELD_IP:5555..."
adb connect "$SHIELD_IP:5555" || die "ADB connect failed"
sleep 2

info "Installing APK..."
adb -s "$SHIELD_IP:5555" install -r "$APK_OUT" || die "Install failed"
success "APK installed"

info "Granting permissions..."
adb -s "$SHIELD_IP:5555" shell appops set "$PACKAGE" SYSTEM_ALERT_WINDOW allow
adb -s "$SHIELD_IP:5555" shell pm grant "$PACKAGE" android.permission.POST_NOTIFICATIONS 2>/dev/null || true
success "Permissions granted"

info "Launching..."
adb -s "$SHIELD_IP:5555" shell am start -n "$PACKAGE/.MainActivity"

echo -e "\n${GREEN}${BOLD}All done! 👍 VPN active / 👎 No VPN — tap widget to dismiss${NC}\n"
