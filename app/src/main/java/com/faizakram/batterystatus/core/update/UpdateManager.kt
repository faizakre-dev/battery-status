package com.faizakram.batterystatus.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/**
 * Singleton updater that talks to the project's GitHub releases: it checks the
 * latest release against the installed version, downloads the APK asset into
 * app cache and hands it to the system installer via `FileProvider`.
 *
 * State lives in a process-wide singleton (not the ViewModel) so an in-flight
 * download keeps running when the user leaves the Settings tab.
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var downloadJob: Job? = null

    @Volatile
    private var release: ReleaseInfo? = null

    @Volatile
    private var apkFile: File? = null

    /** The installed app's `versionName`, normalized once at construction. */
    val currentVersion: String = runCatching {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName.orEmpty()
    }.getOrDefault("").ifBlank { UNKNOWN_VERSION }

    /** Fetches the latest release and transitions [state] accordingly. */
    fun checkForUpdates() {
        val current = _state.value
        if (current is UpdateState.Checking || current is UpdateState.Downloading) return
        _state.value = UpdateState.Checking
        scope.launch {
            try {
                val latest = fetchLatestRelease()
                release = latest
                _state.value = if (isNewerVersion(latest.version, currentVersion)) {
                    val cached = cachedApkFor(latest.version)
                    if (cached != null) {
                        apkFile = cached
                        UpdateState.ReadyToInstall(latest)
                    } else {
                        UpdateState.Available(latest)
                    }
                } else {
                    UpdateState.UpToDate
                }
            } catch (e: UpdateException) {
                _state.value = UpdateState.Failed(e.error)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = UpdateState.Failed(UpdateError.NETWORK)
            }
        }
    }

    /** Downloads the latest known release's APK, reporting percent progress. */
    fun downloadUpdate() {
        val latest = release ?: return
        if (_state.value !is UpdateState.Available && _state.value !is UpdateState.Failed) return
        if (downloadJob?.isActive == true) return

        downloadJob = scope.launch {
            _state.value = UpdateState.Downloading(latest, progressPercent = null)
            try {
                val file = downloadApk(latest) { percent ->
                    val current = _state.value
                    if (current is UpdateState.Downloading) {
                        _state.value = current.copy(progressPercent = percent)
                    }
                }
                apkFile = file
                _state.value = UpdateState.ReadyToInstall(latest)
            } catch (e: UpdateException) {
                _state.value = UpdateState.Failed(e.error, latest)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = UpdateState.Failed(UpdateError.NETWORK, latest)
            }
        }
    }

    /** Whether the system will show the install prompt without extra steps. */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Opens the per-app "Install unknown apps" toggle for this package. */
    fun openInstallPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${context.packageName}".toUri(),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Launches the system installer for the downloaded APK. */
    fun installUpdate(): Boolean {
        val file = apkFile?.takeIf { it.exists() } ?: return false
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun fetchLatestRelease(): ReleaseInfo = withContext(Dispatchers.IO) {
        val connection = URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = READ_TIMEOUT_MILLIS
        connection.setRequestProperty(HEADER_ACCEPT, HEADER_ACCEPT_GITHUB)
        connection.setRequestProperty(HEADER_USER_AGENT, USER_AGENT)
        try {
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_NOT_FOUND -> throw UpdateException(UpdateError.NOT_FOUND)
                in HttpURLConnection.HTTP_OK until HttpURLConnection.HTTP_MULT_CHOICE -> Unit
                else -> throw UpdateException(UpdateError.NETWORK)
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseRelease(body)
        } catch (e: UpdateException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: JSONException) {
            throw UpdateException(UpdateError.PARSE)
        } catch (e: IOException) {
            throw UpdateException(UpdateError.NETWORK)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRelease(body: String): ReleaseInfo {
        val json = JSONObject(body)
        val tag = json.optString(TAG_NAME)
        if (tag.isBlank()) throw UpdateException(UpdateError.PARSE)

        val assets = json.optJSONArray(ASSETS)
        var apkUrl: String? = null
        if (assets != null) {
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                if (asset.optString(NAME) == APK_ASSET_NAME) {
                    apkUrl = asset.optString(BROWSER_DOWNLOAD_URL).ifBlank { null }
                    break
                }
            }
        }
        if (apkUrl == null) throw UpdateException(UpdateError.NO_APK)

        return ReleaseInfo(
            tagName = tag,
            version = normalizeVersion(tag),
            name = json.optString(NAME).ifBlank { tag },
            notes = json.optString(BODY),
            apkUrl = apkUrl,
            publishedAt = json.optString(PUBLISHED_AT),
        )
    }

    private suspend fun downloadApk(
        latest: ReleaseInfo,
        onProgress: (Int?) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val directory = apkDirectory()
        directory.listFiles()?.forEach { file ->
            if (file.name.endsWith(PART_SUFFIX)) file.delete()
        }

        val target = File(directory, apkFileName(latest.version))
        val partial = File(directory, apkFileName(latest.version) + PART_SUFFIX)
        val connection = URL(latest.apkUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = READ_TIMEOUT_MILLIS
        connection.setRequestProperty(HEADER_USER_AGENT, USER_AGENT)
        try {
            if (connection.responseCode !in HttpURLConnection.HTTP_OK until
                HttpURLConnection.HTTP_MULT_CHOICE
            ) {
                throw UpdateException(UpdateError.NETWORK)
            }
            val totalBytes = connection.contentLengthLong
            var writtenBytes = 0L
            var lastPercent = -1
            connection.inputStream.use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        writtenBytes += read
                        if (totalBytes > 0) {
                            val percent =
                                ((writtenBytes * 100) / totalBytes).toInt().coerceIn(0, 99)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                    output.fd.sync()
                }
            }
            onProgress(100)
        } catch (e: UpdateException) {
            partial.delete()
            throw e
        } catch (e: CancellationException) {
            partial.delete()
            throw e
        } catch (e: IOException) {
            partial.delete()
            throw UpdateException(UpdateError.NETWORK)
        } finally {
            connection.disconnect()
        }

        if (target.exists()) target.delete()
        if (!partial.renameTo(target)) {
            partial.delete()
            throw UpdateException(UpdateError.STORAGE)
        }
        target
    }

    private fun apkDirectory(): File {
        val directory = File(context.cacheDir, APK_DIR_NAME)
        if (!directory.exists() && !directory.mkdirs()) {
            throw UpdateException(UpdateError.STORAGE)
        }
        return directory
    }

    private fun cachedApkFor(version: String): File? =
        File(apkDirectory(), apkFileName(version)).takeIf { it.exists() && it.length() > 0 }

    private fun apkFileName(version: String): String = "update-$version.apk"

    private companion object {
        const val REPO_OWNER = "faizakre-dev"
        const val REPO_NAME = "battery-status"
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
        const val APK_ASSET_NAME = "app-debug.apk"
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        const val APK_DIR_NAME = "apk"
        const val PART_SUFFIX = ".part"
        const val UNKNOWN_VERSION = "unknown"

        const val HEADER_ACCEPT = "Accept"
        const val HEADER_ACCEPT_GITHUB = "application/vnd.github+json"
        const val HEADER_USER_AGENT = "User-Agent"
        const val USER_AGENT = "BatteryHealth-Updater"

        const val TAG_NAME = "tag_name"
        const val NAME = "name"
        const val BODY = "body"
        const val ASSETS = "assets"
        const val BROWSER_DOWNLOAD_URL = "browser_download_url"
        const val PUBLISHED_AT = "published_at"

        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 30_000
        const val DOWNLOAD_BUFFER_BYTES = 64 * 1024
    }
}
