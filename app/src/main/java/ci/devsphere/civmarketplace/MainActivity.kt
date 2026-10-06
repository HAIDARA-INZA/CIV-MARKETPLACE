package ci.devsphere.civmarketplace

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ci.devsphere.civmarketplace.data.local.TokenManager
import ci.devsphere.civmarketplace.di.AuthEventBus
import ci.devsphere.civmarketplace.di.ChatNavigationBus
import ci.devsphere.civmarketplace.di.MarketplaceDeepLinkBus
import ci.devsphere.civmarketplace.di.NotificationNavigationBus
import ci.devsphere.civmarketplace.di.PaymentResultBus
import ci.devsphere.civmarketplace.util.NotificationUtils
import ci.devsphere.civmarketplace.ui.components.OfflineBanner
import ci.devsphere.civmarketplace.ui.navigation.AppNavigation
import ci.devsphere.civmarketplace.ui.screens.AppUpdateDialogHost
import ci.devsphere.civmarketplace.ui.theme.MyApplicationTheme
import ci.devsphere.civmarketplace.util.MarketplaceDeepLinkParser
import ci.devsphere.civmarketplace.util.NetworkMonitor
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleWaveIntent(intent)
        handleChatIntent(intent)
        handleNotificationIntent(intent)
        handleMarketplaceDeepLink(intent)
        handleAppUpdateIntent(intent)

        setContent {
            val darkModeStored by tokenManager
                .getDarkMode()
                .collectAsState(initial = null)

            val darkTheme = darkModeStored ?: false

            var forceLogout by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                AuthEventBus.logoutEvent.collect {
                    forceLogout = true
                }
            }

            val systemUiController = rememberSystemUiController()

            DisposableEffect(systemUiController, darkTheme) {
                systemUiController.setSystemBarsColor(
                    color = Color.Transparent,
                    darkIcons = !darkTheme
                )

                systemUiController.setNavigationBarColor(
                    color = Color.Transparent,
                    darkIcons = !darkTheme
                )

                onDispose { }
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                val showOffline by networkMonitor.showOffline.collectAsState()

                Column(modifier = Modifier.fillMaxSize()) {
                    OfflineBanner(isOffline = showOffline, onRetry = networkMonitor::refresh)

                    Box(modifier = Modifier.weight(1f)) {
                        AppNavigation(
                            forceLogout = forceLogout,
                            onLogoutHandled = {
                                forceLogout = false
                            }
                        )
                    }
                }

                AppUpdateDialogHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        handleWaveIntent(intent)
        handleChatIntent(intent)
        handleNotificationIntent(intent)
        handleMarketplaceDeepLink(intent)
        handleAppUpdateIntent(intent)
    }

    private fun handleChatIntent(intent: Intent?) {
        if (intent?.action != "ci.devsphere.civmarketplace.OPEN_CHAT") {
            return
        }

        val userId = intent.getIntExtra("chat_user_id", 0)

        if (userId <= 0) {
            return
        }

        val userName = intent.getStringExtra("chat_user_name")
            ?: "Discussion"
        val conversationId = intent.getIntExtra("conversation_id", 0)
        val productId = intent.getIntExtra("product_id", 0).takeIf { it > 0 }

        if (conversationId > 0) {
            NotificationUtils.clearChatNotifications(this, conversationId)
        }
        ChatNavigationBus.open(userId, userName, productId)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.action != "ci.devsphere.civmarketplace.OPEN_NOTIFICATION") {
            return
        }

        when (intent.getStringExtra("notification_type").orEmpty()) {
            "product_offer" -> {
                NotificationUtils.clearGroup(this, NotificationUtils.MARKETPLACE_GROUP_PREFIX + "product_offer")
                val productId = intent.getIntExtra("product_id", 0)
                if (productId > 0) {
                    MarketplaceDeepLinkBus.openProduct(productId)
                } else {
                    NotificationNavigationBus.openNotifications()
                }
            }
            "vendor_created" -> {
                NotificationUtils.clearGroup(this, NotificationUtils.MARKETPLACE_GROUP_PREFIX + "vendor_created")
                val vendorId = intent.getIntExtra("vendor_id", 0)
                if (vendorId > 0) {
                    MarketplaceDeepLinkBus.openShop(vendorId)
                } else {
                    NotificationNavigationBus.openNotifications()
                }
            }
            "new_order",
            "payment_success",
            "order_updated" -> {
                NotificationUtils.clearGroup(this, NotificationUtils.MARKETPLACE_GROUP_PREFIX + intent.getStringExtra("notification_type"))
                NotificationNavigationBus.openOrders()
            }
            else -> {
                NotificationNavigationBus.openNotifications()
            }
        }
    }

    private fun handleMarketplaceDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return

        if (uri.scheme == "civmarketplace") {
            val path = uri.path.orEmpty()
            when (uri.host) {
                "shop" -> MarketplaceDeepLinkParser.parseShopId(path)?.let {
                    MarketplaceDeepLinkBus.openShop(it)
                }
                "product" -> MarketplaceDeepLinkParser.parseProductId(path)?.let {
                    MarketplaceDeepLinkBus.openProduct(it)
                }
            }
            return
        }

        if (uri.host != "haidara.devsphere.ci") return

        val path = uri.path.orEmpty()
        when {
            path.startsWith("/shop/") -> {
                MarketplaceDeepLinkParser.parseProductId(path)?.let {
                    MarketplaceDeepLinkBus.openProduct(it)
                } ?: MarketplaceDeepLinkParser.parseShopId(path)?.let {
                    MarketplaceDeepLinkBus.openShop(it)
                }
            }
            path.startsWith("/laravel/public/shop/") -> {
                MarketplaceDeepLinkParser.parseProductId(path)?.let {
                    MarketplaceDeepLinkBus.openProduct(it)
                } ?: MarketplaceDeepLinkParser.parseShopId(path)?.let {
                    MarketplaceDeepLinkBus.openShop(it)
                }
            }
        }
    }

    private fun handleAppUpdateIntent(intent: Intent?) {
        if (intent?.getStringExtra("type") != "app_update") return

        val candidate = intent.getStringExtra("url")
        val uri = runCatching { android.net.Uri.parse(candidate) }.getOrNull()
            ?.takeIf { it.scheme == "https" && it.host == "haidara.devsphere.ci" }
            ?: android.net.Uri.parse("https://haidara.devsphere.ci/civ-marketplace/")

        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun handleWaveIntent(intent: Intent?) {
        val uri = intent?.data ?: return

        if (uri.scheme == "civapp" && uri.host == "payment") {
            val status = when (
                uri.getQueryParameter("status")?.lowercase()
            ) {
                "success" -> PaymentResultBus.Status.SUCCESS
                "failed" -> PaymentResultBus.Status.FAILED
                "cancelled" -> PaymentResultBus.Status.CANCELLED
                else -> PaymentResultBus.Status.CANCELLED
            }

            PaymentResultBus.emit(status)
        }
    }
}

