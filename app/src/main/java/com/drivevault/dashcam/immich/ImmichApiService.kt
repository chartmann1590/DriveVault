package com.drivevault.dashcam.immich

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

data class ImmichValidateResponse(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    @SerializedName("isAdmin") val isAdmin: Boolean = false
)

data class ImmichAssetResponse(
    val id: String = "",
    val status: String = "",
    @SerializedName("duplicate") val duplicate: Boolean = false
)

data class ImmichAlbumResponse(
    val id: String = "",
    @SerializedName("albumName") val albumName: String = "",
    val description: String = "",
    @SerializedName("assetCount") val assetCount: Int = 0
)

data class ImmichCreateAlbumRequest(
    @SerializedName("albumName") val albumName: String,
    val description: String = "",
    @SerializedName("assetIds") val assetIds: List<String> = emptyList()
)

data class ImmichAddAssetsRequest(
    val ids: List<String>
)

data class ImmichAddAssetsResponse(
    val successfullyAdded: Int = 0,
    @SerializedName("alreadyInAlbum") val alreadyInAlbum: List<String> = emptyList()
)

interface ImmichApiService {
    @GET("api/users/me")
    suspend fun validateApiKey(@Header("x-api-key") apiKey: String): Response<ImmichValidateResponse>

    @Multipart
    @POST("api/assets")
    suspend fun uploadAsset(
        @Header("x-api-key") apiKey: String,
        @Part assetData: MultipartBody.Part,
        @Part("deviceAssetId") deviceAssetId: RequestBody,
        @Part("deviceId") deviceId: RequestBody,
        @Part("fileCreatedAt") fileCreatedAt: RequestBody,
        @Part("fileModifiedAt") fileModifiedAt: RequestBody,
        @Part("isFavorite") isFavorite: RequestBody,
        @Part("isArchived") isArchived: RequestBody,
        @Part("isOffline") isOffline: RequestBody,
        @Part("duration") duration: RequestBody
    ): Response<ImmichAssetResponse>

    @GET("api/albums")
    suspend fun getAlbums(@Header("x-api-key") apiKey: String): Response<List<ImmichAlbumResponse>>

    @POST("api/albums")
    suspend fun createAlbum(
        @Header("x-api-key") apiKey: String,
        @Body request: ImmichCreateAlbumRequest
    ): Response<ImmichAlbumResponse>

    @PUT("api/albums/{id}/assets")
    suspend fun addAssetsToAlbum(
        @Header("x-api-key") apiKey: String,
        @Path("id") albumId: String,
        @Body request: ImmichAddAssetsRequest
    ): Response<ImmichAddAssetsResponse>
}
