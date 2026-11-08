package ir.hsai.mobile

/** Header syntax only, not a claim that a token is authentic; authentication belongs to the server. */
object SessionTokens {
 private val bearer=Regex("[A-Za-z0-9._~+/-]+=*")
 fun headerSafe(value:String)=value.length in 1..8192&&bearer.matches(value)
}
