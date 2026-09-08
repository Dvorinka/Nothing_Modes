#!/usr/bin/env bash
set -euo pipefail

# Regenerates the self-hosted F-Droid index for a GitHub release.
# Usage: ./landing/fdroid/scripts/fdroid-release.sh <versionName> <versionCode>

VERSION_NAME="${1:?versionName required}"
VERSION_CODE="${2:?versionCode required}"

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_ID="com.tdvorak.nothingmodes"
APK_NAME="NothingModes-github-release.apk"
APK_URL="https://github.com/tdvorak/Nothing_Modes/releases/download/v${VERSION_NAME}/${APK_NAME}"

cd "$REPO_ROOT"

if ! command -v fdroid >/dev/null 2>&1; then
  echo "fdroidserver not found. Install it first:"
  echo "  pip install fdroidserver"
  exit 1
fi

mkdir -p "unsigned"

echo "Downloading release APK..."
curl -L --fail -o "unsigned/${APK_NAME}" "$APK_URL"

# The APK is already release-signed. Place it where fdroid update will find it.
mkdir -p "repo"
cp "unsigned/${APK_NAME}" "repo/${APP_ID}_${VERSION_CODE}.apk"

echo "Updating F-Droid index..."
fdroid update

echo "Cleaning up downloaded APK (kept out of git)..."
rm -f "unsigned/${APK_NAME}"

echo "Done. Commit the generated index files and deploy the landing site."
