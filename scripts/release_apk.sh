#!/usr/bin/env bash
set -euo pipefail

# ISCEB Single-Command APK Build, Dual-Scheme Sign (v1+v2+v3), Manifest Update & Deploy Script
# Usage:
#   ./scripts/release_apk.sh [release_notes] [version_code_override]

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

HOSTING_BASE_URL="${ISCEB_HOSTING_BASE_URL:-https://gen-lang-client-0901479759.web.app}"
HOSTING_BASE_URL="${HOSTING_BASE_URL%/}"
RELEASE_NOTES="${1:-ISCEB v1.6: Universal v1/v2/v3 APK signing compatibility, pre-update workspace backup, QR web download portal & automated release sync.}"
VERSION_CODE_OVERRIDE="${2:-}"
RELEASE_DATE="$(date +%Y-%m-%d)"

echo "==> Reading versionName & versionCode from app/build.gradle.kts..."
VERSION_CODE=$(grep -E '^\s*versionCode\s*=' app/build.gradle.kts | head -n1 | sed -E 's/[^0-9]*([0-9]+).*/\1/')
VERSION_NAME=$(grep -E '^\s*versionName\s*=' app/build.gradle.kts | head -n1 | sed -E 's/.*"([^"]+)".*/\1/')

if [[ -n "$VERSION_CODE_OVERRIDE" ]]; then
  VERSION_CODE="$VERSION_CODE_OVERRIDE"
fi

if [[ -z "$VERSION_CODE" || -z "$VERSION_NAME" ]]; then
  echo "ERROR: Could not parse versionCode / versionName from app/build.gradle.kts"
  exit 1
fi

echo "==> Building ISCEB v${VERSION_NAME} (Build ${VERSION_CODE}) for ${HOSTING_BASE_URL}..."
gradle :app:assembleDebug "-PISCEB_HOSTING_BASE_URL=${HOSTING_BASE_URL}" "-PISCEB_VERSION_CODE=${VERSION_CODE}"

BUILT_APK="app/build/outputs/apk/debug/app-debug.apk"
if [[ ! -f "$BUILT_APK" ]]; then
  echo "ERROR: Built APK not found at $BUILT_APK"
  exit 1
fi

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/opt/android/sdk}}"
ZIPALIGN="$(find "$SDK_ROOT/build-tools" -maxdepth 2 -name zipalign 2>/dev/null | sort -V | tail -n1 || true)"
APKSIGNER="$(find "$SDK_ROOT/build-tools" -maxdepth 2 -name apksigner 2>/dev/null | sort -V | tail -n1 || true)"

mkdir -p public/downloads
STAGED_APK="$(mktemp -u /tmp/isceb-aligned-XXXXXX.apk)"
SIGNED_APK="$(mktemp -u /tmp/isceb-signed-XXXXXX.apk)"

if [[ -n "$ZIPALIGN" && -n "$APKSIGNER" && -f "debug.keystore" ]]; then
  echo "==> Aligning & signing APK (v1 + v2 + v3) with permanent debug.keystore..."
  "$ZIPALIGN" -f -p 4 "$BUILT_APK" "$STAGED_APK"
  "$APKSIGNER" sign \
    --ks "debug.keystore" \
    --ks-key-alias "androiddebugkey" \
    --ks-pass "pass:android" \
    --key-pass "pass:android" \
    --v1-signing-enabled true \
    --v2-signing-enabled true \
    --v3-signing-enabled true \
    --out "$SIGNED_APK" \
    "$STAGED_APK"
  echo "==> Verifying APK signature..."
  "$APKSIGNER" verify --verbose --print-certs "$SIGNED_APK"
  cp "$SIGNED_APK" "ISCEB.apk"
  cp "$SIGNED_APK" "public/downloads/ISCEB-latest.apk"
  rm -f "$STAGED_APK" "$SIGNED_APK" "${SIGNED_APK}.idsig"
else
  echo "WARN: zipalign/apksigner not located; copying Gradle-signed APK directly."
  cp "$BUILT_APK" "ISCEB.apk"
  cp "$BUILT_APK" "public/downloads/ISCEB-latest.apk"
fi

SHA256=$(sha256sum "public/downloads/ISCEB-latest.apk" | awk '{print $1}')
SIZE_BYTES=$(wc -c < "public/downloads/ISCEB-latest.apk" | tr -d ' ')

echo "==> Updating public/version.json..."
ISCEB_RELEASE_NOTES="${RELEASE_NOTES}" python3 - <<PY
import json
import os

release_notes = os.environ.get("ISCEB_RELEASE_NOTES", "")
version_name = "${VERSION_NAME}"
version_code = int("${VERSION_CODE}")
hosting_base = "${HOSTING_BASE_URL}"

manifest = {
    "appId": "com.aistudio.supplychain.scm",
    "appName": "ISCEB",
    "versionName": version_name,
    "versionCode": version_code,
    "releaseDate": "${RELEASE_DATE}",
    "apkUrl": f"{hosting_base}/downloads/ISCEB-latest.apk",
    "downloadPageUrl": f"{hosting_base}/",
    "desktopPortalUrl": f"{hosting_base}/desktop/",
    "apkFileName": "ISCEB-latest.apk",
    "sha256": "${SHA256}",
    "fileSizeBytes": int("${SIZE_BYTES}"),
    "minSupportedVersionCode": 1,
    "releaseNotes": release_notes,
    "versionTag": f"v{version_name}.0" if version_name.count(".") == 1 else f"v{version_name}",
    "latestApkVersionCode": version_code,
    "latestApkVersionName": version_name,
    "apkDownloadUrl": f"{hosting_base}/downloads/ISCEB-latest.apk",
    "apkReleaseNotes": release_notes,
}

with open("public/version.json", "w", encoding="utf-8") as f:
    json.dump(manifest, f, indent=2)
    f.write("\n")
PY

echo "==> Release artifacts prepared:"
echo "    - APK: public/downloads/ISCEB-latest.apk (${SIZE_BYTES} bytes)"
echo "    - SHA-256: ${SHA256}"
echo "    - Manifest: public/version.json (v${VERSION_NAME}, Build ${VERSION_CODE})"

if [[ "${SKIP_FIREBASE_DEPLOY:-0}" != "1" ]] && command -v firebase >/dev/null 2>&1; then
  if firebase login:list 2>&1 | grep -q "@" || [[ -n "${FIREBASE_TOKEN:-}" ]]; then
    echo "==> Deploying to Firebase Hosting (${HOSTING_BASE_URL})..."
    firebase deploy --only hosting
    echo "==> Deployment complete! Permanent URL: ${HOSTING_BASE_URL}/"
  else
    echo "NOTE: Firebase CLI is not logged in inside this environment."
    echo "Push to GitHub for automatic GitHub Pages + Release deploy, or run 'firebase login && firebase deploy --only hosting'."
  fi
fi
