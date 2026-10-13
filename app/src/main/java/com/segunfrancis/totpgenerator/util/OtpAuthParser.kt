package com.segunfrancis.totpgenerator.util

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class OtpAuthData(
    val secret: String,
    val accountName: String,
    val issuer: String? = null,
    val algorithm: String = "HmacSHA1",
    val period: Int = 30,
    val digits: Int = 6
)

object OtpAuthParser {
    fun parse(uriString: String): OtpAuthData? {
        return try {
            val uri = URI(uriString)
            if (uri.scheme?.lowercase() != "otpauth" || uri.host?.lowercase() != "totp") {
                return null
            }

            // 1. Parse Path to find Account Name and Issuer prefix
            // Path looks like: "/Google:alice@gmail.com" or "/alice@gmail.com"
            val path = URLDecoder.decode(uri.path ?: "", StandardCharsets.UTF_8.name())
                .trimStart('/')

            var extractedIssuer = ""
            var accountName = path

            if (path.contains(":")) {
                val parts = path.split(":", limit = 2)
                extractedIssuer = parts[0].trim()
                accountName = parts[1].trim()
            }

            // 2. Parse Query Parameters
            val queryPairs = uri.query?.split("&")?.associate { pair ->
                val parts = pair.split("=", limit = 2)
                val key = parts.getOrNull(0)?.lowercase() ?: ""
                val value = parts.getOrNull(1)?.let {
                    URLDecoder.decode(it, StandardCharsets.UTF_8.name())
                } ?: ""
                key to value
            } ?: emptyMap()

            val secret = queryPairs["secret"]?.takeIf { it.isNotBlank() } ?: return null
            val queryIssuer = queryPairs["issuer"]?.trim() ?: ""
            val algorithm = queryPairs["algorithm"]?.takeIf { it.isNotBlank() } ?: "HmacSHA1"

            // Prefer the query issuer param over path prefix per RFC guidelines
            val finalIssuer = queryIssuer.ifEmpty { extractedIssuer }.takeIf { it.isNotBlank() }
            val period = queryPairs["period"]?.toIntOrNull() ?: 30
            val digits = queryPairs["digits"]?.toIntOrNull() ?: 6

            OtpAuthData(
                secret = secret,
                accountName = accountName.ifBlank { "Scanned Account" },
                issuer = finalIssuer,
                algorithm = algorithm,
                period = period,
                digits = digits
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
