package com.example.bestapp.api

import android.util.Log
import com.example.bestapp.api.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import java.net.SocketTimeoutException
import java.net.ConnectException
import java.net.UnknownHostException
import java.io.IOException

class ApiRepository {
    private val api = RetrofitClient.apiService
    
    companion object {
        private const val TAG = "ApiRepository"
        
        /**
         * Преобразует исключение в понятное сообщение об ошибке для пользователя
         */
        fun getErrorMessage(exception: Exception, defaultMessage: String = "Ошибка загрузки данных"): String {
            return when (exception) {
                is SocketTimeoutException -> "Превышено время ожидания ответа сервера (${RetrofitClient.BASE_URL}). Проверьте подключение к интернету или попробуйте позже."
                is ConnectException -> "Не удалось подключиться к серверу (${RetrofitClient.BASE_URL}). Проверьте:\n• Подключение к интернету\n• Доступность сервера\n• Настройки сети или VPN"
                is UnknownHostException -> "Сервер недоступен (${RetrofitClient.BASE_URL}). Проверьте подключение к интернету."
                is IOException -> {
                    val message = exception.message?.lowercase() ?: ""
                    when {
                        message.contains("timeout") -> "Превышено время ожидания. Проверьте подключение к интернету."
                        message.contains("connection") -> "Ошибка подключения к серверу. Проверьте интернет-соединение."
                        message.contains("network") -> "Проблема с сетью. Проверьте подключение к интернету."
                        else -> "Ошибка сети: ${exception.message ?: defaultMessage}"
                    }
                }
                else -> {
                    val message = exception.message ?: defaultMessage
                    // Если это ошибка от сервера с кодом, оставляем как есть
                    if (message.contains("Ошибка") || message.contains("error") || message.contains("404") || message.contains("500")) {
                        message
                    } else {
                        "$defaultMessage: $message"
                    }
                }
            }
        }
        
        /**
         * Обрабатывает ответ сервера и возвращает Result с понятным сообщением об ошибке
         */
        private fun <T> handleResponse(response: retrofit2.Response<T>, errorContext: String): Result<T> {
            return if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Log.w(TAG, "$errorContext: Response body is null")
                    Result.failure(Exception("Пустой ответ от сервера"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    val errorJson = errorBody?.let {
                        com.google.gson.Gson().fromJson(it, Map::class.java)
                    }
                    errorJson?.get("error")?.toString() ?: when (response.code()) {
                        404 -> "Данные не найдены"
                        401 -> "Требуется авторизация. Войдите заново."
                        403 -> "Доступ запрещен"
                        500 -> "Ошибка сервера. Попробуйте позже."
                        503 -> "Сервер временно недоступен. Попробуйте позже."
                        else -> "$errorContext: ${response.code()}"
                    }
                } catch (e: Exception) {
                    when (response.code()) {
                        404 -> "Данные не найдены"
                        401 -> "Требуется авторизация. Войдите заново."
                        500 -> "Ошибка сервера. Попробуйте позже."
                        else -> "$errorContext: ${response.code()}"
                    }
                }
                Log.e(TAG, "$errorContext failed: code=${response.code()}, message=$errorMessage")
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    // ============= Авторизация =============
    
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting login for email: $email")
                val request = LoginRequest(email, password)
                val response = api.login(request)
                
                Log.d(TAG, "Login response: code=${response.code()}, isSuccessful=${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Сохраняем токен
                        Log.d(TAG, "Saving token after login: ${body.token.take(30)}...")
                        RetrofitClient.setAuthToken(body.token)
                        Log.d(TAG, "Login successful: ${body.user.name}, role: ${body.user.role}, token saved")
                        Result.success(body)
                    } else {
                        Log.e(TAG, "Login response body is null")
                        Result.failure(Exception("Пустой ответ от сервера"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Login failed: code=${response.code()}, message=${response.message()}, body=$errorBody")
                    val errorMessage = try {
                        val errorJson = errorBody?.let { 
                            com.google.gson.Gson().fromJson(it, Map::class.java) 
                        }
                        errorJson?.get("error")?.toString() ?: "Ошибка входа: ${response.code()}"
                    } catch (e: Exception) {
                        "Ошибка входа: ${response.code()}"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login error: ${e.message}", e)
                e.printStackTrace()
                val errorMessage = getErrorMessage(e, "Ошибка входа")
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    suspend fun register(
        email: String,
        password: String,
        name: String,
        phone: String,
        role: String = "master"
    ): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.register(RegisterRequest(email, password, name, phone, role))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d(TAG, "Saving token after registration: ${body.token.take(30)}...")
                    RetrofitClient.setAuthToken(body.token)
                    Log.d(TAG, "Registration successful: ${body.user.name}, token saved")
                    Result.success(body)
                } else {
                    Log.e(TAG, "Registration failed: ${response.code()}")
                    Result.failure(Exception("Ошибка регистрации: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration error", e)
                Result.failure(e)
            }
        }
    }

    // ============= Версионирование приложения =============

    suspend fun checkAppVersion(
        platform: String,
        appVersion: String,
        buildVersion: Int,
        osVersion: String
    ): Result<VersionCheckResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Checking app version: platform=$platform, app=$appVersion($buildVersion), os=$osVersion")
                val request = VersionCheckRequest(
                    platform = platform,
                    appVersion = appVersion,
                    buildVersion = buildVersion,
                    osVersion = osVersion
                )
                val response = api.checkVersion(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    val code = response.code()
                    val body = response.errorBody()?.string()
                    Log.e(TAG, "Version check failed: code=$code, body=$body")
                    Result.failure(Exception("Ошибка проверки версии: $code"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Version check error", e)
                Result.failure(e)
            }
        }
    }
    
    // ============= Заказы =============
    
    suspend fun getOrders(
        status: String? = null,
        deviceType: String? = null,
        orderType: String? = null,
        urgency: String? = null,
        maxDistance: Double? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        sortBy: String? = null,
        masterLatitude: Double? = null,
        masterLongitude: Double? = null
    ): Result<List<ApiOrder>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Requesting orders: status=$status, deviceType=$deviceType, urgency=$urgency, maxDistance=$maxDistance, sortBy=$sortBy")
                val response = api.getOrders(
                    status, deviceType, orderType, urgency, 
                    maxDistance, minPrice, maxPrice, sortBy,
                    masterLatitude, masterLongitude
                )
                Log.d(TAG, "Response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d(TAG, "Got ${body.size} orders from API")
                        if (body.isNotEmpty()) {
                            val firstOrder = body.first()
                            Log.d(TAG, "First order: id=${firstOrder.id}, repairStatus=${firstOrder.repairStatus}, deviceType=${firstOrder.deviceType}, distance=${firstOrder.distance}")
                        }
                        Result.success(body)
                    } else {
                        Log.w(TAG, "Response body is null")
                        Result.success(emptyList())
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Get orders failed: code=${response.code()}, message=${response.message()}, body=$errorBody")
                    Result.failure(Exception("Ошибка получения заказов: ${response.code()} - ${errorBody ?: response.message()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Get orders error: ${e.message}", e)
                e.printStackTrace()
                val errorMessage = getErrorMessage(e, "Ошибка загрузки заказов")
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    suspend fun getOrder(id: Long): Result<ApiOrder> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getOrder(id)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Ошибка получения заказа: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Get order error", e)
                val errorMessage = getErrorMessage(e, "Ошибка загрузки заказа")
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    suspend fun completeOrder(id: Long, finalCost: Double? = null, repairDescription: String? = null): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = com.example.bestapp.api.models.CompleteOrderRequest(
                    finalCost = finalCost,
                    repairDescription = repairDescription
                )
                val response = api.completeOrder(id, request)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val errorJson = errorBody?.let { 
                            com.google.gson.Gson().fromJson(it, Map::class.java) 
                        } as? Map<*, *>
                        errorJson?.get("error") as? String ?: "Ошибка завершения заказа: ${response.code()}"
                    } catch (e: Exception) {
                        "Ошибка завершения заказа: ${response.code()}"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Complete order error", e)
                val errorMessage = getErrorMessage(e, "Ошибка завершения заказа")
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    suspend fun createOrder(request: CreateOrderRequest): Result<CreateOrderResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.createOrder(request)
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Order created: ${response.body()!!.order.id}")
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Ошибка создания заказа: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Create order error", e)
                val errorMessage = getErrorMessage(e, "Ошибка создания заказа")
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    suspend fun optimizeRoute(
        orderIds: List<Long>,
        startLatitude: Double? = null,
        startLongitude: Double? = null
    ): Result<com.example.bestapp.api.models.OptimizedRouteResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = com.example.bestapp.api.models.OptimizeRouteRequest(
                    orderIds = orderIds,
                    startLatitude = startLatitude,
                    startLongitude = startLongitude
                )
                val response = api.optimizeRoute(request)
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Route optimized: ${response.body()!!.orders.size} orders, ${response.body()!!.totalDistance}m, ${response.body()!!.totalTime}min")
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Optimize route failed: code=${response.code()}, body=$errorBody")
                    Result.failure(Exception("Ошибка оптимизации маршрута: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Optimize route error", e)
                Result.failure(e)
            }
        }
    }
    
    // ============= Мастера =============
    
    suspend fun getMasters(
        specialization: String? = null,
        status: String? = null,
        isOnShift: Boolean? = null
    ): Result<List<ApiMaster>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getMasters(specialization, status, isOnShift)
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Got ${response.body()!!.size} masters")
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Ошибка получения мастеров: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Get masters error", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun startShift(latitude: Double, longitude: Double): Result<MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.startShift(LocationRequest(latitude, longitude))
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Shift started")
                    Result.success(response.body()!!)
                } else {
                    Log.e(TAG, "Start shift failed: ${response.code()}")
                    Result.failure(Exception("Ошибка начала смены: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Start shift error", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun endShift(): Result<MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.endShift()
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Shift ended")
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Ошибка завершения смены: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "End shift error", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun updateMasterProfile(
        name: String? = null,
        phone: String? = null,
        email: String? = null,
        specialization: List<String>? = null
    ): Result<MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔧 updateMasterProfile called")
                Log.d(TAG, "   name=$name, phone=$phone, email=$email")
                Log.d(TAG, "   specialization=$specialization (size=${specialization?.size})")
                
                val request = com.example.bestapp.api.models.UpdateMasterProfileRequest(
                    name = name,
                    phone = phone,
                    email = email,
                    specialization = specialization
                )
                
                Log.d(TAG, "🚀 Sending PUT request to ${RetrofitClient.BASE_URL}api/masters/profile")
                Log.d(TAG, "   Request: $request")
                
                val response = api.updateMasterProfile(request)
                
                Log.d(TAG, "📥 Response received: code=${response.code()}, isSuccessful=${response.isSuccessful}")
                
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "✅ Master profile updated successfully")
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ Update master profile failed: code=${response.code()}, body=$errorBody")
                    Result.failure(Exception("Ошибка обновления профиля: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Update master profile error: ${e.javaClass.simpleName} - ${e.message}", e)
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
    
    suspend fun uploadMasterAvatar(photoPart: okhttp3.MultipartBody.Part): Result<com.example.bestapp.api.models.UploadAvatarResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Uploading master avatar")
                val response = api.uploadMasterAvatar(photoPart)
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Master avatar uploaded successfully: ${response.body()!!.photoUrl}")
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Upload master avatar failed: code=${response.code()}, body=$errorBody")
                    Result.failure(Exception("Ошибка загрузки аватара: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload master avatar error", e)
                val errorMessage = getErrorMessage(e, "Ошибка загрузки аватара")
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    // ============= Назначения =============
    
    suspend fun getMyAssignments(status: String? = null): Result<List<ApiAssignment>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getMyAssignments(status)
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Got ${response.body()!!.size} assignments")
                    Result.success(response.body()!!)
                } else {
                    // Проверяем, требуется ли верификация
                    if (response.code() == 403) {
                        val errorBody = response.errorBody()?.string()
                        Log.w(TAG, "Verification required: $errorBody")
                        val errorMessage = try {
                            val errorJson = errorBody?.let { com.google.gson.Gson().fromJson(it, Map::class.java) } as? Map<*, *>
                            errorJson?.get("message") as? String ?: "Требуется верификация для просмотра заказов"
                        } catch (e: Exception) {
                            "Требуется верификация для просмотра заказов"
                        }
                        Result.failure(Exception(errorMessage).apply {
                            // Добавляем специальный флаг для проверки верификации
                            (this as? java.lang.Exception)?.apply {
                                // Сохраняем информацию о необходимости верификации
                            }
                        })
                    } else {
                        Result.failure(Exception("Ошибка получения назначений: ${response.code()}"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Get assignments error", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun getActiveAssignmentForOrder(orderId: Long): Result<ApiAssignment?> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Requesting active assignment for order: $orderId")
                val response = api.getActiveAssignmentForOrder(orderId)
                Log.d(TAG, "Active assignment response: code=${response.code()}, isSuccessful=${response.isSuccessful}")
                if (response.isSuccessful) {
                    val assignment = response.body()
                    Log.d(TAG, "Active assignment received: ${if (assignment != null) "id=${assignment.id}, status=${assignment.status}" else "null"}")
                    Result.success(assignment)
                } else if (response.code() == 404) {
                    // Нет активного назначения
                    Log.d(TAG, "No active assignment found (404) for order: $orderId")
                    Result.success(null)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Get active assignment failed: code=${response.code()}, body=$errorBody")
                    Result.failure(Exception("Ошибка: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Get active assignment error", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun acceptAssignment(assignmentId: Long): Result<MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to accept assignment: $assignmentId")
                val response = api.acceptAssignment(assignmentId)
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Assignment accepted: $assignmentId")
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Accept assignment failed: code=${response.code()}, body=$errorBody")
                    
                    // Проверяем, требуется ли верификация
                    if (response.code() == 403) {
                        val errorMessage = try {
                            val errorJson = errorBody?.let { 
                                com.google.gson.Gson().fromJson(it, Map::class.java) 
                            } as? Map<*, *>
                            errorJson?.get("message") as? String ?: "Для принятия заказов необходимо пройти верификацию"
                        } catch (e: Exception) {
                            "Для принятия заказов необходимо пройти верификацию"
                        }
                        Result.failure(Exception(errorMessage))
                    } else {
                        val errorMessage = try {
                            val errorJson = errorBody?.let { 
                                com.google.gson.Gson().fromJson(it, Map::class.java) 
                            }
                            errorJson?.get("error")?.toString() ?: "Ошибка принятия заказа: ${response.code()}"
                        } catch (e: Exception) {
                            "Ошибка принятия заказа: ${response.code()}"
                        }
                        Result.failure(Exception(errorMessage))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Accept assignment error", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun acceptAssignmentsBatch(assignmentIds: List<Long>): Result<com.example.bestapp.api.models.BatchAcceptResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to accept ${assignmentIds.size} assignments")
                val request = com.example.bestapp.api.models.BatchAcceptRequest(assignmentIds)
                val response = api.acceptAssignmentsBatch(request)
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Batch accept successful: ${response.body()!!.accepted.size} accepted, ${response.body()!!.errors.size} errors")
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Batch accept failed: code=${response.code()}, body=$errorBody")
                    Result.failure(Exception("Ошибка принятия заказов: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Batch accept error", e)
                val errorMessage = getErrorMessage(e, "Ошибка принятия заказов")
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    suspend fun getRejectedAssignments(): Result<List<com.example.bestapp.api.models.ApiRejectedAssignment>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getRejectedAssignments()
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Ошибка получения истории отклонений: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Get rejected assignments error", e)
                val errorMessage = getErrorMessage(e, "Ошибка загрузки истории отклонений")
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    suspend fun rejectAssignment(assignmentId: Long, reason: String): Result<MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.rejectAssignment(assignmentId, RejectReasonRequest(reason))
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Assignment rejected: $assignmentId")
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Ошибка отклонения заказа: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reject assignment error", e)
                Result.failure(e)
            }
        }
    }
    
    // ============= Статистика =============
    
    suspend fun getMasterStats(period: String? = null): Result<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Requesting master stats with period: $period")
                val response = api.getMasterStats(period)
                Log.d(TAG, "Master stats response: code=${response.code()}, isSuccessful=${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d(TAG, "Master stats received: $body")
                        Result.success(body)
                    } else {
                        Log.e(TAG, "Master stats response body is null")
                        Result.failure(Exception("Пустой ответ от сервера"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Get master stats failed: code=${response.code()}, message=${response.message()}, body=$errorBody")
                    
                    val errorMessage = when (response.code()) {
                        401 -> "Требуется авторизация. Войдите в систему заново."
                        403 -> "Доступ запрещен. У вас нет прав для просмотра статистики."
                        404 -> "Профиль мастера не найден."
                        500 -> "Ошибка сервера. Попробуйте позже."
                        else -> "Ошибка получения статистики: ${response.code()} - ${errorBody ?: response.message()}"
                    }
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Get master stats error", e)
                e.printStackTrace()
                val errorMessage = getErrorMessage(e, "Ошибка загрузки статистики")
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    // ============= Расписание =============
    
    suspend fun getSchedule(startDate: String? = null, endDate: String? = null): Result<List<com.example.bestapp.api.models.ApiScheduleItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getSchedule(startDate, endDate)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.schedule)
                } else {
                    Result.failure(Exception("Ошибка получения расписания: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Get schedule error", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun createOrUpdateSchedule(
        date: String,
        startTime: String? = null,
        endTime: String? = null,
        isAvailable: Boolean = true,
        note: String? = null
    ): Result<com.example.bestapp.api.models.ApiScheduleItem> {
        return withContext(Dispatchers.IO) {
            try {
                val request = com.example.bestapp.api.models.CreateScheduleRequest(
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    isAvailable = isAvailable,
                    note = note
                )
                val response = api.createOrUpdateSchedule(request)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Ошибка создания/обновления расписания: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Create/update schedule error", e)
                val errorMessage = getErrorMessage(e, "Ошибка создания/обновления расписания")
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    suspend fun deleteSchedule(date: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.deleteSchedule(date)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Ошибка удаления расписания: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Delete schedule error", e)
                val errorMessage = getErrorMessage(e, "Ошибка удаления расписания")
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    suspend fun createBatchSchedule(
        startDate: String,
        endDate: String,
        startTime: String? = null,
        endTime: String? = null,
        isAvailable: Boolean = true,
        daysOfWeek: List<Int>? = null
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = com.example.bestapp.api.models.BatchScheduleRequest(
                    startDate = startDate,
                    endDate = endDate,
                    startTime = startTime,
                    endTime = endTime,
                    isAvailable = isAvailable,
                    daysOfWeek = daysOfWeek
                )
                val response = api.createBatchSchedule(request)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Ошибка массового создания расписания: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Create batch schedule error", e)
                Result.failure(e)
            }
        }
    }
    
    // ============= Отчеты о работе =============
    
    suspend fun getReports(orderId: Long? = null, status: String? = null): Result<List<com.example.bestapp.api.models.ApiWorkReport>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getReports(orderId, status)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.reports)
                } else {
                    Result.failure(Exception("Ошибка получения отчетов: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Get reports error", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun getReport(id: Long): Result<com.example.bestapp.api.models.ApiWorkReport> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getReport(id)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Ошибка получения отчета: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Get report error", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun createReport(
        orderId: Long,
        workDescription: String,
        totalCost: Double,
        reportType: String = "standard",
        partsUsed: List<com.example.bestapp.api.models.PartUsed>? = null,
        workDuration: Int? = null,
        partsCost: Double = 0.0,
        laborCost: Double = 0.0,
        templateId: Long? = null,
        beforePhotos: List<String> = emptyList(),
        afterPhotos: List<String> = emptyList()
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = com.example.bestapp.api.models.CreateWorkReportRequest(
                    orderId = orderId,
                    reportType = reportType,
                    workDescription = workDescription,
                    partsUsed = partsUsed,
                    workDuration = workDuration,
                    totalCost = totalCost,
                    partsCost = partsCost,
                    laborCost = laborCost,
                    templateId = templateId,
                    beforePhotos = beforePhotos,
                    afterPhotos = afterPhotos
                )
                val response = api.createReport(request)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Ошибка создания отчета: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Create report error", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun signReport(id: Long, signature: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = com.example.bestapp.api.models.SignReportRequest(signature)
                val response = api.signReport(id, request)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Ошибка подписания отчета: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign report error", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun getReportTemplates(): Result<List<com.example.bestapp.api.models.ApiReportTemplate>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getReportTemplates()
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!.templates)
                } else {
                    Result.failure(Exception("Ошибка получения шаблонов: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Get report templates error", e)
                Result.failure(e)
            }
        }
    }
    
    // ============= Чат =============
    
    suspend fun getChatMessages(orderId: Long): Result<List<ApiChatMessage>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getChatMessages(orderId)
                if (response.isSuccessful) {
                    Result.success(response.body() ?: emptyList())
                } else {
                    Result.failure(Exception("Ошибка: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting chat messages", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun sendChatMessage(orderId: Long, request: SendChatMessageRequest): Result<ApiChatMessage> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.sendChatMessage(orderId, request)
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    Result.failure(Exception("Ошибка: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending chat message", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun sendChatImage(orderId: Long, imagePart: okhttp3.MultipartBody.Part): Result<ApiChatMessage> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.sendChatImage(orderId, imagePart)
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    Result.failure(Exception("Ошибка: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending chat image", e)
                Result.failure(e)
            }
        }
    }
    
    // ============= Верификация =============
    
    suspend fun getVerificationDocuments(): Result<List<ApiVerificationDocument>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getVerificationDocuments()
                if (response.isSuccessful) {
                    Result.success(response.body() ?: emptyList())
                } else {
                    Result.failure(Exception("Ошибка загрузки документов: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting verification documents", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun uploadVerificationDocument(
        fileUri: android.net.Uri,
        documentType: String,
        documentName: String,
        inn: String?,
        context: android.content.Context
    ): Result<com.example.bestapp.api.models.UploadDocumentResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Читаем файл из URI
                val inputStream = context.contentResolver.openInputStream(fileUri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Не удалось прочитать файл")
                inputStream.close()
                
                // Создаем MultipartBody.Part для файла
                val imageMediaType = "image/jpeg".toMediaType()
                val requestFile = okhttp3.RequestBody.create(imageMediaType, bytes)
                val filePart = okhttp3.MultipartBody.Part.createFormData(
                    "document",
                    "document_${System.currentTimeMillis()}.jpg",
                    requestFile
                )
                
                // Создаем RequestBody для текстовых полей
                val textMediaType = "text/plain".toMediaType()
                val documentTypeBody = okhttp3.RequestBody.create(textMediaType, documentType)
                val documentNameBody = okhttp3.RequestBody.create(textMediaType, documentName)
                val innBody = inn?.let {
                    okhttp3.RequestBody.create(textMediaType, it)
                }
                
                val response = api.uploadVerificationDocument(
                    filePart,
                    documentTypeBody,
                    documentNameBody,
                    innBody
                )
                
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Upload document failed: ${response.code()}, body=$errorBody")
                    
                    // Пытаемся извлечь сообщение об ошибке из ответа
                    val errorMessage = try {
                        if (errorBody != null) {
                            val errorJson = com.google.gson.Gson().fromJson(errorBody, Map::class.java) as? Map<*, *>
                            errorJson?.get("error") as? String ?: "Ошибка загрузки документа: ${response.code()}"
                        } else {
                            "Ошибка загрузки документа: ${response.code()}"
                        }
                    } catch (e: Exception) {
                        "Ошибка загрузки документа: ${response.code()}"
                    }
                    
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading verification document", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun deleteVerificationDocument(id: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.deleteVerificationDocument(id)
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Ошибка удаления документа: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting verification document", e)
                Result.failure(e)
            }
        }
    }
    
    // ============= Кошелек =============
    
    suspend fun getWallet(): Result<ApiWallet> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Requesting wallet from API...")
                val response = api.getWallet()
                Log.d(TAG, "📥 Wallet response: code=${response.code()}, isSuccessful=${response.isSuccessful}")
                if (response.isSuccessful) {
                    val wallet = response.body()
                    if (wallet != null) {
                        Log.d(TAG, "✅ Wallet received: balance=${wallet.balance}")
                        Result.success(wallet)
                    } else {
                        Log.e(TAG, "❌ Wallet response body is null")
                        Result.failure(Exception("Пустой ответ"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ Wallet request failed: code=${response.code()}, body=$errorBody")
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception getting wallet", e)
                Log.e(TAG, "Exception message: ${e.message}")
                Log.e(TAG, "Exception stack: ${e.stackTraceToString()}")
                val errorMessage = getErrorMessage(e, "Ошибка загрузки кошелька")
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    suspend fun getTransactions(
        limit: Int = 50,
        offset: Int = 0,
        type: String? = null,
        status: String? = null
    ): Result<List<ApiTransaction>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getTransactions(limit, offset, type, status)
                if (response.isSuccessful) {
                    Result.success(response.body() ?: emptyList())
                } else {
                    Result.failure(Exception("Ошибка: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting transactions", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun requestPayout(amount: Double, payoutMethod: String = "bank"): Result<ApiTransaction> {
        return withContext(Dispatchers.IO) {
            try {
                val request = PayoutRequest(amount, payoutMethod)
                val response = api.requestPayout(request)
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting payout", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun topupWallet(amount: Double, paymentMethod: String = "card", description: String? = null): Result<com.example.bestapp.api.models.TopupResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = com.example.bestapp.api.models.TopupRequest(amount, paymentMethod, description)
                val response = api.topupWallet(request)
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val errorJson = errorBody?.let { 
                            com.google.gson.Gson().fromJson(it, Map::class.java) 
                        } as? Map<*, *>
                        errorJson?.get("error")?.toString() ?: "Ошибка пополнения: ${response.code()}"
                    } catch (e: Exception) {
                        "Ошибка пополнения: ${response.code()}"
                    }
                    Log.e(TAG, "Topup wallet failed: ${response.code()}, body=$errorBody")
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error topup wallet", e)
                Result.failure(e)
            }
        }
    }
    
    // ============= Subscriptions =============
    
    suspend fun getMySubscription(): Result<com.example.bestapp.api.models.ApiSubscriptionInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getMySubscription()
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting subscription", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun activateSubscription(subscriptionType: String): Result<com.example.bestapp.api.models.MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = com.example.bestapp.api.models.ActivateSubscriptionRequest(subscriptionType)
                val response = api.activateSubscription(request)
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error activating subscription", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun cancelSubscription(): Result<com.example.bestapp.api.models.MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.cancelSubscription()
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error canceling subscription", e)
                Result.failure(e)
            }
        }
    }
    
    // ============= Promotions =============
    
    suspend fun getMyPromotions(): Result<com.example.bestapp.api.models.ApiPromotionInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getMyPromotions()
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting promotions", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun getPromotionTypes(): Result<Map<String, com.example.bestapp.api.models.ApiPromotionType>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getPromotionTypes()
                if (response.isSuccessful) {
                    Result.success(response.body() ?: emptyMap())
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting promotion types", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun purchasePromotion(promotionType: String): Result<com.example.bestapp.api.models.MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = com.example.bestapp.api.models.PurchasePromotionRequest(promotionType)
                val response = api.purchasePromotion(request)
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error purchasing promotion", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun cancelPromotion(promotionId: Long): Result<com.example.bestapp.api.models.MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.cancelPromotion(promotionId)
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error canceling promotion", e)
                Result.failure(e)
            }
        }
    }
    
    // ============= MLM =============
    
    suspend fun getMLMStructure(): Result<com.example.bestapp.api.models.ApiMLMStructureResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getMLMStructure()
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting MLM structure", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun getMLMStatistics(): Result<com.example.bestapp.api.models.ApiMLMStatisticsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getMLMStatistics()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Result.success(body)
                    } else {
                        // Создаем пустой ответ, если тело пустое
                        val emptyStats = com.example.bestapp.api.models.ApiMLMStatisticsResponse(
                            success = true,
                            statistics = com.example.bestapp.api.models.ApiMLMStatistics(
                                masterId = 0L,
                                userId = 0L,
                                rank = "junior_master",
                                joinDate = "",
                                downline = com.example.bestapp.api.models.ApiMLMDownlineStats(
                                    level1 = 0,
                                    level2 = 0,
                                    level3 = 0,
                                    total = 0,
                                    active = 0
                                ),
                                commissions = com.example.bestapp.api.models.ApiMLMCommissionsStats(
                                    last30Days = com.example.bestapp.api.models.ApiMLMCommissionsByLevel(0, 0.0),
                                    total = com.example.bestapp.api.models.ApiMLMCommissionsByLevel(0, 0.0),
                                    byLevel = emptyMap()
                                )
                            )
                        )
                        Result.success(emptyStats)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = when (response.code()) {
                        404 -> "MLM данные еще не доступны. Начните приглашать мастеров!"
                        401 -> "Требуется авторизация"
                        403 -> "Доступ запрещен"
                        else -> "Ошибка загрузки статистики: ${response.code()}"
                    }
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting MLM statistics", e)
                Result.failure(Exception("Ошибка подключения: ${e.message}"))
            }
        }
    }
    
    suspend fun getMLMCommissions(
        limit: Int = 50,
        offset: Int = 0,
        startDate: String? = null,
        endDate: String? = null
    ): Result<com.example.bestapp.api.models.ApiMLMCommissionsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getMLMCommissions(limit, offset, startDate, endDate)
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting MLM commissions", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun getMLMReferralCode(): Result<com.example.bestapp.api.models.ApiMLMReferralCodeResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getMLMReferralCode()
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting MLM referral code", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun inviteMaster(userId: Long? = null, email: String? = null): Result<com.example.bestapp.api.models.MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = com.example.bestapp.api.models.ApiMLMInviteRequest(userId, email)
                val response = api.inviteMaster(request)
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inviting master", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun getMLMUpline(): Result<com.example.bestapp.api.models.ApiMLMUplineResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getMLMUpline()
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting MLM upline", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun getMLMTeamPerformance(period: Int = 30): Result<com.example.bestapp.api.models.ApiMLMTeamPerformanceResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getMLMTeamPerformance(period)
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting MLM team performance", e)
                Result.failure(e)
            }
        }
    }
    
    // ============= Verification Codes =============
    
    suspend fun sendEmailVerificationCode(): Result<com.example.bestapp.api.models.MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.sendEmailVerificationCode()
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending email verification code", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun sendPhoneVerificationCode(): Result<com.example.bestapp.api.models.MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.sendPhoneVerificationCode()
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending phone verification code", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun verifyEmailCode(code: String): Result<com.example.bestapp.api.models.MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = com.example.bestapp.api.models.VerifyCodeRequest(code)
                val response = api.verifyEmailCode(request)
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying email code", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun verifyPhoneCode(code: String): Result<com.example.bestapp.api.models.MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = com.example.bestapp.api.models.VerifyCodeRequest(code)
                val response = api.verifyPhoneCode(request)
                if (response.isSuccessful) {
                    Result.success(response.body() ?: throw Exception("Пустой ответ"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Result.failure(Exception("Ошибка: ${response.code()}, $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying phone code", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun getVerificationStatus(): Result<com.example.bestapp.api.models.VerificationStatusResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getVerificationStatus()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Result.success(body)
                    } else {
                        Log.w(TAG, "Verification status response body is null")
                        Result.failure(Exception("Пустой ответ от сервера"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val errorJson = errorBody?.let {
                            com.google.gson.Gson().fromJson(it, Map::class.java)
                        }
                        errorJson?.get("error")?.toString() ?: when (response.code()) {
                            404 -> "Данные не найдены"
                            401 -> "Требуется авторизация"
                            403 -> "Доступ запрещен"
                            500 -> "Ошибка сервера"
                            else -> "Ошибка получения статуса: ${response.code()}"
                        }
                    } catch (e: Exception) {
                        "Ошибка получения статуса: ${response.code()}"
                    }
                    Log.e(TAG, "Get verification status failed: code=${response.code()}, message=$errorMessage")
                    Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting verification status", e)
                val errorMessage = getErrorMessage(e, "Ошибка загрузки статуса подтверждения")
                Result.failure(Exception(errorMessage))
            }
        }
    }
}


