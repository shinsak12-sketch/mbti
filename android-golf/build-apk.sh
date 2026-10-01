#!/usr/bin/env bash
# 골프 내기 APK 빌드 (Android SDK 없이: javac + dx + apktool + uber-apk-signer) · 도구는 ../android/tools 공유
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
TOOLS="${APK_TOOLS:-$HERE/../android/tools}"
OUT="${1:-$HERE/out}"
mkdir -p "$TOOLS" "$OUT"
dl(){ [ -s "$2" ] || curl -sS -L --retry 3 -o "$2" "$1"; }
dl https://raw.githubusercontent.com/Sable/android-platforms/master/android-33/android.jar "$TOOLS/android-33.jar"
dl https://github.com/iBotPeaches/Apktool/releases/download/v2.9.3/apktool_2.9.3.jar "$TOOLS/apktool.jar"
dl https://repo1.maven.org/maven2/com/jakewharton/android/repackaged/dalvik-dx/16.0.1/dalvik-dx-16.0.1.jar "$TOOLS/dx.jar"
dl https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar "$TOOLS/uber-apk-signer.jar"

WORK="$OUT/work"; rm -rf "$WORK"; mkdir -p "$WORK/classes"
javac --release 8 -Xlint:-options -cp "$TOOLS/android-33.jar" -d "$WORK/classes" $(find "$HERE/src" -name '*.java')
java -cp "$TOOLS/dx.jar" com.android.dx.command.Main --dex --min-sdk-version=24 --output="$WORK/classes.dex" "$WORK/classes"
rm -rf "$WORK/apk"; cp -r "$HERE/skel" "$WORK/apk"; cp "$WORK/classes.dex" "$WORK/apk/classes.dex"
WWW="$WORK/apk/assets/www"; mkdir -p "$WWW/icons"
cp "$HERE/../golf.html" "$WWW/index.html"; cp "$HERE/../golf.html" "$WWW/golf.html"; cp "$HERE/../golf.webmanifest" "$WWW/"
cp "$HERE"/../icons/golf-*.png "$WWW/icons/" 2>/dev/null || true
java -jar "$TOOLS/apktool.jar" b "$WORK/apk" --use-aapt2 -o "$WORK/unsigned.apk" -p "$TOOLS/framework" >/dev/null
java -jar "$TOOLS/uber-apk-signer.jar" -a "$WORK/unsigned.apk" -o "$OUT" --allowResign >/dev/null
mv -f "$OUT"/unsigned-aligned-debugSigned.apk "$OUT/golf-naegi.apk"
rm -f "$OUT"/unsigned-aligned-debugSigned.apk.idsig
ls -la "$OUT/golf-naegi.apk"
