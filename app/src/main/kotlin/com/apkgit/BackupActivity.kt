package com.apkgit

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.material3.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val appName = stringResource(R.string.app_name)
    val currentDate = java.time.LocalDate.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
    val fileName = "$appName-$currentDate-config.json"

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                val configText = ConfigManager.json.encodeToString(
                    AppConfigData.serializer(), ConfigManager.current
                )
                outputStream.write(configText.toByteArray())
                Toast.makeText(context, context.getString(R.string.success), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val loadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                val content = reader.readText()
                ConfigManager.importConfig(context, content)
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_build)) },
                navigationIcon = {
                    Surface(
                        onClick = onBackClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSectionTitle(stringResource(R.string.backup_section_config))
            }
            item {
                SettingsCard {
                    SettingsItem(
                        title = stringResource(R.string.backup_save),
                        icon = R.drawable.ic_cloud_upload,
                        subtitle = stringResource(R.string.backup_save_subtitle),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        saveLauncher.launch(fileName)
                    }

                    SettingsItem(
                        title = stringResource(R.string.backup_load),
                        icon = R.drawable.ic_cloud_download,
                        subtitle = stringResource(R.string.backup_load_subtitle),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    ) {
                        loadLauncher.launch(arrayOf("application/json", "application/octet-stream"))
                    }
                }
            }

            item {
                SettingsSectionTitle(stringResource(R.string.backup_section_cache))
            }
            item {
                SettingsCard {
                    SettingsItem(
                        title = stringResource(R.string.backup_clear_apk_cache),
                        icon = R.drawable.ic_delete,
                        subtitle = stringResource(R.string.backup_clear_apk_cache_subtitle),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        ConfigManager.clearCache(context, "apk")
                    }

                    SettingsItem(
                        title = stringResource(R.string.backup_clear_icons_cache),
                        icon = R.drawable.ic_delete,
                        subtitle = stringResource(R.string.backup_clear_icons_cache_subtitle),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    ) {
                        ConfigManager.clearCache(context, "png")
                    }
                }
            }
        }
    }
}

class BackupActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApkGitTheme(darkTheme = isDarkTheme()) {
                BackupScreen(onBackClick = { finish() })
            }
        }
    }
}