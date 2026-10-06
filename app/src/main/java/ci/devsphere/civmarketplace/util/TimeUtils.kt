package ci.devsphere.civmarketplace.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object TimeUtils {

    private val isoFormatter = DateTimeFormatter.ISO_DATE_TIME
    private val fallbackFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val outputTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val outputDateFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())
    private val outputDateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM 'à' HH:mm", Locale.getDefault())

    private fun parseDateTime(value: String): LocalDateTime? {
        return runCatching {
            LocalDateTime.parse(value, isoFormatter)
        }.getOrNull() ?: runCatching {
            LocalDateTime.parse(value, fallbackFormatter)
        }.getOrNull() ?: runCatching {
            Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime()
        }.getOrNull()
    }

    fun formatChatMessageTime(value: String): String {
        val dateTime = parseDateTime(value) ?: return value
        val now = LocalDateTime.now()
        return when {
            dateTime.toLocalDate() == now.toLocalDate() -> dateTime.format(outputTimeFormatter)
            dateTime.toLocalDate() == now.minusDays(1).toLocalDate() -> "Hier"
            dateTime.isAfter(now.minusWeeks(1)) -> dateTime.format(outputDateFormatter)
            else -> dateTime.format(outputDateTimeFormatter)
        }
    }

    fun humanReadableDateTime(value: String): String {
        val dateTime = parseDateTime(value) ?: return value
        val now = LocalDateTime.now()
        val days = ChronoUnit.DAYS.between(dateTime.toLocalDate(), now.toLocalDate())
        return when {
            days == 0L -> "Aujourd'hui ${dateTime.format(outputTimeFormatter)}"
            days == 1L -> "Hier ${dateTime.format(outputTimeFormatter)}"
            days < 7 -> dateTime.format(DateTimeFormatter.ofPattern("EEEE HH:mm", Locale.getDefault()))
            else -> dateTime.format(outputDateTimeFormatter)
        }
    }
}

