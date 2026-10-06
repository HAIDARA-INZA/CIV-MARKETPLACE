package ci.devsphere.civmarketplace.util

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import ci.devsphere.civmarketplace.BuildConfig
import ci.devsphere.civmarketplace.data.local.TokenManager
import ci.devsphere.civmarketplace.data.model.AppVersionDto
import ci.devsphere.civmarketplace.domain.repository.AppVersionRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class AppUpdateState {
    data object Hidden : AppUpdateState()
    data class Available(
        val version: AppVersionDto,
        val required: Boolean
    ) : AppUpdateState()

    data class Downloading(
        val version: AppVersionDto,
        val progress: Int?
    ) : AppUpdateState()

    data class DownloadReady(
        val version: AppVersionDto
    ) : AppUpdateState()

    data class DownloadError(
        val version: AppVersionDto,
        val message: String
    ) : AppUpdateState()
}

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appVersionRepository: AppVersionRepository,
    private val tokenManager: TokenManager
) : DefaultLifecycleObserver {

    companion object {
        const val FCM_TOPIC = "all_users"
        const val LANDING_URL = "https://haidara.devsphere.ci/civ-marketplace/"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<AppUpdateState>(AppUpdateState.Hidden)
    val state: StateFlow<AppUpdateState> = _state

    private var currentVersion: AppVersionDto? = null
    private var downloadId: Long? = null
    private var downloadReceiverRegistered = false

    override fun onStart(owner: LifecycleOwner) {
        subscribeToUpdatesTopic()
        checkForUpdate(force = false)
    }

    fun subscribeToUpdatesTopic() {
        FirebaseMessaging.getInstance()
            .subscribeToTopic(FCM_TOPIC)
            .addOnFailureListener { error ->
                android.util.Log.w("AppUpdateManager", "Unable to subscribe to app updates topic", error)
            }
    }

    fun checkForUpdate(force: Boolean = false) {
        scope.launch {
            val ignoredVersion = tokenManager.getIgnoredAppUpdateVersionCode().first()

            val result = appVersionRepository.getLatestVersion()
            result.onSuccess { release ->
                val installedVersionCode = BuildConfig.VERSION_CODE
                val required = AppVersionPolicy.isRequired(installedVersionCode, release)
                val shouldOffer = AppVersionPolicy.shouldOffer(
                    installedVersionCode = installedVersionCode,
                    release = release,
                    ignoredVersionCode = ignoredVersion
                )

                if (release.versionCode > installedVersionCode && (force || shouldOffer)) {
                    currentVersion = release
                    _state.value = AppUpdateState.Available(
                        version = release,
                        required = required
                    )
                }
            }.onFailure {
                // silent fail: we do not block the user if the update check fails
            }
        }
    }

    fun dismiss(version: AppVersionDto, required: Boolean) {
        if (!required) {
            scope.launch {
                tokenManager.setIgnoredAppUpdateVersionCode(version.versionCode)
            }
        }
        _state.value = AppUpdateState.Hidden
    }

    fun download(version: AppVersionDto) {
        val apkUrl = version.apkUrl.trim()
        val parsedUri = runCatching { Uri.parse(apkUrl) }.getOrNull()
        if (apkUrl.isBlank() || parsedUri == null || parsedUri.scheme != "https") {
            _state.value = AppUpdateState.DownloadError(
                version = version,
                message = "Le lien APK doit être sécurisé (HTTPS)."
            )
            return
        }

        _state.value = AppUpdateState.Downloading(version, null)

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Mise à jour CIV Marketplace")
            .setDescription("Téléchargement de la version ${version.versionName}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "civ_marketplace_${version.versionCode}.apk"
            )

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        registerDownloadReceiver(downloadManager, version)
    }

    fun retryDownload(version: AppVersionDto) {
        download(version)
    }

    fun install(activity: Activity) {
        val version = currentVersion ?: return
        val file = File(
            activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "civ_marketplace_${version.versionCode}.apk"
        )

        if (!file.exists()) {
            _state.value = AppUpdateState.DownloadError(
                version = version,
                message = "Le fichier APK n’a pas été trouvé."
            )
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            intent.data = Uri.parse("package:${activity.packageName}")
            activity.startActivity(intent)
            return
        }

        val uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            file
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        activity.startActivity(installIntent)
    }

    private fun registerDownloadReceiver(downloadManager: DownloadManager, version: AppVersionDto) {
        if (downloadReceiverRegistered) return

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id != downloadId) return

                val query = DownloadManager.Query().setFilterById(id)
                val cursor = downloadManager.query(query)
                if (!cursor.moveToFirst()) {
                    cursor.close()
                    _state.value = AppUpdateState.DownloadError(
                        version = version,
                        message = "Le téléchargement a échoué." 
                    )
                    return
                }

                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = if (statusIndex >= 0) cursor.getInt(statusIndex) else DownloadManager.STATUS_FAILED
                cursor.close()

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        _state.value = AppUpdateState.DownloadReady(version)
                    }
                    DownloadManager.STATUS_FAILED -> {
                        _state.value = AppUpdateState.DownloadError(
                            version = version,
                            message = "Le téléchargement a échoué. Vérifie votre connexion et réessaie."
                        )
                    }
                }
            }
        }

        context.applicationContext.registerReceiver(
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
        downloadReceiverRegistered = true
    }
}

