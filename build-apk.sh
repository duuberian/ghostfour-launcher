#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -f ./gradlew ]; then
  echo "gradlew not found. Trying to generate wrapper with local gradle..."
  if command -v gradle >/dev/null 2>&1; then
    gradle wrapper
  else
    echo "Error: gradle not installed and gradlew missing."
    echo "Install Gradle once, then re-run: ./build-apk.sh"
    exit 1
  fi
fi

chmod +x ./gradlew
./gradlew assembleDebug

echo "APK built: app/build/outputs/apk/debug/app-debug.apk"
