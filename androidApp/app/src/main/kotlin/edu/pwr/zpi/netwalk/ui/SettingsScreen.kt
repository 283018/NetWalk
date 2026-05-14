package edu.pwr.zpi.netwalk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
    ) {
        Text(
            text = "Server Configuration",
            modifier = Modifier.padding(bottom = 8.dp),
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
            label = "Iperf Port",
            value = editableSettings.iperfPort,
            onValueChange = { editableSettings = editableSettings.copy(iperfPort = it) },
            placeholder = viewModel.defaults.iperfPort,
            isValid = ::isValidPort,
        )

        // TODO: disable flag change support option in future
        SettingStringField(
            label = "Iperf Length",
            value = editableSettings.iperfTime,
            onValueChange = { editableSettings = editableSettings.copy(iperfTime = it) },
            placeholder = viewModel.defaults.iperfTime,
        )
        SettingStringField(
            label = "Iperf Streams",
            value = editableSettings.iperfParallel,
            onValueChange = { editableSettings = editableSettings.copy(iperfParallel = it) },
            placeholder = viewModel.defaults.iperfParallel,
        )

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
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Button(
            onClick = {
                scope.launch {
                    viewModel.saveAllSettings(editableSettings)

                    saveStatus = "Saved"
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

        Spacer(modifier = Modifier.height(8.dp))

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
) {
    val isError = value.isNotEmpty() && !isValid(value)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = MaterialTheme.colorScheme.primary) },
        placeholder = { Text(placeholder, color = Color.Gray.copy(alpha = 0.6f)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
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
            errorBorderColor = Color.Red,
            errorLabelColor = Color.Red,
        ),
    )
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
