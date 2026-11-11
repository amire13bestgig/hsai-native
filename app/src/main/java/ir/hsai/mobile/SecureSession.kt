package ir.hsai.mobile

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SessionStore {
 fun read():JSONObject?
 fun save(value:JSONObject)
 fun clear()
 var authority:String
 var dark:Boolean
 var reminderMigrationDone:Boolean
 var reminderNotice:Boolean
 /** nonce دستگاه برای جریان مرورگری ورود با گوگل (رشتهٔ «nonce:timestamp»). */
 var oauthNonce:String
}
class SecureSession(context:Context):SessionStore {
 private val prefs=context.getSharedPreferences("native_vault",Context.MODE_PRIVATE)
 private val alias="hsai_native_session_v1"
 private fun key():SecretKey {
  val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
  if(!store.containsAlias(alias)) KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
   init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());generateKey()
  }
  return store.getKey(alias,null) as SecretKey
 }
 override fun read():JSONObject? { return try {
  val stored=prefs.getString("session",null)
  if(stored==null){prefs.edit().remove("payment").apply();return null}
  val parts=stored.split(':');require(parts.size==2)
  val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)))
  JSONObject(String(c.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),Charsets.UTF_8)).also{require(SessionTokens.headerSafe(it.str("token")))}
 } catch(_:Exception) { prefs.edit().remove("session").remove("payment").apply();null } }
 override fun save(value:JSONObject) {
  val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key())
  val b64={b:ByteArray -> Base64.encodeToString(b,Base64.NO_WRAP)}
  check(prefs.edit().putString("session",b64(c.iv)+":"+b64(c.doFinal(value.toString().toByteArray(Charsets.UTF_8)))).commit()) { "ذخیره امن نشست انجام نشد." }
 }
 override fun clear() { prefs.edit().remove("session").remove("payment").apply() }
 override var authority:String
  get()=prefs.getString("payment","").orEmpty()
  set(v) { prefs.edit().putString("payment",v).apply() }
 override var reminderMigrationDone:Boolean
  get()=prefs.getBoolean("reminder_migration_v2",false)
  set(v){prefs.edit().putBoolean("reminder_migration_v2",v).apply()}
 override var reminderNotice:Boolean
  get()=prefs.getBoolean("reminder_notice_v2",false)
  set(v){prefs.edit().putBoolean("reminder_notice_v2",v).apply()}
 override var dark:Boolean
  get()=prefs.getBoolean("dark",false)
  set(v) { prefs.edit().putBoolean("dark",v).apply() }
 // عمداً رمزنگاری‌نشده: nonce رمز نیست، شناسهٔ کوتاه‌عمر همان نصب است و امضای
 // برگهٔ دست‌دهی روی سرور به آن مقید می‌شود. هدف: بازگشت از مرورگر حتی اگر
 // اندروید فرایند اپ را در پس‌زمینه کشته باشد.
 override var oauthNonce:String
  get()=prefs.getString("oauth_nonce","").orEmpty()
  set(v) { prefs.edit().putString("oauth_nonce",v).apply() }
}
