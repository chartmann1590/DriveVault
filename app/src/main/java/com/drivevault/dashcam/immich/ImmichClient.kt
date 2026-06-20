package com.drivevault.dashcam.immich

import android.content.Context
import com.drivevault.dashcam.data.repository.SettingsRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

sealed class ImmichResult<out T> {
    data class Success<T>(val data: T) : ImmichResult<T>()
    data class Error(val message: String, val code: Int = 0) : ImmichResult<Nothing>()
    data class NetworkError(val message: String) : ImmichResult<Nothing>()
    data class AuthError(val message: String) : ImmichResult<Nothing>()
}

class ImmichClient(private val context: Context) {

    private val settings by lazy { SettingsRepository(context) }

    private val gson: Gson = GsonBuilder().setLenient().create()

    private fun buildRetrofit(): Retrofit? {
        val baseUrl = settings.immichServerUrl.trimEnd('/')
        if (baseUrl.isBlank()) return null
        val url = "$baseUrl/"

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .apply {
                if (com.drivevault.dashcam.BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private fun getApi(): ImmichApiService? = buildRetrofit()?.create(ImmichApiService::class.java)

    suspend fun validateConnection(): ImmichResult<ImmichValidateResponse> {
        return try {
            val api = getApi() ?: return ImmichResult.Error("Server URL not configured")
            val apiKey = settings.immichApiKey
            if (apiKey.isBlank()) return ImmichResult.AuthError("API key not set")

            val response = api.validateApiKey(apiKey)
            when {
                response.isSuccessful -> ImmichResult.Success(response.body()!!)
                response.code() == 401 -> ImmichResult.AuthError("Invalid API key")
                else -> ImmichResult.Error("Server error: ${response.code()}", response.code())
            }
        } catch (e: java.net.UnknownHostException) {
            ImmichResult.NetworkError("Server unreachable: ${e.message}")
        } catch (e: javax.net.ssl.SSLException) {
            ImmichResult.NetworkError("SSL/TLS error: ${e.message}")
        } catch (e: java.net.SocketTimeoutException) {
            ImmichResult.NetworkError("Connection timed out")
        } catch (e: Exception) {
            ImmichResult.Error("Connection failed: ${e.message}")
        }
    }

    suspend fun uploadVideo(file: File, createdAt: Long, durationSecs: Long): ImmichResult<ImmichAssetResponse> {
        return try {
            val api = getApi() ?: return ImmichResult.Error("Server URL not configured")
            val apiKey = settings.immichApiKey

            val mediaType = "video/mp4".toMediaType()
            val requestBody = file.asRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData("assetData", file.name, requestBody)

            val response = api.uploadAsset(
                apiKey = apiKey,
                assetData = part,
                deviceAssetId = file.name.toRequestBody("text/plain".toMediaType()),
                deviceId = "DriveVault-Dashcam".toRequestBody("text/plain".toMediaType()),
                fileCreatedAt = createdAt.toString().toRequestBody("text/plain".toMediaType()),
                fileModifiedAt = createdAt.toString().toRequestBody("text/plain".toMediaType()),
                isFavorite = "false".toRequestBody("text/plain".toMediaType()),
                isArchived = "false".toRequestBody("text/plain".toMediaType()),
                isOffline = "false".toRequestBody("text/plain".toMediaType()),
                duration = durationSecs.toString().toRequestBody("text/plain".toMediaType())
            )

            when {
                response.isSuccessful -> ImmichResult.Success(response.body()!!)
                response.code() == 401 -> ImmichResult.AuthError("Invalid API key")
                else -> ImmichResult.Error("Upload failed: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ImmichResult.Error("Upload failed: ${e.message}")
        }
    }

    suspend fun uploadMetadataSidecar(jsonFile: File, assetId: String): ImmichResult<Boolean> {
        return try {
            val api = getApi() ?: return ImmichResult.Error("Server URL not configured")
            val apiKey = settings.immichApiKey
            if (!jsonFile.exists()) return ImmichResult.Error("Metadata file not found")

            val jsonContent = jsonFile.readText()
            @Suppress("UNCHECKED_CAST")
            val metadata = Gson().fromJson(jsonContent, Map::class.java) as Map<String, Any?>

            val response = api.updateAsset(apiKey, assetId, metadata)
            when {
                response.isSuccessful -> ImmichResult.Success(true)
                response.code() == 401 -> ImmichResult.AuthError("Invalid API key")
                response.code() == 404 -> ImmichResult.Error("Asset not found on server; it may have been deleted")
                else -> ImmichResult.Error("Metadata upload failed: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            ImmichResult.Error("Metadata upload failed: ${e.message}")
        }
    }

    suspend fun findOrCreateAlbum(albumName: String): ImmichResult<String> {
        return try {
            val api = getApi() ?: return ImmichResult.Error("Server URL not configured")
            val apiKey = settings.immichApiKey

            val albumsResponse = api.getAlbums(apiKey)
            if (albumsResponse.isSuccessful) {
                val existing = albumsResponse.body()?.find { it.albumName == albumName }
                if (existing != null) return ImmichResult.Success(existing.id)
            }

            val createResponse = api.createAlbum(apiKey, ImmichCreateAlbumRequest(albumName = albumName))
            if (createResponse.isSuccessful && createResponse.body() != null) {
                ImmichResult.Success(createResponse.body()!!.id)
            } else {
                ImmichResult.Error("Failed to create album")
            }
        } catch (e: Exception) {
            ImmichResult.Error("Album error: ${e.message}")
        }
    }

    suspend fun addAssetToAlbum(albumId: String, assetId: String): ImmichResult<Boolean> {
        return try {
            val api = getApi() ?: return ImmichResult.Error("Not configured")
            val apiKey = settings.immichApiKey
            val response = api.addAssetsToAlbum(apiKey, albumId, ImmichAddAssetsRequest(listOf(assetId)))
            if (response.isSuccessful) ImmichResult.Success(true)
            else ImmichResult.Error("Failed to add to album")
        } catch (e: Exception) {
            ImmichResult.Error("Album add error: ${e.message}")
        }
    }

    companion object {
        fun isValidServerUrl(url: String): Boolean {
            if (url.isBlank()) return false
            return try {
                val uri = java.net.URI(url)
                uri.scheme in listOf("http", "https") && uri.host.isNotBlank()
            } catch (_: Exception) { false }
        }
    }
}
