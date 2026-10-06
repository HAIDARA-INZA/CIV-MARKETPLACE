package ci.devsphere.civmarketplace.util

object RoleUtils {
    fun isSeller(role: String): Boolean {
        val normalized = role.lowercase()
        return normalized.contains("vendeur") || normalized.contains("seller")
    }

    fun isClient(role: String): Boolean {
        val normalized = role.lowercase()
        return normalized == "client" || normalized.contains("client")
    }

    fun canBuy(role: String): Boolean = isClient(role) || role.lowercase().contains("vendeur_client")

    fun canSell(role: String): Boolean = isSeller(role)
}

