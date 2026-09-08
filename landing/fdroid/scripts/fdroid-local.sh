#!/usr/bin/env bash
set -euo pipefail

# Builds the githubRelease APK locally and updates the self-hosted F-Droid index.
# Usage: ./landing/fdroid/scripts/fdroid-local.sh

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

cd "$REPO_ROOT"

if ! command -v fdroid >/dev/null 2>&1; then
    echo "fdroidserver not found. Install it first:"
    echo "  python3 -m venv /tmp/fdroid-venv"
    echo "  /tmp/fdroid-venv/bin/pip install fdroidserver"
    exit 1
fi

if [ ! -f keystore.p12 ]; then
    echo "Creating F-Droid repo signing key..."
    fdroid update --create-key
fi

echo "Building githubRelease APK..."
"$REPO_ROOT/../../../gradlew" :app:assembleGithubRelease

VERSION_CODE=$(
    grep -E '^\s+versionCode\s*=' "$REPO_ROOT/../../../app/build.gradle.kts" |
        sed 's/.*=\s*//' | tr -d ' '
)
mkdir -p "repo"
cp "$REPO_ROOT/../../../app/build/outputs/apk/github/release/app-github-release.apk" \
    "repo/com.tdvorak.nothingmodes_${VERSION_CODE}.apk"

echo "Updating F-Droid index..."
if [ -x /home/tdvorak/Android/Sdk/build-tools/37.0.0/apksigner ]; then
    export PATH="/home/tdvorak/Android/Sdk/build-tools/37.0.0:$PATH"
fi
fdroid update

echo "Done. Deploy the landing/fdroid/repo directory to the configured repo URL."
