package com.example.bestapp.api

import com.example.bestapp.api.models.*
import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody
import retrofit2.http.Multipart

interface ApiService {
    
    // ============= Авторизация =============
    
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>
    
    // ============= Заказы =============
    
    @GET("api/orders")
    suspend fun getOrders(
        @Query("status") status: String? = null,
        @Query("deviceType") deviceType: String? = null,
        @Query("orderType") orderType: String? = null,
        @Query("urgency") urgency: String? = null,
        @Query("maxDistance") maxDistance: Double? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("masterLatitude") masterLatitude: Double? = null,
        @Query("masterLongitude") masterLongitude: Double? = null
    ): Response<List<ApiOrder>>
    
    @GET("api/orders/{id}")
    suspend fun getOrder(@Path("id") id: Long): Response<ApiOrder>
    
    @PUT("api/orders/{id}/complete")
    suspend fun completeOrder(
        @Path("id") id: Long,
        @Body request: com.example.bestapp.api.models.CompleteOrderRequest
    ): Response<MessageResponse>
    
    @POST("api/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<CreateOrderResponse>
    
    @PUT("api/orders/{id}")
    suspend fun updateOrder(
        @Path("id") id: Long,
        @Body updates: Map<String, Any>
    ): Response<CreateOrderResponse>
    
    @DELETE("api/orders/{id}")
    suspend fun cancelOrder(@Path("id") id: Long): Response<MessageResponse>
    
    @POST("api/orders/optimize-route")
    suspend fun optimizeRoute(@Body request: com.example.bestapp.api.models.OptimizeRouteRequest): Response<com.example.bestapp.api.models.OptimizedRouteResponse>
    
    // ============= Мастера =============
    
    @GET("api/masters")
    suspend fun getMasters(
        @Query("specialization") specialization: String? = null,
        @Query("status") status: String? = null,
        @Query("isOnShift") isOnShift: Boolean? = null
    ): Response<List<ApiMaster>>
    
    @GET("api/masters/{id}")
    suspend fun getMaster(@Path("id") id: Long): Response<ApiMaster>
    
    @PUT("api/masters/profile")
    suspend fun updateMasterProfile(
        @Body request: UpdateMasterProfileRequest
    ): Response<MessageResponse>
    
    @Multipart
    @POST("api/masters/profile/photo")
    suspend fun uploadMasterAvatar(
        @Part photo: okhttp3.MultipartBody.Part
    ): Response<com.example.bestapp.api.models.UploadAvatarResponse>
    
    @POST("api/masters/shift/start")
    suspend fun startShift(@Body location: LocationRequest): Response<MessageResponse>
    
    @POST("api/masters/shift/end")
    suspend fun endShift(): Response<MessageResponse>
    
    @GET("api/masters/stats/me")
    suspend fun getMasterStats(
        @Query("period") period: String? = null // 'day', 'week', 'month', 'all'
    ): Response<Map<String, Any>>
    
    // ============= Кошелек =============
    
    @GET("api/masters/wallet")
    suspend fun getWallet(): Response<com.example.bestapp.api.models.ApiWallet>
    
    @GET("api/masters/wallet/transactions")
    suspend fun getTransactions(
        @Query("limit") limit: Int? = 50,
        @Query("offset") offset: Int? = 0,
        @Query("type") type: String? = null,
        @Query("status") status: String? = null
    ): Response<List<com.example.bestapp.api.models.ApiTransaction>>
    
    @POST("api/masters/wallet/payout")
    suspend fun requestPayout(@Body request: com.example.bestapp.api.models.PayoutRequest): Response<com.example.bestapp.api.models.ApiTransaction>
    
    @POST("api/masters/wallet/topup")
    suspend fun topupWallet(@Body request: com.example.bestapp.api.models.TopupRequest): Response<com.example.bestapp.api.models.TopupResponse>
    
    // ============= Расписание =============
    
    @GET("api/masters/schedule")
    suspend fun getSchedule(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<com.example.bestapp.api.models.ApiScheduleResponse>
    
    @POST("api/masters/schedule")
    suspend fun createOrUpdateSchedule(
        @Body request: com.example.bestapp.api.models.CreateScheduleRequest
    ): Response<com.example.bestapp.api.models.ApiScheduleItem>
    
    @DELETE("api/masters/schedule/{date}")
    suspend fun deleteSchedule(@Path("date") date: String): Response<com.example.bestapp.api.models.MessageResponse>
    
    @POST("api/masters/schedule/batch")
    suspend fun createBatchSchedule(
        @Body request: com.example.bestapp.api.models.BatchScheduleRequest
    ): Response<com.example.bestapp.api.models.MessageResponse>
    
    // ============= Отчеты о работе =============
    
    @GET("api/reports")
    suspend fun getReports(
        @Query("orderId") orderId: Long? = null,
        @Query("status") status: String? = null
    ): Response<com.example.bestapp.api.models.ApiWorkReportsResponse>
    
    @GET("api/reports/{id}")
    suspend fun getReport(@Path("id") id: Long): Response<com.example.bestapp.api.models.ApiWorkReport>
    
    @POST("api/reports")
    suspend fun createReport(@Body request: com.example.bestapp.api.models.CreateWorkReportRequest): Response<com.example.bestapp.api.models.MessageResponse>
    
    @PUT("api/reports/{id}")
    suspend fun updateReport(
        @Path("id") id: Long,
        @Body request: Map<String, Any>
    ): Response<com.example.bestapp.api.models.MessageResponse>
    
    @POST("api/reports/{id}/sign")
    suspend fun signReport(
        @Path("id") id: Long,
        @Body request: com.example.bestapp.api.models.SignReportRequest
    ): Response<com.example.bestapp.api.models.MessageResponse>
    
    @GET("api/reports/templates")
    suspend fun getReportTemplates(): Response<com.example.bestapp.api.models.ApiReportTemplatesResponse>
    
    @POST("api/reports/templates")
    suspend fun createReportTemplate(
        @Body request: com.example.bestapp.api.models.CreateReportTemplateRequest
    ): Response<com.example.bestapp.api.models.MessageResponse>
    
    // ============= Назначения =============
    
    @GET("api/assignments/my")
    suspend fun getMyAssignments(
        @Query("status") status: String? = null
    ): Response<List<ApiAssignment>>
    
    @GET("api/assignments/rejected")
    suspend fun getRejectedAssignments(): Response<List<ApiRejectedAssignment>>
    
    @GET("api/assignments/order/{orderId}/active")
    suspend fun getActiveAssignmentForOrder(
        @Path("orderId") orderId: Long
    ): Response<ApiAssignment>
    
    @POST("api/assignments/{id}/accept")
    suspend fun acceptAssignment(@Path("id") id: Long): Response<MessageResponse>
    
    @POST("api/assignments/batch/accept")
    suspend fun acceptAssignmentsBatch(@Body request: BatchAcceptRequest): Response<BatchAcceptResponse>
    
    @POST("api/assignments/{id}/reject")
    suspend fun rejectAssignment(
        @Path("id") id: Long,
        @Body reason: RejectReasonRequest
    ): Response<MessageResponse>
    
    @GET("api/assignments/order/{orderId}/history")
    suspend fun getAssignmentHistory(
        @Path("orderId") orderId: Long
    ): Response<List<ApiAssignment>>
    
    // ============= Чат =============
    
    @GET("api/chat/{orderId}/messages")
    suspend fun getChatMessages(@Path("orderId") orderId: Long): Response<List<ApiChatMessage>>
    
    @POST("api/chat/{orderId}/messages")
    suspend fun sendChatMessage(
        @Path("orderId") orderId: Long,
        @Body request: SendChatMessageRequest
    ): Response<ApiChatMessage>
    
    @Multipart
    @POST("api/chat/{orderId}/messages/image")
    suspend fun sendChatImage(
        @Path("orderId") orderId: Long,
        @Part image: okhttp3.MultipartBody.Part
    ): Response<ApiChatMessage>
    
    // ============= Подписки =============
    
    @GET("api/subscriptions/my")
    suspend fun getMySubscription(): Response<com.example.bestapp.api.models.ApiSubscriptionInfo>
    
    @POST("api/subscriptions/activate")
    suspend fun activateSubscription(@Body request: com.example.bestapp.api.models.ActivateSubscriptionRequest): Response<com.example.bestapp.api.models.MessageResponse>
    
    @POST("api/subscriptions/cancel")
    suspend fun cancelSubscription(): Response<com.example.bestapp.api.models.MessageResponse>
    
    // ============= Продвижения =============
    
    @GET("api/promotions/my")
    suspend fun getMyPromotions(): Response<com.example.bestapp.api.models.ApiPromotionInfo>
    
    @GET("api/promotions/types")
    suspend fun getPromotionTypes(): Response<Map<String, com.example.bestapp.api.models.ApiPromotionType>>
    
    @POST("api/promotions/purchase")
    suspend fun purchasePromotion(@Body request: com.example.bestapp.api.models.PurchasePromotionRequest): Response<com.example.bestapp.api.models.MessageResponse>
    
    @POST("api/promotions/{id}/cancel")
    suspend fun cancelPromotion(@Path("id") id: Long): Response<com.example.bestapp.api.models.MessageResponse>

    // ============= Версионирование приложения =============

    @POST("api/version/check")
    suspend fun checkVersion(
        @Body request: com.example.bestapp.api.models.VersionCheckRequest
    ): Response<com.example.bestapp.api.models.VersionCheckResponse>
    
    // ============= Верификация =============
    
    @GET("api/verification/documents")
    suspend fun getVerificationDocuments(): Response<List<com.example.bestapp.api.models.ApiVerificationDocument>>
    
    @Multipart
    @POST("api/verification/documents")
    suspend fun uploadVerificationDocument(
        @Part document: okhttp3.MultipartBody.Part,
        @Part("documentType") documentType: okhttp3.RequestBody,
        @Part("documentName") documentName: okhttp3.RequestBody,
        @Part("inn") inn: okhttp3.RequestBody?
    ): Response<com.example.bestapp.api.models.UploadDocumentResponse>
    
    @DELETE("api/verification/documents/{id}")
    suspend fun deleteVerificationDocument(@Path("id") id: Long): Response<com.example.bestapp.api.models.MessageResponse>
    
    // ============= Verification Codes =============
    
    @POST("api/verification-codes/send-email-code")
    suspend fun sendEmailVerificationCode(): Response<com.example.bestapp.api.models.MessageResponse>
    
    @POST("api/verification-codes/send-phone-code")
    suspend fun sendPhoneVerificationCode(): Response<com.example.bestapp.api.models.MessageResponse>
    
    @POST("api/verification-codes/verify-email-code")
    suspend fun verifyEmailCode(@Body request: com.example.bestapp.api.models.VerifyCodeRequest): Response<com.example.bestapp.api.models.MessageResponse>
    
    @POST("api/verification-codes/verify-phone-code")
    suspend fun verifyPhoneCode(@Body request: com.example.bestapp.api.models.VerifyCodeRequest): Response<com.example.bestapp.api.models.MessageResponse>
    
    @GET("api/verification-codes/status")
    suspend fun getVerificationStatus(): Response<com.example.bestapp.api.models.VerificationStatusResponse>
    
    // ============= MLM =============
    
    @GET("api/mlm/structure")
    suspend fun getMLMStructure(): Response<com.example.bestapp.api.models.ApiMLMStructureResponse>
    
    @GET("api/mlm/statistics")
    suspend fun getMLMStatistics(): Response<com.example.bestapp.api.models.ApiMLMStatisticsResponse>
    
    @GET("api/mlm/commissions")
    suspend fun getMLMCommissions(
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<com.example.bestapp.api.models.ApiMLMCommissionsResponse>
    
    @GET("api/mlm/my-referral-code")
    suspend fun getMLMReferralCode(): Response<com.example.bestapp.api.models.ApiMLMReferralCodeResponse>
    
    @POST("api/mlm/invite")
    suspend fun inviteMaster(@Body request: com.example.bestapp.api.models.ApiMLMInviteRequest): Response<com.example.bestapp.api.models.MessageResponse>
    
    @GET("api/mlm/upline")
    suspend fun getMLMUpline(): Response<com.example.bestapp.api.models.ApiMLMUplineResponse>
    
    @GET("api/mlm/team-performance")
    suspend fun getMLMTeamPerformance(@Query("period") period: Int? = null): Response<com.example.bestapp.api.models.ApiMLMTeamPerformanceResponse>
}




