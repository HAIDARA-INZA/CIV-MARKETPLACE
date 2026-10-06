package ci.devsphere.civmarketplace.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ci.devsphere.civmarketplace.di.ChatNavigationBus
import ci.devsphere.civmarketplace.di.MarketplaceDeepLinkBus
import ci.devsphere.civmarketplace.di.MarketplaceDeepLinkTarget
import ci.devsphere.civmarketplace.di.NotificationNavigationBus
import ci.devsphere.civmarketplace.di.NotificationNavigationTarget
import ci.devsphere.civmarketplace.ui.screens.AuthViewModel
import ci.devsphere.civmarketplace.ui.screens.CartScreen
import ci.devsphere.civmarketplace.ui.screens.ChatConversationScreen
import ci.devsphere.civmarketplace.ui.screens.CheckoutScreen
import ci.devsphere.civmarketplace.ui.screens.EditProfileScreen
import ci.devsphere.civmarketplace.ui.screens.FavoritesScreen
import ci.devsphere.civmarketplace.ui.screens.ForgotPasswordScreen
import ci.devsphere.civmarketplace.ui.screens.LegalScreen
import ci.devsphere.civmarketplace.ui.screens.LegalType
import ci.devsphere.civmarketplace.ui.screens.LoginScreen
import ci.devsphere.civmarketplace.ui.screens.NotificationsScreen
import ci.devsphere.civmarketplace.ui.screens.OnboardingScreen
import ci.devsphere.civmarketplace.ui.screens.ProductDetailScreen
import ci.devsphere.civmarketplace.ui.screens.RegisterScreen
import ci.devsphere.civmarketplace.ui.screens.ResetPasswordScreen
import ci.devsphere.civmarketplace.ui.screens.SettingsScreen
import ci.devsphere.civmarketplace.ui.screens.SplashScreen
import ci.devsphere.civmarketplace.ui.screens.VerificationScreen
import ci.devsphere.civmarketplace.ui.screens.VendorStoreScreen
import ci.devsphere.civmarketplace.ui.screens.client.ClientOrdersScreen

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = hiltViewModel(),
    forceLogout: Boolean = false,
    onLogoutHandled: () -> Unit = {}
) {
    val navController = rememberNavController()

    val chatTarget by ChatNavigationBus.target.collectAsState()
    val deepLinkTarget by MarketplaceDeepLinkBus.target.collectAsState()
    val notificationTarget by NotificationNavigationBus.target.collectAsState()

    /*
     * true seulement après que Splash a confirmé que l'utilisateur est connecté.
     * Cela évite d'ouvrir une discussion si l'utilisateur doit encore se connecter.
     */
    var navigationReady by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(forceLogout) {
        if (forceLogout) {
            navigationReady = false

            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }

            onLogoutHandled()
        }
    }

    /*
     * Cas app déjà ouverte : clic sur une notification.
     * La discussion s'ouvre immédiatement.
     */
    LaunchedEffect(chatTarget, navigationReady) {
        val target = chatTarget ?: return@LaunchedEffect

        if (navigationReady) {
            navController.navigate(
                Screen.Chat.createRoute(
                    target.userId,
                    Uri.encode(target.userName),
                    productId = target.productId
                )
            ) {
                launchSingleTop = true
            }

            ChatNavigationBus.consume()
        }
    }

    LaunchedEffect(deepLinkTarget, navigationReady) {
        val target = deepLinkTarget ?: return@LaunchedEffect

        if (navigationReady) {
            val route = when (target) {
                is MarketplaceDeepLinkTarget.Shop -> {
                    Screen.VendorStore.createRoute(
                        target.shopId,
                        Uri.encode("Boutique")
                    )
                }
                is MarketplaceDeepLinkTarget.Product -> {
                    Screen.ProductDetail.createRoute(target.productId)
                }
            }

            navController.navigate(route) {
                launchSingleTop = true
            }

            MarketplaceDeepLinkBus.consume()
        }
    }

    LaunchedEffect(notificationTarget, navigationReady) {
        val target = notificationTarget ?: return@LaunchedEffect

        if (navigationReady) {
            val route = when (target) {
                NotificationNavigationTarget.Orders -> Screen.ClientOrders.route
                NotificationNavigationTarget.Notifications -> Screen.Notifications.route
            }

            navController.navigate(route) {
                launchSingleTop = true
            }

            NotificationNavigationBus.consume()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navigationReady = false

                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToHome = { destination ->
                    if (destination.startsWith("verify_email")) {
                        navController.navigate(destination) {
                            popUpTo(Screen.Splash.route) {
                                inclusive = true
                            }
                        }
                    } else {
                        navigationReady = true

                        /*
                         * Ordre de priorité :
                         * 1. discussion venant d'une notification ;
                         * 2. boutique/produit venant d'un lien ;
                         * 3. accueil normal.
                         */
                        val finalRoute = when {
                            chatTarget != null -> {
                                Screen.Chat.createRoute(
                                    chatTarget!!.userId,
                                    Uri.encode(chatTarget!!.userName),
                                    productId = chatTarget!!.productId
                                )
                            }

                            deepLinkTarget is MarketplaceDeepLinkTarget.Shop -> {
                                Screen.VendorStore.createRoute(
                                    (deepLinkTarget as MarketplaceDeepLinkTarget.Shop).shopId,
                                    Uri.encode("Boutique")
                                )
                            }

                            deepLinkTarget is MarketplaceDeepLinkTarget.Product -> {
                                Screen.ProductDetail.createRoute(
                                    (deepLinkTarget as MarketplaceDeepLinkTarget.Product).productId
                                )
                            }

                            notificationTarget is NotificationNavigationTarget.Orders -> {
                                Screen.ClientOrders.route
                            }

                            notificationTarget is NotificationNavigationTarget.Notifications -> {
                                Screen.Notifications.route
                            }

                            else -> {
                                Screen.Main.createRoute(destination)
                            }
                        }

                        navController.navigate(finalRoute) {
                            popUpTo(Screen.Splash.route) {
                                inclusive = true
                            }
                        }

                        if (chatTarget != null) {
                            ChatNavigationBus.consume()
                        }
                        if (deepLinkTarget != null) {
                            MarketplaceDeepLinkBus.consume()
                        }
                        if (notificationTarget != null) {
                            NotificationNavigationBus.consume()
                        }
                    }
                },
                onNavigateToOnboarding = {
                    navigationReady = false

                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.Splash.route) {
                        popUpTo(Screen.Onboarding.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                onNavigateToVerify = { email ->
                    navController.navigate(
                        Screen.VerifyEmail.createRoute(Uri.encode(email))
                    )
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Splash.route) {
                        popUpTo(0) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToVerify = { email ->
                    navController.navigate(
                        Screen.VerifyEmail.createRoute(Uri.encode(email))
                    )
                }
            )
        }

        composable(Screen.VerifyEmail.route) { backStackEntry ->
            val email = Uri.decode(
                backStackEntry.arguments?.getString("email") ?: ""
            )

            VerificationScreen(
                email = email,
                onVerificationSuccess = {
                    navController.navigate(Screen.Splash.route) {
                        popUpTo(0)
                    }
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBackToLogin = {
                    navController.popBackStack()
                },
                onNavigateToReset = { email ->
                    navController.navigate(
                        Screen.ResetPassword.createRoute(Uri.encode(email))
                    )
                }
            )
        }

        composable(Screen.ResetPassword.route) { backStackEntry ->
            val email = Uri.decode(
                backStackEntry.arguments?.getString("email") ?: ""
            )

            ResetPasswordScreen(
                email = email,
                onSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                    }
                }
            )
        }

        composable(
            route = Screen.Main.route,
            arguments = listOf(
                navArgument("role") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val role = backStackEntry.arguments
                ?.getString("role")
                ?: "client"

            MainShell(
                role = role,
                onNavigateToDetail = { productId ->
                    navController.navigate(
                        Screen.ProductDetail.createRoute(productId)
                    )
                },
                onNavigateToCart = {
                    navController.navigate(Screen.Cart.route)
                },
                onNavigateToOrders = {
                    navController.navigate(Screen.ClientOrders.route)
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onNavigateToVendorStore = { vendorId, vendorName ->
                    navController.navigate(
                        Screen.VendorStore.createRoute(
                            vendorId,
                            Uri.encode(vendorName)
                        )
                    )
                },
                onNavigateToChat = { id, name, productId ->
                    navController.navigate(
                        Screen.Chat.createRoute(
                            id,
                            Uri.encode(name),
                            productId
                        )
                    )
                },
                onLogout = {
                    navigationReady = false

                    authViewModel.logout()

                    navController.navigate(Screen.Login.route) {
                        popUpTo(0)
                    }
                }
            )
        }

        composable(Screen.ProductDetail.route) { backStackEntry ->
            val productId = backStackEntry.arguments
                ?.getString("productId")
                ?.toIntOrNull()
                ?: 0

            ProductDetailScreen(
                productId = productId,
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = { id, name, productId ->
                    navController.navigate(
                        Screen.Chat.createRoute(
                            id,
                            Uri.encode(name),
                            productId
                        )
                    )
                }
            )
        }

        composable(
            route = Screen.VendorStore.route,
            arguments = listOf(
                navArgument("vendorId") {
                    type = NavType.IntType
                },
                navArgument("vendorName") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            VendorStoreScreen(
                vendorId = backStackEntry.arguments?.getInt("vendorId") ?: 0,
                vendorName = Uri.decode(
                    backStackEntry.arguments
                        ?.getString("vendorName")
                        ?: "Boutique"
                ),
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { productId ->
                    navController.navigate(
                        Screen.ProductDetail.createRoute(productId)
                    )
                }
            )
        }

        composable(Screen.Cart.route) {
            CartScreen(
                onNavigateToCheckout = {
                    navController.navigate(Screen.Checkout.route)
                }
            )
        }

        composable(Screen.Checkout.route) {
            CheckoutScreen(
                onPaymentFinished = {
                    navController.popBackStack()
                },
                onRetry = {
                    navController.navigate(Screen.Checkout.route) {
                        popUpTo(Screen.Checkout.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ClientOrders.route) {
            ClientOrdersScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { productId ->
                    navController.navigate(
                        Screen.ProductDetail.createRoute(productId)
                    )
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToLegal = { type ->
                    navController.navigate(
                        Screen.Legal.createRoute(type.name)
                    )
                }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Legal.route,
            arguments = listOf(
                navArgument("type") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val type = backStackEntry.arguments
                ?.getString("type")
                ?: "TERMS"

            LegalScreen(
                type = if (type == "PRIVACY") {
                    LegalType.PRIVACY
                } else {
                    LegalType.TERMS
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.IntType
                },
                navArgument("userName") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getInt("userId") ?: 0

            val userName = Uri.decode(
                backStackEntry.arguments
                    ?.getString("userName")
                    ?: "Discussion"
            )

            val productId = backStackEntry.arguments
                ?.getString("productId")
                ?.toIntOrNull()
                ?: 0

            ChatConversationScreen(
                userId = userId,
                userName = userName,
                productId = productId.takeIf { it > 0 },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

