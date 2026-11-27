#!/usr/bin/env bash
# Verify exact signed outputs, signer identity, alignment, permissions and size.
set -euo pipefail
: "${ANDROID_HOME:?Android SDK is required}"
: "${EXPECTED_SIGNER_SHA256:?Expected keystore certificate fingerprint is required}"
export PATH="$ANDROID_HOME/build-tools/37.0.0:$PATH"
mkdir -p release-apks
for flavor in offline network; do
  apk="app/build/outputs/apk/$flavor/release/app-$flavor-release.apk"
  test -s "$apk" || { echo "::error::Missing signed APK: $apk"; exit 1; }
  report=$(apksigner verify --verbose --print-certs --min-sdk-version 28 "$apk")
  echo "$report"
  actual=$(printf '%s\n' "$report" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')
  test "${actual,,}" = "${EXPECTED_SIGNER_SHA256,,}" || { echo '::error::Unexpected signing certificate'; exit 1; }
  zipalign -c -v 4 "$apk"
  permissions=$(aapt dump permissions "$apk")
  if [[ "$flavor" == offline ]]; then
    if grep -q 'android.permission.INTERNET' <<< "$permissions"; then
      echo '::error::Offline APK declares INTERNET'; exit 1
    fi
  else
    grep -q 'android.permission.INTERNET' <<< "$permissions"
  fi
  test "$(stat -c%s "$apk")" -le 31457280
  cp "$apk" "release-apks/HyperLauncherAI-$flavor-release.apk"
  echo "::notice::Verified $flavor: signed by expected key, aligned, permissions correct, under 30 MiB."
done
(cd release-apks && sha256sum ./*.apk > SHA256SUMS.txt)
