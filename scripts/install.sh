#!/usr/bin/env bash
# Usage: ./install.sh <tv-ip> [path/to/app-release.apk]
set -e
TV_IP="${1:?Usage: ./install.sh <tv-ip> [apk]}"
APK="${2:-app/build/outputs/apk/release/app-release.apk}"

adb connect "$TV_IP:5555"
adb -s "$TV_IP:5555" install -r "$APK"

# Try to make it the default Home. If the TV refuses, press Home on the remote
# and choose "Clean TV" > Always.
adb -s "$TV_IP:5555" shell cmd package set-home-activity com.devsahil.cleantv/.MainActivity || \
  echo "Could not set default automatically. Press Home on the remote and pick Clean TV."
echo "Done."
