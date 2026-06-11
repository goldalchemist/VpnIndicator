# VpnIndicator

# VPN Indicator for Android

A lightweight, free, ad-free Android overlay widget that displays a simple visual indicator showing whether a VPN is active on your device.

**👍 VPN Active | 👎 No VPN**

---

## The Story

Like many people, I relied on a third-party VPN indicator app that eventually stopped working. The replacement I found was riddled with ads and wanted a subscription just to tell me whether my VPN was on or off. That felt wrong — it's a one-line network check, not a service worth renting.

So I had an idea, described what I wanted, and built this using Claude AI (Anthropic) as the development tool. The concept, testing, and persistence through the build process were entirely human. The code was written with AI assistance. Either way — it exists, it works, it's free, and it always will be.

---

## Features

- 👍 / 👎 overlay widget visible over any app or home screen
- Works with **any VPN** — WireGuard, OpenVPN, PIA, NordVPN, built-in Android VPN, anything
- Polls every **3 seconds** — updates automatically
- **Draggable** — reposition anywhere on screen
- **Tap to dismiss**
- Runs as a persistent foreground service — survives app switching
- No ads. No subscriptions. No accounts. No tracking. No nonsense.

---

## Compatibility

Should work on any Android device running **Android 8.0 (Oreo) or higher**, including:

- 📺 Android TV devices (Nvidia Shield, etc.)
- 📱 Phones
- 📟 Tablets
- 📦 Streaming sticks and boxes

---

## Installation

### Option 1 — Sideload APK (easiest)
1. Download `VpnIndicator-v1.0-release.apk` from the [Releases](../../releases) page
2. On your device, enable **Install from unknown sources** (Settings → Security)
3. Copy the APK to your device and open it to install
4. Launch **VPN Indicator** from your app drawer
5. Grant the **"Display over other apps"** permission when prompted

### Option 2 — ADB install (Android TV / Nvidia Shield)
Enable Developer Options on your device first:
> Settings → Device Preferences → About → click *Build* 7 times → Developer Options → enable Network Device Debug

Then from a Linux/Windows machine with ADB installed:
```bash
adb connect <your-device-ip>:5555
adb install VpnIndicator-v1.0-release.apk
adb shell appops set com.g0xre.vpnindicator SYSTEM_ALERT_WINDOW allow
adb shell am start -n com.g0xre.vpnindicator/.MainActivity
```

### Option 3 — Build from source
See [Building from Source](#building-from-source) below.

---

## Permissions

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw the overlay widget over other apps |
| `ACCESS_NETWORK_STATE` | Detect VPN transport on active networks |
| `FOREGROUND_SERVICE` | Keep the service running in the background |
| `POST_NOTIFICATIONS` | Required foreground service notification (Android 13+) |

No internet permission. No location. No data collection of any kind.

---

## Building from Source

### Requirements
- Linux, Windows, or macOS
- Java 21 JDK
- Android SDK (downloaded automatically by build script on Linux)

### Quick build on Linux/Kali
```bash
git clone https://github.com/goldalchemist/VpnIndicator.git
cd vpn-indicator-android
chmod +x build.sh
./build.sh <your-device-ip>    # builds AND deploys to device
# or
./build.sh                     # build only
```

The `build.sh` script handles everything — Android SDK download, licence acceptance, compile, install, and permission granting.

### Manual build
```bash
# Set Java 21 and Android SDK path
echo "sdk.dir=$HOME/android-sdk" > local.properties
./gradlew clean assembleRelease --no-daemon
# APK at: app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## How It Works

The app uses Android's `ConnectivityManager` to check all active network interfaces every 3 seconds. If any interface reports `TRANSPORT_VPN` capability, the VPN is active and the widget shows 👍. Simple, reliable, zero fluff.

The overlay is drawn using `TYPE_APPLICATION_OVERLAY` — the standard Android system overlay API. The widget is a foreground service with a minimal notification (required by Android for persistent background services).

---

## Project Structure

```
vpn-indicator-android/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/g0xre/vpnindicator/
│   │   ├── MainActivity.java          # Permission request + service launcher
│   │   └── VpnOverlayService.java     # Overlay widget + VPN polling logic
│   └── res/
│       ├── layout/overlay_widget.xml  # Widget layout
│       ├── drawable/widget_background.xml
│       └── values/{strings,themes}.xml
├── build.sh                           # One-shot Linux build + deploy script
├── build.gradle
└── settings.gradle
```

---

## Contributing

PRs welcome. Ideas for future versions:
- Configurable poll interval
- Colour-coded indicator (green/red) as an option
- Notification-only mode (no overlay) for devices that don't support overlays
- Auto-start on boot

---

## Credits

Conceived by **G0XRE** (licensed amateur radio operator, Grimsby, UK).
Built with assistance from **Claude AI** by Anthropic.
Tested on Nvidia Shield (Android TV).

---

## Licence

GNU General Public License v3.0 — see [LICENSE](LICENSE) for full text.

Free to use, free to modify, free to share. Cannot be sold, rented, or relicensed as proprietary software. Free forever.
