#!/usr/bin/env bash
# دانلود وابستگی‌های لازم برای typecheck.sh (یک‌بار؛ حدود ۹۰ مگابایت)
# android.jar (Platform 35) + Compose 1.7.6 + Material3 1.3.1 + activity/lifecycle/work/core/okhttp
set -u
DEPS="${DEPS:-$(cd "$(dirname "$0")" && pwd)/deps}"
mkdir -p "$DEPS/aars" "$DEPS/jars" "$DEPS/sdk"

GOOGLE=https://dl.google.com/dl/android/maven2
CENTRAL=https://repo1.maven.org/maven2

echo "== android.jar (Platform 35)"
if [ ! -f "$DEPS/sdk/android-35/android.jar" ]; then
  curl -sfL -o "$DEPS/platform.zip" "https://dl.google.com/android/repository/platform-35_r02.zip"
  unzip -q -o "$DEPS/platform.zip" "*/android.jar" -d "$DEPS/sdk"
  rm -f "$DEPS/platform.zip"
fi

echo "== androidx (google maven)"
cat > "$DEPS/coords.txt" <<'EOF'
androidx/compose/ui/ui-android/1.7.6/ui-android-1.7.6.aar
androidx/compose/ui/ui-graphics-android/1.7.6/ui-graphics-android-1.7.6.aar
androidx/compose/ui/ui-text-android/1.7.6/ui-text-android-1.7.6.aar
androidx/compose/ui/ui-unit-android/1.7.6/ui-unit-android-1.7.6.aar
androidx/compose/ui/ui-geometry-android/1.7.6/ui-geometry-android-1.7.6.aar
androidx/compose/ui/ui-util-android/1.7.6/ui-util-android-1.7.6.aar
androidx/compose/foundation/foundation-android/1.7.6/foundation-android-1.7.6.aar
androidx/compose/foundation/foundation-layout-android/1.7.6/foundation-layout-android-1.7.6.aar
androidx/compose/material3/material3-android/1.3.1/material3-android-1.3.1.aar
androidx/compose/material/material-icons-core-android/1.7.6/material-icons-core-android-1.7.6.aar
androidx/compose/runtime/runtime-android/1.7.6/runtime-android-1.7.6.aar
androidx/compose/runtime/runtime-saveable-android/1.7.6/runtime-saveable-android-1.7.6.aar
androidx/activity/activity-compose/1.9.3/activity-compose-1.9.3.aar
androidx/activity/activity/1.9.3/activity-1.9.3.aar
androidx/lifecycle/lifecycle-runtime-compose-android/2.8.7/lifecycle-runtime-compose-android-2.8.7.aar
androidx/lifecycle/lifecycle-runtime-ktx-android/2.8.7/lifecycle-runtime-ktx-android-2.8.7.aar
androidx/lifecycle/lifecycle-viewmodel-compose-android/2.8.7/lifecycle-viewmodel-compose-android-2.8.7.aar
androidx/lifecycle/lifecycle-viewmodel-android/2.8.7/lifecycle-viewmodel-android-2.8.7.aar
androidx/lifecycle/lifecycle-viewmodel-savedstate/2.8.7/lifecycle-viewmodel-savedstate-2.8.7.aar
androidx/lifecycle/lifecycle-runtime-android/2.8.7/lifecycle-runtime-android-2.8.7.aar
androidx/core/core-ktx/1.15.0/core-ktx-1.15.0.aar
androidx/core/core/1.15.0/core-1.15.0.aar
androidx/work/work-runtime-ktx/2.10.0/work-runtime-ktx-2.10.0.aar
androidx/work/work-runtime/2.10.0/work-runtime-2.10.0.aar
androidx/exifinterface/exifinterface/1.4.2/exifinterface-1.4.2.aar
androidx/savedstate/savedstate/1.2.1/savedstate-1.2.1.aar
androidx/concurrent/concurrent-futures/1.1.0/concurrent-futures-1.1.0.aar
androidx/annotation/annotation-jvm/1.9.1/annotation-jvm-1.9.1.jar
androidx/collection/collection-jvm/1.4.5/collection-jvm-1.4.5.jar
androidx/lifecycle/lifecycle-common-jvm/2.8.7/lifecycle-common-jvm-2.8.7.jar
EOF
while read -r p; do
  f="$DEPS/aars/$(basename "$p")"
  [ -f "$f" ] || curl -sfL -o "$f" "$GOOGLE/$p" || echo "FAIL $p"
done < "$DEPS/coords.txt"

echo "== کتابخانه‌های JVM (maven central)"
for u in \
  "$CENTRAL/com/squareup/okhttp3/okhttp/4.12.0/okhttp-4.12.0.jar" \
  "$CENTRAL/com/squareup/okio/okio-jvm/3.6.0/okio-jvm-3.6.0.jar" \
  "$CENTRAL/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.9.0/kotlinx-coroutines-core-jvm-1.9.0.jar" \
  "$CENTRAL/org/jetbrains/kotlinx/kotlinx-coroutines-android/1.9.0/kotlinx-coroutines-android-1.9.0.jar" ; do
  f="$DEPS/jars/$(basename "$u")"
  [ -f "$f" ] || curl -sfL -o "$f" "$u" || echo "FAIL $u"
done

echo "== استخراج classes.jar از AAR"
for a in "$DEPS"/aars/*.aar; do
  n="$(basename "$a" .aar)"
  mkdir -p "$DEPS/x/$n"
  unzip -q -o "$a" classes.jar -d "$DEPS/x/$n" 2>/dev/null && cp "$DEPS/x/$n/classes.jar" "$DEPS/jars/$n.jar"
done
cp "$DEPS"/aars/*.jar "$DEPS/jars/" 2>/dev/null
echo "آماده: $DEPS  ($(ls "$DEPS"/jars/*.jar | wc -l) jar)"
