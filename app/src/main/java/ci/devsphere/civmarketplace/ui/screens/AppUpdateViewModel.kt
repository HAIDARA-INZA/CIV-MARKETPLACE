package ci.devsphere.civmarketplace.ui.screens

import android.app.Activity
import androidx.lifecycle.ViewModel
import ci.devsphere.civmarketplace.data.model.AppVersionDto
import ci.devsphere.civmarketplace.util.AppUpdateManager
import ci.devsphere.civmarketplace.util.AppUpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val appUpdateManager: AppUpdateManager
) : ViewModel() {
    val state = appUpdateManager.state

    fun check() = appUpdateManager.checkForUpdate(force = true)
    fun dismiss(version: AppVersionDto, required: Boolean) = appUpdateManager.dismiss(version, required)
    fun download(version: AppVersionDto) = appUpdateManager.download(version)
    fun retry(version: AppVersionDto) = appUpdateManager.retryDownload(version)
    fun install(activity: Activity) = appUpdateManager.install(activity)
}
