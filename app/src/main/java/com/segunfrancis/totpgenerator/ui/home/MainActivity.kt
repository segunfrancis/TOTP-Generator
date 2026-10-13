package com.segunfrancis.totpgenerator.ui.home

import android.content.ClipData
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.nativeClipboardManager
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.segunfrancis.totpgenerator.ui.theme.TOTPGeneratorTheme
import kotlin.math.ceil
import kotlin.math.floor

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .allowManualInput()
            .build()

        val scanner = GmsBarcodeScanning.getClient(this, options)

        fun startQrScanner() {
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val rawValue: String? = barcode.rawValue
                    if (rawValue != null) {
                        handleScannedUri(rawValue)
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Scanning failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnCanceledListener {
                    Toast.makeText(this, "Scanning canceled", Toast.LENGTH_SHORT).show()
                }
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val secrets by viewModel.secretsFlow.collectAsState()
            TOTPGeneratorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        secrets = secrets,
                        modifier = Modifier.padding(innerPadding),
                        showAddAccountDialog = uiState.showAddAccountDialog,
                        accountName = uiState.accountName,
                        secretKeyInput = uiState.secretKeyInput,
                        isAddAccountEnabled = uiState.isAddAccountEnabled,
                        onAccountNameChange = viewModel::updateAccountName,
                        onSecretKeyChange = viewModel::updateSecretKeyInput,
                        onEnterSecretKeyClick = viewModel::showAddAccountDialog,
                        onDismissDialog = viewModel::dismissAddAccountDialog,
                        onAddAccountSubmit = viewModel::addAccount,
                        onDeleteSecret = viewModel::deleteSecret,
                        onScanQrClick = { startQrScanner() }
                    )
                }
            }
        }
    }

    private fun handleScannedUri(rawValue: String) {
        viewModel.onQrCodeScanned(rawValue)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    secrets: List<HomeListItem>,
    modifier: Modifier = Modifier,
    showAddAccountDialog: Boolean = false,
    accountName: String = "",
    secretKeyInput: String = "",
    isAddAccountEnabled: Boolean = false,
    onAccountNameChange: (String) -> Unit = {},
    onSecretKeyChange: (String) -> Unit = {},
    onEnterSecretKeyClick: () -> Unit = {},
    onDismissDialog: () -> Unit = {},
    onAddAccountSubmit: () -> Unit = {},
    onDeleteSecret: (String) -> Unit = {},
    onScanQrClick: () -> Unit = {}
) {
    if (showAddAccountDialog) {
        AddAccountDialog(
            accountName = accountName,
            secretKey = secretKeyInput,
            isAddAccountEnabled = isAddAccountEnabled,
            onAccountNameChange = onAccountNameChange,
            onSecretKeyChange = onSecretKeyChange,
            onDismissRequest = onDismissDialog,
            onAddAccountClick = onAddAccountSubmit
        )
    }

    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        val focusRequester = remember { FocusRequester() }
        var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }
        val fabVisible = true
        val context = LocalContext.current
        val clipboardManager = LocalClipboard.current.nativeClipboardManager
        val items = remember(onScanQrClick, onEnterSecretKeyClick) {
            listOf(
                Triple(Icons.Filled.CameraAlt, "Scan QR code") {
                    onScanQrClick()
                },
                Triple(Icons.Filled.Keyboard, "Enter secret key") {
                    onEnterSecretKeyClick()
                }
            )
        }

        BackHandler(fabMenuExpanded) { fabMenuExpanded = false }

        if (secrets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No accounts added yet.\nTap + to add an account or scan a QR code.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(secrets, key = { it.id }) { secret ->
                    val isUrgent = secret.remainingSeconds <= 5
                    val tokenColor = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

                    Card(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth(),
                        onClick = {
                            clipboardManager.setPrimaryClip(
                                ClipData.newPlainText(
                                    secret.token,
                                    AnnotatedString(secret.token)
                                )
                            )
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = secret.accountName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!secret.issuer.isNullOrBlank()) {
                                        Text(
                                            text = secret.issuer,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                IconButton(onClick = { onDeleteSecret(secret.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete account",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TotpCountdownIndicator(
                                    remainingSeconds = secret.remainingSeconds,
                                    totalSeconds = secret.period
                                )
                                Spacer(Modifier.width(16.dp))
                                AnimatedContent(targetState = secret.token) { tokenText ->
                                    Text(
                                        text = tokenText,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = tokenColor,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 4.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButtonMenu(
            modifier = Modifier.align(Alignment.BottomEnd),
            expanded = fabMenuExpanded,
            button = {
                // A FAB should have a tooltip associated with it.
                TooltipBox(
                    positionProvider =
                        TooltipDefaults.rememberTooltipPositionProvider(
                            if (fabMenuExpanded) {
                                TooltipAnchorPosition.Start
                            } else {
                                TooltipAnchorPosition.Above
                            }
                        ),
                    tooltip = {
                        PlainTooltip(
                            modifier =
                                Modifier.semantics {
                                    liveRegion = LiveRegionMode.Assertive
                                    paneTitle = "Toggle menu"
                                }
                        ) {
                            Text("Toggle menu")
                        }
                    },
                    state = rememberTooltipState(),
                ) {
                    ToggleFloatingActionButton(
                        modifier =
                            Modifier
                                .semantics {
                                    traversalIndex = -1f
                                    stateDescription =
                                        if (fabMenuExpanded) "Expanded" else "Collapsed"
                                    contentDescription = "Toggle menu"
                                }
                                .animateFloatingActionButton(
                                    visible = fabVisible || fabMenuExpanded,
                                    alignment = Alignment.BottomEnd,
                                )
                                .focusRequester(focusRequester),
                        checked = fabMenuExpanded,
                        onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                    ) {
                        val progress = checkedProgress
                        val imageVector by remember(progress) {
                            derivedStateOf {
                                if (progress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                            }
                        }
                        Icon(
                            painter = rememberVectorPainter(imageVector),
                            contentDescription = null,
                            modifier = Modifier.animateIcon(checkedProgress = { progress }),
                        )
                    }
                }
            },
        ) {
            items.forEachIndexed { i, item ->
                FloatingActionButtonMenuItem(
                    modifier =
                        Modifier
                            .semantics {
                                isTraversalGroup = true
                                // Add a custom a11y action to allow closing the menu when focusing
                                // the last menu item, since the close button comes before the first
                                // menu item in the traversal order.
                                if (i == items.size - 1) {
                                    customActions =
                                        listOf(
                                            CustomAccessibilityAction(
                                                label = "Close menu",
                                                action = {
                                                    fabMenuExpanded = false
                                                    true
                                                },
                                            )
                                        )
                                }
                            }
                            .then(
                                if (i == 0) {
                                    Modifier.onKeyEvent {
                                        // Navigating back from the first item should go back to the
                                        // FAB menu button.
                                        if (
                                            it.type == KeyEventType.KeyDown &&
                                            (it.key == Key.DirectionUp ||
                                                    it.key == Key.NumPadDirectionUp ||
                                                    (it.isShiftPressed && it.key == Key.Tab))
                                        ) {
                                            focusRequester.requestFocus()
                                            return@onKeyEvent true
                                        }
                                        return@onKeyEvent false
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                    onClick = {
                        fabMenuExpanded = false
                        item.third()
                    },
                    icon = { Icon(item.first, contentDescription = item.second) },
                    text = {
                        var textLayoutResult: TextLayoutResult? by remember { mutableStateOf(null) }
                        Text(
                            modifier =
                                Modifier.layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints)
                                    val newTextLayoutResult = textLayoutResult!!
                                    val minX =
                                        (0 until newTextLayoutResult.lineCount).minOf(
                                            newTextLayoutResult::getLineLeft
                                        )
                                    val maxX =
                                        (0 until newTextLayoutResult.lineCount).maxOf(
                                            newTextLayoutResult::getLineRight
                                        )

                                    layout(ceil(maxX - minX).toInt(), placeable.height) {
                                        placeable.place(-floor(minX).toInt(), 0)
                                    }
                                },
                            text = item.second,
                            onTextLayout = { textLayoutResult = it },
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun AddAccountDialog(
    accountName: String,
    secretKey: String,
    isAddAccountEnabled: Boolean,
    onAccountNameChange: (String) -> Unit,
    onSecretKeyChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onAddAccountClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Add Account") },
        text = {
            Column {
                OutlinedTextField(
                    value = accountName,
                    onValueChange = onAccountNameChange,
                    label = { Text("Account name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = secretKey,
                    onValueChange = onSecretKeyChange,
                    label = { Text("Secret key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAddAccountClick,
                enabled = isAddAccountEnabled
            ) {
                Text("Add Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

/**
 * A circular countdown indicator that empties **clockwise** (the ring is full when the TOTP
 * window just started and drains toward 12 o'clock as time runs out) and shows the number of
 * seconds remaining in its centre.
 */
@Composable
private fun TotpCountdownIndicator(
    modifier: Modifier = Modifier,
    remainingSeconds: Long,
    totalSeconds: Int = 30
) {
    val windowMillis = (if (totalSeconds > 0) totalSeconds else 30) * 1000L
    // Sample the wall clock on every frame so the ring glides continuously at a constant,
    // slow speed instead of stepping once per second and pausing between marks. This uses the
    // same clock source as MainViewModel's countdown, so the ring, the seconds text and the
    // token rotation all stay in sync.
    var remainingMillis by remember { mutableLongStateOf(remainingMillisInWindow(windowMillis)) }
    LaunchedEffect(windowMillis) {
        while (true) {
            withFrameNanos {
                remainingMillis = remainingMillisInWindow(windowMillis)
            }
        }
    }
    val progress = remainingMillis.toFloat() / windowMillis
    val isUrgent = remainingSeconds <= 5
    val indicatorColor = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            // Material3's determinate indicator always draws its arc from 12 o'clock sweeping
            // clockwise by (progress * 360). With a shrinking countdown that makes the arc's
            // edge retreat anti-clockwise. Rotating the whole indicator by the *remaining*
            // angle pins the arc's end at 12 o'clock instead, so the ring empties clockwise.
            progress = { progress },
            modifier = Modifier.rotate((1f - progress) * 360f),
            color = indicatorColor,
            gapSize = 0.dp
        )
        Text(
            text = remainingSeconds.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = indicatorColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Millis left until the current [windowMillis]-sized TOTP window ends. */
private fun remainingMillisInWindow(windowMillis: Long): Long =
    windowMillis - System.currentTimeMillis() % windowMillis

@Preview(showSystemUi = true)
@Composable
fun MainScreenPreview() {
    TOTPGeneratorTheme {
        MainScreen(secrets = emptyList())
    }
}
