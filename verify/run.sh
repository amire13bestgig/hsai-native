#!/usr/bin/env bash
# بازبینیِ اجراییِ قراردادهای کلاینت (بدون نیاز به Android SDK).
# استفاده:  KOTLINC=/path/to/kotlinc bash run.sh
# پیش‌فرض‌ها: vendor/json-20240303.jar برای org.json و kotlin-stdlib کنار kotlinc.
set -u
here="$(cd "$(dirname "$0")" && pwd)"
# مسیر سورس: هم در چیدمان کاری (src/hsai-native) و هم در بستهٔ تحویلی (hsai-native/) کار می‌کند.
REL="app/src/main/java/ir/hsai/mobile"
SRC=""
for c in "$here/../src/hsai-native/$REL" "$here/../hsai-native/$REL" "$here/../$REL"; do
  if [ -d "$c" ]; then SRC="$c"; break; fi
done
if [ -z "$SRC" ]; then echo "پوشهٔ سورس اپ پیدا نشد؛ SRC را در اسکریپت تنظیم کنید."; exit 2; fi
KOTLINC="${KOTLINC:-kotlinc}"
JSON_JAR="${JSON_JAR:-$(ls "$here"/vendor/json-*.jar 2>/dev/null | head -1)}"
STDLIB="${STDLIB:-}"

if [ -z "$JSON_JAR" ] || [ ! -f "$JSON_JAR" ]; then
  echo "json jar پیدا نشد؛ آن را در vendor/json-20240303.jar بگذارید یا JSON_JAR را تنظیم کنید."; exit 2
fi
if [ -z "$STDLIB" ]; then
  STDLIB="$(dirname "$(readlink -f "$KOTLINC")")/../lib/kotlin-stdlib.jar"
fi
if [ ! -f "$STDLIB" ]; then
  echo "kotlin-stdlib پیدا نشد؛ STDLIB=/path/kotlin-stdlib.jar را تنظیم کنید."; exit 2
fi

mkdir -p "$here/out"
"$KOTLINC" -nowarn -cp "$JSON_JAR" -d "$here/out" \
  "$SRC/ApiContract.kt" "$SRC/UiPresentation.kt" "$SRC/UiExtras.kt" "$SRC/Domain.kt" "$SRC/JsonExt.kt" "$SRC/ApiRoutes.kt" "$here/Harness.kt" || exit 3
java -Dfile.encoding=UTF-8 -cp "$here/out:$JSON_JAR:$STDLIB" ir.hsai.mobile.HarnessKt
