#!/usr/bin/env bash
# Costruisce la versione PC (Windows) di Grabatron.
# Requisiti: JDK 17+, curl, zip, python3 (per zig, compilatore del launcher .exe).
set -euo pipefail
cd "$(dirname "$0")"
OUT=out; PKG=$OUT/Grabatron
rm -rf $OUT && mkdir -p $OUT/classes $PKG/lib .cache

# 1) compila lo strato PC contro le classi originali del gioco e LWJGL
CP="vendor/grabatron-classes.jar:$(ls lib/*.jar | grep -v natives | tr '\n' ':')"
find src -name '*.java' > $OUT/sources.txt
javac -nowarn -encoding UTF-8 --release 17 -d $OUT/classes -cp "$CP" @$OUT/sources.txt

# 2) game.jar = classi originali + strato PC (che ha la precedenza)
mkdir -p $OUT/jarroot && (cd $OUT/jarroot && jar xf ../../vendor/grabatron-classes.jar)
cp -r $OUT/classes/. $OUT/jarroot/
rm -rf $OUT/jarroot/META-INF
for j in lwjgl lwjgl-glfw lwjgl-opengl lwjgl-openal lwjgl-stb; do cp lib/$j.jar lib/$j-natives-windows.jar $PKG/lib/; done
printf "Main-Class: desktop.Main\nClass-Path: %s\n" "$(cd $PKG && ls lib/*.jar | tr '\n' ' ')" > $OUT/manifest.txt
jar cfm $PKG/game.jar $OUT/manifest.txt -C $OUT/jarroot .

# 3) dati, Java per Windows, launcher
cp -r data $PKG/data && cp LEGGIMI.txt $PKG/
JRE_URL=https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.9%2B10/OpenJDK21U-jre_x64_windows_hotspot_21.0.9_10.zip
[ -f .cache/jre.zip ] || curl -sSL -o .cache/jre.zip "$JRE_URL"
mkdir -p $OUT/jre && unzip -q .cache/jre.zip -d $OUT/jre && mv $OUT/jre/* $PKG/jre
if [ -f launcher/Grabatron.exe ] && [ "${REBUILD_LAUNCHER:-0}" != 1 ]; then
  cp launcher/Grabatron.exe $PKG/
else
  python3 -m pip install --user -q ziglang
  (cd launcher && python3 -m ziglang cc -target x86_64-windows-gnu -O2 -municode -Wl,--subsystem,windows \
     -o Grabatron.exe launcher.c grabatron.rc -luser32 -lkernel32 && rm -f Grabatron.pdb)
  cp launcher/Grabatron.exe $PKG/
fi

# 4) zip finale
(cd $OUT && zip -qr -9 Grabatron_PC_Windows.zip Grabatron)
echo "Fatto: $OUT/Grabatron_PC_Windows.zip"
