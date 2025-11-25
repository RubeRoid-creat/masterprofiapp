package com.example.bestapp.api

import android.util.Log
import com.example.bestapp.api.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApiRepository {
    private val api = RetrofitClient.apiService
    
    companion object {
        private const val TAG = "ApiRepository"
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
                Result.failure(e)
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
                Result.failure(e)
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
                Result.failure(e)
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
                Result.failure(e)
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
                Result.failure(e)
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
    
    // ============= Назначения =============
    
    suspend fun getMyAssignments(status: String? = null): Result<List<ApiAssignment>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getMyAssignments(status)
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Got ${response.body()!!.size} assignments")
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Ошибка получения назначений: ${response.code()}"))
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
                Result.failure(e)
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
                Result.failure(e)
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
                val response = api.getMasterStats(period)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Ошибка получения статистики: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Get master stats error", e)
                Result.failure(e)
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
                Result.failure(e)
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
                Result.failure(e)
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
                Result.failure(e)
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
}


