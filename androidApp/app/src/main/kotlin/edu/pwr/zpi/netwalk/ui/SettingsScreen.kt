package edu.pwr.zpi.netwalk.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.net.URL

@Composable
fun SettingsScreen(
    viewModel: NetworkViewModel,
    onNavigateBack: () -> Unit,
) {
    // collected flows
    val savedSettings by viewModel.uiSettingsState.collectAsStateWithLifecycle()

    // local state for ui editing
    var editableSettings by remember { mutableStateOf(viewModel.uiSettingsState.value) }

    LaunchedEffect(savedSettings) {
        editableSettings = savedSettings
    }

    val scope = rememberCoroutineScope()
    var saveStatus by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(8.dp),
    ) {
        Text(
            text = "Server Configuration",
            modifier = Modifier.padding(bottom = 6.dp),
            color = MaterialTheme.colorScheme.primary,
        )

        SettingStringField(
            label = "Server Url",
            value = editableSettings.serverUrl,
            onValueChange = { editableSettings = editableSettings.copy(serverUrl = it) },
            placeholder = viewModel.defaults.serverUrl,
            isValid = ::isValidUrl,
            errorText = "Not a valid URL.",
        )

        SettingStringField(
            label = "Iperf server IP",
            value = editableSettings.iperfIp,
            onValueChange = { editableSettings = editableSettings.copy(iperfIp = it) },
            placeholder = viewModel.defaults.iperfIp,
            isValid = ::isValidIp,
            errorText = "Not a valid IP address.",
        )

        SettingStringField(
            label = "Iperf Port (-p)",
            value = editableSettings.iperfPort,
            onValueChange = { editableSettings = editableSettings.copy(iperfPort = it) },
            placeholder = viewModel.defaults.iperfPort,
            isValid = ::isValidPort,
        )

        SettingStringField(
            label = "Iperf Length (-t)",
            value = editableSettings.iperfTime,
            onValueChange = { editableSettings = editableSettings.copy(iperfTime = it) },
            placeholder = viewModel.defaults.iperfTime,
            isValid = ::isValidPositiveIntOrEmpty,
            errorText = "Must be a positive number.",
        )
        SettingStringField(
            label = "Iperf Streams (-P)",
            value = editableSettings.iperfParallel,
            onValueChange = { editableSettings = editableSettings.copy(iperfParallel = it) },
            placeholder = viewModel.defaults.iperfParallel,
            isValid = ::isValidPositiveIntOrEmpty,
            errorText = "Must be a positive number.",
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(
                checked = editableSettings.useUdp,
                onCheckedChange = { checked ->
                    editableSettings = editableSettings.copy(useUdp = checked)
                },
            )
            Text(
                text = "Use UDP protocol (-u)",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp),
            )
        }

        SettingStringField(
            label = "Iperf max package size (-M)",
            value = editableSettings.packageSize,
            onValueChange = { editableSettings = editableSettings.copy(packageSize = it) },
            placeholder = viewModel.defaults.packageSize,
            enabled = !editableSettings.useUdp,
            explanationText = "Applied only then using TCP",
            isValid = ::isValidSizeFormatOrEmpty,
            errorText = "Invalid size (e.g., 500, 1K).",
        )

        SettingStringField(
            label = "Iperf buffer length (-l)",
            value = editableSettings.bufferLength,
            onValueChange = { editableSettings = editableSettings.copy(bufferLength = it) },
            placeholder = viewModel.defaults.bufferLength,
            enabled = editableSettings.useUdp,
            explanationText = "Applied only then using UDP",
            isValid = ::isValidSizeFormatOrEmpty,
            errorText = "Invalid size (e.g., 500, 1K).",
        )

        SettingStringField(
            label = "Iperf target bandwidth (-b)",
            value = editableSettings.targetBandwidth,
            onValueChange = { editableSettings = editableSettings.copy(targetBandwidth = it) },
            placeholder = viewModel.defaults.targetBandwidth,
            enabled = editableSettings.useUdp,
            explanationText = "Applied only then using UDP",
            isValid = ::isValidSizeFormatOrEmpty,
            errorText = "Invalid size (e.g., 500, 1K).",
        )

        var showSendImmediatelyExplanation by remember { mutableStateOf(false) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(
                checked = editableSettings.sendImmediately,
                onCheckedChange = { checked ->
                    editableSettings = editableSettings.copy(sendImmediately = checked)
                },
            )
            Text(
                text = "Send immediately",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 6.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = { showSendImmediatelyExplanation = true }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Explanation for Send Immediately",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (showSendImmediatelyExplanation) {
            AlertDialog(
                onDismissRequest = { showSendImmediatelyExplanation = false },
                title = { Text("Send immediately") },
                text = { Text("Send measurements immediately, without queueing.") },
                confirmButton = {
                    TextButton(onClick = { showSendImmediatelyExplanation = false }) {
                        Text("OK")
                    }
                },
            )
        }

        val context = LocalContext.current

        Button(
            onClick = {
                scope.launch {
                    viewModel.saveAllSettings(editableSettings)

                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.align(Alignment.End),
            enabled = isValidUrl(editableSettings.serverUrl) && isValidIp(editableSettings.iperfIp),
        ) {
            Text("Save & apply")
        }

        saveStatus?.let {
            Text(
                text = it,
                color = if (it == "Saved") Color.Green else Color.Red,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        TextButton(
            onClick = onNavigateBack,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("<- Back to Network Info")
        }
    }
}

@Composable
fun SettingStringField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isValid: (String) -> Boolean = { true },
    errorText: String = "Invalid input",
    enabled: Boolean = true,
    explanationText: String? = null,
) {
    val isError = value.isNotEmpty() && !isValid(value)

    var showExplanation by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = MaterialTheme.colorScheme.primary) },
        placeholder = { Text(placeholder, color = Color.Gray.copy(alpha = 0.6f)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        enabled = enabled,
        trailingIcon = if (explanationText != null) {
            {
                IconButton(onClick = { showExplanation = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray,
                    )
                }
            }
        } else {
            null
        },
        supportingText = {
            if (isError) Text(errorText)
        },
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.Gray,
            disabledTextColor = Color.Gray.copy(alpha = 0.5f),
            disabledBorderColor = Color.DarkGray.copy(alpha = 0.3f),
            disabledLabelColor = Color.Gray.copy(alpha = 0.5f),
            disabledContainerColor = Color.Gray.copy(alpha = 0.1f),
            errorBorderColor = Color.Red,
            errorLabelColor = Color.Red,
        ),
    )

    if (showExplanation && explanationText != null) {
        AlertDialog(
            onDismissRequest = { showExplanation = false },
            title = { Text(label) },
            text = { Text(explanationText) },
            confirmButton = {
                TextButton(onClick = { showExplanation = false }) {
                    Text("OK")
                }
            },
        )
    }
}

private fun isValidUrl(url: String): Boolean =
    try {
        val parsed = java.net.URL(url)
        parsed.protocol == "http" || parsed.protocol == "https"
    } catch (e: Exception) {
        false
    }

private fun isValidIp(ip: String): Boolean {
    val ipv4Pattern = """^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""".toRegex()
    val match = ipv4Pattern.matchEntire(ip) ?: return false

    return match.groupValues.drop(1).all {
        it.toInt() in 0..255
    }
}

private fun isValidPort(port: String): Boolean {
    if (port.isEmpty()) return true // allow empty port if using public iperf server
    val portInt = port.toIntOrNull() ?: return false
    return portInt in 1..65535
}

private fun isValidPositiveIntOrEmpty(value: String): Boolean {
    if (value.isEmpty()) return true
    val intVal = value.toIntOrNull() ?: return false
    return intVal > 0
}

private fun isValidSizeFormatOrEmpty(value: String): Boolean {
    if (value.isEmpty()) return true
    val regex = """^\d+(\.\d+)?[kKmMgG]?$""".toRegex()
    return regex.matches(value)
}
