package com.bestapp.client.data.api

import com.bestapp.client.data.api.models.OrderMediaDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface MediaApiService {
    
    @Multipart
    @POST("api/orders/{orderId}/media/upload")
    suspend fun uploadMedia(
        @Path("orderId") orderId: Long,
        @Part files: List<MultipartBody.Part>,
        @Part("description") description: RequestBody?
    ): Response<MediaUploadResponse>
    
    @GET("api/orders/{orderId}/media")
    suspend fun getOrderMedia(@Path("orderId") orderId: Long): Response<List<OrderMediaDto>>
    
    @DELETE("api/orders/{orderId}/media/{mediaId}")
    suspend fun deleteMedia(
        @Path("orderId") orderId: Long,
        @Path("mediaId") mediaId: Long
    ): Response<Unit>
}

data class MediaUploadResponse(
    val message: String,
    val media: List<OrderMediaDto>
)





