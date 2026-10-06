package ci.devsphere.civmarketplace.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.ui.graphics.vector.ImageVector
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Catégorie d'erreur réseau/API (Bug 3).
 * Chaque type est associé à une icône + un message dans [ApiErrorMapper],
 * pour que l'UI affiche toujours la bonne illustration au bon endroit.
 */
enum class ErrorType {
    NO_CONNECTION,   // pas de connexion internet -> wifi/cloud barré
    TIMEOUT,         // serveur trop lent -> sablier
    SERVER_ERROR,    // 5xx / panne backend -> outil de maintenance
    NOT_FOUND,       // 404 -> loupe
    VALIDATION,      // 400/422 saisie invalide -> icône d'alerte discrète
    UNAUTHORIZED,    // 401/403
    UNKNOWN
}

/** Erreur prête à afficher : message utilisateur + type pour choisir l'icône. */
data class AppError(val type: ErrorType, val message: String)

/**
 * Exception qui garde le [AppError] d'origine (type + message) au lieu de le perdre.
 * Les repositories renvoient ce type via [ApiErrorMapper.toException] ; les ViewModels
 * peuvent ensuite reconstruire l'icône correcte avec [ApiErrorMapper.fromCaught].
 */
class AppErrorException(val appError: AppError) : Exception(appError.message)

/** Transforme les erreurs réseau/API en messages courts et compréhensibles. */
object ApiErrorMapper {
    fun toException(error: Throwable): Exception = AppErrorException(toAppError(error))

    /** Point d'entrée unique pour l'UI (Bug 3) : type + message déjà résolus. */
    fun toAppError(error: Throwable): AppError {
        val type = typeFor(error)
        return AppError(type, messageFor(error))
    }

    /**
     * À utiliser dans les ViewModels au moment du `.onFailure { }` : si l'exception
     * vient d'un repository (donc déjà un [AppErrorException]), on récupère son type
     * d'origine ; sinon on retombe sur UNKNOWN avec le message fourni.
     */
    fun fromCaught(error: Throwable, fallbackMessage: String): AppError {
        return (error as? AppErrorException)?.appError
            ?: AppError(ErrorType.UNKNOWN, error.message ?: fallbackMessage)
    }

    /** Icône associée à chaque type d'erreur — seule source de vérité (pas de duplication ailleurs). */
    fun iconFor(type: ErrorType): ImageVector = when (type) {
        ErrorType.NO_CONNECTION -> Icons.Filled.CloudOff
        ErrorType.TIMEOUT -> Icons.Filled.HourglassBottom
        ErrorType.SERVER_ERROR -> Icons.Filled.Build
        ErrorType.NOT_FOUND -> Icons.Filled.SearchOff
        ErrorType.VALIDATION -> Icons.Filled.WarningAmber // icône discrète, pas alarmante
        ErrorType.UNAUTHORIZED -> Icons.Filled.ErrorOutline
        ErrorType.UNKNOWN -> Icons.Filled.ErrorOutline
    }

    private fun typeFor(error: Throwable): ErrorType = when (error) {
        is UnknownHostException, is ConnectException -> ErrorType.NO_CONNECTION
        is SocketTimeoutException -> ErrorType.TIMEOUT
        is SSLException -> ErrorType.NO_CONNECTION
        is HttpException -> when (error.code()) {
            400, 422 -> ErrorType.VALIDATION
            401, 403 -> ErrorType.UNAUTHORIZED
            404 -> ErrorType.NOT_FOUND
            408 -> ErrorType.TIMEOUT
            in 500..599 -> ErrorType.SERVER_ERROR
            else -> ErrorType.UNKNOWN
        }
        is IOException -> ErrorType.NO_CONNECTION
        else -> ErrorType.UNKNOWN
    }

    fun messageFor(error: Throwable): String = when (error) {
        is UnknownHostException, is ConnectException -> "Aucune connexion Internet. Vérifiez votre réseau puis réessayez."
        is SocketTimeoutException -> "La connexion est trop lente. Réessayez dans quelques instants."
        is SSLException -> "Connexion sécurisée impossible. Vérifiez la date de votre téléphone ou réessayez plus tard."
        is HttpException -> httpMessage(error)
        is IOException -> "Impossible de joindre le serveur. Vérifiez votre connexion puis réessayez."
        else -> "Une erreur inattendue est survenue. Réessayez dans quelques instants."
    }

    private fun httpMessage(error: HttpException): String {
        val apiMessage = runCatching {
            val json = JSONObject(error.response()?.errorBody()?.string().orEmpty())
            val fields = json.optJSONObject("errors")
            fields?.keys()?.asSequence()?.firstOrNull()?.let { key ->
                fields.optJSONArray(key)?.optString(0)
            } ?: json.optString("message")
        }.getOrNull()?.takeUnless { it.isNullOrBlank() || it.startsWith("validation.") }
        if (apiMessage != null) return apiMessage

        return when (error.code()) {
            400, 422 -> "Vérifiez les informations saisies puis réessayez."
            401 -> "Identifiants ou session invalide. Réessayez."
            403 -> "Vous n’êtes pas autorisé à effectuer cette action."
            404 -> "L’élément demandé est introuvable."
            408 -> "La demande a expiré. Réessayez."
            429 -> "Trop de tentatives. Patientez un instant avant de réessayer."
            in 500..599 -> "Le service est temporairement indisponible. Réessayez dans un instant."
            else -> "La demande n’a pas pu être traitée. Réessayez."
        }
    }
}

