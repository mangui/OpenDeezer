#!/usr/bin/env bash
# Reproducible Portal Go release build with a permanent package ID and signer.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

: "${ANDROID_KEYSTORE_FILE:?Path to the private Portal Go PKCS12 keystore is required}"
: "${ANDROID_KEYSTORE_PASSWORD:?Keystore password is required}"
: "${PORTAL_VERSION_CODE:?Set an integer greater than the last installed versionCode}"

if [[ ! -f "$ANDROID_KEYSTORE_FILE" || ! "$PORTAL_VERSION_CODE" =~ ^[0-9]+$ ]]; then
  echo "Invalid keystore path or PORTAL_VERSION_CODE" >&2
  exit 2
fi

export ANDROID_KEY_ALIAS="${ANDROID_KEY_ALIAS:-portalgo}"
export ANDROID_KEY_PASSWORD="${ANDROID_KEY_PASSWORD:-$ANDROID_KEYSTORE_PASSWORD}"

extra_gradle_args=()
# Offline developer fallback only: release Lint requires uncached Google Maven
# dependencies in the local toolchain. CI should keep Lint enabled.
if [[ "${PORTAL_OFFLINE_SKIP_LINT:-false}" == "true" ]]; then
  extra_gradle_args=(
    -x lintVitalAnalyzeMobileRelease
    -x lintVitalReportMobileRelease
    -x lintVitalMobileRelease
  )
fi

(
  cd "$ROOT/gui/android"
  "${GRADLE_BIN:-./gradlew}" --no-daemon -PupstreamUpdates=false \
    -PportalBuild=true -PportalVersionCode="$PORTAL_VERSION_CODE" \
    assembleMobileRelease "${extra_gradle_args[@]}"
)

sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$sdk" ]]; then
  echo "ANDROID_SDK_ROOT or ANDROID_HOME must be set to verify the APK" >&2
  exit 2
fi

apk="$ROOT/gui/android/app/build/outputs/apk/mobile/release/app-mobile-release.apk"
apksigner="$sdk/build-tools/34.0.0/apksigner"
aapt="$sdk/build-tools/34.0.0/aapt"
expected="a0db72939c78c981274e90b262fa0ec50e6d8c7e32ebdc974a207f055f9ab4e9"
actual="$("$apksigner" verify --print-certs "$apk" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')"
if [[ "$actual" != "$expected" ]]; then
  echo "Unexpected APK signing certificate (not the stable Portal Go key)" >&2
  exit 1
fi

badge="$("$aapt" dump badging "$apk" | head -n 1)"
if [[ "$badge" != *"name='fr.cyclooo.opendeezer.familyportal'"* ||
      "$badge" != *"versionCode='$PORTAL_VERSION_CODE'"* ]]; then
  echo "Unexpected package ID or versionCode: $badge" >&2
  exit 1
fi

echo "Signed, verified Portal Go APK: $apk"
echo "$badge"
