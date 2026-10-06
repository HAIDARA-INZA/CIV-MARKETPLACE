package ci.devsphere.civmarketplace.util

/**
 * Formate les prix en FCFA (Bug design #3.2) : "905520.0 FCFA" → "905 520 FCFA".
 * Le FCFA n'a pas de sous-unité (pas de centimes), donc on arrondit toujours à l'entier,
 * avec un espace comme séparateur de milliers (convention d'affichage courante en Afrique de l'Ouest).
 */
object PriceFormatter {

    fun format(price: Double): String = "${groupThousands(Math.round(price))} FCFA"

    fun format(price: Float): String = format(price.toDouble())

    /** Pour les prix reçus en String depuis l'API (souvent "905520.0" ou "905520"). */
    fun format(price: String): String {
        val value = price.toDoubleOrNull() ?: return "$price FCFA"
        return format(value)
    }

    private fun groupThousands(value: Long): String {
        val isNegative = value < 0
        val digits = kotlin.math.abs(value).toString()
        val grouped = digits.reversed().chunked(3).joinToString(" ").reversed()
        return if (isNegative) "-$grouped" else grouped
    }
}

