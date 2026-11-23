#!/usr/bin/env bash
# Compila el APK sin Android Studio ni el SDK oficial.
# Requisitos (Ubuntu): apt-get install aapt zipalign apksigner dalvik-exchange default-jdk
# y el jar de plataforma de Robolectric (android-all 14), que trae clases y recursos de Android:
#   https://repo1.maven.org/maven2/org/robolectric/android-all/14-robolectric-10818077/android-all-14-robolectric-10818077.jar
# Uso: ANDROID_JAR=/ruta/android-all.jar KEYSTORE=/ruta/plan.jks KS_PASS=clave ./build.sh
set -euo pipefail
APP="$(cd "$(dirname "$0")" && pwd)"
OUT="${OUT:-/tmp/futbol-plan-build}"
: "${ANDROID_JAR:?Falta ANDROID_JAR}"
: "${KEYSTORE:?Falta KEYSTORE}"
: "${KS_PASS:?Falta KS_PASS}"

rm -rf "$OUT"; mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex"

aapt2 compile --dir "$APP/res" -o "$OUT/res.zip"
aapt2 link -I "$ANDROID_JAR" --manifest "$APP/AndroidManifest.xml" -A "$APP/assets" \
  --java "$OUT/gen" -o "$OUT/base.apk" "$OUT/res.zip"

javac --release 8 -nowarn -Xlint:-options -encoding UTF-8 -cp "$ANDROID_JAR" -d "$OUT/classes" \
  $(find "$APP/src" "$OUT/gen" -name '*.java')
dalvik-exchange --dex --min-sdk-version=26 --output="$OUT/dex/classes.dex" "$OUT/classes"

cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && zip -q "$OUT/unsigned.apk" classes.dex)
zipalign -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
apksigner sign --ks "$KEYSTORE" --ks-pass "pass:$KS_PASS" --out "$OUT/PlanVerano.apk" "$OUT/aligned.apk"
apksigner verify --verbose "$OUT/PlanVerano.apk" | head -5
echo "APK: $OUT/PlanVerano.apk"
