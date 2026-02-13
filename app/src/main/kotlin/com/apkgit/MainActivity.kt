package com.apkgit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun HomeScreen(searchQuery: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCheckingUpdates by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 14.dp)
    ) {
        if (isCheckingUpdates) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        AppList(
            searchQuery = searchQuery,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    if (!isCheckingUpdates) {
                        scope.launch {
                            isCheckingUpdates = true
                            ConfigManager.checkAllUpdates(context)
                            Toast.makeText(context, context.getString(R.string.search_updates_checked), Toast.LENGTH_SHORT).show()
                            isCheckingUpdates = false
                        }
                    }
                },
                enabled = !isCheckingUpdates
            ) {
                Text(stringResource(R.string.search_updates))
            }
        }
    }
}

@Composable
fun LanguageSelectorDialog(
    initialLang: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val languageCodes = listOf(
        "en", // English
        "ru", // Russian
        "uk", // Ukrainian
        "de", // German
        "fr", // French
        "es", // Spanish
        "it", // Italian
        "pt", // Portuguese
        "pl", // Polish
        "nl", // Dutch
        "tr", // Turkish
        "zh", // Chinese
        "ja", // Japanese
        "ko", // Korean
        "ar", // Arabic
        "hi"  // Hindi
    )
    val languages = languageCodes.map { code ->
        val locale = Locale.forLanguageTag(code)
        val name = locale.getDisplayLanguage(locale)
            .replaceFirstChar { it.titlecase(locale) }
        code to name
    }

    var selectedLang by remember { mutableStateOf(initialLang) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language_dialog_title)) },
        text = {
            LazyColumn(
                Modifier
                    .selectableGroup()
                    .heightIn(max = 168.dp)
            ) {
                items(languages) { (key, name) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (key == selectedLang),
                                onClick = { selectedLang = key },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (key == selectedLang),
                            onClick = null
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedLang) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun PaletteSelectorDialog(
    initialTheme: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val themes = listOf(
        "system" to stringResource(R.string.theme_system),
        "light" to stringResource(R.string.theme_light),
        "dark" to stringResource(R.string.theme_dark)
    )

    var selectedTheme by remember { mutableStateOf(initialTheme) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme_dialog_title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                themes.forEach { (key, name) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (key == selectedTheme),
                                onClick = { selectedTheme = key },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (key == selectedTheme),
                            onClick = null
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedTheme) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

@Composable
fun SettingsItem(
    title: String,
    icon: Int? = null,
    subtitle: String? = null,
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onClick
    ) {
        ListItem(
            headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
            supportingContent = subtitle?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
            leadingContent = icon?.let {
                {
                    Icon(
                        painter = painterResource(it),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }

    val currentLocale = remember {
        prefs.getString("language", Locale.getDefault().language) ?: "en"
    }
    val currentTheme = remember {
        prefs.getString("theme", "system") ?: "system"
    }

    var githubToken by rememberSaveable {
        mutableStateOf(
            prefs.getString("github_token", "") ?: ""
        )
    }

    val focusManager = LocalFocusManager.current
    var wasFocused by remember { mutableStateOf(false) }

    BackHandler(enabled = wasFocused) {
        focusManager.clearFocus()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingsSectionTitle(stringResource(R.string.settings_section_account))
        }
        item {
            TextField(
                value = githubToken,
                onValueChange = { githubToken = it },
                label = { Text(stringResource(R.string.personal_access_token)) },
                placeholder = { Text(stringResource(R.string.personal_access_token)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp).offset(x = 4.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .onFocusChanged {
                        if (wasFocused && !it.isFocused) {
                            prefs.edit().putString("github_token", githubToken).apply()
                        }
                        wasFocused = it.isFocused
                    },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        item {
            SettingsSectionTitle(stringResource(R.string.settings_section_interface))
        }
        item {
            SettingsCard {
                SettingsItem(stringResource(R.string.settings_language), R.drawable.ic_language,
                    stringResource(R.string.settings_language_subtitle),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    showLanguageDialog = true
                }
                SettingsItem(stringResource(R.string.settings_palette), R.drawable.ic_palette,
                    stringResource(R.string.settings_palette_subtitle),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                ) {
                    showPaletteDialog = true
                }
            }
        }

        item {
            SettingsSectionTitle(stringResource(R.string.settings_section_system))
        }
        item {
            SettingsCard {
                SettingsItem(stringResource(R.string.settings_build), R.drawable.ic_build,
                    stringResource(R.string.settings_build_subtitle),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    context.startActivity(Intent(context, BackupActivity::class.java))
                }

                SettingsItem(stringResource(R.string.settings_info), R.drawable.ic_info,
                    stringResource(R.string.settings_info_subtitle),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                ) {
                    context.startActivity(Intent(context, AboutActivity::class.java))
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectorDialog(
            initialLang = currentLocale,
            onDismiss = { showLanguageDialog = false },
            onConfirm = { newLang ->
                prefs.edit().putString("language", newLang).apply()
                showLanguageDialog = false
                (context as? Activity)?.recreate()
            }
        )
    }

    if (showPaletteDialog) {
        PaletteSelectorDialog(
            initialTheme = currentTheme,
            onDismiss = { showPaletteDialog = false },
            onConfirm = { newTheme ->
                prefs.edit().putString("theme", newTheme).apply()
                showPaletteDialog = false
                (context as? Activity)?.recreate()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkGitScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchQuery by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute) {
        focusManager.clearFocus(force = true)
        isFocused = false
    }

    BackHandler(enabled = isFocused) {
        focusManager.clearFocus(force = true)
        isFocused = false
        if (searchQuery.isNotEmpty()) { searchQuery = "" }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                DockedSearchBar(
                    modifier = Modifier.fillMaxWidth(),
                    expanded = false,
                    onExpandedChange = {},
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = { focusManager.clearFocus() },
                            modifier = Modifier.onFocusChanged { isFocused = it.isFocused },
                            expanded = false,
                            onExpandedChange = {},
                            placeholder = { Text(stringResource(R.string.search_text)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_search),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.offset(x = 8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_close),
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        )
                    },
                    content = {}
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(
                                if (currentRoute == "home") R.drawable.ic_home_filled else R.drawable.ic_home
                            ),
                            contentDescription = stringResource(R.string.home)
                        )
                    },
                    label = { Text(stringResource(R.string.home)) },
                    selected = currentRoute == "home",
                    onClick = { if (currentRoute != "home") navController.navigate("home") },
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(
                                if (currentRoute == "settings") R.drawable.ic_settings_filled else R.drawable.ic_settings
                            ),
                            contentDescription = stringResource(R.string.settings)
                        )
                    },
                    label = { Text(stringResource(R.string.settings)) },
                    selected = currentRoute == "settings",
                    onClick = { if (currentRoute != "settings") navController.navigate("settings") },
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(searchQuery) }
            composable("settings") { SettingsScreen() }
        }
    }
}

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ConfigManager.load(this)
        setContent {
            ApkGitTheme(darkTheme = isDarkTheme()) {
                ApkGitScreen()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        ConfigManager.refreshInstalledVersions(this)
    }
}