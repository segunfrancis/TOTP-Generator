package com.segunfrancis.totpgenerator

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object TotpCalculator {
    fun generateTOTP(secretBase32: String, timeStepSeconds: Long = 30, digits: Int = 6): String {
        // Step 1: Establish Inputs
        val secretBytes = Base32.decode(secretBase32)
        val currentUnixTime = System.currentTimeMillis() / 1000
        val timeCounter = currentUnixTime / timeStepSeconds

        // Convert the 64-bit time counter into an 8-byte array (big-endian)
        val buffer = ByteBuffer.allocate(8)
        buffer.putLong(timeCounter)
        val timeBytes = buffer.array()

        // Step 2: Compute the Cryptographic Hash (HMAC-SHA1)
        val mac = Mac.getInstance("HmacSHA1")
        val keySpec = SecretKeySpec(secretBytes, "HmacSHA1")
        mac.init(keySpec)
        val hmacHash = mac.doFinal(timeBytes)

        // Step 3: Dynamic Truncation
        // Get the last 4 bits of the last byte to use as an offset index (0 to 15)
        val offset = hmacHash[hmacHash.size - 1].toInt() and 0x0F

        // Extract a 4-byte chunk starting at that offset index
        val binary = ((hmacHash[offset].toInt() and 0x7F) shl 24) or
                ((hmacHash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hmacHash[offset + 2].toInt() and 0xFF) shl 8) or
                (hmacHash[offset + 3].toInt() and 0xFF)

        // Step 4: Modulo Scaling
        val modulo = 10.0.pow(digits).toInt()
        val otp = binary % modulo

        // Zero-pad the code if it's shorter than requested digits
        return otp.toString().padStart(digits, '0')
    }
}