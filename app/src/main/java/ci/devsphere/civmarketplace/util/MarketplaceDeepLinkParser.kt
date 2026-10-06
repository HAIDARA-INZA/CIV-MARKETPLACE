package ci.devsphere.civmarketplace.util

object MarketplaceDeepLinkParser {
    fun parseShopId(path: String): Int? {
        val segments = normalizeSegments(path)
        if (segments.isEmpty()) return null

        val shopSegmentIndex = when {
            segments.first() == "shop" -> 1
            segments.first() == "public-product" -> null
            else -> 0
        } ?: return null

        return when {
            segments.size == 2 && segments[0] == "shop" -> routeId(segments[shopSegmentIndex])
            segments.size == 1 -> routeId(segments[0])
            else -> null
        }
    }

    fun parseProductId(path: String): Int? {
        val segments = normalizeSegments(path)
        if (segments.isEmpty()) return null

        return when {
            segments.first() == "shop" && segments.size >= 3 -> routeId(segments[2])
            segments.first() == "shop" && segments.size == 2 -> null
            segments.first() == "public-product" && segments.size >= 2 -> segments[1].toIntOrNull()
            segments.size == 1 -> routeId(segments[0])
            else -> segments.firstOrNull()?.toIntOrNull()
        }
    }

    private fun routeId(segment: String): Int? =
        segment.toIntOrNull() ?: segment.substringAfterLast('-', "").toIntOrNull()

    private fun normalizeSegments(path: String): List<String> {
        val withoutPrefix = path
            .trim()
            .removePrefix("/")
            .replaceFirst("^laravel/public/".toRegex(), "")
            .replaceFirst("^laravel/public".toRegex(), "")
            .trimStart('/')

        return withoutPrefix
            .split('/')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}

