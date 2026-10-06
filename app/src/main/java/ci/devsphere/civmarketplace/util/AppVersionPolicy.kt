package ci.devsphere.civmarketplace.util

import ci.devsphere.civmarketplace.data.model.AppVersionDto

object AppVersionPolicy {
    fun isRequired(installedVersionCode: Int, release: AppVersionDto): Boolean =
        release.isForceUpdate || installedVersionCode < release.minSupportedVersionCode

    fun shouldOffer(
        installedVersionCode: Int,
        release: AppVersionDto,
        ignoredVersionCode: Int?
    ): Boolean {
        if (release.versionCode <= installedVersionCode) return false
        if (isRequired(installedVersionCode, release)) return true
        return ignoredVersionCode != release.versionCode
    }
}
