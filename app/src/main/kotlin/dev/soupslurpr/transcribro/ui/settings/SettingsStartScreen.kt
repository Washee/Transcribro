package dev.soupslurpr.transcribro.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.soupslurpr.transcribro.R
import dev.soupslurpr.transcribro.dataStore
import dev.soupslurpr.transcribro.preferences.PreferencesViewModel
import dev.soupslurpr.transcribro.ui.reusablecomposables.ScreenLazyColumn
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun SettingsStartScreen(
    onClickLicense: () -> Unit,
    onClickPrivacyPolicy: () -> Unit,
    onClickCredits: () -> Unit,
) {
    val preferencesViewModel: PreferencesViewModel = viewModel(
        factory = PreferencesViewModel.PreferencesViewModelFactory(LocalContext.current.dataStore)
    )

    val preferencesUiState by preferencesViewModel.uiState.collectAsState()

    val localUriHandler = LocalUriHandler.current

    ScreenLazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        item {
            SettingsCategory(
                stringResource(R.string.theme)
            )
        }
        item {
            val preference = preferencesUiState.pitchBlackBackground
            SettingsSwitchItem(
                name = stringResource(id = R.string.pitch_black_background_setting_name),
                description = stringResource(id = R.string.pitch_black_background_setting_description),
                checked = preference.second.value,
                onCheckedChange = {
                    preferencesViewModel.setPreference(
                        preference.first,
                        it
                    )
                }
            )
        }
        item {
            SettingsCategory(
                stringResource(R.string.voice_input_keyboard_setting_category)
            )
        }
        item {
            val preference = preferencesUiState.autoSwitchToPreviousInputMethod
            SettingsSwitchItem(
                name = stringResource(id = R.string.auto_switch_to_previous_input_method_setting_name),
                description = stringResource(id = R.string.auto_switch_to_previous_input_method_setting_description),
                checked = preference.second.value,
                onCheckedChange = {
                    preferencesViewModel.setPreference(
                        preference.first,
                        it
                    )
                }
            )
        }
        item {
            val preference = preferencesUiState.autoStopRecognition
            SettingsSwitchItem(
                name = stringResource(id = R.string.auto_stop_recognition_setting_name),
                description = stringResource(id = R.string.auto_stop_recognition_setting_description),
                checked = preference.second.value,
                onCheckedChange = {
                    preferencesViewModel.setPreference(
                        preference.first,
                        it
                    )
                }
            )
        }
        item {
            val preference = preferencesUiState.autoStartRecognition
            SettingsSwitchItem(
                name = stringResource(id = R.string.auto_start_recognition_setting_name),
                description = stringResource(id = R.string.auto_start_recognition_setting_description),
                checked = preference.second.value,
                onCheckedChange = {
                    preferencesViewModel.setPreference(
                        preference.first,
                        it
                    )
                }
            )
        }
        item {
            val preference = preferencesUiState.autoSendTranscription
            SettingsSwitchItem(
                name = stringResource(id = R.string.auto_send_transcription_setting_name),
                description = stringResource(id = R.string.auto_send_transcription_setting_description),
                checked = preference.second.value,
                onCheckedChange = {
                    preferencesViewModel.setPreference(
                        preference.first,
                        it
                    )
                }
            )
        }
        item {
            SettingsCategory(
                stringResource(R.string.wyoming_setting_category)
            )
        }
        item {
            val preference = preferencesUiState.useWyoming
            SettingsSwitchItem(
                name = stringResource(id = R.string.wyoming_activate_setting_name),
                description = stringResource(id = R.string.wyoming_activate_setting_description),
                checked = preference.second.value,
                onCheckedChange = {
                    preferencesViewModel.setPreference(
                        preference.first,
                        it
                    )
                }
            )
        }
        item {
            val preference = preferencesUiState.wyomingAddress
            SettingsTextFieldItem (
                name = stringResource(id = R.string.wyoming_address_setting_name),
                description = stringResource(id = R.string.wyoming_address_setting_description),
                value = preference.second.value,
                onValueChange = {
                    preferencesViewModel.setPreference(
                        preference.first,
                        it
                    )
                }
            )
        }
        item {
            val preference = preferencesUiState.wyomingPort
            SettingsNumberFieldItem (
                name = stringResource(id = R.string.wyoming_port_setting_name),
                description = stringResource(id = R.string.wyoming_port_setting_description),
                value = preference.second.value,
                onValueChange = {
                    preferencesViewModel.setPreference(
                        preference.first,
                        it
                    )
                }
            )
        }
        item {
            val preference = preferencesUiState.wyomingSSL
            SettingsSwitchItem(
                name = stringResource(id = R.string.wyoming_ssl_setting_name),
                description = stringResource(id = R.string.wyoming_ssl_setting_description),
                checked = preference.second.value,
                onCheckedChange = {
                    preferencesViewModel.setPreference(
                        preference.first,
                        it
                    )
                }
            )
        }
        item {
            SettingsCategory(
                stringResource(R.string.about_setting_category)
            )
        }
        item {
            SettingsIconItem(
                name = stringResource(id = R.string.view_source_code_setting_name),
                description = stringResource(id = R.string.view_source_code_setting_description),
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                onClick = {
                    localUriHandler.openUri("https://github.com/soupslurpr/Transcribro")
                }
            )
        }
        item {
            SettingsIconItem(
                name = stringResource(id = R.string.license_setting_name),
                description = stringResource(id = R.string.license_setting_description),
                icon = Icons.Filled.Info,
                onClick = onClickLicense
            )
        }
        item {
            SettingsIconItem(
                name = stringResource(id = R.string.privacy_policy_setting_name),
                description = stringResource(id = R.string.privacy_policy_setting_description),
                icon = Icons.Filled.Info,
                onClick = onClickPrivacyPolicy
            )
        }
        item {
            SettingsIconItem(
                name = stringResource(id = R.string.credits_setting_name),
                description = stringResource(id = R.string.credits_setting_description),
                icon = Icons.Filled.Info,
                onClick = onClickCredits
            )
        }
    }
}

@Composable
fun SettingsCategory(category: String) {
    Text(
        text = category,
        modifier = Modifier.padding(top = 8.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun SettingsSwitchItem(
    modifier: Modifier = Modifier,
    name: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        modifier = modifier
            .toggleable(
                value = checked,
                onValueChange = { onCheckedChange(it) }
            ),
        headlineContent = {
            Text(
                name,
                fontWeight = FontWeight.SemiBold
            )
        },
        supportingContent = run {
            if (description != null) {
                { Text(description) }
            } else {
                null
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null
            )
        }
    )
}
@Composable
fun SettingsTextFieldItem(
    modifier: Modifier = Modifier,
    name: String,
    description: String? = null,
    value: String,
    onValueChange: (String) -> Unit
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = value)) }

    // Sync TextFieldValue with external value if it changes
    LaunchedEffect(value) {
        if (textFieldValue.text != value) {
            textFieldValue = TextFieldValue(text = value)
        }
    }

    ListItem(
        modifier = modifier,
        headlineContent = {
            Text(name, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Column {
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = {
                        textFieldValue = it
                        onValueChange(it.text)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
        }
    )
}

@Composable
fun SettingsNumberFieldItem(
    modifier: Modifier = Modifier,
    name: String,
    description: String? = null,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    var text by remember { mutableStateOf(value.toString()) }

    ListItem(
        modifier = modifier,
        headlineContent = {
            Text(name, fontWeight = FontWeight.SemiBold)
        },
        supportingContent = {
            Column {
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        it.toIntOrNull()?.let(onValueChange)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
        }
    )
}

@Composable
fun SettingsIconItem(
    modifier: Modifier = Modifier,
    name: String,
    description: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        modifier = modifier
            .clickable(
                onClick = onClick
            ),
        headlineContent = {
            Text(
                name,
                fontWeight = FontWeight.SemiBold
            )
        },
        supportingContent = run {
            if (description != null) {
                { Text(description) }
            } else {
                null
            }
        },
        trailingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        }
    )
}