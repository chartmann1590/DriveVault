package com.drivevault.dashcam.data.feedback

import android.content.Context
import com.drivevault.dashcam.BuildConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object GithubApiClient {

    private const val BASE_URL = "https://api.github.com/"
    private const val ACCEPT = "application/vnd.github+json"
    private const val API_VERSION = "2022-11-28"

    private val gson = Gson()

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun token(): String = BuildConfig.GITHUB_API_TOKEN
    private fun owner(): String = BuildConfig.GITHUB_REPO_OWNER
    private fun repo(): String = BuildConfig.GITHUB_REPO_NAME

    fun isConfigured(): Boolean {
        return token().isNotBlank() && owner().isNotBlank() && repo().isNotBlank()
    }

    private fun Request.Builder.addAuth(): Request.Builder {
        val t = token()
        if (t.isNotBlank()) {
            addHeader("Authorization", "Bearer $t")
        }
        return this
    }

    private fun Request.Builder.addCommonHeaders(): Request.Builder {
        addHeader("Accept", ACCEPT)
        addHeader("X-GitHub-Api-Version", API_VERSION)
        addHeader("User-Agent", "DriveVault-Android/1.0")
        return this
    }

    suspend fun createIssue(title: String, body: String): Result<GithubIssue> = withContext(Dispatchers.IO) {
        runCatching {
            val request = CreateIssueRequest(title, body)
            val json = gson.toJson(request)
            val req = Request.Builder()
                .url("${BASE_URL}repos/${owner()}/${repo()}/issues")
                .post(json.toRequestBody("application/json".toMediaType()))
                .addAuth()
                .addCommonHeaders()
                .build()
            client.newCall(req).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw Exception("GitHub API error ${response.code}: ${responseBody.take(200)}")
                }
                gson.fromJson(responseBody, GithubIssue::class.java)
                    ?: throw Exception("Failed to parse issue response")
            }
        }
    }

    suspend fun getIssue(issueNumber: Int): Result<GithubIssue> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("${BASE_URL}repos/${owner()}/${repo()}/issues/$issueNumber")
                .get()
                .addAuth()
                .addCommonHeaders()
                .build()
            client.newCall(req).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw Exception("GitHub API error ${response.code}: ${responseBody.take(200)}")
                }
                gson.fromJson(responseBody, GithubIssue::class.java)
                    ?: throw Exception("Failed to parse issue response")
            }
        }
    }

    suspend fun getComments(issueNumber: Int): Result<List<GithubComment>> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("${BASE_URL}repos/${owner()}/${repo()}/issues/$issueNumber/comments")
                .get()
                .addAuth()
                .addCommonHeaders()
                .build()
            client.newCall(req).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw Exception("GitHub API error ${response.code}: ${responseBody.take(200)}")
                }
                val type = object : TypeToken<List<GithubComment>>() {}.type
                gson.fromJson<List<GithubComment>>(responseBody, type) ?: emptyList()
            }
        }
    }

    suspend fun postComment(issueNumber: Int, body: String): Result<GithubComment> = withContext(Dispatchers.IO) {
        runCatching {
            val request = PostCommentRequest(body)
            val json = gson.toJson(request)
            val req = Request.Builder()
                .url("${BASE_URL}repos/${owner()}/${repo()}/issues/$issueNumber/comments")
                .post(json.toRequestBody("application/json".toMediaType()))
                .addAuth()
                .addCommonHeaders()
                .build()
            client.newCall(req).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw Exception("GitHub API error ${response.code}: ${responseBody.take(200)}")
                }
                gson.fromJson(responseBody, GithubComment::class.java)
                    ?: throw Exception("Failed to parse comment response")
            }
        }
    }

    suspend fun uploadAsset(
        filename: String,
        base64Content: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val assetDir = BuildConfig.FEEDBACK_ASSETS_DIR
            val path = "$assetDir/$filename"
            val request = UploadAssetRequest(
                message = "Add feedback attachment: $filename",
                content = base64Content
            )
            val json = gson.toJson(request)
            val req = Request.Builder()
                .url("${BASE_URL}repos/${owner()}/${repo()}/contents/$path")
                .put(json.toRequestBody("application/json".toMediaType()))
                .addAuth()
                .addCommonHeaders()
                .build()
            client.newCall(req).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw Exception("GitHub upload error ${response.code}: ${responseBody.take(200)}")
                }
                val uploadResp = gson.fromJson(responseBody, UploadAssetResponse::class.java)
                uploadResp?.content?.downloadUrl
                    ?: throw Exception("No download URL in upload response")
            }
        }
    }
}
