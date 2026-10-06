package ci.devsphere.civmarketplace.data.remote

import ci.devsphere.civmarketplace.data.model.AppVersionDto
import retrofit2.http.GET

interface AppVersionService {
    @GET("app/version/latest")
    suspend fun getLatestVersion(): AppVersionDto
}
