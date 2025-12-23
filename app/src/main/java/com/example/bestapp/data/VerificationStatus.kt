package com.example.bestapp.data

enum class VerificationStatus(val displayName: String) {
    NOT_VERIFIED("Не верифицирован"),
    PENDING("На проверке"),
    VERIFIED("Верифицирован"),
    REJECTED("Отклонен")
}

data class VerificationDocument(
    val id: String,
    val type: DocumentType,
    val fileName: String,
    val filePath: String,
    val uploadedAt: Long = System.currentTimeMillis()
)

enum class DocumentType(val displayName: String) {
    PASSPORT("Паспорт"),
    CERTIFICATE("Сертификат"),
    DIPLOMA("Диплом"),
    PORTFOLIO("Портфолио")
}

data class MasterVerification(
    val masterId: Long,
    val status: VerificationStatus = VerificationStatus.NOT_VERIFIED,
    val documents: List<VerificationDocument> = emptyList(),
    val portfolioImages: List<String> = emptyList(),
    val rejectionReason: String? = null,
    val submittedAt: Long? = null,
    val verifiedAt: Long? = null
)







