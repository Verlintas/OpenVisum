#!/usr/bin/env bash
# Embeds the libVLC runtime (dylibs + plugins) into a built OpenVisum.app so it
# no longer depends on macos/Vendor at runtime.
set -euo pipefail

cd "$(dirname "$0")/.."
APP="${1:-build/Build/Products/Debug/OpenVisum.app}"
VLC="Vendor/VLC.app/Contents/MacOS"

[ -d "$APP" ] || { echo "app not found: $APP"; exit 1; }
[ -d "$VLC" ] || { echo "VLC runtime missing, run Scripts/fetch-vlc.sh first"; exit 1; }

FRAMEWORKS="$APP/Contents/Frameworks"
RESOURCES="$APP/Contents/Resources/vlc"
mkdir -p "$FRAMEWORKS" "$RESOURCES"

cp "$VLC/lib/libvlc.5.dylib" "$FRAMEWORKS/"
cp "$VLC/lib/libvlccore.9.dylib" "$FRAMEWORKS/"
ln -sf libvlc.5.dylib "$FRAMEWORKS/libvlc.dylib"
ln -sf libvlccore.9.dylib "$FRAMEWORKS/libvlccore.dylib"

rm -rf "$RESOURCES/plugins"
mkdir -p "$RESOURCES"
cp -R "$VLC/plugins" "$RESOURCES/plugins"

# Make the bundled dylibs relocatable and signed.
for lib in "$FRAMEWORKS"/*.dylib; do
  [ -L "$lib" ] && continue
  codesign --force --sign - "$lib" >/dev/null 2>&1 || true
done
codesign --force --deep --sign - "$APP" >/dev/null 2>&1 || true

echo "libVLC embedded into $APP"
