package com.bestapp.client.data.api

import com.bestapp.client.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // Auth endpoints
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    // Order endpoints
    @POST("api/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<OrderDto>
    
    @GET("api/orders")
    suspend fun getOrders(
        @Query("status") status: String? = null
    ): Response<List<OrderDto>>
    
    @GET("api/orders/{id}")
    suspend fun getOrderById(@Path("id") orderId: Long): Response<OrderDto>
    
    @GET("api/orders/{id}/status-history")
    suspend fun getOrderStatusHistory(@Path("id") orderId: Long): Response<List<OrderStatusHistoryDto>>
    
    @GET("api/orders/history")
    suspend fun getOrdersHistory(): Response<List<OrderDto>>
    
    @PUT("api/orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") orderId: Long): Response<OrderDto>
    
    @PUT("api/orders/{id}/complete")
    suspend fun completeOrder(
        @Path("id") orderId: Long,
        @Body request: CompleteOrderRequest
    ): Response<CompleteOrderResponse>
    
    @GET("api/orders/client/devices")
    suspend fun getClientDevices(): Response<List<ClientDeviceDto>>
    
    @POST("api/orders/reorder/{orderId}")
    suspend fun reorderOrder(@Path("orderId") orderId: Long): Response<ReorderOrderResponse>
    
    // Price list endpoints
    @GET("api/prices")
    suspend fun getPrices(
        @Query("category") category: String? = null,
        @Query("type") type: String? = null // 'service' or 'part'
    ): Response<List<PriceDto>>
    
    @GET("api/prices/services")
    suspend fun getServices(
        @Query("category") category: String? = null
    ): Response<List<PriceDto>>
    
    @GET("api/prices/parts")
    suspend fun getParts(
        @Query("category") category: String? = null
    ): Response<List<PriceDto>>
    
    @GET("api/prices/categories")
    suspend fun getPriceCategories(
        @Query("type") type: String? = null // 'service' or 'part'
    ): Response<List<String>>
    
    // Service categories and templates
    @GET("api/services/categories")
    suspend fun getCategories(
        @Query("parent_id") parentId: Long? = null
    ): Response<List<ServiceCategoryDto>>
    
    @GET("api/services/categories/{id}")
    suspend fun getCategoryById(@Path("id") categoryId: Long): Response<ServiceCategoryDto>
    
    @GET("api/services/templates")
    suspend fun getTemplates(
        @Query("category_id") categoryId: Long? = null,
        @Query("device_type") deviceType: String? = null,
        @Query("popular") popular: Boolean? = null
    ): Response<List<ServiceTemplateDto>>
    
    @GET("api/services/templates/{id}")
    suspend fun getTemplateById(@Path("id") templateId: Long): Response<ServiceTemplateDto>
    
    // Master endpoints
    @GET("api/masters")
    suspend fun getMasters(
        @Query("status") status: String? = null,
        @Query("isOnShift") isOnShift: Boolean? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("radius") radius: Double? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<List<MasterDto>>
    
    @GET("api/masters/{id}")
    suspend fun getMasterById(@Path("id") masterId: Long): Response<MasterDto>
    
    // FCM endpoints
    @POST("api/fcm/register")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): Response<FcmTokenResponse>
    
    @DELETE("api/fcm/unregister")
    suspend fun unregisterFcmToken(@Body request: FcmTokenRequest): Response<FcmTokenResponse>
    
    // Version check endpoint
    @POST("api/version/check")
    suspend fun checkVersion(@Body request: com.bestapp.client.data.api.models.VersionCheckRequest): Response<com.bestapp.client.data.api.models.VersionCheckResponse>
    
    @GET("api/masters/{id}/portfolio")
    suspend fun getMasterPortfolio(@Path("id") masterId: Long): Response<List<PortfolioItemDto>>
    
    @GET("api/masters/{id}/certificates")
    suspend fun getMasterCertificates(@Path("id") masterId: Long): Response<List<CertificateDto>>
    
    // Reviews endpoints
    @GET("api/reviews/master/{masterId}")
    suspend fun getMasterReviews(
        @Path("masterId") masterId: Long,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<ReviewsResponse>
    
    @GET("api/reviews/order/{orderId}")
    suspend fun getOrderReview(@Path("orderId") orderId: Long): Response<ReviewDto>
    
    @POST("api/reviews")
    suspend fun createReview(@Body request: CreateReviewRequest): Response<CreateReviewResponse>
    
    @PUT("api/reviews/{id}")
    suspend fun updateReview(
        @Path("id") reviewId: Long,
        @Body request: UpdateReviewRequest
    ): Response<UpdateReviewResponse>
    
    @DELETE("api/reviews/{id}")
    suspend fun deleteReview(@Path("id") reviewId: Long): Response<Unit>
    
    // Chat endpoints
    @GET("api/chat/{orderId}/messages")
    suspend fun getChatMessages(@Path("orderId") orderId: Long): Response<List<ChatMessageDto>>
    
    @POST("api/chat/{orderId}/messages")
    suspend fun sendChatMessage(
        @Path("orderId") orderId: Long,
        @Body request: SendChatMessageRequest
    ): Response<ChatMessageDto>
    
    @Multipart
    @POST("api/chat/{orderId}/messages/image")
    suspend fun sendChatImage(
        @Path("orderId") orderId: Long,
        @Part image: okhttp3.MultipartBody.Part
    ): Response<ChatMessageDto>
    
    // Reports endpoints
    @GET("api/reports")
    suspend fun getReports(
        @Query("orderId") orderId: Long? = null,
        @Query("status") status: String? = null
    ): Response<List<WorkReportDto>>
    
    @GET("api/reports/{id}")
    suspend fun getReportById(@Path("id") reportId: Long): Response<WorkReportDto>
    
    @POST("api/reports/{id}/sign")
    suspend fun signReport(
        @Path("id") reportId: Long,
        @Body request: SignReportRequest
    ): Response<MessageResponse>
    
    // Loyalty endpoints
    @GET("api/loyalty/balance")
    suspend fun getLoyaltyBalance(): Response<LoyaltyBalanceDto>
    
    @GET("api/loyalty/history")
    suspend fun getLoyaltyHistory(
        @Query("limit") limit: Int? = null
    ): Response<LoyaltyHistoryDto>
    
    @POST("api/loyalty/use")
    suspend fun useLoyaltyPoints(@Body request: UseLoyaltyPointsRequest): Response<UseLoyaltyPointsResponse>
    
    @GET("api/loyalty/info")
    suspend fun getLoyaltyInfo(): Response<LoyaltyInfoDto>
    
    // Payment endpoints
    @POST("api/payments")
    suspend fun createPayment(@Body request: CreatePaymentRequest): Response<CreatePaymentResponse>
    
    @GET("api/payments/my")
    suspend fun getMyPayments(): Response<List<PaymentDto>>
    
    @GET("api/payments/{id}")
    suspend fun getPaymentById(@Path("id") paymentId: Long): Response<PaymentDto>
}



