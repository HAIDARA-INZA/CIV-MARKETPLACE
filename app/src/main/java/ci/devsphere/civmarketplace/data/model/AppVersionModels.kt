package ci.devsphere.civmarketplace.data.model

import com.google.gson.annotations.SerializedName

data class AppVersionDto(
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("version_name") val versionName: String,
    @SerializedName("apk_url") val apkUrl: String,
    val changelog: String? = null,
    @SerializedName("min_supported_version_code") val minSupportedVersionCode: Int,
    @SerializedName("is_force_update") val isForceUpdate: Boolean
)
