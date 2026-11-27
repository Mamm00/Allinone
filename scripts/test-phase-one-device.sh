#!/usr/bin/env bash
set -euo pipefail
if ./gradlew :app:connectedOfflineDebugAndroidTest --stacktrace > /tmp/phase-one-device.log 2>&1; then
  tail -30 /tmp/phase-one-device.log
else
  tail -100 /tmp/phase-one-device.log | while IFS= read -r line; do echo "::error::$line"; done
  find app/build/outputs/androidTest-results -name '*.xml' -exec cat {} \; 2>/dev/null | head -200 | while IFS= read -r line; do echo "::error::$line"; done
  exit 1
fi
adb shell am start -W -n com.hyperlauncher.ai.debug/com.hyperlauncher.ai.MainActivity
adb shell input keyevent KEYCODE_HOME
adb shell am start -W -n com.hyperlauncher.ai.debug/com.hyperlauncher.ai.MainActivity
adb shell pidof com.hyperlauncher.ai.debug
# Exercise recreation in landscape then portrait. UI assertions are in DrawerUiTest.
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
adb shell am start -W -n com.hyperlauncher.ai.debug/com.hyperlauncher.ai.MainActivity
adb shell settings put system user_rotation 0
adb shell pidof com.hyperlauncher.ai.debug
if adb logcat -d -s AndroidRuntime:E | grep -q 'Process: com.hyperlauncher.ai.debug'; then
  echo '::error::Launcher crashed during device checks'; exit 1
fi
echo '::notice::Drawer UI tests, Room v1-to-v2 migration, and launcher smoke checks passed on Android 14.'
