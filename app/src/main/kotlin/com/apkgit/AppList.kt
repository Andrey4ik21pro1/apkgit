package com.apkgit

import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.drawable.toBitmap

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState

import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppBottomSheet(onDismiss: () -> Unit, onAddClick: suspend (owner: String, repo: String, filter: String) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    var repoOwner by remember { mutableStateOf("") }
    var repoName by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("") }

    val isOwnerValid = remember(repoOwner) {
        repoOwner.trim().none { it.isWhitespace() || it == '/' || it == '\\' }
    }
    val isRepoValid = remember(repoName) {
        repoName.trim().none { it.isWhitespace() || it == '/' || it == '\\' }
    }
    val isFilterValid = remember(filter) {
        filter.trim().none { it == '/' || it == '\\' }
    }

    val finalFilter = remember(filter) {
        val trimmed = filter.trim()
        if (trimmed.isBlank() || trimmed.endsWith(".apk")) trimmed else "$trimmed.apk"
    }
    val isDuplicate = remember(repoOwner, repoName, finalFilter) {
        ConfigManager.current.apps.any { it.owner == repoOwner.trim() && it.repo == repoName.trim() && it.filter == finalFilter }
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = stringResource(R.string.apps_add), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = repoOwner,
                onValueChange = { repoOwner = it },
                label = { Text(stringResource(R.string.add_owner)) },
                isError = !isOwnerValid,
                supportingText = if (!isOwnerValid) {
                    { Text(stringResource(R.string.invalid_characters)) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = repoName,
                onValueChange = { repoName = it },
                label = { Text(stringResource(R.string.add_repo)) },
                isError = !isRepoValid,
                supportingText = if (!isRepoValid) {
                    { Text(stringResource(R.string.invalid_characters)) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                label = { Text(stringResource(R.string.add_filter)) },
                isError = isDuplicate || !isFilterValid,
                supportingText = if (isDuplicate) {
                    { Text(stringResource(R.string.duplicate)) }
                } else if (!isFilterValid) {
                    { Text(stringResource(R.string.invalid_characters)) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            onAddClick(repoOwner.trim(), repoName.trim(), finalFilter)
                            onDismiss()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && !isDuplicate &&
                        repoOwner.isNotBlank() && isOwnerValid &&
                        repoName.isNotBlank() && isRepoValid &&
                        filter.isNotBlank() && isFilterValid,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.add))
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(onDismiss: () -> Unit, app: AppEntry) {
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                AppIcon(app = app, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            InfoRow(label = stringResource(R.string.add_owner), value = app.owner)
            InfoRow(label = stringResource(R.string.add_repo), value = app.repo)
            InfoRow(label = stringResource(R.string.add_filter), value = app.filter)

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            InfoRow(label = stringResource(R.string.installed_version), value = app.installedVersion)
            InfoRow(label = stringResource(R.string.latest_version), value = app.latestVersion)

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/${app.owner}/${app.repo}"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_github),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.view_on_github))
            }
        }
    }
}

@Composable
fun AppIcon(app: AppEntry, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val iconBitmap by produceState<Bitmap?>(initialValue = null, app.packageName) {
        value = try {
            context.packageManager.getApplicationIcon(app.packageName).toBitmap()
        } catch (e: PackageManager.NameNotFoundException) {
            val iconFile = File(context.cacheDir, "${app.packageName}.png")
            if (iconFile.exists()) BitmapFactory.decodeFile(iconFile.absolutePath)
            else null
        }
    }

    val painter = iconBitmap?.let { BitmapPainter(it.asImageBitmap()) }
        ?: painterResource(R.drawable.ic_help)

    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
fun AppItem(
    app: AppEntry,
    onUpdateClick: () -> Unit,
    isInDragMode: Boolean,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier
) {
    val installedVersion = app.installedVersion.trim()
    val latestVersion = app.latestVersion.trim()
    val showUpdateIcon = installedVersion != latestVersion && latestVersion != "N/A" && latestVersion.isNotBlank()

    val version = when {
        installedVersion == "N/A" || installedVersion.isBlank() -> ""
        showUpdateIcon -> " ($installedVersion -> $latestVersion)"
        else -> " ($installedVersion)"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isInDragMode) {
            Icon(
                painter = painterResource(R.drawable.ic_drag_handle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = dragHandleModifier.padding(end = 8.dp)
            )
        }
        AppIcon(app = app, modifier = Modifier.size(50.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${app.name}$version",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isInDragMode) {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else if (showUpdateIcon) {
            IconButton(onClick = onUpdateClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_apk_install),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppList(searchQuery: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var showBottomSheet by remember { mutableStateOf(false) }
    var isInDragMode by remember { mutableStateOf(false) }
    var appToDelete by remember { mutableStateOf<AppEntry?>(null) }
    var appForInfo by remember { mutableStateOf<AppEntry?>(null) }

    val apps = ConfigManager.current.apps
    val localApps = remember { mutableStateListOf<AppEntry>().apply { addAll(apps) } }

    LaunchedEffect(apps) {
        if (!isInDragMode) {
            if (localApps.toList() != apps) {
                localApps.clear()
                localApps.addAll(apps)
            }
        }
    }

    BackHandler(enabled = isInDragMode) {
        val currentList = localApps.toList()
        if (currentList != apps) {
            ConfigManager.reorderApps(context, currentList)
        }
        isInDragMode = false
    }

    val appsToDisplay = when {
        searchQuery.isBlank() -> localApps

        else -> localApps.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localApps.add(to.index, localApps.removeAt(from.index))
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = appsToDisplay,
            key = { _, app -> app.packageName }
        ) { index, app ->
            ReorderableItem(
                state = reorderableState,
                key = app.packageName
            ) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)

                AppItem(
                    app = app,
                    onUpdateClick = {
                        ConfigManager.downloadAndInstallLatest(context, app)
                    },
                    isInDragMode = isInDragMode,
                    onDeleteClick = { appToDelete = app },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { shadowElevation = elevation.toPx() }
                        .combinedClickable(
                            onClick = {
                                if (!isInDragMode) {
                                    appForInfo = app
                                }
                            },
                            onLongClick = {
                                if (!isInDragMode) {
                                    isInDragMode = true
                                }
                            }
                        ),
                    dragHandleModifier = if (isInDragMode) Modifier.draggableHandle() else Modifier
                )
            }
        }

        if (!isInDragMode) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBottomSheet = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.apps_add),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }

    appForInfo?.let { app ->
        InfoBottomSheet(
            onDismiss = { appForInfo = null },
            app = app
        )
    }

    appToDelete?.let { app ->
        AlertDialog(
            onDismissRequest = { appToDelete = null },
            title = { Text(stringResource(R.string.are_you_sure)) },
            text = {
                Text(stringResource(R.string.remove, app.name))
            },
            confirmButton = {
                Button(
                    onClick = {
                        localApps.remove(app)
                        ConfigManager.reorderApps(context, localApps.toList())
                        if (localApps.isEmpty()) isInDragMode = false
                        appToDelete = null
                    }
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { appToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showBottomSheet) {
        AddAppBottomSheet(
            onDismiss = { showBottomSheet = false },
            onAddClick = { owner, repo, filter ->
                ConfigManager.addAppFromUrl(context, owner, repo, filter)
            }
        )
    }
}