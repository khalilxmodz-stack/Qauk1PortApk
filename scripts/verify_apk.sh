#!/usr/bin/env bash
set -euo pipefail
APK="${1:?usage: verify_apk.sh app.apk}"
echo "== APK: $APK ($(stat -c%s "$APK") bytes)"
unzip -l "$APK" | grep -E 'lib/|classes[0-9]*\.dex|AndroidManifest' || true
need() { unzip -l "$APK" | grep -q "$1" || { echo "VERIFY FAILED: missing $1"; exit 1; }; }
for abi in armeabi-v7a arm64-v8a; do
  for so in libmain.so libSDL2.so libGL.so; do need "lib/$abi/$so"; done
done
need classes.dex; need AndroidManifest.xml
unzip -p "$APK" classes.dex | grep -aq "By KhalilXmodz.Port" || { echo "VERIFY FAILED: splash text not in dex"; exit 1; }
BT=$(ls -d "${ANDROID_HOME:-$ANDROID_SDK_ROOT}"/build-tools/* | sort -V | tail -1)
"$BT/apksigner" verify --verbose "$APK"
"$BT/aapt" dump badging "$APK" | grep -E "^package:|native-code|sdkVersion|launchable-activity" || true
echo "VERIFY OK"
