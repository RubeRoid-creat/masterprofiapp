package com.bestapp.client.data.api.models

import com.google.gson.annotations.SerializedName

// Auth models
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String,
    val role: String = "client" // Явно указываем роль клиента
)

data class AuthResponse(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    @SerializedName("created_at") val createdAt: String
)

// Order models
data class CreateOrderRequest(
    val address: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("device_type") val deviceType: String,
    @SerializedName("device_category") val deviceCategory: String? = null, // "large", "small", "builtin"
    @SerializedName("device_brand") val deviceBrand: String? = null,
    @SerializedName("device_model") val deviceModel: String? = null,
    @SerializedName("device_serial_number") val deviceSerialNumber: String? = null,
    @SerializedName("device_year") val deviceYear: Int? = null,
    @SerializedName("warranty_status") val warrantyStatus: String? = null, // "warranty", "post_warranty"
    @SerializedName("problem_short_description") val problemShortDescription: String? = null,
    @SerializedName("problem_description") val problemDescription: String,
    @SerializedName("problem_when_started") val problemWhenStarted: String? = null,
    @SerializedName("problem_conditions") val problemConditions: String? = null,
    @SerializedName("problem_error_codes") val problemErrorCodes: String? = null,
    @SerializedName("problem_attempted_fixes") val problemAttemptedFixes: String? = null,
    @SerializedName("address_street") val addressStreet: String? = null,
    @SerializedName("address_building") val addressBuilding: String? = null,
    @SerializedName("address_apartment") val addressApartment: String? = null,
    @SerializedName("address_floor") val addressFloor: Int? = null,
    @SerializedName("address_entrance_code") val addressEntranceCode: String? = null,
    @SerializedName("address_landmark") val addressLandmark: String? = null,
    @SerializedName("arrival_time") val arrivalTime: String? = null,
    @SerializedName("desired_repair_date") val desiredRepairDate: String? = null,
    @SerializedName("urgency") val urgency: String? = null, // "emergency", "urgent", "planned"
    @SerializedName("priority") val priority: String? = null, // "emergency", "urgent", "regular", "planned"
    @SerializedName("order_source") val orderSource: String? = "app",
    @SerializedName("order_type") val orderType: String = "regular", // "regular" или "urgent"
    @SerializedName("client_budget") val clientBudget: Double? = null,
    @SerializedName("payment_type") val paymentType: String? = null, // "cash", "card", "online", "installment"
    @SerializedName("intercom_working") val intercomWorking: Boolean? = true,
    @SerializedName("parking_available") val parkingAvailable: Boolean? = true,
    @SerializedName("has_pets") val hasPets: Boolean? = false,
    @SerializedName("has_small_children") val hasSmallChildren: Boolean? = false,
    @SerializedName("preferred_contact_method") val preferredContactMethod: String? = "call",
    @SerializedName("problem_tags") val problemTags: List<String>? = null,
    @SerializedName("problem_category") val problemCategory: String? = null // "electrical", "mechanical", "electronic", "software"
)

data class OrderDto(
    val id: Long,
    @SerializedName("order_number") val orderNumber: String?,
    @SerializedName("client_id") val clientId: Long,
    @SerializedName("client_name") val clientName: String,
    @SerializedName("client_phone") val clientPhone: String,
    @SerializedName("client_email") val clientEmail: String?,
    val address: String,
    val latitude: Double,
    val longitude: Double,
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
    @SerializedName("address_street") val addressStreet: String?,
    @SerializedName("address_building") val addressBuilding: String?,
    @SerializedName("address_apartment") val addressApartment: String?,
    @SerializedName("address_floor") val addressFloor: Int?,
    @SerializedName("address_entrance_code") val addressEntranceCode: String?,
    @SerializedName("address_landmark") val addressLandmark: String?,
    @SerializedName("request_status") val requestStatus: String?,
    @SerializedName("priority") val priority: String?,
    @SerializedName("order_source") val orderSource: String?,
    @SerializedName("repair_status") val repairStatus: String,
    @SerializedName("payment_status") val paymentStatus: String?,
    @SerializedName("estimated_cost") val estimatedCost: Double?,
    @SerializedName("final_cost") val finalCost: Double?,
    @SerializedName("client_budget") val clientBudget: Double?,
    @SerializedName("payment_type") val paymentType: String?,
    @SerializedName("master_id") val masterId: Long?,
    @SerializedName("master_name") val masterName: String?,
    @SerializedName("arrival_time") val arrivalTime: String?,
    @SerializedName("desired_repair_date") val desiredRepairDate: String?,
    @SerializedName("urgency") val urgency: String?,
    @SerializedName("is_urgent") val isUrgent: Boolean?,
    @SerializedName("intercom_working") val intercomWorking: Int?,
    @SerializedName("parking_available") val parkingAvailable: Int?,
    @SerializedName("has_pets") val hasPets: Int?,
    @SerializedName("has_small_children") val hasSmallChildren: Int?,
    @SerializedName("preferred_contact_method") val preferredContactMethod: String?,
    @SerializedName("assignment_date") val assignmentDate: String?,
    @SerializedName("preliminary_diagnosis") val preliminaryDiagnosis: String?,
    @SerializedName("repair_complexity") val repairComplexity: String?,
    @SerializedName("estimated_repair_time") val estimatedRepairTime: Int?,
    @SerializedName("required_parts") val requiredParts: String?,
    @SerializedName("special_equipment") val specialEquipment: String?,
    @SerializedName("problem_tags") val problemTags: List<String>?,
    @SerializedName("problem_category") val problemCategory: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    val media: List<OrderMediaDto>? = null,
    @SerializedName("media_count") val mediaCount: Int? = null
)

data class OrderMediaDto(
    val id: Long,
    @SerializedName("order_id") val orderId: Long,
    @SerializedName("media_type") val mediaType: String, // "photo", "video", "document", "audio"
    @SerializedName("file_url") val fileUrl: String?,
    @SerializedName("file_name") val fileName: String?,
    @SerializedName("file_size") val fileSize: Long?,
    @SerializedName("mime_type") val mimeType: String?,
    val description: String?,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    val duration: Int?,
    @SerializedName("created_at") val createdAt: String?
)

data class OrdersResponse(
    val orders: List<OrderDto>
)

// Complete order request
data class CompleteOrderRequest(
    @SerializedName("final_cost") val finalCost: Double? = null,
    @SerializedName("repair_description") val repairDescription: String? = null
)

data class CompleteOrderResponse(
    val message: String,
    val order: OrderDto
)

// Reorder models
data class ClientDeviceDto(
    @SerializedName("device_type") val deviceType: String,
    @SerializedName("device_brand") val deviceBrand: String?,
    @SerializedName("device_model") val deviceModel: String?,
    @SerializedName("device_serial_number") val deviceSerialNumber: String?,
    @SerializedName("device_year") val deviceYear: Int?,
    @SerializedName("device_category") val deviceCategory: String?,
    @SerializedName("last_order_date") val lastOrderDate: String,
    @SerializedName("order_count") val orderCount: Int,
    @SerializedName("last_order_id") val lastOrderId: Long
)

data class ReorderOrderResponse(
    val message: String,
    val order: OrderDto,
    @SerializedName("is_warranty_case") val isWarrantyCase: Boolean
)

// FCM models
data class FcmTokenRequest(
    val token: String,
    @SerializedName("device_type") val deviceType: String? = "android",
    @SerializedName("device_id") val deviceId: String? = null
)

data class FcmTokenResponse(
    val message: String
)

// Order status history
data class OrderStatusHistoryDto(
    val id: Long,
    @SerializedName("order_id") val orderId: Long,
    @SerializedName("old_status") val oldStatus: String?,
    @SerializedName("new_status") val newStatus: String,
    @SerializedName("changed_by") val changedBy: Long?,
    @SerializedName("changed_by_name") val changedByName: String?,
    @SerializedName("changed_by_role") val changedByRole: String?,
    val note: String?,
    @SerializedName("created_at") val createdAt: String
)

// Service models
data class ServiceCategoryDto(
    val id: Long,
    val name: String,
    @SerializedName("parent_id") val parentId: Long?,
    val icon: String?,
    @SerializedName("order_index") val orderIndex: Int,
    val description: String?,
    @SerializedName("subcategories_count") val subcategoriesCount: Int? = null,
    val subcategories: List<ServiceCategoryDto>? = null
)

data class ServiceTemplateDto(
    val id: Long,
    @SerializedName("category_id") val categoryId: Long?,
    val name: String,
    val description: String?,
    @SerializedName("fixed_price") val fixedPrice: Double?,
    @SerializedName("estimated_time") val estimatedTime: Int?,
    @SerializedName("device_type") val deviceType: String?,
    @SerializedName("is_popular") val isPopular: Int?,
    @SerializedName("created_at") val createdAt: String?
)

// Master models
data class MasterDto(
    val id: Long,
    @SerializedName("user_id") val userId: Long,
    val name: String?,
    val phone: String?,
    val email: String?,
    val specialization: List<String>,
    val rating: Double,
    @SerializedName("completed_orders") val completedOrders: Int,
    val status: String,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("is_on_shift") val isOnShift: Boolean,
    @SerializedName("photo_url") val photoUrl: String?,
    val bio: String?,
    @SerializedName("experience_years") val experienceYears: Int?,
    val portfolio: List<PortfolioItemDto>? = null,
    val certificates: List<CertificateDto>? = null,
    val distance: Double? = null, // Расстояние в метрах
    @SerializedName("distance_km") val distanceKm: String? = null, // Форматированное расстояние в км
    @SerializedName("estimated_arrival_time") val estimatedArrivalTime: Int? = null, // Время прибытия в минутах
    @SerializedName("arrival_time_formatted") val arrivalTimeFormatted: String? = null, // Форматированное время прибытия
    @SerializedName("distance_formatted") val distanceFormatted: String? = null // Форматированное расстояние
)

data class PortfolioItemDto(
    val id: Long,
    @SerializedName("master_id") val masterId: Long,
    @SerializedName("image_url") val imageUrl: String,
    val description: String?,
    val category: String?,
    @SerializedName("order_index") val orderIndex: Int,
    @SerializedName("created_at") val createdAt: String?
)

data class CertificateDto(
    val id: Long,
    @SerializedName("master_id") val masterId: Long,
    val title: String,
    val issuer: String?,
    @SerializedName("issue_date") val issueDate: String?,
    @SerializedName("expiry_date") val expiryDate: String?,
    @SerializedName("certificate_url") val certificateUrl: String,
    val description: String?,
    @SerializedName("order_index") val orderIndex: Int,
    @SerializedName("created_at") val createdAt: String?
)

// Review models
data class ReviewDto(
    val id: Long,
    @SerializedName("order_id") val orderId: Long,
    @SerializedName("master_id") val masterId: Long,
    @SerializedName("client_id") val clientId: Long,
    val rating: Int,
    val comment: String?,
    @SerializedName("client_name") val clientName: String?,
    @SerializedName("master_name") val masterName: String?,
    @SerializedName("order_number") val orderNumber: String?,
    @SerializedName("device_type") val deviceType: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class ReviewsResponse(
    val reviews: List<ReviewDto>,
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("average_rating") val averageRating: Double,
    @SerializedName("rating_distribution") val ratingDistribution: Map<Int, Int>?
)

data class CreateReviewRequest(
    @SerializedName("order_id") val orderId: Long,
    val rating: Int,
    val comment: String? = null
)

data class CreateReviewResponse(
    val message: String,
    val review: ReviewDto
)

data class UpdateReviewRequest(
    val rating: Int? = null,
    val comment: String? = null
)

data class UpdateReviewResponse(
    val message: String,
    val review: ReviewDto
)

// Chat models
data class ChatMessageDto(
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

// Error response
data class ErrorResponse(
    val message: String,
    val error: String? = null
)

// Report models
data class WorkReportDto(
    val id: Long,
    @SerializedName("order_id") val orderId: Long,
    @SerializedName("master_id") val masterId: Long,
    @SerializedName("client_id") val clientId: Long,
    @SerializedName("report_type") val reportType: String?,
    @SerializedName("work_description") val workDescription: String?,
    @SerializedName("parts_used") val partsUsed: List<PartUsedDto>?,
    @SerializedName("work_duration") val workDuration: Int?,
    @SerializedName("total_cost") val totalCost: Double?,
    @SerializedName("parts_cost") val partsCost: Double?,
    @SerializedName("labor_cost") val laborCost: Double?,
    @SerializedName("before_photos") val beforePhotos: List<String>?,
    @SerializedName("after_photos") val afterPhotos: List<String>?,
    @SerializedName("client_signature") val clientSignature: String?,
    @SerializedName("client_signed_at") val clientSignedAt: String?,
    @SerializedName("master_signed_at") val masterSignedAt: String?,
    val status: String?, // "draft", "pending", "signed", "completed"
    @SerializedName("template_id") val templateId: Long?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class PartUsedDto(
    val name: String?,
    val quantity: Int?,
    val cost: Double?
)

data class SignReportRequest(
    val signature: String // Base64
)

data class MessageResponse(
    val message: String
)

// Loyalty models
data class LoyaltyBalanceDto(
    val balance: Int,
    val config: LoyaltyConfigDto
)

data class LoyaltyConfigDto(
    @SerializedName("rubles_per_point") val rublesPerPoint: Double,
    @SerializedName("min_points_to_use") val minPointsToUse: Int,
    @SerializedName("max_discount") val maxDiscount: Double
)

data class LoyaltyHistoryDto(
    val points: List<LoyaltyPointDto>,
    val transactions: List<LoyaltyTransactionDto>,
    val balance: Int
)

data class LoyaltyPointDto(
    val id: Long,
    val points: Int,
    @SerializedName("source_type") val sourceType: String,
    @SerializedName("source_id") val sourceId: Long?,
    val description: String?,
    @SerializedName("expires_at") val expiresAt: String?,
    @SerializedName("created_at") val createdAt: String
)

data class LoyaltyTransactionDto(
    val id: Long,
    @SerializedName("points_used") val pointsUsed: Int,
    @SerializedName("order_id") val orderId: Long?,
    val description: String?,
    @SerializedName("created_at") val createdAt: String
)

data class UseLoyaltyPointsRequest(
    val points: Int,
    @SerializedName("order_id") val orderId: Long? = null,
    val description: String? = null
)

data class UseLoyaltyPointsResponse(
    val message: String,
    val discount: Double,
    @SerializedName("points_used") val pointsUsed: Int,
    @SerializedName("new_balance") val newBalance: Int
)

data class LoyaltyInfoDto(
    val config: LoyaltyInfoConfigDto,
    val description: String
)

data class LoyaltyInfoConfigDto(
    @SerializedName("points_per_order") val pointsPerOrder: Int,
    @SerializedName("points_per_review") val pointsPerReview: Int,
    @SerializedName("points_per_referral") val pointsPerReferral: Int,
    @SerializedName("rubles_per_point") val rublesPerPoint: Double,
    @SerializedName("min_points_to_use") val minPointsToUse: Int,
    @SerializedName("points_expiry_days") val pointsExpiryDays: Int
)

// Payment models
data class CreatePaymentRequest(
    @SerializedName("order_id") val orderId: Long,
    val amount: Double,
    @SerializedName("payment_method") val paymentMethod: String // "cash", "card", "online", "yoomoney", "qiwi"
)

data class CreatePaymentResponse(
    val message: String,
    val payment: PaymentDto
)

data class PaymentDto(
    val id: Long,
    @SerializedName("order_id") val orderId: Long,
    @SerializedName("order_number") val orderNumber: String?,
    @SerializedName("client_id") val clientId: Long,
    val amount: Double,
    val currency: String,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("payment_provider") val paymentProvider: String?,
    @SerializedName("payment_status") val paymentStatus: String,
    @SerializedName("device_type") val deviceType: String?,
    @SerializedName("device_brand") val deviceBrand: String?,
    @SerializedName("device_model") val deviceModel: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("paid_at") val paidAt: String?,
    @SerializedName("receipt_url") val receiptUrl: String?
)

