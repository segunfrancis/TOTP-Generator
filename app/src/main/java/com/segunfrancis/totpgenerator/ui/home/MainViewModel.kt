package com.segunfrancis.totpgenerator.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segunfrancis.totpgenerator.TotpCalculator
import com.segunfrancis.totpgenerator.data.LocalDatabase
import com.segunfrancis.totpgenerator.util.OtpAuthParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.seconds

data class HomeListItem(
    val id: String,
    val accountName: String,
    val issuer: String? = null,
    val token: String,
    val remainingSeconds: Long,
    val period: Int = 30
)

data class MainUiState(
    val showAddAccountDialog: Boolean = false,
    val accountName: String = "",
    val secretKeyInput: String = ""
) {
    val isAddAccountEnabled: Boolean
        get() = accountName.isNotBlank() && secretKeyInput.isNotBlank()
}

class MainViewModel : ViewModel() {

    private var shouldTrigger: Boolean = true

    private val _uiState: MutableStateFlow<MainUiState> = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val tickerFlow = flow {
        while (shouldTrigger) {
            emit(System.currentTimeMillis())
            delay(1.seconds)
        }
    }

    val secretsFlow: StateFlow<List<HomeListItem>> = combine(
        LocalDatabase.getAllSecrets(),
        tickerFlow
    ) { secrets, currentMillis ->
        val currentSeconds = currentMillis / 1000
        secrets.map { secret ->
            val period = if (secret.period > 0) secret.period else 30
            val remainingSeconds = period - (currentSeconds % period)
            val token = try {
                TotpCalculator.generateTOTP(
                    secretBase32 = secret.secretKey,
                    timeStepSeconds = period.toLong(),
                    digits = secret.digits
                )
            } catch (e: Exception) {
                e.printStackTrace()
                "------"
            }
            HomeListItem(
                id = secret.id,
                accountName = secret.account,
                issuer = secret.issuer,
                token = token,
                remainingSeconds = remainingSeconds,
                period = period
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun showAddAccountDialog() {
        _uiState.update { it.copy(showAddAccountDialog = true) }
    }

    fun dismissAddAccountDialog() {
        _uiState.update {
            it.copy(
                showAddAccountDialog = false,
                accountName = "",
                secretKeyInput = ""
            )
        }
    }

    fun updateAccountName(name: String) {
        _uiState.update { it.copy(accountName = name) }
    }

    fun updateSecretKeyInput(key: String) {
        _uiState.update { it.copy(secretKeyInput = key) }
    }

    fun addAccount() {
        val account = _uiState.value.accountName.trim()
        val newKey = _uiState.value.secretKeyInput.trim()
        if (newKey.isNotBlank()) {
            LocalDatabase.setSecretInformation(account = account, secretKey = newKey)
        }
        dismissAddAccountDialog()
    }

    fun deleteSecret(id: String) {
        LocalDatabase.deleteSecretById(id)
    }

    fun onQrCodeScanned(rawValue: String) {
        val trimmed = rawValue.trim()
        val parsedData = OtpAuthParser.parse(trimmed)
        if (parsedData != null) {
            LocalDatabase.setSecretInformation(
                account = parsedData.accountName,
                secretKey = parsedData.secret,
                issuer = parsedData.issuer,
                algorithm = parsedData.algorithm,
                digits = parsedData.digits,
                period = parsedData.period
            )
        } else if (trimmed.isNotBlank()) {
            LocalDatabase.setSecretInformation(
                account = "Scanned Account",
                secretKey = trimmed
            )
        }
    }

    override fun onCleared() {
        shouldTrigger = false
    }
}
