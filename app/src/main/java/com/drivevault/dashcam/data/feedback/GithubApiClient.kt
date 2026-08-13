package com.drivevault.dashcam.data.feedback

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

/**
 * Talks to the cloudflare-worker/ feedback relay, not api.github.com directly. See
 * cloudflare-worker/src/index.ts, which holds the GitHub token server-side as a Worker
 * secret. Previously this embedded BuildConfig.GITHUB_API_TOKEN client-side as a Bearer
 * header, which shipped a real repo-write PAT in every release build (extractable from
 * the APK).
 */
object GithubApiClient {

    private const val BASE_URL = "https://drivevault-github-feedback.charles-h-hartmann1.workers.dev"

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

    fun isConfigured(): Boolean = true

    suspend fun createIssue(title: String, body: String): Result<GithubIssue> = withContext(Dispatchers.IO) {
        runCatching {
            val request = CreateIssueRequest(title, body)
            val json = gson.toJson(request)
            val req = Request.Builder()
                .url("$BASE_URL/issue")
                .post(json.toRequestBody("application/json".toMediaType()))
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
                .url("$BASE_URL/issue/$issueNumber")
                .get()
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
                .url("$BASE_URL/issue/$issueNumber/comments")
                .get()
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
                .url("$BASE_URL/issue/$issueNumber/comments")
                .post(json.toRequestBody("application/json".toMediaType()))
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
            val request = UploadAssetRequest(
                filename = filename,
                contentBase64 = base64Content
            )
            val json = gson.toJson(request)
            val req = Request.Builder()
                .url("$BASE_URL/upload-image")
                .post(json.toRequestBody("application/json".toMediaType()))
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
