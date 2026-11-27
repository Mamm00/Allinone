package com.hyperlauncher.ai.data.local

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object HiddenPin {
    fun create(pin: String): String {
        require(pin.length in 6..12 && pin.all {it in '0'..'9'}) {"Use 6–12 digits"}
        val salt=ByteArray(16).also {SecureRandom().nextBytes(it)}
        val hash=derive(pin,salt,120000)
        return "120000:${Base64.getEncoder().encodeToString(salt)}:${Base64.getEncoder().encodeToString(hash)}"
    }
    fun verify(pin: String,encoded: String): Boolean=runCatching {
        require(pin.length in 6..12 && pin.all {it in '0'..'9'})
        val parts=encoded.split(':');require(parts.size==3)
        val rounds=parts[0].toInt();require(rounds in 100000..500000)
        val salt=Base64.getDecoder().decode(parts[1]);val expected=Base64.getDecoder().decode(parts[2])
        require(salt.size==16 && expected.size==32)
        MessageDigest.isEqual(expected,derive(pin,salt,rounds))
    }.getOrDefault(false)
    private fun derive(pin: String,salt: ByteArray,rounds: Int): ByteArray {
        val spec=PBEKeySpec(pin.toCharArray(),salt,rounds,256)
        return try {SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded} finally {spec.clearPassword()}
    }
}
