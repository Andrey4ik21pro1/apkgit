package com.apkgit

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap

import androidx.compose.runtime.*

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

import java.io.File
import java.net.URL

@Serializable
private data class GitHubAsset(val name: String, @SerialName("browser_download_url") val browserDownloadUrl: String)

@Serializable
private data class GitHubReleaseInfo(@SerialName("tag_name") val tagName: String, val assets: List<GitHubAsset> = emptyList())

@Serializable
data class AppEntry(
    val name: String,
    val owner: String,
    val repo: String,
    val filter: String,
    val packageName: String,
    val installedVersion: String,
    val latestVersion: String
)

@Serializable
data class AppConfigData(
    val apps: List<AppEntry>
)

object ConfigManager {
    var current: AppConfigData by mutableStateOf(AppConfigData(emptyList()))
        private set

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    private val downloadSemaphore = Semaphore(3)

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private const val fileName = "config.json"

    /// Logic ///

    private fun createDefaultConfig(context: Context, forceSave: Boolean = false): AppConfigData {
        val appName = context.getString(R.string.app_name)
        val packageName = context.packageName
        val githubLink = context.getString(R.string.about_github_link)

        val match = Regex("github\\.com/([^/]+)/([^/]+)").find(githubLink)
        val (owner, repo) = match?.let { it.groupValues[1] to it.groupValues[2] }
            ?: githubLink.trim().split("/").let { parts ->
                if (parts.size == 2 && !githubLink.contains(":")) {
                    parts[0] to parts[1]
                } else {
                    "" to ""
                }
            }

        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        val appVersion = packageInfo.versionName ?: "N/A"

        val data = AppConfigData(apps = listOf(
            AppEntry(
                name = appName,
                owner = owner,
                repo = repo,
                filter = "ApkGit-v*.apk",
                packageName = packageName,
                installedVersion = appVersion,
                latestVersion = "N/A"
            )
        ))

        if (forceSave) {
            scope.launch {
                mutex.withLock { save(context, data) }
                checkAllUpdates(context)
            }
        }

        return data
    }

    suspend fun save(context: Context, data: AppConfigData) {
        if (current == data) return
        try {
            withContext(Dispatchers.IO) {
                val jsonString = json.encodeToString(data)
                current = data
                File(context.filesDir, fileName).writeText(jsonString)
            }
        } catch (e: Exception) {
            val message = context.getString(R.string.error, e.message ?: "Failed to save config")
            showToast(context, message)
        }
    }

    private suspend fun fetchGithubApi(owner: String, repo: String, token: String? = null): String {
        val apiUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
        return withContext(Dispatchers.IO) {
            val connection = URL(apiUrl).openConnection() as java.net.HttpURLConnection
            try {
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                connection.setRequestProperty("Accept", "application/vnd.github+json")

                if (!token.isNullOrBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer $token")
                }

                val code = connection.responseCode
                if (code !in 200..299) {
                    val errorJson = connection.errorStream?.bufferedReader()?.readText()
                    val extractedMessage = try {
                        errorJson?.let {
                            json.parseToJsonElement(it).jsonObject["message"]?.jsonPrimitive?.content
                        }
                    } catch (e: Exception) {
                        null
                    }
                    val errorMsg = extractedMessage ?: connection.responseMessage
                    throw Exception("GitHub API Error $code: $errorMsg")
                }
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
    }

    /// MainActivity ///

    fun load(context: Context) {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            try {
                val content = file.readText()
                current = json.decodeFromString<AppConfigData>(content)
                scope.launch { refreshInstalledVersions(context) }
            } catch (e: Exception) {
                if (e is SerializationException || e is IllegalArgumentException) {
                    current = createDefaultConfig(context, forceSave = true)
                } else {
                    current = createDefaultConfig(context, forceSave = false)
                    e.printStackTrace()
                }
            }
        } else {
            current = createDefaultConfig(context, forceSave = true)
        }
    }

    fun refreshInstalledVersions(context: Context) {
        scope.launch {
            if (current.apps.isEmpty()) return@launch

            val pm = context.packageManager
            mutex.withLock {
                val updatedApps = current.apps.map { app ->
                    val installedVersion = getCleanedVersionName(pm, app.packageName)
                    if (app.installedVersion != installedVersion) {
                        app.copy(installedVersion = installedVersion)
                    } else {
                        app
                    }
                }
                if (updatedApps != current.apps) {
                    save(context, current.copy(apps = updatedApps))
                }
            }
        }
    }

    suspend fun checkAllUpdates(context: Context) {
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val token = prefs.getString("github_token", null)

            val updatedAppsList = current.apps.map { app ->
                async {
                    try {
                        val resultJson = downloadSemaphore.withPermit { fetchGithubApi(app.owner, app.repo, token) }
                        val releaseInfo = json.decodeFromString<GitHubReleaseInfo>(resultJson)

                        val asset = releaseInfo.assets.firstOrNull { createFilterRegex(app.filter).matches(it.name) }
                        val newVersion = asset?.let { extractVersionFromFilename(it.name, app.filter) }
                            ?: releaseInfo.tagName

                        val installedVersion = getCleanedVersionName(context.packageManager, app.packageName)

                        app.copy(latestVersion = newVersion, installedVersion = installedVersion)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        app
                    }
                }
            }.awaitAll()

            mutex.withLock {
                if (updatedAppsList != current.apps) {
                    save(context, current.copy(apps = updatedAppsList))
                }
            }
        }
    }

    /// BackupActivity ///

    fun importConfig(context: Context, content: String) {
        scope.launch(Dispatchers.IO) {
            try {
                if (content.isBlank()) throw Exception("File is empty")
                val newData = json.decodeFromString<AppConfigData>(content)
                if (newData.apps.isEmpty()) throw Exception("Config contains no apps")

                mutex.withLock { save(context, newData) }
                val message = context.getString(R.string.success)
                showToast(context, message, Toast.LENGTH_SHORT)

            } catch (e: Exception) {
                val message = context.getString(R.string.error, e.message ?: "Invalid config format")
                showToast(context, message)
            }
        }
    }

    fun clearCache(context: Context, type: String) {
        try {
            val cacheDir = context.cacheDir
            val files = cacheDir.listFiles { file ->
                file.isFile && file.extension.equals(type, ignoreCase = true)
            }
            files?.forEach { file ->
                try { file.delete() }
                catch (e: Exception) {}
            }

            val message = context.getString(R.string.success)
            showToast(context, message, Toast.LENGTH_SHORT)

        } catch (e: Exception) {
            val message = context.getString(R.string.error, e.message ?: "Unknown error")
            showToast(context, message)
        }
    }

    /// AppList ///

    suspend fun addAppFromUrl(context: Context, owner: String, repo: String, filter: String) {
        withContext(Dispatchers.IO) {
            var apkFile: File? = null
            try {
                apkFile = downloadLatestApk(context, owner, repo, filter, showToast = false)

                val pm = context.packageManager
                @Suppress("DEPRECATION")
                val packageInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
                    ?: throw Exception("Could not parse APK file")

                val appInfo = packageInfo.applicationInfo ?: throw Exception("ApplicationInfo is null")
                appInfo.sourceDir = apkFile.absolutePath
                appInfo.publicSourceDir = apkFile.absolutePath

                val appName = appInfo.loadLabel(pm).toString()
                val packageName = appInfo.packageName
                val latestVersion = extractVersionFromFilename(apkFile.name, filter) ?: packageInfo.versionName ?: "N/A"

                try {
                    val iconDrawable = appInfo.loadIcon(pm)
                    val iconBitmap = iconDrawable.toBitmap()
                    val iconFile = File(context.cacheDir, "$packageName.png")
                    java.io.FileOutputStream(iconFile).use { iconBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val installedVersion = getCleanedVersionName(pm, packageName)
                val newApp = AppEntry(
                    name = appName, owner = owner, repo = repo, filter = filter,
                    packageName = packageName, installedVersion = installedVersion, latestVersion = latestVersion
                )
                mutex.withLock {
                    val newConfig = current.copy(apps = current.apps + newApp)
                    save(context, newConfig)
                }
                installApk(context, apkFile)
            } catch (e: Exception) {
                try { apkFile?.delete() } catch (e: Exception) {}
                val message = context.getString(R.string.error, e.message ?: "Unknown error")
                showToast(context, message)
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            showToast(context, context.getString(R.string.permission_install_request))
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }
        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }

    suspend fun downloadLatestApk(context: Context, owner: String, repo: String, filter: String, showToast: Boolean = true): File {
        return try {
            val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val token = prefs.getString("github_token", null)

            val resultJson = fetchGithubApi(owner, repo, token)
            val releaseInfo = json.decodeFromString<GitHubReleaseInfo>(resultJson)

            val asset = releaseInfo.assets.firstOrNull { createFilterRegex(filter).matches(it.name) }
                ?: throw Exception("No matching APK found for filter")

            val apkFile = File(context.cacheDir, asset.name)
            if (!apkFile.exists()) {
                if (showToast) {
                    val message = context.getString(R.string.downloading)
                    showToast(context, message, Toast.LENGTH_SHORT)
                }
                val connection = URL(asset.browserDownloadUrl).openConnection() as java.net.HttpURLConnection
                try {
                    connection.connectTimeout = 10000
                    connection.readTimeout = 300000 // press F to the Indian bros on that potato speed, im a lazy ass and i dont want fix it

                    connection.inputStream.use { input ->
                        apkFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    runCatching { if (apkFile.exists()) apkFile.delete() }
                    throw e
                } finally {
                    connection.disconnect()
                }
            }
            apkFile
        } catch (e: java.net.UnknownHostException) {
            throw Exception("No internet connection")
        } catch (e: java.net.SocketTimeoutException) {
            throw Exception("Connection timed out")
        } catch (e: Exception) {
            throw e
        }
    }

    fun downloadAndInstallLatest(context: Context, app: AppEntry) {
        scope.launch(Dispatchers.IO) {
            try {
                val apkFile = downloadLatestApk(context, app.owner, app.repo, app.filter)
                installApk(context, apkFile)
            } catch (e: Exception) {
                val message = context.getString(R.string.error, e.message ?: "Unknown error")
                showToast(context, message)
            }
        }
    }

    fun deleteApp(context: Context, packageName: String) {
        scope.launch {
            mutex.withLock {
                val newApps = current.apps.filter { it.packageName != packageName }
                if (newApps != current.apps) {
                    save(context, current.copy(apps = newApps))
                }
            }
        }
    }

    fun reorderApps(context: Context, newAppOrder: List<AppEntry>) {
        scope.launch {
            mutex.withLock {
                val currentMap = current.apps.associateBy { it.packageName }
                val updatedApps = newAppOrder.mapNotNull { currentMap[it.packageName] }
                if (updatedApps != current.apps) {
                    save(context, current.copy(apps = updatedApps))
                }
            }
        }
    }

    /// Other ///

    private fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_LONG) {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(context, message, duration).show()
        }
    }

    private fun getCleanedVersionName(pm: PackageManager, packageName: String): String {
        return try {
            pm.getPackageInfo(packageName, 0).versionName?.split('-')?.first() ?: "N/A"
        } catch (e: PackageManager.NameNotFoundException) {
            "N/A"
        }
    }

    private fun extractVersionFromFilename(filename: String, filter: String): String? {
        if (!filter.contains("*")) return null
        val regexPattern = filter.replace(".", "\\.").replace("*", "(.+)")
        return Regex(regexPattern).find(filename)?.groupValues?.get(1)
    }

    private fun createFilterRegex(filter: String): Regex {
        val pattern = filter.replace(".", "\\.").replace("*", ".*")
        return Regex(pattern)
    }
}