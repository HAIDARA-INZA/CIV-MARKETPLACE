package ci.devsphere.civmarketplace

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import ci.devsphere.civmarketplace.util.MessagingPresenceManager
import ci.devsphere.civmarketplace.util.NotificationUtils
import ci.devsphere.civmarketplace.util.SyncManager
import ci.devsphere.civmarketplace.util.AppUpdateManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CIVMarketplaceApp : Application() {

    @Inject
    lateinit var messagingPresenceManager: MessagingPresenceManager

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var appUpdateManager: AppUpdateManager

    override fun onCreate() {
        super.onCreate()

        NotificationUtils.createNotificationChannels(this)

        ProcessLifecycleOwner.get()
            .lifecycle
            .addObserver(messagingPresenceManager)

        ProcessLifecycleOwner.get()
            .lifecycle
            .addObserver(syncManager)

        ProcessLifecycleOwner.get()
            .lifecycle
            .addObserver(appUpdateManager)

        appUpdateManager.subscribeToUpdatesTopic()
    }
}

