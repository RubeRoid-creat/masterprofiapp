package com.bestapp.client.data.api.models

import com.google.gson.annotations.SerializedName

data class VersionCheckRequest(
    val platform: String = "android_client",
    @SerializedName("app_version") val appVersion: String
)

data class VersionCheckResponse(
    @SerializedName("update_required") val updateRequired: Boolean,
    @SerializedName("force_update") val forceUpdate: Boolean,
    @SerializedName("current_version") val currentVersion: String,
    @SerializedName("release_notes") val releaseNotes: String,
    @SerializedName("download_url") val downloadUrl: String?,
    val supported: Boolean
)
