package ci.devsphere.civmarketplace.domain.repository

import ci.devsphere.civmarketplace.data.model.AppVersionDto

interface AppVersionRepository {
    suspend fun getLatestVersion(): Result<AppVersionDto>
}
