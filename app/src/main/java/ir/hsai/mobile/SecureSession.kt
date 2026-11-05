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

class SecureSession(context:Context) {
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
 fun read():JSONObject? { return try {
  val parts=prefs.getString("session",null)?.split(':') ?: return null
  val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)))
  JSONObject(String(c.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),Charsets.UTF_8))
 } catch(_:Exception) { prefs.edit().remove("session").apply();null } }
 fun save(value:JSONObject) {
  val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key())
  val b64={b:ByteArray -> Base64.encodeToString(b,Base64.NO_WRAP)}
  check(prefs.edit().putString("session",b64(c.iv)+":"+b64(c.doFinal(value.toString().toByteArray(Charsets.UTF_8)))).commit()) { "ذخیره امن نشست انجام نشد." }
 }
 fun clear() { prefs.edit().remove("session").remove("payment").apply() }
 var authority:String
  get()=prefs.getString("payment","").orEmpty()
  set(v) { prefs.edit().putString("payment",v).apply() }
 var dark:Boolean
  get()=prefs.getBoolean("dark",false)
  set(v) { prefs.edit().putBoolean("dark",v).apply() }
}
