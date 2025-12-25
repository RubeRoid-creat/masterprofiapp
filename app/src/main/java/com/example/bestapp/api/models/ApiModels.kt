package com.example.bestapp.api.models

import com.google.gson.annotations.SerializedName

// ============= Auth Models =============

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String,
    val role: String = "master" // По умолчанию регистрируем как мастера
)

data class LoginResponse(
    val message: String,
    val token: String,
    val user: ApiUser
)

data class ApiUser(
    val id: Long,
    val email: String,
    val name: String,
    val phone: String,
    val role: String,
    val masterId: Long? = null,
    val clientId: Long? = null,
    val isOnShift: Boolean? = null
)

// ============= Order Models =============

data class ApiOrder(
    val id: Long,
    @SerializedName("order_number") val orderNumber: String?,
    @SerializedName("client_id") val clientId: Long,
    @SerializedName("device_type") val deviceType: String,
    @SerializedName("device_category") val deviceCategory: String?,
    @SerializedName("device_brand") val deviceBrand: String?,
    @SerializedName("device_model") val deviceModel: String?,
    @SerializedName("device_serial_number") val deviceSerialNumber: String?,
    @SerializedName("device_year") val deviceYear: Int?,
    @SerializedName("warranty_status") val warrantyStatus: String?,
    @SerializedName("problem_short_description") val problemShortDescription: String?,
    @SerializedName("problem_description") val problemDescription: String,
    @SerializedName("problem_when_started") val problemWhenStarted: String?,
    @SerializedName("problem_conditions") val problemConditions: String?,
    @SerializedName("problem_error_codes") val problemErrorCodes: String?,
    @SerializedName("problem_attempted_fixes") val problemAttemptedFixes: String?,
    @SerializedName("problem_tags") val problemTags: List<String>?,
    @SerializedName("problem_category") val problemCategory: String?,
    @SerializedName("problem_seasonality") val problemSeasonality: String?,
    val address: String,
    @SerializedName("address_street") val addressStreet: String?,
    @SerializedName("address_building") val addressBuilding: String?,
    @SerializedName("address_apartment") val addressApartment: String?,
    @SerializedName("address_floor") val addressFloor: Int?,
    @SerializedName("address_entrance_code") val addressEntranceCode: String?,
    @SerializedName("address_landmark") val addressLandmark: String?,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("arrival_time") val arrivalTime: String?,
    @SerializedName("desired_repair_date") val desiredRepairDate: String?,
    @SerializedName("urgency") val urgency: String?,
    @SerializedName("priority") val priority: String?,
    @SerializedName("request_status") val requestStatus: String?,
    @SerializedName("order_type") val orderType: String,
    @SerializedName("order_source") val orderSource: String?,
    @SerializedName("repair_status") val repairStatus: String,
    @SerializedName("payment_status") val paymentStatus: String?,
    @SerializedName("estimated_cost") val estimatedCost: Double?,
    @SerializedName("final_cost") val finalCost: Double?,
    @SerializedName("client_budget") val clientBudget: Double?,
    @SerializedName("payment_type") val paymentType: String?,
    @SerializedName("intercom_working") val intercomWorking: Int?,
    @SerializedName("parking_available") val parkingAvailable: Int?,
    @SerializedName("has_pets") val hasPets: Int?,
    @SerializedName("has_small_children") val hasSmallChildren: Int?,
    @SerializedName("preferred_contact_method") val preferredContactMethod: String?,
    @SerializedName("master_gender_preference") val masterGenderPreference: String?,
    @SerializedName("master_min_experience") val masterMinExperience: Int?,
    @SerializedName("preferred_master_id") val preferredMasterId: Long?,
    @SerializedName("assigned_master_id") val assignedMasterId: Long?,
    @SerializedName("assignment_date") val assignmentDate: String?,
    @SerializedName("preliminary_diagnosis") val preliminaryDiagnosis: String?,
    @SerializedName("required_parts") val requiredParts: String?,
    @SerializedName("special_equipment") val specialEquipment: String?,
    @SerializedName("repair_complexity") val repairComplexity: String?,
    @SerializedName("estimated_repair_time") val estimatedRepairTime: Int?,
    @SerializedName("client_name") val clientName: String,
    @SerializedName("client_phone") val clientPhone: String,
    @SerializedName("client_email") val clientEmail: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    val distance: Double? = null, // Расстояние до заказа в метрах (только для мастеров)
    val media: List<ApiOrderMedia>? = null,
    
    // Информация о назначении (если есть)
    @SerializedName("assignment_id") val assignmentId: Long? = null,
    @SerializedName("assignment_status") val assignmentStatus: String? = null,
    @SerializedName("assignment_expires_at") val assignmentExpiresAt: String? = null
)

data class ApiOrderMedia(
    val id: Long,
    @SerializedName("order_id") val orderId: Long,
    @SerializedName("media_type") val mediaType: String,
    @SerializedName("file_url") val fileUrl: String?,
    @SerializedName("file_name") val fileName: String?,
    @SerializedName("file_size") val fileSize: Long?,
    @SerializedName("mime_type") val mimeType: String?,
    val description: String?,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    val duration: Int?,
    @SerializedName("created_at") val createdAt: String?
)

data class CreateOrderRequest(
    val deviceType: String,
    val deviceBrand: String?,
    val deviceModel: String?,
    val problemDescription: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val arrivalTime: String?,
    val orderType: String = "regular"
)

data class CreateOrderResponse(
    val message: String,
    val order: ApiOrder
)

// ============= Master Models =============

data class ApiMaster(
    val id: Long,
    @SerializedName("user_id") val userId: Long,
    val name: String,
    val phone: String,
    val email: String,
    val specialization: List<String>,
    val rating: Double,
    @SerializedName("completed_orders") val completedOrders: Int,
    val status: String,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("is_on_shift") val isOnShift: Boolean
)

data class LocationRequest(
    val latitude: Double,
    val longitude: Double
)

data class UpdateMasterProfileRequest(
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val specialization: List<String>? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

// ============= Версионирование приложения =============

data class VersionCheckRequest(
    val platform: String,
    @SerializedName("app_version") val appVersion: String,
    @SerializedName("build_version") val buildVersion: Int,
    @SerializedName("os_version") val osVersion: String
)

data class VersionCheckResponse(
    @SerializedName("update_required") val updateRequired: Boolean,
    @SerializedName("force_update") val forceUpdate: Boolean,
    @SerializedName("current_version") val currentVersion: String,
    @SerializedName("release_notes") val releaseNotes: String?,
    @SerializedName("download_url") val downloadUrl: String?,
    val supported: Boolean
)

// ============= Assignment Models =============

data class ApiAssignment(
    @SerializedName("assignment_id") val id: Long,
    @SerializedName("order_id") val orderId: Long,
    @SerializedName("master_id") val masterId: Long,
    @SerializedName("assignment_status") val status: String,
    @SerializedName("assignment_created_at") val assignedAt: String?, // Может быть null если API не вернул поле
    @SerializedName("expires_at") val expiresAt: String?, // Может быть null для старых назначений
    @SerializedName("responded_at") val respondedAt: String?,
    @SerializedName("rejection_reason") val rejectionReason: String?,
    @SerializedName("attempt_number") val attemptNumber: Int?,
    
    // Все поля заказа (полная информация, как для клиента)
    @SerializedName("id") val orderDbId: Long?, // id заказа из таблицы orders (может конфликтовать с assignment_id)
    @SerializedName("order_number") val orderNumber: String?,
    @SerializedName("client_id") val clientId: Long?,
    @SerializedName("device_type") val deviceType: String?,
    @SerializedName("device_category") val deviceCategory: String?,
    @SerializedName("device_brand") val deviceBrand: String?,
    @SerializedName("device_model") val deviceModel: String?,
    @SerializedName("device_serial_number") val deviceSerialNumber: String?,
    @SerializedName("device_year") val deviceYear: Int?,
    @SerializedName("warranty_status") val warrantyStatus: String?,
    @SerializedName("problem_short_description") val problemShortDescription: String?,
    @SerializedName("problem_description") val problemDescription: String?,
    @SerializedName("problem_when_started") val problemWhenStarted: String?,
    @SerializedName("problem_conditions") val problemConditions: String?,
    @SerializedName("problem_error_codes") val problemErrorCodes: String?,
    @SerializedName("problem_attempted_fixes") val problemAttemptedFixes: String?,
    @SerializedName("problem_tags") val problemTags: String?, // JSON строка или список
    @SerializedName("problem_category") val problemCategory: String?,
    @SerializedName("problem_seasonality") val problemSeasonality: String?,
    val address: String?,
    @SerializedName("address_street") val addressStreet: String?,
    @SerializedName("address_building") val addressBuilding: String?,
    @SerializedName("address_apartment") val addressApartment: String?,
    @SerializedName("address_floor") val addressFloor: Int?,
    @SerializedName("address_entrance_code") val addressEntranceCode: String?,
    @SerializedName("address_landmark") val addressLandmark: String?,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("arrival_time") val arrivalTime: String?,
    @SerializedName("desired_repair_date") val desiredRepairDate: String?,
    val urgency: String?,
    @SerializedName("estimated_cost") val estimatedCost: Double?,
    @SerializedName("final_cost") val finalCost: Double?,
    @SerializedName("client_budget") val clientBudget: Double?,
    @SerializedName("payment_type") val paymentType: String?,
    @SerializedName("intercom_working") val intercomWorking: Int?,
    @SerializedName("parking_available") val parkingAvailable: Int?,
    @SerializedName("has_pets") val hasPets: Int?,
    @SerializedName("has_small_children") val hasSmallChildren: Int?,
    @SerializedName("preferred_contact_method") val preferredContactMethod: String?,
    @SerializedName("master_gender_preference") val masterGenderPreference: String?,
    @SerializedName("master_min_experience") val masterMinExperience: Int?,
    @SerializedName("preferred_master_id") val preferredMasterId: Long?,
    @SerializedName("assigned_master_id") val assignedMasterId: Long?,
    @SerializedName("assignment_date") val assignmentDate: String?,
    @SerializedName("preliminary_diagnosis") val preliminaryDiagnosis: String?,
    @SerializedName("required_parts") val requiredParts: String?,
    @SerializedName("special_equipment") val specialEquipment: String?,
    @SerializedName("repair_complexity") val repairComplexity: String?,
    @SerializedName("estimated_repair_time") val estimatedRepairTime: Int?,
    @SerializedName("request_status") val requestStatus: String?,
    val priority: String?,
    @SerializedName("order_source") val orderSource: String?,
    @SerializedName("order_type") val orderType: String?,
    @SerializedName("repair_status") val repairStatus: String?,
    @SerializedName("related_order_id") val relatedOrderId: Long?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    // Информация о клиенте
    @SerializedName("client_name") val clientName: String?,
    @SerializedName("client_phone") val clientPhone: String?,
    @SerializedName("client_email") val clientEmail: String?
)

data class RejectReasonRequest(
    val reason: String
)

data class BatchAcceptRequest(
    @SerializedName("assignment_ids") val assignmentIds: List<Long>
)

// ============= Route Optimization Models =============

data class OptimizeRouteRequest(
    @SerializedName("order_ids") val orderIds: List<Long>,
    @SerializedName("start_latitude") val startLatitude: Double? = null,
    @SerializedName("start_longitude") val startLongitude: Double? = null
)

data class OptimizedRouteResponse(
    val orders: List<RouteOrderItem>,
    @SerializedName("total_distance") val totalDistance: Double,
    @SerializedName("total_time") val totalTime: Int,
    @SerializedName("start_location") val startLocation: List<Double>? = null
)

data class RouteOrderItem(
    val order: ApiOrder,
    @SerializedName("distance_from_previous") val distanceFromPrevious: Double,
    @SerializedName("time_from_previous") val timeFromPrevious: Int,
    @SerializedName("cumulative_distance") val cumulativeDistance: Double,
    @SerializedName("cumulative_time") val cumulativeTime: Int
)

data class BatchAcceptResponse(
    val message: String,
    val accepted: List<BatchAcceptResult>,
    val errors: List<BatchAcceptError>
)

data class BatchAcceptResult(
    @SerializedName("assignment_id") val assignmentId: Long,
    @SerializedName("order_id") val orderId: Long,
    val success: Boolean
)

data class BatchAcceptError(
    @SerializedName("assignment_id") val assignmentId: Long,
    val error: String
)

data class ApiRejectedAssignment(
    val id: Long,
    @SerializedName("order_id") val orderId: Long,
    val status: String,
    @SerializedName("rejected_at") val rejectedAt: String,
    @SerializedName("rejection_reason") val rejectionReason: String?,
    val order: ApiRejectedOrder
)

data class ApiRejectedOrder(
    val id: Long,
    @SerializedName("device_type") val deviceType: String,
    @SerializedName("device_brand") val deviceBrand: String?,
    @SerializedName("device_model") val deviceModel: String?,
    @SerializedName("problem_description") val problemDescription: String,
    @SerializedName("client_address") val clientAddress: String,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("estimated_cost") val estimatedCost: Double?,
    val urgency: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("repair_status") val repairStatus: String,
    val client: ApiRejectedOrderClient
)

data class ApiRejectedOrderClient(
    val name: String,
    val phone: String
)

// ============= Common Response Models =============

data class MessageResponse(
    val message: String,
    val isOnShift: Boolean? = null
)

data class UploadAvatarResponse(
    val message: String,
    @SerializedName("photo_url") val photoUrl: String
)

data class ErrorResponse(
    val error: String,
    val message: String? = null
)

// ============= Chat Models =============

data class ApiChatMessage(
    val id: Long,
    @SerializedName("order_id") val orderId: Long,
    @SerializedName("sender_id") val senderId: Long,
    @SerializedName("sender_name") val senderName: String,
    @SerializedName("sender_role") val senderRole: String,
    @SerializedName("message_type") val messageType: String, // "text", "image", "system"
    @SerializedName("message_text") val messageText: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("image_thumbnail_url") val imageThumbnailUrl: String?,
    @SerializedName("read_at") val readAt: String?,
    @SerializedName("created_at") val createdAt: String
)

data class SendChatMessageRequest(
    val message: String
)

// ============= Admin Chat Models =============

data class ApiAdminChatMessage(
    val id: Long,
    @SerializedName("user_id") val userId: Long,
    @SerializedName("sender_id") val senderId: Long,
    @SerializedName("sender_role") val senderRole: String, // 'user' or 'admin'
    @SerializedName("message_type") val messageType: String, // 'text', 'image', 'file'
    @SerializedName("message_text") val messageText: String?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("image_thumbnail_url") val imageThumbnailUrl: String?,
    @SerializedName("file_url") val fileUrl: String?,
    @SerializedName("file_name") val fileName: String?,
    @SerializedName("read_at") val readAt: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("sender_name") val senderName: String,
    @SerializedName("sender_user_role") val senderUserRole: String?
)

data class SendAdminChatMessageRequest(
    val message: String
)

data class UnreadCountResponse(
    @SerializedName("unread_count") val unreadCount: Int
)

// ============= Feedback Models =============

data class ApiFeedback(
    val id: Long,
    @SerializedName("user_id") val userId: Long,
    @SerializedName("feedback_type") val feedbackType: String, // 'suggestion', 'bug_report', 'complaint', 'praise', 'other'
    val subject: String,
    val message: String,
    val attachments: String?, // JSON массив
    val status: String, // 'new', 'in_progress', 'resolved', 'closed'
    @SerializedName("admin_response") val adminResponse: String?,
    @SerializedName("responded_by") val respondedBy: Long?,
    @SerializedName("responded_at") val respondedAt: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("user_name") val userName: String?,
    @SerializedName("user_email") val userEmail: String?
)

// ============= Wallet Models =============

data class ApiWallet(
    val balance: Double,
    @SerializedName("pending_payouts") val pendingPayouts: Double,
    @SerializedName("total_earned") val totalEarned: Double,
    @SerializedName("total_payouts") val totalPayouts: Double,
    @SerializedName("available_for_payout") val availableForPayout: Double
)

data class ApiTransaction(
    val id: Long,
    @SerializedName("master_id") val masterId: Long,
    @SerializedName("order_id") val orderId: Long?,
    @SerializedName("order_number") val orderNumber: String?,
    @SerializedName("final_cost") val finalCost: Double?,
    @SerializedName("transaction_type") val transactionType: String, // "income", "payout", "refund", "commission"
    val amount: Double,
    val description: String?,
    val status: String, // "pending", "completed", "failed", "cancelled"
    @SerializedName("commission_percentage") val commissionPercentage: Double?,
    @SerializedName("commission_amount") val commissionAmount: Double?,
    @SerializedName("payout_method") val payoutMethod: String?,
    @SerializedName("payout_details") val payoutDetails: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("completed_at") val completedAt: String?
)

data class PayoutRequest(
    val amount: Double,
    @SerializedName("payout_method") val payoutMethod: String? = "bank",
    @SerializedName("payout_details") val payoutDetails: Map<String, String>? = null
)

data class TopupRequest(
    val amount: Double,
    @SerializedName("payment_method") val paymentMethod: String? = "card",
    val description: String? = null
)

data class TopupResponse(
    val message: String,
    val transaction: ApiTransaction,
    @SerializedName("new_balance") val newBalance: Double
)

// ============= Schedule Models =============

data class ApiScheduleItem(
    val id: Long,
    val date: String, // YYYY-MM-DD
    @SerializedName("start_time") val startTime: String?, // HH:MM
    @SerializedName("end_time") val endTime: String?, // HH:MM
    @SerializedName("is_available") val isAvailable: Boolean,
    val note: String?
)

data class ApiScheduleResponse(
    val schedule: List<ApiScheduleItem>
)

data class CreateScheduleRequest(
    val date: String, // YYYY-MM-DD
    @SerializedName("start_time") val startTime: String? = null, // HH:MM
    @SerializedName("end_time") val endTime: String? = null, // HH:MM
    @SerializedName("is_available") val isAvailable: Boolean = true,
    val note: String? = null
)

data class BatchScheduleRequest(
    @SerializedName("start_date") val startDate: String, // YYYY-MM-DD
    @SerializedName("end_date") val endDate: String, // YYYY-MM-DD
    @SerializedName("start_time") val startTime: String? = null, // HH:MM
    @SerializedName("end_time") val endTime: String? = null, // HH:MM
    @SerializedName("is_available") val isAvailable: Boolean = true,
    @SerializedName("days_of_week") val daysOfWeek: List<Int>? = null // [0,1,2,3,4,5,6] где 0=воскресенье
)

// ============= Work Report Models =============

data class ApiWorkReport(
    val id: Long,
    @SerializedName("order_id") val orderId: Long,
    @SerializedName("master_id") val masterId: Long,
    @SerializedName("client_id") val clientId: Long,
    @SerializedName("report_type") val reportType: String,
    @SerializedName("work_description") val workDescription: String,
    @SerializedName("parts_used") val partsUsed: List<PartUsed>? = emptyList(),
    @SerializedName("work_duration") val workDuration: Int?,
    @SerializedName("total_cost") val totalCost: Double,
    @SerializedName("parts_cost") val partsCost: Double,
    @SerializedName("labor_cost") val laborCost: Double,
    @SerializedName("before_photos") val beforePhotos: List<String>? = emptyList(),
    @SerializedName("after_photos") val afterPhotos: List<String>? = emptyList(),
    @SerializedName("client_signature") val clientSignature: String?,
    @SerializedName("client_signed_at") val clientSignedAt: String?,
    @SerializedName("master_signed_at") val masterSignedAt: String?,
    val status: String,
    @SerializedName("template_id") val templateId: Long?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class PartUsed(
    val name: String,
    val quantity: Int,
    val cost: Double
)

data class ApiWorkReportsResponse(
    val reports: List<ApiWorkReport>
)

data class CreateWorkReportRequest(
    @SerializedName("order_id") val orderId: Long,
    @SerializedName("report_type") val reportType: String? = "standard",
    @SerializedName("work_description") val workDescription: String,
    @SerializedName("parts_used") val partsUsed: List<PartUsed>? = null,
    @SerializedName("work_duration") val workDuration: Int? = null,
    @SerializedName("total_cost") val totalCost: Double,
    @SerializedName("parts_cost") val partsCost: Double? = 0.0,
    @SerializedName("labor_cost") val laborCost: Double? = 0.0,
    @SerializedName("template_id") val templateId: Long? = null,
    @SerializedName("before_photos") val beforePhotos: List<String>? = null,
    @SerializedName("after_photos") val afterPhotos: List<String>? = null
)

data class SignReportRequest(
    val signature: String // Base64
)

data class ApiReportTemplate(
    val id: Long,
    @SerializedName("master_id") val masterId: Long?,
    val name: String,
    val description: String?,
    @SerializedName("work_description_template") val workDescriptionTemplate: String,
    @SerializedName("default_parts") val defaultParts: List<PartUsed>? = emptyList(),
    @SerializedName("default_labor_cost") val defaultLaborCost: Double?
)

data class ApiReportTemplatesResponse(
    val templates: List<ApiReportTemplate>
)

data class CreateReportTemplateRequest(
    val name: String,
    val description: String? = null,
    @SerializedName("work_description_template") val workDescriptionTemplate: String,
    @SerializedName("default_parts") val defaultParts: List<PartUsed>? = null,
    @SerializedName("default_labor_cost") val defaultLaborCost: Double? = null
)

data class CompleteOrderRequest(
    @SerializedName("final_cost") val finalCost: Double? = null,
    @SerializedName("repair_description") val repairDescription: String? = null
)


// ============= Verification Models =============

data class ApiVerificationDocument(
    val id: Long,
    @SerializedName("master_id") val masterId: Long,
    @SerializedName("document_type") val documentType: String,
    @SerializedName("document_name") val documentName: String,
    @SerializedName("file_url") val fileUrl: String,
    @SerializedName("file_name") val fileName: String?,
    @SerializedName("file_size") val fileSize: Long?,
    @SerializedName("mime_type") val mimeType: String?,
    val status: String, // 'pending', 'approved', 'rejected'
    @SerializedName("rejection_reason") val rejectionReason: String?,
    @SerializedName("reviewed_by") val reviewedBy: Long?,
    @SerializedName("reviewed_at") val reviewedAt: String?,
    @SerializedName("created_at") val createdAt: String
)

data class UploadDocumentResponse(
    val message: String,
    val document: ApiVerificationDocument
)

// ============= Verification Codes Models =============

data class VerifyCodeRequest(
    val code: String
)

data class VerificationStatusResponse(
    @SerializedName("emailVerified") val emailVerified: Boolean,
    @SerializedName("phoneVerified") val phoneVerified: Boolean
)

// ============= MLM Models =============

data class ApiMLMNetworkMember(
    @SerializedName("user_id") val userId: Long,
    val name: String,
    val email: String,
    @SerializedName("master_id") val masterId: Long,
    val rating: Double,
    @SerializedName("completed_orders") val completedOrders: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("verification_status") val verificationStatus: String?,
    val activity: String // 'active' или 'inactive'
)

data class ApiMLMNetworkStructure(
    @SerializedName("level_1") val level1: List<ApiMLMNetworkMember>,
    @SerializedName("level_2") val level2: List<ApiMLMNetworkMember>,
    @SerializedName("level_3") val level3: List<ApiMLMNetworkMember>,
    @SerializedName("total_members") val totalMembers: Int,
    @SerializedName("active_members") val activeMembers: Int
)

data class ApiMLMStructureResponse(
    val success: Boolean,
    val structure: ApiMLMNetworkStructure
)

data class ApiMLMCommission(
    val id: Long,
    @SerializedName("order_id") val orderId: Long,
    @SerializedName("order_number") val orderNumber: String?,
    @SerializedName("final_cost") val finalCost: Double?,
    @SerializedName("from_user_id") val fromUserId: Long,
    @SerializedName("from_master_name") val fromMasterName: String,
    @SerializedName("to_user_id") val toUserId: Long,
    @SerializedName("to_master_name") val toMasterName: String,
    val amount: Double,
    @SerializedName("commission_rate") val commissionRate: Double,
    @SerializedName("commission_amount") val commissionAmount: Double,
    val level: Int,
    @SerializedName("commission_type") val commissionType: String,
    val status: String,
    val description: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("completed_at") val completedAt: String?
)

data class ApiMLMCommissionsResponse(
    val success: Boolean,
    val commissions: List<ApiMLMCommission>,
    val pagination: ApiPagination
)

data class ApiPagination(
    val total: Int,
    val limit: Int,
    val offset: Int
)

data class ApiMLMCommissionsByLevel(
    val count: Int,
    val amount: Double
)

data class ApiMLMCommissionsStats(
    @SerializedName("last_30_days") val last30Days: ApiMLMCommissionsByLevel,
    val total: ApiMLMCommissionsByLevel,
    @SerializedName("by_level") val byLevel: Map<String, ApiMLMCommissionsByLevel>
)

data class ApiMLMDownlineStats(
    @SerializedName("level_1") val level1: Int,
    @SerializedName("level_2") val level2: Int,
    @SerializedName("level_3") val level3: Int,
    val total: Int,
    val active: Int
)

data class ApiMLMStatistics(
    @SerializedName("master_id") val masterId: Long,
    @SerializedName("user_id") val userId: Long,
    val rank: String,
    @SerializedName("join_date") val joinDate: String,
    val downline: ApiMLMDownlineStats,
    val commissions: ApiMLMCommissionsStats
)

data class ApiMLMStatisticsResponse(
    val success: Boolean,
    val statistics: ApiMLMStatistics
)

data class ApiMLMReferralCode(
    @SerializedName("referral_code") val referralCode: String,
    @SerializedName("referral_link") val referralLink: String,
    @SerializedName("user_id") val userId: Long
)

data class ApiMLMReferralCodeResponse(
    val success: Boolean,
    @SerializedName("referral_code") val referralCode: String,
    @SerializedName("referral_link") val referralLink: String,
    @SerializedName("user_id") val userId: Long
)

data class ApiMLMInviteRequest(
    @SerializedName("user_id") val userId: Long? = null,
    val email: String? = null
)

data class ApiMLMTeamPerformance(
    @SerializedName("total_orders") val totalOrders: Int,
    @SerializedName("total_revenue") val totalRevenue: Double,
    @SerializedName("active_members") val activeMembers: Int,
    @SerializedName("by_level") val byLevel: Map<String, ApiMLMTeamLevelStats>
)

data class ApiMLMTeamLevelStats(
    val orders: Int,
    val revenue: Double,
    val active: Int
)

data class ApiMLMTeamPerformanceResponse(
    val success: Boolean,
    @SerializedName("period_days") val periodDays: Int,
    @SerializedName("team_performance") val teamPerformance: ApiMLMTeamPerformance
)

data class ApiMLMUplineMember(
    @SerializedName("user_id") val userId: Long,
    @SerializedName("sponsor_id") val sponsorId: Long,
    val level: Int,
    @SerializedName("sponsor_info") val sponsorInfo: ApiMLMUplineSponsorInfo?
)

data class ApiMLMUplineSponsorInfo(
    val id: Long,
    val name: String,
    val email: String,
    @SerializedName("master_id") val masterId: Long?,
    val rating: Double?,
    @SerializedName("completed_orders") val completedOrders: Int?,
    val rank: String?
)

data class ApiMLMUplineResponse(
    val success: Boolean,
    val upline: List<ApiMLMUplineMember>
)


