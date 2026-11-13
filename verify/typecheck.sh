#!/usr/bin/env bash
# بازبینی «نوع» لایهٔ بصری و کل ماژول، بدون Gradle/AGP.
#
# چه چیزی بررسی می‌شود؟
#   ۱) apicheck.kt  : امضای همهٔ APIهای Compose/Material3 که در بازطراحی استفاده شده‌اند
#   ۲) کل فایل‌های main: حل نوع‌ها (frontend) با android.jar 35 + Compose 1.7.6 + Material3 1.3.1
#
# چه چیزی بررسی نمی‌شود؟
#   کدبایت نهایی؛ چون پلاگین کامپایلر Compose و AAPT در این محیط نیست. اجرای واقعی
#   `./gradlew assembleDebug` روی ماشین دارای Android SDK جای این بررسی را می‌گیرد.
#
# استفاده:  KOTLINC=/path/to/kotlinc bash typecheck.sh          (با کش موجود در $DEPS)
#           DEPS=/tmp/cp SDK_JAR=/tmp/cp/sdk/android-35/android.jar bash typecheck.sh
set -u
here="$(cd "$(dirname "$0")" && pwd)"
KOTLINC="${KOTLINC:-kotlinc}"
DEPS="${DEPS:-$here/deps}"
SDK_JAR="${SDK_JAR:-}"
REL="app/src/main/java/ir/hsai/mobile"
SRC=""
for c in "$here/../src/hsai-native/$REL" "$here/../hsai-native/$REL" "$here/../$REL"; do
  [ -d "$c" ] && SRC="$c" && break
done
[ -z "$SRC" ] && { echo "پوشهٔ سورس اپ پیدا نشد."; exit 2; }
[ -z "$SDK_JAR" ] && SDK_JAR="$(ls "$DEPS"/sdk/*/android.jar 2>/dev/null | head -1)"
if [ -z "$SDK_JAR" ] || [ ! -f "$SDK_JAR" ]; then
  echo "android.jar پیدا نشد. با bash deps.sh دانلود کنید یا SDK_JAR را تنظیم کنید."; exit 2
fi
JARS="$(ls "$DEPS"/jars/*.jar 2>/dev/null | tr '\n' ':')"
[ -z "$JARS" ] && { echo "jarهای androidx پیدا نشد؛ bash deps.sh را اجرا کنید."; exit 2; }

# کلاس R و ReminderMigration ساختگی: R را AAPT می‌سازد و ReminderMigration.kt به guava
# (ListenableFuture) نیاز دارد که در WorkManager واقعی از طریق Gradle می‌آید.
mkdir -p "$DEPS/stub" "$DEPS/out"
cat > "$DEPS/stub/R.kt" <<'KT'
package ir.hsai.mobile
object R {
 object drawable { const val brand_mark=1; const val ic_notification=2; const val app_icon_art=3; const val brand_monochrome=4 }
 object font { const val vazirmatn=10; const val vazirmatn_black=11; const val vazirmatn_bold=12; const val vazirmatn_medium=13; const val vazirmatn_regular=14; const val vazirmatn_semibold=15 }
 object raw { const val legal_glossary_v53=20 }
 object mipmap { const val ic_launcher=30; const val ic_launcher_round=31 }
 object style { const val AppTheme=40 }
 object xml { const val file_paths=50; const val data_extraction_rules=51 }
}
KT
cat > "$DEPS/stub/ReminderStub.kt" <<'KT'
package ir.hsai.mobile
import android.content.Context
object ReminderMigration { const val TAG="x"; const val MARKER="y"; suspend fun run(context:Context):Boolean=false }
KT
"$KOTLINC" -nowarn -cp "$SDK_JAR" -d "$DEPS/stub.jar" "$DEPS/stub/R.kt" "$DEPS/stub/ReminderStub.kt" || exit 3

CP="$SDK_JAR:$DEPS/stub.jar:$JARS"
# تنها خطاهای پذیرفته‌شده: نبود پلاگین کامپایلر Compose در مرحلهٔ تولید کد (backend) — نه خطای نوع/امضا.
accept() { grep "error:" "$1" | grep -v "Backend Internal error" | grep -v "Back-end (JVM) Internal error" | grep -v "not compatible with this version of compiler" || true; }

echo "== ۱/۲ بازبینی امضای APIهای Compose"
"$KOTLINC" -nowarn -jvm-target 17 -cp "$CP" -d "$DEPS/out_api" "$here/apicheck.kt" > "$DEPS/api.log" 2>&1
REAL_API="$(accept "$DEPS/api.log")"
if [ -n "$REAL_API" ]; then
  echo "FAIL: امضای API ناسازگار است"; echo "$REAL_API" | head -20; exit 4
fi
echo "   ok"

echo "== ۲/۲ حل نوع‌های کل ماژول (frontend)"
cd "$SRC"
FILES="$(ls *.kt | grep -v '^ReminderMigration.kt$')"
JAVA_OPTS="-Xmx560m -XX:+UseSerialGC" "$KOTLINC" -nowarn -jvm-target 17 -cp "$CP" -d "$DEPS/out_main" $FILES > "$DEPS/module.log" 2>&1
# تنها خطاهای پذیرفته‌شده: نبود پلاگین Compose در مرحلهٔ تولید کد (backend) — نه خطای نوع.
REAL="$(accept "$DEPS/module.log")"
if [ -n "$REAL" ]; then
  echo "FAIL: خطای نوع"; echo "$REAL" | head -30; exit 5
fi
echo "   ok  (۰ خطای نوع)"
echo
echo "نکته: ReminderMigration.kt در این بررسی کامپایل نشد (نیازمند guava/ListenableFuture در Gradle) و در این بازطراحی تغییری نداشته است."
