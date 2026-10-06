package ci.devsphere.civmarketplace.data.repository

import ci.devsphere.civmarketplace.data.model.AppVersionDto
import ci.devsphere.civmarketplace.data.remote.AppVersionService
import ci.devsphere.civmarketplace.domain.repository.AppVersionRepository
import ci.devsphere.civmarketplace.util.ApiErrorMapper
import javax.inject.Inject

class AppVersionRepositoryImpl @Inject constructor(
    private val appVersionService: AppVersionService
) : AppVersionRepository {
    override suspend fun getLatestVersion(): Result<AppVersionDto> = try {
        Result.success(appVersionService.getLatestVersion())
    } catch (exception: Exception) {
        Result.failure(ApiErrorMapper.toException(exception))
    }
}
