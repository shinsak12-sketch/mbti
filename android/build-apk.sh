#!/usr/bin/env bash
# 점심 복불복 APK 빌드 (Android SDK 없이: javac + dx + apktool + uber-apk-signer)
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
TOOLS="${APK_TOOLS:-$HERE/tools}"
OUT="${1:-$HERE/out}"
mkdir -p "$TOOLS" "$OUT"
dl(){ [ -s "$2" ] || curl -sS -L --retry 3 -o "$2" "$1"; }
dl https://raw.githubusercontent.com/Sable/android-platforms/master/android-33/android.jar "$TOOLS/android-33.jar"
dl https://github.com/iBotPeaches/Apktool/releases/download/v2.9.3/apktool_2.9.3.jar "$TOOLS/apktool.jar"
dl https://repo1.maven.org/maven2/com/jakewharton/android/repackaged/dalvik-dx/16.0.1/dalvik-dx-16.0.1.jar "$TOOLS/dx.jar"
dl https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar "$TOOLS/uber-apk-signer.jar"

WORK="$OUT/work"; rm -rf "$WORK"; mkdir -p "$WORK/classes"
# 1) Java -> class (Java 8 바이트코드, dx 호환)
javac --release 8 -Xlint:-options -cp "$TOOLS/android-33.jar" -d "$WORK/classes" $(find "$HERE/src" -name '*.java')
# 2) class -> classes.dex
java -cp "$TOOLS/dx.jar" com.android.dx.command.Main --dex --min-sdk-version=24 --output="$WORK/classes.dex" "$WORK/classes"
# 3) apktool 스켈레톤 구성 (매니페스트/리소스/assets + dex)
rm -rf "$WORK/apk"; cp -r "$HERE/skel" "$WORK/apk"; cp "$WORK/classes.dex" "$WORK/apk/classes.dex"
# 웹 게임 파일 동기화 (repo 루트의 게임 페이지들)
WWW="$WORK/apk/assets/www"; mkdir -p "$WWW/icons"
for f in lunch.html seotda.html pan.html archery.html race.html; do [ -f "$HERE/../$f" ] && cp "$HERE/../$f" "$WWW/"; done
[ -f "$WWW/lunch.html" ] && cp "$WWW/lunch.html" "$WWW/index.html"
cp "$HERE"/../icons/*.png "$WWW/icons/" 2>/dev/null || true
mkdir -p "$WWW/bgm"; cp "$HERE"/../bgm/*.mp3 "$WWW/bgm/" 2>/dev/null || true
# 4) apktool build (내장 aapt2)
java -jar "$TOOLS/apktool.jar" b "$WORK/apk" --use-aapt2 -o "$WORK/unsigned.apk" -p "$TOOLS/framework" >/dev/null
# 5) 정렬 + v1/v2/v3 서명 (디버그 키 자동 생성)
java -jar "$TOOLS/uber-apk-signer.jar" -a "$WORK/unsigned.apk" -o "$OUT" --allowResign >/dev/null
mv -f "$OUT"/unsigned-aligned-debugSigned.apk "$OUT/lunch-bokbulbok.apk"
rm -f "$OUT"/unsigned-aligned-debugSigned.apk.idsig
ls -la "$OUT/lunch-bokbulbok.apk"
