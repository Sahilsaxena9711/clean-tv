#!/usr/bin/env bash
# Usage:
#   ./debloat.sh <tv-ip> list            -> list installed packages
#   ./debloat.sh <tv-ip> disable <pkg>   -> disable a package (reversible)
#   ./debloat.sh <tv-ip> enable <pkg>    -> undo
# Review the list yourself before disabling anything. Never disable:
#   com.android.* system core, com.google.android.gms, com.google.android.tvlauncher
#   (keep the stock launcher disabled only AFTER Clean TV works), the TV's input/tuner/settings apps.
set -e
TV_IP="${1:?tv ip}"; ACTION="${2:?list|disable|enable}"; PKG="$3"
S="$TV_IP:5555"
adb connect "$S" >/dev/null
case "$ACTION" in
  list)    adb -s "$S" shell pm list packages -3 ; echo "--- system apps ---"; adb -s "$S" shell pm list packages -s ;;
  disable) adb -s "$S" shell pm disable-user --user 0 "$PKG" ;;
  enable)  adb -s "$S" shell pm enable "$PKG" ;;
esac
