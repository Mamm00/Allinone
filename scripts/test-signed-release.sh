#!/usr/bin/env bash
# Upgrade from the established permanent signing identity, launch both editions,
# exercise HOME and recreation, and fail on crashes. Does not claim physical QA.
set -euo pipefail
for flavor in offline network; do
  package=com.hyperlauncher.ai
  [[ "$flavor" == offline ]] || package+=.network
  adb install --no-streaming "baseline-release/HyperLauncherAI-$flavor-release.apk"
  adb install --no-streaming -r "release-apks/HyperLauncherAI-$flavor-release.apk"
  adb logcat -c
  adb shell am start -W -n "$package/com.hyperlauncher.ai.MainActivity"
  adb shell cmd package set-home-activity "$package/com.hyperlauncher.ai.MainActivity"
  adb shell input keyevent KEYCODE_HOME
  adb shell settings put system accelerometer_rotation 0
  adb shell settings put system user_rotation 1
  adb shell am start -W -n "$package/com.hyperlauncher.ai.MainActivity"
  adb shell settings put system user_rotation 0
  adb shell am force-stop "$package"
  adb shell am start -W -n "$package/com.hyperlauncher.ai.MainActivity"
  adb shell pidof "$package"
  if adb logcat -d -s AndroidRuntime:E | grep -Fq "Process: $package"; then
    echo "::error::Signed $flavor build crashed during launch/recreation"; exit 1
  fi
  echo "::notice::Signed $flavor build upgraded from v1.0.1, launched and survived recreation on API 34."
done
