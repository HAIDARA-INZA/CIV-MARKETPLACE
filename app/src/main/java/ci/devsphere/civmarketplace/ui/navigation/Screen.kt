package ci.devsphere.civmarketplace.ui.navigation

/**
 * Source unique de vérité pour toutes les routes de navigation.
 * Toute nouvelle route doit être ajoutée ici — jamais en string hardcodée.
 */
sealed class Screen(val route: String) {

    // ── Auth ──────────────────────────────────────────────────────────────
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Onboarding : Screen("onboarding")

    object VerifyEmail : Screen("verify_email/{email}") {
        fun createRoute(email: String) = "verify_email/$email"
    }
    object ForgotPassword : Screen("forgot_password")
    object ResetPassword : Screen("reset_password/{email}") {
        fun createRoute(email: String) = "reset_password/$email"
    }

    // ── Shell principal (role-based) ───────────────────────────────────────
    object Main : Screen("main/{role}") {
        fun createRoute(role: String) = "main/$role"
    }

    // ── Accueil (onglet principal dans le shell) ───────────────────────────
    object Home : Screen("home")

    // ── Produits ──────────────────────────────────────────────────────────
    object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
        fun createRoute(productId: Int) = "product_detail/$productId"
    }
    object VendorStore : Screen("vendor_store/{vendorId}/{vendorName}") {
        fun createRoute(vendorId: Int, vendorName: String) = "vendor_store/$vendorId/$vendorName"
    }

    // ── Panier & Paiement ─────────────────────────────────────────────────
    object Cart : Screen("cart")
    object Checkout : Screen("checkout")

    // ── Profil & Paramètres ───────────────────────────────────────────────
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object Settings : Screen("settings")

    // ── Commandes ─────────────────────────────────────────────────────────
    object ClientOrders : Screen("client_orders")

    // ── Favoris ───────────────────────────────────────────────────────────
    object Favorites : Screen("favorites")

    // ── Notifications ─────────────────────────────────────────────────────
    object Notifications : Screen("notifications")

    // ── Légal ─────────────────────────────────────────────────────────────
    object Legal : Screen("legal/{type}") {
        fun createRoute(type: String) = "legal/$type"
    }

    // ── Chat ──────────────────────────────────────────────────────────────
    object Chat : Screen("chat/{userId}/{userName}/{productId}") {
        fun createRoute(userId: Int, userName: String, productId: Int? = null) =
            if (productId == null) "chat/$userId/$userName/0" else "chat/$userId/$userName/$productId"
    }
}

