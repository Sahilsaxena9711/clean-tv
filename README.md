# Clean TV launcher

A minimal Android TV home screen: clock, one row of favorites, an All apps grid. No network calls, no recommendation rows, no background services.

Tested target: Nokia 50UHDADNDT8P (Android TV 11, API 30).

## Build
1. Install Android Studio (Koala or newer).
2. Open this folder, let Gradle sync.
3. Build > Generate Signed Bundle / APK > APK > release (or run `./gradlew assembleRelease` after adding the Gradle wrapper via Android Studio).
4. APK lands in `app/build/outputs/apk/release/`.

## Install on the TV
1. TV: Settings > Device Preferences > About > tap Build 7 times.
2. TV: Settings > Device Preferences > Developer options > turn on USB debugging (and network/ADB debugging if listed).
3. Find the TV's IP: Settings > Network.
4. Computer (same Wi-Fi): `./scripts/install.sh <tv-ip>`
5. Accept the "Allow debugging" prompt on the TV.
6. Press Home on the remote, choose Clean TV, tap Always.

If Home does not offer a choice, run `adb shell cmd package set-home-activity com.devsahil.cleantv/.MainActivity`.

## Use
- D-pad to move, OK to open.
- Hold OK on a tile to pin or unpin it.
- All apps opens the full alphabetical grid; Back returns home.
- Every launch first asks Android to drop cached background processes.

## Debloat (optional, reversible)
`./scripts/debloat.sh <tv-ip> list`, then disable only apps you recognise. Undo with `enable`.

## Limits
Without root the launcher can only kill cached background processes, not force-stop system services. Disabling via ADB is what actually stops preinstalled apps from running.
