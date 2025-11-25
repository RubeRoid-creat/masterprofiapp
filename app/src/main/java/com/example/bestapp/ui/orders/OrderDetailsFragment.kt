package com.example.bestapp.ui.orders

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.bestapp.R
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.api.models.ApiOrder
import com.example.bestapp.data.Order
import com.example.bestapp.data.RepairStatus
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.LinearLayout
import kotlinx.coroutines.launch
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import android.location.Location
import java.util.Locale

class OrderDetailsFragment : Fragment() {

    private var currentOrder: Order? = null
    private var currentAssignmentId: Long? = null
    private var currentAssignmentStatus: String? = null
    private var lastLoadedOrderId: Long? = null
    private val apiRepository = ApiRepository()
    
    // Геолокация
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    companion object {
        private const val TAG = "OrderDetailsFragment"
    }
    
    // Регистрация для запроса разрешений на геолокацию
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            buildRoute()
        } else {
            Toast.makeText(
                context,
                "Для построения маршрута нужен доступ к геолокации",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    // Views
    private var toolbar: MaterialToolbar? = null
    private var orderId: TextView? = null
    private var orderDate: TextView? = null
    private var priorityText: TextView? = null
    private var orderTimer: TextView? = null
    private var countdownTimer: com.example.bestapp.ui.common.CountdownTimerView? = null
    private var clientName: TextView? = null
    private var clientPhone: TextView? = null
    private var clientEmail: TextView? = null
    private var preferredContactMethod: TextView? = null
    private var clientAddress: TextView? = null
    private var addressDetails: TextView? = null
    private var addressLandmark: TextView? = null
    private var arrivalTime: TextView? = null
    private var desiredRepairDate: TextView? = null
    private var urgencyText: TextView? = null
    private var intercomWorking: TextView? = null
    private var parkingAvailable: TextView? = null
    private var hasPets: TextView? = null
    private var hasSmallChildren: TextView? = null
    private var deviceInfo: TextView? = null
    private var deviceCategory: TextView? = null
    private var deviceModel: TextView? = null
    private var deviceSerialNumber: TextView? = null
    private var deviceYear: TextView? = null
    private var warrantyStatus: TextView? = null
    private var requestStatusChip: Chip? = null
    private var orderTypeChip: Chip? = null
    private var problemDescription: TextView? = null
    private var problemShortDescription: TextView? = null
    private var problemWhenStarted: TextView? = null
    private var problemConditions: TextView? = null
    private var problemErrorCodes: TextView? = null
    private var problemAttemptedFixes: TextView? = null
    private var problemCategory: TextView? = null
    private var problemTagsGroup: com.google.android.material.chip.ChipGroup? = null
    private var orderCost: TextView? = null
    private var orderCostHeader: TextView? = null
    private var orderDistanceHeader: TextView? = null
    private var orderTimeHeader: TextView? = null
    private var clientBudget: TextView? = null
    private var paymentType: TextView? = null
    private var finalCost: TextView? = null
    private var financeCard: com.google.android.material.card.MaterialCardView? = null
    private var financeSectionTitle: TextView? = null
    private var serviceCard: com.google.android.material.card.MaterialCardView? = null
    private var serviceSectionTitle: TextView? = null
    private var preliminaryDiagnosis: TextView? = null
    private var repairComplexity: TextView? = null
    private var estimatedRepairTime: TextView? = null
    private var requiredParts: TextView? = null
    private var specialEquipment: TextView? = null
    private var mediaCard: com.google.android.material.card.MaterialCardView? = null
    private var mediaSectionTitle: TextView? = null
    private var mediaList: LinearLayout? = null
    private var actionButtons: View? = null
    private var btnAccept: MaterialButton? = null
    private var btnReject: MaterialButton? = null
    private var btnShowOnMap: MaterialButton? = null
    private var btnBuildRoute: MaterialButton? = null
    private var btnChat: MaterialButton? = null
    private var btnCreateReport: MaterialButton? = null
    private var btnCompleteOrder: MaterialButton? = null
    private var btnCallClient: MaterialButton? = null
    private var btnSmsClient: MaterialButton? = null
    private var btnCopyAddress: MaterialButton? = null
    
    private var currentApiOrder: ApiOrder? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_order_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Инициализируем FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        initViews(view)
        setupToolbar()
        setupButtons()
        // Сбрасываем lastLoadedOrderId при создании view, чтобы гарантировать загрузку
        lastLoadedOrderId = null
        loadOrderData()
    }
    
    override fun onResume() {
        super.onResume()
        // Обновляем данные только если orderId изменился или еще не загружался
        val orderId = arguments?.getLong("orderId")
        if (orderId != null && orderId != 0L) {
            // Всегда обновляем данные при возврате на экран, чтобы увидеть актуальный статус
            if (orderId != lastLoadedOrderId) {
                loadOrderData()
            } else {
                // Если тот же заказ, все равно обновляем для отображения актуального статуса
                loadOrderData()
            }
        }
    }

    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        orderId = view.findViewById(R.id.order_id)
        orderDate = view.findViewById(R.id.order_date)
        priorityText = view.findViewById(R.id.priority_text)
        orderTimer = view.findViewById(R.id.order_timer)
        requestStatusChip = view.findViewById(R.id.request_status_chip)
        orderTypeChip = view.findViewById(R.id.order_type_chip)
        clientName = view.findViewById(R.id.client_name)
        clientPhone = view.findViewById(R.id.client_phone)
        clientEmail = view.findViewById(R.id.client_email)
        preferredContactMethod = view.findViewById(R.id.preferred_contact_method)
        clientAddress = view.findViewById(R.id.client_address)
        addressDetails = view.findViewById(R.id.address_details)
        addressLandmark = view.findViewById(R.id.address_landmark)
        arrivalTime = view.findViewById(R.id.arrival_time)
        desiredRepairDate = view.findViewById(R.id.desired_repair_date)
        urgencyText = view.findViewById(R.id.urgency_text)
        intercomWorking = view.findViewById(R.id.intercom_working)
        parkingAvailable = view.findViewById(R.id.parking_available)
        hasPets = view.findViewById(R.id.has_pets)
        hasSmallChildren = view.findViewById(R.id.has_small_children)
        deviceInfo = view.findViewById(R.id.device_info)
        deviceCategory = view.findViewById(R.id.device_category)
        deviceModel = view.findViewById(R.id.device_model)
        deviceSerialNumber = view.findViewById(R.id.device_serial_number)
        deviceYear = view.findViewById(R.id.device_year)
        warrantyStatus = view.findViewById(R.id.warranty_status)
        problemDescription = view.findViewById(R.id.problem_description)
        problemShortDescription = view.findViewById(R.id.problem_short_description)
        problemWhenStarted = view.findViewById(R.id.problem_when_started)
        problemConditions = view.findViewById(R.id.problem_conditions)
        problemErrorCodes = view.findViewById(R.id.problem_error_codes)
        problemAttemptedFixes = view.findViewById(R.id.problem_attempted_fixes)
        problemCategory = view.findViewById(R.id.problem_category)
        problemTagsGroup = view.findViewById(R.id.problem_tags_group)
        orderCost = view.findViewById(R.id.order_cost)
        orderCostHeader = view.findViewById(R.id.order_cost_header)
        orderDistanceHeader = view.findViewById(R.id.order_distance_header)
        orderTimeHeader = view.findViewById(R.id.order_time_header)
        clientBudget = view.findViewById(R.id.client_budget)
        paymentType = view.findViewById(R.id.payment_type)
        finalCost = view.findViewById(R.id.final_cost)
        financeCard = view.findViewById(R.id.finance_card)
        financeSectionTitle = view.findViewById(R.id.finance_section_title)
        serviceCard = view.findViewById(R.id.service_card)
        serviceSectionTitle = view.findViewById(R.id.service_section_title)
        preliminaryDiagnosis = view.findViewById(R.id.preliminary_diagnosis)
        repairComplexity = view.findViewById(R.id.repair_complexity)
        estimatedRepairTime = view.findViewById(R.id.estimated_repair_time)
        requiredParts = view.findViewById(R.id.required_parts)
        specialEquipment = view.findViewById(R.id.special_equipment)
        mediaCard = view.findViewById(R.id.media_card)
        mediaSectionTitle = view.findViewById(R.id.media_section_title)
        mediaList = view.findViewById(R.id.media_list)
        actionButtons = view.findViewById(R.id.action_buttons)
        btnAccept = view.findViewById(R.id.btn_accept)
        btnReject = view.findViewById(R.id.btn_reject)
        btnShowOnMap = view.findViewById(R.id.btn_show_on_map)
        btnBuildRoute = view.findViewById(R.id.btn_build_route)
        btnChat = view.findViewById(R.id.btn_chat)
        btnCreateReport = view.findViewById(R.id.btn_create_report)
        btnCompleteOrder = view.findViewById(R.id.btn_complete_order)
        // Кнопки звонка и SMS удалены из layout
        // btnCallClient = view.findViewById(R.id.btn_call_client)
        // btnSmsClient = view.findViewById(R.id.btn_sms_client)
        // btnCopyAddress = view.findViewById(R.id.btn_copy_address)
        
        // Логируем инициализацию кнопок
        Log.d(TAG, "Buttons initialized: actionButtons=${actionButtons != null}, btnAccept=${btnAccept != null}, btnReject=${btnReject != null}")
    }

    private fun setupToolbar() {
        toolbar?.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadOrderData() {
        val orderId = arguments?.getLong("orderId")
        
        if (orderId == null || orderId == 0L) {
            Toast.makeText(
                context,
                "Не удалось загрузить заказ: ID не указан",
                Toast.LENGTH_SHORT
            ).show()
            findNavController().navigateUp()
            return
        }
        
        // Очищаем предыдущие данные перед загрузкой новых
        currentOrder = null
        currentAssignmentId = null
        currentAssignmentStatus = null
        clearUI()
        
        Log.d(TAG, "Loading order data for orderId: $orderId")
        lastLoadedOrderId = orderId
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Загружаем заказ из API
            val orderResult = apiRepository.getOrder(orderId)
            orderResult.onSuccess { apiOrder ->
                currentApiOrder = apiOrder
                
                // Конвертируем ApiOrder в Order (для обратной совместимости)
                currentOrder = Order(
                    id = apiOrder.id,
                    clientId = apiOrder.clientId,
                    clientName = apiOrder.clientName,
                    clientPhone = apiOrder.clientPhone,
                    clientAddress = apiOrder.address,
                    latitude = apiOrder.latitude,
                    longitude = apiOrder.longitude,
                    deviceType = apiOrder.deviceType,
                    deviceBrand = apiOrder.deviceBrand ?: "",
                    deviceModel = apiOrder.deviceModel ?: "",
                    problemDescription = apiOrder.problemDescription,
                    requestStatus = com.example.bestapp.data.OrderRequestStatus.NEW,
                    orderType = if (apiOrder.orderType == "urgent" || apiOrder.priority == "urgent") com.example.bestapp.data.OrderType.URGENT else com.example.bestapp.data.OrderType.REGULAR,
                    arrivalTime = apiOrder.arrivalTime,
                    status = when(apiOrder.repairStatus) {
                        "new" -> RepairStatus.NEW
                        "in_progress" -> RepairStatus.IN_PROGRESS
                        "completed" -> RepairStatus.COMPLETED
                        "cancelled" -> RepairStatus.CANCELLED
                        else -> RepairStatus.NEW
                    },
                    estimatedCost = apiOrder.estimatedCost,
                    createdAt = java.util.Date() // TODO: Parse from apiOrder.createdAt
                )
                
                // Обновляем UI сразу с заказом
                updateUI()
                
                // Загружаем активное назначение для этого заказа
                val activeAssignmentResult = apiRepository.getActiveAssignmentForOrder(orderId)
                activeAssignmentResult.onSuccess { assignment ->
                    if (assignment != null) {
                        currentAssignmentId = assignment.id
                        currentAssignmentStatus = assignment.status
                        Log.d(TAG, "Found active assignment: $currentAssignmentId for order: $orderId, status: ${assignment.status}")
                        
                        // Запускаем таймер обратного отсчета, если назначение pending
                        if (assignment.status == "pending" && assignment.expiresAt != null) {
                            try {
                                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                                val expiresAt = dateFormat.parse(assignment.expiresAt) ?: try {
                                    val dateFormat2 = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                    dateFormat2.parse(assignment.expiresAt)
                                } catch (e2: Exception) {
                                    null
                                }
                                
                                if (expiresAt != null && expiresAt.after(java.util.Date())) {
                                    // Останавливаем предыдущий таймер
                                    countdownTimer?.stop()
                                    
                                    // Запускаем новый таймер
                                    orderTimer?.visibility = View.VISIBLE
                                    countdownTimer = com.example.bestapp.ui.common.CountdownTimerView(
                                        orderTimer!!,
                                        expiresAt,
                                        onExpired = {
                                            orderTimer?.text = "⏱️ Время истекло"
                                            orderTimer?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                                        }
                                    )
                                } else {
                                    orderTimer?.visibility = View.GONE
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing expiresAt: ${assignment.expiresAt}", e)
                                orderTimer?.visibility = View.GONE
                            }
                        } else {
                            orderTimer?.visibility = View.GONE
                            countdownTimer?.stop()
                            countdownTimer = null
                        }
                    } else {
                        currentAssignmentId = null
                        currentAssignmentStatus = null
                        orderTimer?.visibility = View.GONE
                        countdownTimer?.stop()
                        countdownTimer = null
                        Log.d(TAG, "No active assignment found for order: $orderId")
                    }
                    // Обновляем UI после загрузки назначения, чтобы показать кнопки
                    updateUI()
                }.onFailure { error ->
                    Log.e(TAG, "Failed to load active assignment, trying getMyAssignments", error)
                    // Fallback: загружаем все назначения мастера
                    val assignmentsResult = apiRepository.getMyAssignments()
                    assignmentsResult.onSuccess { assignments ->
                        Log.d(TAG, "Loaded ${assignments.size} assignments")
                        val assignment = assignments.find { it.orderId == orderId && (it.status == "pending" || it.status == "new") }
                        currentAssignmentId = assignment?.id
                        currentAssignmentStatus = assignment?.status
                        Log.d(TAG, "Found assignment: $currentAssignmentId for order: $orderId, status: $currentAssignmentStatus")
                        updateUI()
                    }.onFailure { error2 ->
                        Log.e(TAG, "Failed to load assignments", error2)
                        currentAssignmentId = null
                        currentAssignmentStatus = null
                        updateUI()
                    }
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load order", error)
                Toast.makeText(
                    context,
                    "Не удалось загрузить заказ: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun clearUI() {
        // Очищаем UI перед загрузкой новых данных
        orderId?.text = ""
        priorityText?.text = ""
        requestStatusChip?.text = ""
        orderTypeChip?.text = ""
        orderDate?.text = ""
        clientName?.text = ""
        clientPhone?.text = ""
        clientEmail?.text = ""
        preferredContactMethod?.text = ""
        clientAddress?.text = ""
        addressDetails?.text = ""
        addressLandmark?.text = ""
        arrivalTime?.text = ""
        desiredRepairDate?.text = ""
        urgencyText?.text = ""
        intercomWorking?.text = ""
        parkingAvailable?.text = ""
        hasPets?.text = ""
        hasSmallChildren?.text = ""
        deviceInfo?.text = ""
        deviceCategory?.text = ""
        deviceModel?.text = ""
        deviceSerialNumber?.text = ""
        deviceYear?.text = ""
        warrantyStatus?.text = ""
        problemDescription?.text = ""
        problemShortDescription?.text = ""
        problemWhenStarted?.text = ""
        problemConditions?.text = ""
        problemErrorCodes?.text = ""
        problemAttemptedFixes?.text = ""
        problemCategory?.text = ""
        problemTagsGroup?.removeAllViews()
        orderCost?.text = ""
        clientBudget?.text = ""
        paymentType?.text = ""
        finalCost?.text = ""
        preliminaryDiagnosis?.text = ""
        repairComplexity?.text = ""
        estimatedRepairTime?.text = ""
        requiredParts?.text = ""
        specialEquipment?.text = ""
        mediaList?.removeAllViews()
        actionButtons?.visibility = View.GONE
        financeCard?.visibility = View.GONE
        financeSectionTitle?.visibility = View.GONE
        serviceCard?.visibility = View.GONE
        serviceSectionTitle?.visibility = View.GONE
        mediaCard?.visibility = View.GONE
        mediaSectionTitle?.visibility = View.GONE
        clientEmail?.visibility = View.GONE
        preferredContactMethod?.visibility = View.GONE
        addressDetails?.visibility = View.GONE
        addressLandmark?.visibility = View.GONE
        desiredRepairDate?.visibility = View.GONE
        urgencyText?.visibility = View.GONE
        intercomWorking?.visibility = View.GONE
        parkingAvailable?.visibility = View.GONE
        hasPets?.visibility = View.GONE
        hasSmallChildren?.visibility = View.GONE
    }
    
    private fun updateUI() {
        val apiOrder = currentApiOrder
        currentOrder?.let { order ->
            Log.d(TAG, "Updating UI for order: ${order.id}, client: ${order.clientName}")
            
            // Основная информация
            val orderNumber = apiOrder?.orderNumber ?: order.id.toString()
            orderId?.text = if (apiOrder?.orderNumber != null) "Заявка №${apiOrder.orderNumber}" else "Заказ #${order.id}"
            // Отображаем статус на основе repairStatus из API
            val statusText = when(apiOrder?.repairStatus ?: order.status.toString()) {
                "new" -> "Новый"
                "in_progress" -> "В работе"
                "completed" -> "Завершен"
                "cancelled" -> "Отменен"
                else -> when(order.status) {
                    RepairStatus.NEW -> "Новый"
                    RepairStatus.DIAGNOSTICS -> "Диагностика"
                    RepairStatus.WAITING_PARTS -> "Ожидание запчастей"
                    RepairStatus.IN_PROGRESS -> "В работе"
                    RepairStatus.READY -> "Готов"
                    RepairStatus.COMPLETED -> "Завершен"
                    RepairStatus.CANCELLED -> "Отменен"
                }
            }
            requestStatusChip?.text = statusText
            orderTypeChip?.text = order.orderType.displayName
            
            // Приоритет
            apiOrder?.priority?.let { priority ->
                priorityText?.text = "Приоритет: ${when(priority) {
                    "emergency" -> "Экстренный"
                    "urgent" -> "Срочный"
                    "regular" -> "Обычный"
                    "planned" -> "Плановый"
                    else -> priority
                }}"
                priorityText?.visibility = View.VISIBLE
            } ?: run { priorityText?.visibility = View.GONE }
            
            orderDate?.text = order.getFormattedCreatedDate()
            orderCost?.text = order.getFormattedCost()
            
            // Информация о клиенте
            clientName?.text = "👤 ${order.clientName}"
            clientPhone?.text = "📞 ${formatPhone(order.clientPhone)}"
            
            apiOrder?.clientEmail?.let { email ->
                clientEmail?.text = "✉️ Email: $email"
                clientEmail?.visibility = View.VISIBLE
            } ?: run { clientEmail?.visibility = View.GONE }
            
            apiOrder?.preferredContactMethod?.let { method ->
                preferredContactMethod?.text = "Предпочтительный способ связи: ${when(method) {
                    "call" -> "Звонок"
                    "sms" -> "SMS"
                    "chat" -> "Чат"
                    else -> method
                }}"
                preferredContactMethod?.visibility = View.VISIBLE
            } ?: run { preferredContactMethod?.visibility = View.GONE }
            
            // Адрес
            clientAddress?.text = "📍 ${order.clientAddress}"
            
            // Детали адреса
            val addressParts = mutableListOf<String>()
            apiOrder?.addressBuilding?.let { addressParts.add("д. $it") }
            apiOrder?.addressApartment?.let { addressParts.add("кв. $it") }
            if (addressParts.isNotEmpty()) {
                addressDetails?.text = addressParts.joinToString(", ")
                addressDetails?.visibility = View.VISIBLE
            } else {
                addressDetails?.visibility = View.GONE
            }
            
            apiOrder?.addressFloor?.let { floor ->
                val floorText = if (addressParts.isNotEmpty()) ", этаж $floor" else "Этаж: $floor"
                addressDetails?.text = "${addressDetails?.text}$floorText"
                addressDetails?.visibility = View.VISIBLE
            }
            
            apiOrder?.addressEntranceCode?.let { code ->
                val codeText = if (addressDetails?.visibility == View.VISIBLE) ", код $code" else "Код домофона: $code"
                addressDetails?.text = "${addressDetails?.text}$codeText"
                addressDetails?.visibility = View.VISIBLE
            }
            
            apiOrder?.addressLandmark?.let { landmark ->
                addressLandmark?.text = "Ориентир: $landmark"
                addressLandmark?.visibility = View.VISIBLE
            } ?: run { addressLandmark?.visibility = View.GONE }
            
            // Временные параметры
            arrivalTime?.text = "⏰ Приезд: ${order.arrivalTime ?: "не указано"}"
            
            apiOrder?.desiredRepairDate?.let { date ->
                desiredRepairDate?.text = "Желаемая дата ремонта: $date"
                desiredRepairDate?.visibility = View.VISIBLE
            } ?: run { desiredRepairDate?.visibility = View.GONE }
            
            apiOrder?.urgency?.let { urgency ->
                urgencyText?.text = "Срочность: ${when(urgency) {
                    "emergency" -> "Экстренный (сегодня)"
                    "urgent" -> "Срочный (завтра)"
                    "planned" -> "Плановый (в течение недели)"
                    else -> urgency
                }}"
                urgencyText?.visibility = View.VISIBLE
            } ?: run { urgencyText?.visibility = View.GONE }
            
            // Особенности доступа
            apiOrder?.intercomWorking?.let { working ->
                intercomWorking?.text = "Домофон: ${if (working == 1) "Работает" else "Не работает"}"
                intercomWorking?.visibility = View.VISIBLE
            } ?: run { intercomWorking?.visibility = View.GONE }
            
            apiOrder?.parkingAvailable?.let { parking ->
                parkingAvailable?.text = "Парковка: ${if (parking == 1) "Доступна" else "Недоступна"}"
                parkingAvailable?.visibility = View.VISIBLE
            } ?: run { parkingAvailable?.visibility = View.GONE }
            
            apiOrder?.hasPets?.let { pets ->
                hasPets?.text = "Домашние животные: ${if (pets == 1) "Да" else "Нет"}"
                hasPets?.visibility = View.VISIBLE
            } ?: run { hasPets?.visibility = View.GONE }
            
            apiOrder?.hasSmallChildren?.let { children ->
                hasSmallChildren?.text = "Маленькие дети: ${if (children == 1) "Да" else "Нет"}"
                hasSmallChildren?.visibility = View.VISIBLE
            } ?: run { hasSmallChildren?.visibility = View.GONE }
            
            // Информация о технике
            deviceInfo?.text = "${order.deviceType} ${order.getDeviceFullName()}"
            
            apiOrder?.deviceCategory?.let { category ->
                deviceCategory?.text = "Категория: ${when(category) {
                    "large" -> "Крупная"
                    "small" -> "Мелкая"
                    "builtin" -> "Встраиваемая"
                    else -> category
                }}"
                deviceCategory?.visibility = View.VISIBLE
            } ?: run { deviceCategory?.visibility = View.GONE }
            
            apiOrder?.deviceModel?.let { model ->
                deviceModel?.text = "Модель: $model"
                deviceModel?.visibility = View.VISIBLE
            } ?: run { deviceModel?.visibility = View.GONE }
            
            apiOrder?.deviceSerialNumber?.let { serial ->
                deviceSerialNumber?.text = "Серийный номер: $serial"
                deviceSerialNumber?.visibility = View.VISIBLE
            } ?: run { deviceSerialNumber?.visibility = View.GONE }
            
            apiOrder?.deviceYear?.let { year ->
                deviceYear?.text = "Год выпуска/покупки: $year"
                deviceYear?.visibility = View.VISIBLE
            } ?: run { deviceYear?.visibility = View.GONE }
            
            apiOrder?.warrantyStatus?.let { warranty ->
                warrantyStatus?.text = "Гарантия: ${if (warranty == "warranty") "На гарантии" else "Постгарантийный"}"
                warrantyStatus?.visibility = View.VISIBLE
            } ?: run { warrantyStatus?.visibility = View.GONE }
            
            // Описание проблемы
            apiOrder?.problemShortDescription?.let { shortDesc ->
                problemShortDescription?.text = shortDesc
                problemShortDescription?.visibility = View.VISIBLE
            } ?: run { problemShortDescription?.visibility = View.GONE }
            
            problemDescription?.text = order.problemDescription
            
            apiOrder?.problemWhenStarted?.let { whenStarted ->
                problemWhenStarted?.text = "Когда началась: $whenStarted"
                problemWhenStarted?.visibility = View.VISIBLE
            } ?: run { problemWhenStarted?.visibility = View.GONE }
            
            apiOrder?.problemConditions?.let { conditions ->
                problemConditions?.text = "Условия проявления: $conditions"
                problemConditions?.visibility = View.VISIBLE
            } ?: run { problemConditions?.visibility = View.GONE }
            
            apiOrder?.problemErrorCodes?.let { codes ->
                problemErrorCodes?.text = "Коды ошибок: $codes"
                problemErrorCodes?.visibility = View.VISIBLE
            } ?: run { problemErrorCodes?.visibility = View.GONE }
            
            apiOrder?.problemAttemptedFixes?.let { fixes ->
                problemAttemptedFixes?.text = "Что уже пробовали: $fixes"
                problemAttemptedFixes?.visibility = View.VISIBLE
            } ?: run { problemAttemptedFixes?.visibility = View.GONE }
            
            apiOrder?.problemCategory?.let { category ->
                problemCategory?.text = "Категория проблемы: ${when(category) {
                    "electrical" -> "Электрика"
                    "mechanical" -> "Механика"
                    "electronic" -> "Электроника"
                    "software" -> "Программное обеспечение"
                    else -> category
                }}"
                problemCategory?.visibility = View.VISIBLE
            } ?: run { problemCategory?.visibility = View.GONE }
            
            // Теги проблемы
            apiOrder?.problemTags?.let { tags ->
                problemTagsGroup?.removeAllViews()
                tags.forEach { tag ->
                    val chip = Chip(requireContext())
                    chip.text = tag
                    chip.isCheckable = false
                    problemTagsGroup?.addView(chip)
                }
                problemTagsGroup?.visibility = View.VISIBLE
            } ?: run { problemTagsGroup?.visibility = View.GONE }
            
            // Финансовые параметры
            var hasFinanceInfo = false
            apiOrder?.clientBudget?.let { budget ->
                clientBudget?.text = "Бюджет клиента: ${budget.toInt()} ₽"
                clientBudget?.visibility = View.VISIBLE
                hasFinanceInfo = true
            } ?: run { clientBudget?.visibility = View.GONE }
            
            apiOrder?.paymentType?.let { payment ->
                paymentType?.text = "Тип оплаты: ${when(payment) {
                    "cash" -> "Наличные"
                    "card" -> "Карта"
                    "online" -> "Онлайн"
                    "installment" -> "Рассрочка"
                    else -> payment
                }}"
                paymentType?.visibility = View.VISIBLE
                hasFinanceInfo = true
            } ?: run { paymentType?.visibility = View.GONE }
            
            apiOrder?.finalCost?.let { final ->
                finalCost?.text = "Итоговая стоимость: ${final.toInt()} ₽"
                finalCost?.visibility = View.VISIBLE
                hasFinanceInfo = true
            } ?: run { finalCost?.visibility = View.GONE }
            
            if (hasFinanceInfo) {
                financeCard?.visibility = View.VISIBLE
                financeSectionTitle?.visibility = View.VISIBLE
            }
            
            // Служебная информация (для мастера)
            var hasServiceInfo = false
            apiOrder?.preliminaryDiagnosis?.let { diagnosis ->
                preliminaryDiagnosis?.text = "Предварительный диагноз: $diagnosis"
                preliminaryDiagnosis?.visibility = View.VISIBLE
                hasServiceInfo = true
            } ?: run { preliminaryDiagnosis?.visibility = View.GONE }
            
            apiOrder?.repairComplexity?.let { complexity ->
                repairComplexity?.text = "Сложность ремонта: ${when(complexity) {
                    "simple" -> "Простой"
                    "medium" -> "Средний"
                    "complex" -> "Сложный"
                    else -> complexity
                }}"
                repairComplexity?.visibility = View.VISIBLE
                hasServiceInfo = true
            } ?: run { repairComplexity?.visibility = View.GONE }
            
            apiOrder?.estimatedRepairTime?.let { time ->
                estimatedRepairTime?.text = "Расчетное время работы: $time мин."
                estimatedRepairTime?.visibility = View.VISIBLE
                hasServiceInfo = true
            } ?: run { estimatedRepairTime?.visibility = View.GONE }
            
            apiOrder?.requiredParts?.let { parts ->
                requiredParts?.text = "Необходимые запчасти: $parts"
                requiredParts?.visibility = View.VISIBLE
                hasServiceInfo = true
            } ?: run { requiredParts?.visibility = View.GONE }
            
            apiOrder?.specialEquipment?.let { equipment ->
                specialEquipment?.text = "Специальное оборудование: $equipment"
                specialEquipment?.visibility = View.VISIBLE
                hasServiceInfo = true
            } ?: run { specialEquipment?.visibility = View.GONE }
            
            if (hasServiceInfo) {
                serviceCard?.visibility = View.VISIBLE
                serviceSectionTitle?.visibility = View.VISIBLE
            }
            
            // Медиафайлы
            apiOrder?.media?.let { media ->
                if (media.isNotEmpty()) {
                    mediaList?.removeAllViews()
                    media.forEach { mediaItem ->
                        val mediaView = TextView(requireContext())
                        val icon = when(mediaItem.mediaType) {
                            "photo" -> "📷"
                            "video" -> "🎥"
                            "document" -> "📄"
                            "audio" -> "🎵"
                            else -> "📎"
                        }
                        mediaView.text = "$icon ${mediaItem.fileName ?: mediaItem.mediaType}"
                        mediaView.textSize = 14f
                        mediaView.setPadding(0, 8, 0, 8)
                        mediaList?.addView(mediaView)
                    }
                    mediaCard?.visibility = View.VISIBLE
                    mediaSectionTitle?.text = "📎 Медиафайлы (${media.size})"
                    mediaSectionTitle?.visibility = View.VISIBLE
                }
            } ?: run {
                mediaCard?.visibility = View.GONE
                mediaSectionTitle?.visibility = View.GONE
            }

            // Показать кнопки только если есть назначение со статусом "pending" или "new"
            val shouldShowButtons = currentAssignmentId != null && 
                    (currentAssignmentStatus == "pending" || currentAssignmentStatus == "new")
            Log.d(TAG, "Should show buttons: $shouldShowButtons (assignmentId=$currentAssignmentId, assignmentStatus=$currentAssignmentStatus, orderStatus=${order.status})")
            
            if (shouldShowButtons) {
                actionButtons?.visibility = View.VISIBLE
                Log.d(TAG, "Buttons are now VISIBLE")
            } else {
                actionButtons?.visibility = View.GONE
                Log.d(TAG, "Buttons are now GONE (assignmentId=$currentAssignmentId, status=$currentAssignmentStatus)")
            }
            
            // Показать кнопку создания отчета для заказов в работе или завершенных
            val canCreateReport = order.status == RepairStatus.IN_PROGRESS || order.status == RepairStatus.COMPLETED
            btnCreateReport?.visibility = if (canCreateReport) View.VISIBLE else View.GONE
            Log.d(TAG, "Can create report: $canCreateReport (orderStatus=${order.status})")
            
            // Показать кнопку завершения заказа только для заказов в работе
            val canCompleteOrder = order.status == RepairStatus.IN_PROGRESS
            btnCompleteOrder?.visibility = if (canCompleteOrder) View.VISIBLE else View.GONE
            Log.d(TAG, "Can complete order: $canCompleteOrder (orderStatus=${order.status})")
        }
    }

    private fun formatPhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return if (digits.length == 11 && digits.startsWith("7")) {
            "+7 (${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7, 9)}-${digits.substring(9, 11)}"
        } else {
            phone
        }
    }

    private fun setupButtons() {
        btnAccept?.setOnClickListener {
            acceptOrder()
        }

        btnReject?.setOnClickListener {
            rejectOrder()
        }

        btnShowOnMap?.setOnClickListener {
            showOrderOnMap()
        }

        btnBuildRoute?.setOnClickListener {
            requestLocationAndBuildRoute()
        }

        btnChat?.setOnClickListener {
            val bundle = Bundle().apply {
                putLong("orderId", currentOrder?.id ?: 0L)
            }
            findNavController().navigate(R.id.action_order_details_to_chat, bundle)
        }
        
        btnCreateReport?.setOnClickListener {
            val orderId = currentOrder?.id ?: currentApiOrder?.id ?: 0L
            if (orderId > 0) {
                val bundle = Bundle().apply {
                    putLong("orderId", orderId)
                }
                findNavController().navigate(R.id.action_order_details_to_work_report, bundle)
            } else {
                Toast.makeText(context, "Не удалось определить ID заказа", Toast.LENGTH_SHORT).show()
            }
        }
        
        btnCompleteOrder?.setOnClickListener {
            showCompleteOrderDialog()
        }
        
        // Загружаем информацию о маршруте при открытии экрана (если есть координаты)
        loadRouteInfo()
    }
    
    private fun showCompleteOrderDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_complete_order, null)
        val inputFinalCost = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.input_final_cost)
        val inputRepairDescription = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.input_repair_description)
        
        // Устанавливаем текущую стоимость, если есть
        currentApiOrder?.estimatedCost?.let {
            inputFinalCost.setText(it.toString())
        }
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Завершить заказ")
            .setView(dialogView)
            .setPositiveButton("Завершить") { _, _ ->
                val finalCost = inputFinalCost.text?.toString()?.toDoubleOrNull()
                val repairDescription = inputRepairDescription.text?.toString()?.takeIf { it.isNotBlank() }
                
                if (finalCost == null) {
                    Toast.makeText(context, "Укажите финальную стоимость", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                completeOrder(finalCost, repairDescription)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun completeOrder(finalCost: Double, repairDescription: String?) {
        val orderId = currentOrder?.id ?: currentApiOrder?.id ?: 0L
        if (orderId == 0L) {
            Toast.makeText(context, "Не удалось определить ID заказа", Toast.LENGTH_SHORT).show()
            return
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = apiRepository.completeOrder(orderId, finalCost, repairDescription)
                result.onSuccess {
                    Toast.makeText(context, "Заказ успешно завершен", Toast.LENGTH_SHORT).show()
                    // Обновляем данные заказа
                    loadOrderData()
                    // Возвращаемся назад через небольшую задержку
                    viewLifecycleOwner.lifecycleScope.launch {
                        kotlinx.coroutines.delay(1000)
                        findNavController().navigateUp()
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Failed to complete order", error)
                    Toast.makeText(
                        context,
                        "Не удалось завершить заказ: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error completing order", e)
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showOrderOnMap() {
        currentOrder?.let { order ->
            val latitude = order.latitude
            val longitude = order.longitude

            if (latitude != null && longitude != null) {
                val bundle = Bundle().apply {
                    putDouble("latitude", latitude)
                    putDouble("longitude", longitude)
                    putLong("orderId", order.id)
                }
                findNavController().navigate(R.id.action_order_details_to_orders_map, bundle)
            } else {
                Toast.makeText(
                    context,
                    "Координаты заказа отсутствуют",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun acceptOrder() {
        val assignmentId = currentAssignmentId
        val order = currentOrder
        
        if (assignmentId == null || order == null) {
            Toast.makeText(
                context,
                "Не удалось принять заявку: назначение не найдено",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Принимаем заказ через API
            val result = apiRepository.acceptAssignment(assignmentId)
            result.onSuccess {
                Log.d(TAG, "Order accepted successfully: assignmentId=$assignmentId")
                Toast.makeText(
                    context,
                    "Заявка #${order.id} принята в работу",
                    Toast.LENGTH_LONG
                ).show()
                
                // Обновляем список заказов в "Мои заявки" через EventBus или напрямую
                // Это будет сделано автоматически при возврате на экран через onResume
                
                // Вернуться назад
                findNavController().navigateUp()
            }.onFailure { error ->
                Log.e(TAG, "Failed to accept assignment", error)
                Toast.makeText(
                    context,
                    "Не удалось принять заявку: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun rejectOrder() {
        val assignmentId = currentAssignmentId
        val order = currentOrder
        
        if (assignmentId == null || order == null) {
            Toast.makeText(
                context,
                "Не удалось отклонить заявку: назначение не найдено",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Отклоняем заказ через API
            val result = apiRepository.rejectAssignment(assignmentId, "Мастер отклонил заявку")
            result.onSuccess {
                Log.d(TAG, "Order rejected successfully: assignmentId=$assignmentId")
                Toast.makeText(
                    context,
                    "Заявка #${order.id} отклонена",
                    Toast.LENGTH_LONG
                ).show()
                
                // Вернуться назад
                findNavController().navigateUp()
            }.onFailure { error ->
                Log.e(TAG, "Failed to reject assignment", error)
                Toast.makeText(
                    context,
                    "Не удалось отклонить заявку: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun requestLocationAndBuildRoute() {
        currentOrder?.let { order ->
            if (order.latitude == null || order.longitude == null) {
                Toast.makeText(
                    context,
                    "Координаты заказа отсутствуют",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            // Проверяем разрешения на геолокацию
            val hasFineLocation = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val hasCoarseLocation = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasFineLocation || hasCoarseLocation) {
                buildRoute()
            } else {
                // Запрашиваем разрешения
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun buildRoute() {
        currentOrder?.let { order ->
            val latitude = order.latitude
            val longitude = order.longitude

            if (latitude == null || longitude == null) {
                Toast.makeText(
                    context,
                    "Координаты заказа отсутствуют",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            // Получаем текущее местоположение мастера
            try {
                val locationTask: Task<Location> = fusedLocationClient.lastLocation
                locationTask.addOnSuccessListener { location ->
                    if (location != null) {
                        // Рассчитываем расстояние и время
                        val distance = calculateDistance(
                            location.latitude, location.longitude,
                            latitude, longitude
                        )
                        val estimatedTime = calculateEstimatedTime(distance)
                        
                        // Обновляем текст кнопки с информацией о расстоянии и времени
                        updateRouteButton(distance, estimatedTime)
                        
                        // Используем текущее местоположение как точку отправления
                        openNavigationApp(
                            fromLat = location.latitude,
                            fromLon = location.longitude,
                            toLat = latitude,
                            toLon = longitude,
                            distance = distance,
                            estimatedTime = estimatedTime
                        )
                    } else {
                        // Если местоположение недоступно, открываем маршрут без точки отправления
                        openNavigationApp(
                            fromLat = null,
                            fromLon = null,
                            toLat = latitude,
                            toLon = longitude,
                            distance = null,
                            estimatedTime = null
                        )
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Ошибка получения местоположения", e)
                    // Открываем маршрут без точки отправления
                    openNavigationApp(
                        fromLat = null,
                        fromLon = null,
                        toLat = latitude,
                        toLon = longitude,
                        distance = null,
                        estimatedTime = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при построении маршрута", e)
                // Открываем маршрут без точки отправления
                openNavigationApp(
                    fromLat = null,
                    fromLon = null,
                    toLat = latitude,
                    toLon = longitude,
                    distance = null,
                    estimatedTime = null
                )
            }
        }
    }
    
    /**
     * Рассчитывает расстояние между двумя точками (формула Haversine)
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Радиус Земли в метрах
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Рассчитывает примерное время в пути (в минутах)
     */
    private fun calculateEstimatedTime(distanceMeters: Double): Int {
        // Средняя скорость в городе ~40 км/ч = 11.1 м/с
        // Добавляем 30% на светофоры и пробки
        val averageSpeed = 11.1 * 0.7 // ~7.8 м/с
        val timeSeconds = distanceMeters / averageSpeed
        return (timeSeconds / 60).toInt() + 1 // +1 минута на парковку
    }
    
    /**
     * Форматирует расстояние для отображения
     */
    private fun formatDistance(meters: Double): String {
        return when {
            meters < 1000 -> "${meters.toInt()} м"
            else -> String.format(Locale.getDefault(), "%.1f км", meters / 1000)
        }
    }
    
    /**
     * Обновляет текст кнопки с информацией о маршруте
     */
    private fun updateRouteButton(distance: Double?, estimatedTime: Int?) {
        btnBuildRoute?.let { button ->
            val baseText = "📍 Построить маршрут"
            if (distance != null && estimatedTime != null) {
                val distanceText = formatDistance(distance)
                val timeText = "$estimatedTime мин"
                button.text = "$baseText\n$distanceText • $timeText"
            } else {
                button.text = baseText
            }
        }
    }
    
    /**
     * Показывает информацию о маршруте в Toast
     */
    private fun showRouteInfo(distance: Double?, estimatedTime: Int?) {
        if (distance != null && estimatedTime != null) {
            val distanceText = formatDistance(distance)
            Toast.makeText(
                context,
                "Расстояние: $distanceText, Время в пути: ~$estimatedTime мин",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Загружает информацию о маршруте при открытии экрана
     */
    private fun loadRouteInfo() {
        currentOrder?.let { order ->
            val latitude = order.latitude
            val longitude = order.longitude
            
            if (latitude != null && longitude != null) {
                // Проверяем разрешения на геолокацию
                val hasFineLocation = ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                
                val hasCoarseLocation = ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                
                if (hasFineLocation || hasCoarseLocation) {
                    // Получаем текущее местоположение и рассчитываем маршрут
                    try {
                        val locationTask: Task<Location> = fusedLocationClient.lastLocation
                        locationTask.addOnSuccessListener { location ->
                            if (location != null) {
                                val distance = calculateDistance(
                                    location.latitude, location.longitude,
                                    latitude, longitude
                                )
                                val estimatedTime = calculateEstimatedTime(distance)
                                updateRouteButton(distance, estimatedTime)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка при загрузке информации о маршруте", e)
                    }
                }
            }
        }
    }

    private fun openNavigationApp(
        fromLat: Double?,
        fromLon: Double?,
        toLat: Double,
        toLon: Double,
        distance: Double? = null,
        estimatedTime: Int? = null
    ) {
        val packageManager = requireActivity().packageManager
        val availableApps = mutableListOf<Pair<String, Intent>>()

        // 1. Yandex Navigator (приоритет)
        val yandexNavUri = if (fromLat != null && fromLon != null) {
            Uri.parse("yandexnavi://build_route?lat_from=$fromLat&lon_from=$fromLon&lat_to=$toLat&lon_to=$toLon")
        } else {
            Uri.parse("yandexnavi://build_route?lat_to=$toLat&lon_to=$toLon")
        }
        val yandexNavIntent = Intent(Intent.ACTION_VIEW, yandexNavUri).apply {
            setPackage("ru.yandex.yandexnavi")
        }
        if (yandexNavIntent.resolveActivity(packageManager) != null) {
            availableApps.add("Yandex Navigator" to yandexNavIntent)
        }

        // 2. Google Maps
        val googleMapsUri = if (fromLat != null && fromLon != null) {
            Uri.parse("google.navigation:q=$toLat,$toLon")
        } else {
            Uri.parse("geo:$toLat,$toLon?q=$toLat,$toLon")
        }
        val googleMapsIntent = Intent(Intent.ACTION_VIEW, googleMapsUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (googleMapsIntent.resolveActivity(packageManager) != null) {
            availableApps.add("Google Maps" to googleMapsIntent)
        }

        // 3. Яндекс.Карты
        val yandexMapsUri = if (fromLat != null && fromLon != null) {
            Uri.parse("yandexmaps://build_route?lat_from=$fromLat&lon_from=$fromLon&lat_to=$toLat&lon_to=$toLon")
        } else {
            Uri.parse("yandexmaps://build_route_on_map?lat_to=$toLat&lon_to=$toLon")
        }
        val yandexMapsIntent = Intent(Intent.ACTION_VIEW, yandexMapsUri).apply {
            setPackage("ru.yandex.yandexmaps")
        }
        if (yandexMapsIntent.resolveActivity(packageManager) != null) {
            availableApps.add("Яндекс.Карты" to yandexMapsIntent)
        }

        // Если есть доступные приложения, показываем диалог выбора
        if (availableApps.isNotEmpty()) {
            if (availableApps.size == 1) {
                // Если только одно приложение, открываем его сразу
                startActivity(availableApps[0].second)
                // Показываем информацию о маршруте
                showRouteInfo(distance, estimatedTime)
            } else {
                // Показываем диалог выбора с информацией о маршруте
                val dialogTitle = if (distance != null && estimatedTime != null) {
                    val distanceText = formatDistance(distance)
                    "Выберите навигатор\n$distanceText • $estimatedTime мин"
                } else {
                    "Выберите навигатор"
                }
                
                val appNames = availableApps.map { it.first }.toTypedArray()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(dialogTitle)
                    .setItems(appNames) { _, which ->
                        startActivity(availableApps[which].second)
                        // Показываем информацию о маршруте
                        showRouteInfo(distance, estimatedTime)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        } else {
            // Если нет установленных навигаторов, открываем в браузере
            val webUri = if (fromLat != null && fromLon != null) {
                Uri.parse("https://yandex.ru/maps/?rtext=$fromLat,$fromLon~$toLat,$toLon&rtt=auto")
            } else {
                Uri.parse("https://yandex.ru/maps/?pt=$toLon,$toLat&z=16")
            }
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            startActivity(webIntent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Останавливаем таймер
        countdownTimer?.stop()
        countdownTimer = null
        
        toolbar = null
        orderId = null
        orderDate = null
        priorityText = null
        orderTimer = null
        clientName = null
        clientPhone = null
        clientEmail = null
        preferredContactMethod = null
        clientAddress = null
        addressDetails = null
        addressLandmark = null
        arrivalTime = null
        desiredRepairDate = null
        urgencyText = null
        intercomWorking = null
        parkingAvailable = null
        hasPets = null
        hasSmallChildren = null
        deviceInfo = null
        deviceCategory = null
        deviceModel = null
        deviceSerialNumber = null
        deviceYear = null
        warrantyStatus = null
        requestStatusChip = null
        orderTypeChip = null
        problemDescription = null
        problemShortDescription = null
        problemWhenStarted = null
        problemConditions = null
        problemErrorCodes = null
        problemAttemptedFixes = null
        problemCategory = null
        problemTagsGroup = null
        orderCost = null
        clientBudget = null
        paymentType = null
        finalCost = null
        financeCard = null
        financeSectionTitle = null
        serviceCard = null
        serviceSectionTitle = null
        preliminaryDiagnosis = null
        repairComplexity = null
        estimatedRepairTime = null
        requiredParts = null
        specialEquipment = null
        mediaCard = null
        mediaSectionTitle = null
        mediaList = null
        actionButtons = null
        btnAccept = null
        btnReject = null
        btnShowOnMap = null
        btnBuildRoute = null
        btnChat = null
    }
}

