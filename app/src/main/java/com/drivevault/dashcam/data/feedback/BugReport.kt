package com.drivevault.dashcam.data.feedback

import com.google.gson.annotations.SerializedName

data class BugReport(
    val number: Int,
    val title: String,
    val status: String,
    val createdAt: String,
    val htmlUrl: String
)

data class CreateIssueRequest(
    val title: String,
    val body: String,
    val labels: List<String> = listOf("bug")
)

data class GithubIssue(
    val number: Int,
    val title: String,
    val state: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("created_at") val createdAt: String,
    val body: String? = null
)

data class GithubUser(
    val login: String
)

data class GithubComment(
    val id: Long,
    val body: String,
    @SerializedName("created_at") val createdAt: String,
    val user: GithubUser
)

data class PostCommentRequest(
    val body: String
)

data class UploadAssetRequest(
    val message: String,
    val content: String
)

data class UploadContentInfo(
    @SerializedName("download_url") val downloadUrl: String? = null,
    @SerializedName("html_url") val htmlUrl: String? = null
)

data class UploadAssetResponse(
    val content: UploadContentInfo? = null
)
