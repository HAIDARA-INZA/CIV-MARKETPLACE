package ci.devsphere.civmarketplace.data.remote

import ci.devsphere.civmarketplace.data.model.SyncResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface SyncService {
    @GET("sync")
    suspend fun sync(
        @Query("last_seen") lastSeen: String? = null
    ): SyncResponseDto
}

