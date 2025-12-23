package com.bestapp.client.data.repository

import com.bestapp.client.data.api.ApiService
import com.bestapp.client.data.api.models.*
import com.bestapp.client.data.local.PreferencesManager
import kotlinx.coroutines.flow.first
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

class ApiRepository(
    private val apiService: ApiService,
    private val prefsManager: PreferencesManager
) {
    
    // Общий метод для обработки HTTP ошибок
    private suspend fun <T> handleErrorResponse(response: retrofit2.Response<T>, defaultMessage: String): ApiResult.Error {
        return when (response.code()) {
            401 -> {
                // Токен истек - очищаем данные
                prefsManager.clearAuthData()
                ApiResult.Error("Сессия истекла. Пожалуйста, войдите снова.")
            }
            403 -> {
                // Доступ запрещен - парсим сообщение об ошибке
                val errorBody = response.errorBody()?.string()
                val errorMessage = if (errorBody != null && errorBody.isNotBlank()) {
                    try {
                        if (errorBody.startsWith("{") && errorBody.contains("error")) {
                            val errorJson = org.json.JSONObject(errorBody)
                            errorJson.optString("error", errorJson.optString("message", "Доступ запрещен"))
                        } else {
                            errorBody
                        }
                    } catch (e: Exception) {
                        "Доступ запрещен. Проверьте права доступа."
                    }
                } else {
                    "Доступ запрещен. Проверьте права доступа."
                }
                ApiResult.Error(errorMessage)
            }
            else -> {
                val errorBody = response.errorBody()?.string()
                val errorMessage = if (errorBody != null && errorBody.isNotBlank()) {
                    try {
                        if (errorBody.startsWith("{") && errorBody.contains("error")) {
                            val errorJson = org.json.JSONObject(errorBody)
                            errorJson.optString("error", errorJson.optString("message", defaultMessage))
                        } else {
                            errorBody
                        }
                    } catch (e: Exception) {
                        "$defaultMessage (${response.code()})"
                    }
                } else {
                    "$defaultMessage (${response.code()})"
                }
                ApiResult.Error(errorMessage)
            }
        }
    }

    // Auth methods
    suspend fun register(
        name: String,
        email: String,
        password: String,
        phone: String
    ): ApiResult<AuthResponse> {
        return try {
            val request = RegisterRequest(name, email, password, phone)
            val response = apiService.register(request)
            
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                // Save auth data
                prefsManager.saveAuthData(
                    token = authResponse.token,
                    userId = authResponse.user.id,
                    name = authResponse.user.name,
                    email = authResponse.user.email,
                    phone = authResponse.user.phone
                )
                ApiResult.Success(authResponse)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка регистрации")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    suspend fun login(email: String, password: String): ApiResult<AuthResponse> {
        return try {
            val request = LoginRequest(email, password)
            val response = apiService.login(request)
            
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                // Save auth data
                prefsManager.saveAuthData(
                    token = authResponse.token,
                    userId = authResponse.user.id,
                    name = authResponse.user.name,
                    email = authResponse.user.email,
                    phone = authResponse.user.phone
                )
                ApiResult.Success(authResponse)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка входа")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    suspend fun logout() {
        prefsManager.clearAuthData()
    }

    // Order methods
    suspend fun createOrder(request: CreateOrderRequest): ApiResult<OrderDto> {
        return try {
            android.util.Log.d("ApiRepository", "Creating order: deviceType=${request.deviceType}, address=${request.address}")
            val response = apiService.createOrder(request)
            
            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d("ApiRepository", "Order created successfully: id=${response.body()!!.id}")
                ApiResult.Success(response.body()!!)
            } else {
                android.util.Log.e("ApiRepository", "Failed to create order: code=${response.code()}, message=${response.message()}")
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("ApiRepository", "Error body: $errorBody")
                handleErrorResponse(response, "Ошибка создания заказа")
            }
        } catch (e: Exception) {
            android.util.Log.e("ApiRepository", "Exception creating order", e)
            ApiResult.Error(e.message ?: "Неизвестная ошибка: ${e.javaClass.simpleName}")
        }
    }

    suspend fun getOrders(status: String? = null): ApiResult<List<OrderDto>> {
        return try {
            val response = apiService.getOrders(status)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения заказов")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    suspend fun getOrderById(orderId: Long): ApiResult<OrderDto> {
        return try {
            val response = apiService.getOrderById(orderId)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                handleErrorResponse(response, "Ошибка получения заказа")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun getOrderStatusHistory(orderId: Long): ApiResult<List<com.bestapp.client.data.api.models.OrderStatusHistoryDto>> {
        return try {
            val response = apiService.getOrderStatusHistory(orderId)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения истории статусов")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }

    suspend fun cancelOrder(orderId: Long): ApiResult<OrderDto> {
        return try {
            val response = apiService.cancelOrder(orderId)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка отмены заказа")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun completeOrder(orderId: Long, finalCost: Double? = null, repairDescription: String? = null): ApiResult<OrderDto> {
        return try {
            val request = com.bestapp.client.data.api.models.CompleteOrderRequest(
                finalCost = finalCost,
                repairDescription = repairDescription
            )
            val response = apiService.completeOrder(orderId, request)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!.order)
            } else {
                val errorBody = response.errorBody()?.string()
                ApiResult.Error(errorBody ?: response.message() ?: "Ошибка завершения заказа")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun getClientDevices(): ApiResult<List<com.bestapp.client.data.api.models.ClientDeviceDto>> {
        return try {
            val response = apiService.getClientDevices()
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения истории техники")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun reorderOrder(orderId: Long): ApiResult<com.bestapp.client.data.api.models.ReorderOrderResponse> {
        return try {
            val response = apiService.reorderOrder(orderId)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = if (errorBody != null && errorBody.isNotBlank()) {
                    try {
                        // Пытаемся распарсить JSON ошибки
                        if (errorBody.startsWith("{") && errorBody.contains("error")) {
                            val errorJson = org.json.JSONObject(errorBody)
                            errorJson.optString("error", errorJson.optString("details", "Ошибка создания повторного заказа"))
                        } else {
                            errorBody
                        }
                    } catch (e: Exception) {
                        errorBody
                    }
                } else {
                    response.message() ?: "Ошибка создания повторного заказа"
                }
                ApiResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            android.util.Log.e("ApiRepository", "Ошибка reorderOrder: ${e.message}", e)
            ApiResult.Error(e.message ?: "Неизвестная ошибка: ${e.javaClass.simpleName}")
        }
    }

    // Get current user ID
    suspend fun getCurrentUserId(): Long? {
        return prefsManager.userId.first()
    }

    // Get current user name
    suspend fun getCurrentUserName(): String? {
        return prefsManager.userName.first()
    }

    // Get current user phone
    suspend fun getCurrentUserPhone(): String? {
        return prefsManager.userPhone.first()
    }

    // Get current user email
    suspend fun getCurrentUserEmail(): String? {
        return prefsManager.userEmail.first()
    }

    // Check if logged in
    suspend fun isLoggedIn(): Boolean {
        return prefsManager.authToken.first() != null
    }
    
    // Service categories and templates
    suspend fun getCategories(parentId: Long? = null): ApiResult<List<ServiceCategoryDto>> {
        return try {
            val response = apiService.getCategories(parentId)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения категорий")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun getCategoryById(categoryId: Long): ApiResult<ServiceCategoryDto> {
        return try {
            val response = apiService.getCategoryById(categoryId)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения категории")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun getTemplates(
        categoryId: Long? = null,
        deviceType: String? = null,
        popular: Boolean? = null
    ): ApiResult<List<ServiceTemplateDto>> {
        return try {
            val response = apiService.getTemplates(categoryId, deviceType, popular)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения шаблонов")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun getTemplateById(templateId: Long): ApiResult<ServiceTemplateDto> {
        return try {
            val response = apiService.getTemplateById(templateId)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения шаблона")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    // Master endpoints
    suspend fun getMasterById(masterId: Long): ApiResult<MasterDto> {
        return try {
            val response = apiService.getMasterById(masterId)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения мастера")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun getMasterPortfolio(masterId: Long): ApiResult<List<com.bestapp.client.data.api.models.PortfolioItemDto>> {
        return try {
            val response = apiService.getMasterPortfolio(masterId)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения портфолио")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun getMasterCertificates(masterId: Long): ApiResult<List<com.bestapp.client.data.api.models.CertificateDto>> {
        return try {
            val response = apiService.getMasterCertificates(masterId)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения сертификатов")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    // Reviews endpoints
    suspend fun getMasterReviews(masterId: Long, limit: Int? = null, offset: Int? = null): ApiResult<com.bestapp.client.data.api.models.ReviewsResponse> {
        return try {
            val response = apiService.getMasterReviews(masterId, limit, offset)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения отзывов")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun getOrderReview(orderId: Long): ApiResult<com.bestapp.client.data.api.models.ReviewDto?> {
        return try {
            val response = apiService.getOrderReview(orderId)
            
            if (response.isSuccessful) {
                ApiResult.Success(response.body())
            } else if (response.code() == 404) {
                ApiResult.Success(null) // Отзыв не найден - это нормально
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения отзыва")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun createReview(orderId: Long, rating: Int, comment: String? = null): ApiResult<com.bestapp.client.data.api.models.ReviewDto> {
        return try {
            val request = com.bestapp.client.data.api.models.CreateReviewRequest(
                orderId = orderId,
                rating = rating,
                comment = comment
            )
            val response = apiService.createReview(request)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!.review)
            } else {
                val errorBody = response.errorBody()?.string()
                ApiResult.Error(errorBody ?: response.message() ?: "Ошибка создания отзыва")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun updateReview(reviewId: Long, rating: Int? = null, comment: String? = null): ApiResult<com.bestapp.client.data.api.models.ReviewDto> {
        return try {
            val request = com.bestapp.client.data.api.models.UpdateReviewRequest(
                rating = rating,
                comment = comment
            )
            val response = apiService.updateReview(reviewId, request)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!.review)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка обновления отзыва")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun deleteReview(reviewId: Long): ApiResult<Unit> {
        return try {
            val response = apiService.deleteReview(reviewId)
            
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка удаления отзыва")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    // Chat methods
    suspend fun getChatMessages(orderId: Long): ApiResult<List<com.bestapp.client.data.api.models.ChatMessageDto>> {
        return try {
            val response = apiService.getChatMessages(orderId)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                handleErrorResponse(response, "Ошибка получения сообщений")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun sendChatMessage(orderId: Long, message: String): ApiResult<com.bestapp.client.data.api.models.ChatMessageDto> {
        return try {
            val request = com.bestapp.client.data.api.models.SendChatMessageRequest(message)
            val response = apiService.sendChatMessage(orderId, request)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка отправки сообщения")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun sendChatImage(orderId: Long, imagePart: okhttp3.MultipartBody.Part): ApiResult<com.bestapp.client.data.api.models.ChatMessageDto> {
        return try {
            val response = apiService.sendChatImage(orderId, imagePart)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка отправки изображения")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    // Report methods
    suspend fun getReports(orderId: Long? = null, status: String? = null): ApiResult<List<com.bestapp.client.data.api.models.WorkReportDto>> {
        return try {
            val response = apiService.getReports(orderId, status)
            
            if (response.isSuccessful && response.body() != null) {
                // Backend возвращает массив напрямую, не обернутый в объект
                ApiResult.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("ApiRepository", "Ошибка получения отчетов: ${response.code()}, ${errorBody}")
                ApiResult.Error(response.message() ?: "Ошибка получения отчетов (${response.code()})")
            }
        } catch (e: Exception) {
            android.util.Log.e("ApiRepository", "Исключение при получении отчетов", e)
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun getReportById(reportId: Long): ApiResult<com.bestapp.client.data.api.models.WorkReportDto> {
        return try {
            val response = apiService.getReportById(reportId)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения отчета")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun signReport(reportId: Long, signature: String): ApiResult<com.bestapp.client.data.api.models.MessageResponse> {
        return try {
            val request = com.bestapp.client.data.api.models.SignReportRequest(signature)
            val response = apiService.signReport(reportId, request)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка подписания отчета")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    // Loyalty methods
    suspend fun getLoyaltyBalance(): ApiResult<com.bestapp.client.data.api.models.LoyaltyBalanceDto> {
        return try {
            val response = apiService.getLoyaltyBalance()
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения баланса баллов")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun getLoyaltyHistory(limit: Int? = null): ApiResult<com.bestapp.client.data.api.models.LoyaltyHistoryDto> {
        return try {
            val response = apiService.getLoyaltyHistory(limit)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения истории баллов")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun useLoyaltyPoints(points: Int, orderId: Long? = null, description: String? = null): ApiResult<com.bestapp.client.data.api.models.UseLoyaltyPointsResponse> {
        return try {
            val request = com.bestapp.client.data.api.models.UseLoyaltyPointsRequest(points, orderId, description)
            val response = apiService.useLoyaltyPoints(request)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка использования баллов")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun getLoyaltyInfo(): ApiResult<com.bestapp.client.data.api.models.LoyaltyInfoDto> {
        return try {
            val response = apiService.getLoyaltyInfo()
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения информации о программе")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    // Payment methods
    suspend fun createPayment(orderId: Long, amount: Double, paymentMethod: String): ApiResult<com.bestapp.client.data.api.models.CreatePaymentResponse> {
        return try {
            val request = com.bestapp.client.data.api.models.CreatePaymentRequest(orderId, amount, paymentMethod)
            val response = apiService.createPayment(request)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка создания платежа")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun getMyPayments(): ApiResult<List<com.bestapp.client.data.api.models.PaymentDto>> {
        return try {
            val response = apiService.getMyPayments()
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения платежей")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
    
    suspend fun getPaymentById(paymentId: Long): ApiResult<com.bestapp.client.data.api.models.PaymentDto> {
        return try {
            val response = apiService.getPaymentById(paymentId)
            
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message() ?: "Ошибка получения платежа")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Неизвестная ошибка")
        }
    }
}

