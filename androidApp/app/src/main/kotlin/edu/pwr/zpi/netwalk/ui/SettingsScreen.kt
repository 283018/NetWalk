package edu.pwr.zpi.netwalk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import edu.pwr.zpi.netwalk.settings.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URL

@Composable
fun SettingsScreen(
    viewModel: NetworkViewModel,
    onNavigateBack: () -> Unit,
) {
    var urlInput by remember {
        mutableStateOf(TextFieldValue(viewModel.getCurrentServerUrl()))
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
        )

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("Server URL") },
            placeholder = {
                Text(SettingsRepository.DEFAULT_URL, color = Color.Gray.copy(alpha = 0.6f))
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(
                color = Color(0xFF000000),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            isError = urlInput.text.isNotEmpty() && !isValidUrl(urlInput.text),
            supportingText = {
                if (urlInput.text.isNotEmpty() && !isValidUrl(urlInput.text)) {
                    Text(
                        "Not a valid URL.",
                        color = Color.Red,
                    )
                }
            },
        )

        Button(
            onClick = {
                if (isValidUrl(urlInput.text)) {
                    scope.launch {
                        viewModel.updateServerUrl(urlInput.text)
                        saveStatus = "Saved"
                        delay(2000)
                        saveStatus = null
                        onNavigateBack()
                    }
                } else {
                    saveStatus = "Invalid URL"
                }
            },
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 8.dp),
            enabled = urlInput.text.isNotEmpty(),
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

private fun isValidUrl(url: String): Boolean =
    try {
        val parsed = java.net.URL(url)
        parsed.protocol == "http" || parsed.protocol == "https"
    } catch (e: Exception) {
        false
    }
