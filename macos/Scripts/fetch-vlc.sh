#!/usr/bin/env bash
# Downloads VLC (universal) and the libVLC headers into macos/Vendor.
# VLC.app is used as the libVLC runtime for development builds and is not
# committed to the repository.
set -euo pipefail

cd "$(dirname "$0")/.."
VLC_VERSION="${VLC_VERSION:-3.0.24}"
MIRROR="${VLC_MIRROR:-https://mirrors.tuna.tsinghua.edu.cn/videolan-ftp/vlc}"

mkdir -p Vendor
if [ ! -d Vendor/VLC.app ]; then
  echo "downloading VLC $VLC_VERSION (universal)…"
  curl -L --fail -o /tmp/vlc.dmg \
    "$MIRROR/$VLC_VERSION/macosx/vlc-$VLC_VERSION-universal.dmg"
  MOUNT=$(mktemp -d)
  hdiutil attach /tmp/vlc.dmg -nobrowse -readonly -mountpoint "$MOUNT" >/dev/null
  cp -R "$MOUNT/VLC.app" Vendor/VLC.app
  hdiutil detach "$MOUNT" >/dev/null
  rm -f /tmp/vlc.dmg
fi

if [ ! -d Vendor/include/vlc ]; then
  echo "downloading libVLC headers…"
  curl -L --fail -o /tmp/vlc-src.tar.xz \
    "$MIRROR/$VLC_VERSION/vlc-$VLC_VERSION.tar.xz"
  mkdir -p /tmp/vlc-headers
  tar -xf /tmp/vlc-src.tar.xz -C /tmp/vlc-headers \
    --strip-components=1 "vlc-$VLC_VERSION/include/vlc"
  cp -R /tmp/vlc-headers/include/vlc Vendor/include/
  rm -rf /tmp/vlc-src.tar.xz /tmp/vlc-headers
fi

echo "VLC runtime ready at macos/Vendor/VLC.app"
