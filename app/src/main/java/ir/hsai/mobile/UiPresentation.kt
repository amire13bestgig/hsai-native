package ir.hsai.mobile

import org.json.JSONArray
import org.json.JSONObject

/** Presentation only. Never mutates API data or removes zero/false financial values. */
object UiPresentation {
 private val internal = setOf("id", "user_id", "conversation_id", "subscription_id", "type", "slug",
  "access_token", "session_token", "client_secret", "secret_key", "authorization", "credentials", "password_hash", "system_prompt", "internal_reasoning", "chain_of_thought", "debug", "internal",
  "analysis_json", "features_json", "messages_json", "verification_json", "token", "refresh_token", "password", "api_key", "image_base64", "image_data_url")
 fun keys(data:JSONObject):List<String> = data.keys().asSequence().filter { key ->
  if(key.lowercase(java.util.Locale.ROOT) in internal || data.isNull(key))false else when(val value=data.opt(key)) {
   is String -> value.isNotBlank()
   is JSONArray -> value.length()>0
   is JSONObject -> value.length()>0
   else -> true
  }
 }.toList()
 fun primary(data:JSONObject)=keys(data).filter { it in Domain.labels }
 fun secondary(data:JSONObject)=keys(data).filter { it !in Domain.labels }
}
