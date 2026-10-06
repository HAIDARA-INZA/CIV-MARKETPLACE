package ci.devsphere.civmarketplace.di

import ci.devsphere.civmarketplace.data.remote.AuthService
import ci.devsphere.civmarketplace.data.remote.AppVersionService
import ci.devsphere.civmarketplace.data.remote.ChatService
import ci.devsphere.civmarketplace.data.remote.FlexibleBooleanAdapter
import ci.devsphere.civmarketplace.data.remote.OrderService
import ci.devsphere.civmarketplace.data.remote.PaymentService
import ci.devsphere.civmarketplace.data.remote.ProductService
import ci.devsphere.civmarketplace.data.remote.SyncService
import ci.devsphere.civmarketplace.data.remote.UserService
import ci.devsphere.civmarketplace.data.local.TokenManager
import ci.devsphere.civmarketplace.BuildConfig
import ci.devsphere.civmarketplace.util.Constants
import ci.devsphere.civmarketplace.util.NetworkMonitor
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ConnectionPool
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Bug 2 — évite de laisser chaque requête échouer individuellement (timeout, DNS...)
 * quand on sait déjà qu'il n'y a pas de connexion : on coupe immédiatement avec
 * un message clair, capté ensuite par ApiErrorMapper (ErrorType.NO_CONNECTION).
 */
class NoConnectivityException : IOException("Aucune connexion Internet.")

private const val GET_MAX_ATTEMPTS = 3
private const val GET_RETRY_BASE_DELAY_MS = 300L

// Deux @Provides distincts renvoyant tous les deux un simple `Interceptor` créeraient
// un conflit de binding Dagger (type dupliqué) : on les qualifie pour lever l'ambiguïté.
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ConnectivityInterceptor

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthInterceptor

/** Bus d'événements global : émet Unit quand l'API renvoie 401 */
object AuthEventBus {
    private val _logoutEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    fun emitLogout() {
        CoroutineScope(Dispatchers.IO).launch { _logoutEvent.emit(Unit) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    @ConnectivityInterceptor
    fun provideConnectivityInterceptor(networkMonitor: NetworkMonitor): Interceptor {
        return Interceptor { chain ->
            if (!networkMonitor.isOnline.value) {
                throw NoConnectivityException()
            }

            val request = chain.request()
            var attempt = 0
            var response: okhttp3.Response? = null
            while (response == null) {
                try {
                    response = chain.proceed(request)
                    networkMonitor.recordApiResponse()
                } catch (exception: IOException) {
                    if (Thread.currentThread().isInterrupted) throw exception

                    val isRetryableGet = request.method.equals("GET", ignoreCase = true) &&
                        attempt < GET_MAX_ATTEMPTS - 1
                    if (!isRetryableGet) {
                        networkMonitor.recordApiFailure()
                        throw exception
                    }

                    val delayMillis = GET_RETRY_BASE_DELAY_MS * (1L shl attempt)
                    attempt++
                    try {
                        Thread.sleep(delayMillis)
                    } catch (interrupted: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("La requête a été interrompue.", interrupted)
                    }
                }
            }
            response
        }
    }

    @Provides
    @Singleton
    @AuthInterceptor
    fun provideAuthInterceptor(tokenManager: TokenManager): Interceptor {
        return Interceptor { chain ->
            val token = runBlocking { tokenManager.getToken().first() }
            val requestBuilder = chain.request().newBuilder()
                .addHeader("Accept", "application/json")

            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val response = chain.proceed(requestBuilder.build())

            // Si le serveur répond 401 → normalement "session expirée, token invalide".
            // ⚠️ Bug corrigé : un mauvais email/mot de passe sur /login renvoie AUSSI un 401,
            // mais dans ce cas il n'y avait PAS de token envoyé (utilisateur pas encore connecté) —
            // ce n'était donc pas une session expirée, et ça ne doit surtout pas déclencher un
            // logout global qui réinitialise l'écran de connexion (et efface ce que l'utilisateur
            // venait de taper). On ne déclenche le logout que si un token avait bien été envoyé.
            if (response.code == 401 && !token.isNullOrBlank()) {
                runBlocking { tokenManager.clearAuthData() }
                AuthEventBus.emitLogout()
            }

            response
        }
    }

    @Provides
    @Singleton
    fun provideConnectionPool(): ConnectionPool = ConnectionPool()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        connectionPool: ConnectionPool,
        @AuthInterceptor authInterceptor: Interceptor,
        @ConnectivityInterceptor connectivityInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .retryOnConnectionFailure(false)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // La vérification de connectivité doit être le tout premier interceptor :
            // on ne veut même pas tenter le token/refresh si on sait déjà qu'on est hors-ligne.
            .addInterceptor(connectivityInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        val booleanAdapter = FlexibleBooleanAdapter()

        return GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Boolean::class.java, booleanAdapter)
            .registerTypeAdapter(Boolean::class.javaPrimitiveType ?: Boolean::class.java, booleanAdapter)
            .create()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_API_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideAppVersionService(retrofit: Retrofit): AppVersionService {
        return retrofit.create(AppVersionService::class.java)
    }

    @Provides
    @Singleton
    fun provideProductService(retrofit: Retrofit): ProductService {
        return retrofit.create(ProductService::class.java)
    }

    @Provides
    @Singleton
    fun providePaymentService(retrofit: Retrofit): PaymentService {
        return retrofit.create(PaymentService::class.java)
    }

    @Provides
    @Singleton
    fun provideChatService(retrofit: Retrofit): ChatService {
        return retrofit.create(ChatService::class.java)
    }

    @Provides
    @Singleton
    fun provideReviewService(retrofit: Retrofit): ci.devsphere.civmarketplace.data.remote.ReviewService {
        return retrofit.create(ci.devsphere.civmarketplace.data.remote.ReviewService::class.java)
    }

    @Provides
    @Singleton
    fun provideOrderService(retrofit: Retrofit): OrderService {
        return retrofit.create(OrderService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserService(retrofit: Retrofit): UserService {
        return retrofit.create(UserService::class.java)
    }

    @Provides
    @Singleton
    fun provideSyncService(retrofit: Retrofit): SyncService {
        return retrofit.create(SyncService::class.java)
    }
}

