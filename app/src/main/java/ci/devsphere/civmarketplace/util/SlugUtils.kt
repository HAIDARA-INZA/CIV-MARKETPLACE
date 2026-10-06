package ci.devsphere.civmarketplace.util

import java.text.Normalizer

object SlugUtils {
    fun toSlug(value: String?): String {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty()) return "boutique"

        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replace("\\p{M}".toRegex(), "")
            .lowercase()
            .replace("[^a-z0-9]+".toRegex(), "-")
            .replace("^-+|-$".toRegex(), "")

        return normalized.ifEmpty { "boutique" }
    }

    fun routeSlug(value: String?, id: Int): String = "${toSlug(value)}-$id"
}
