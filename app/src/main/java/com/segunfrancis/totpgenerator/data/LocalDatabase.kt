package com.segunfrancis.totpgenerator.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class Secret(
    val id: String,
    val account: String,
    val secretKey: String,
    val issuer: String? = null,
    val algorithm: String? = "HmacSHA1",
    val digits: Int = 6,
    val period: Int = 30
)

object LocalDatabase {

    private val _secrets = MutableStateFlow<List<Secret>>(emptyList())

    fun setSecretInformation(
        account: String,
        secretKey: String,
        issuer: String? = null,
        algorithm: String? = "HmacSHA1",
        digits: Int = 6,
        period: Int = 30
    ) {
        val id = UUID.randomUUID().toString()
        val newSecret = Secret(
            id = id,
            account = account,
            secretKey = secretKey,
            issuer = issuer,
            algorithm = algorithm ?: "HmacSHA1",
            digits = digits,
            period = period
        )
        _secrets.update { currentSecrets ->
            listOf(newSecret) + currentSecrets
        }
    }

    fun deleteSecretById(id: String) {
        _secrets.update { currentSecrets ->
            currentSecrets.filterNot { it.id == id }
        }
    }

    fun getAllSecrets(): Flow<List<Secret>> {
        return _secrets.asStateFlow()
    }
}
