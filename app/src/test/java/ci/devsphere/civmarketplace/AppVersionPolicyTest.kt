package ci.devsphere.civmarketplace

import ci.devsphere.civmarketplace.data.model.AppVersionDto
import ci.devsphere.civmarketplace.util.AppVersionPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionPolicyTest {
    @Test
    fun optional_update_can_be_dismissed_but_mandatory_update_cannot() {
        val release = version(versionCode = 12, minVersion = 10)

        assertTrue(AppVersionPolicy.shouldOffer(11, release, ignoredVersionCode = null))
        assertFalse(AppVersionPolicy.shouldOffer(11, release, ignoredVersionCode = 12))
        assertTrue(AppVersionPolicy.shouldOffer(9, release, ignoredVersionCode = 12))
        assertTrue(AppVersionPolicy.shouldOffer(11, release.copy(isForceUpdate = true), ignoredVersionCode = 12))
    }

    @Test
    fun installed_latest_version_does_not_offer_update() {
        assertFalse(
            AppVersionPolicy.shouldOffer(
                installedVersionCode = 12,
                release = version(versionCode = 12, minVersion = 10),
                ignoredVersionCode = null
            )
        )
    }

    private fun version(
        versionCode: Int,
        minVersion: Int,
        isForceUpdate: Boolean = false
    ) = AppVersionDto(
        versionCode = versionCode,
        versionName = "1.2.0",
        apkUrl = "https://example.com/app.apk",
        minSupportedVersionCode = minVersion,
        isForceUpdate = isForceUpdate
    )
}
