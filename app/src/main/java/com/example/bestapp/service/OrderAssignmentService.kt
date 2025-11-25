package com.example.bestapp.service

import com.example.bestapp.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

/**
 * Сервис для управления назначением заказов мастерам
 * Реализует логику последовательного оповещения мастеров
 */
object OrderAssignmentService {
    private val repository = DataRepository
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Активные таймеры ожидания ответа
    private val activeTimers = mutableMapOf<Long, Job>()
    
    // События для UI
    private val _assignmentEvents = MutableStateFlow<AssignmentEvent?>(null)
    val assignmentEvents: StateFlow<AssignmentEvent?> = _assignmentEvents.asStateFlow()
    
    /**
     * Начать процесс назначения мастеров для заказа
     */
    fun startOrderAssignment(orderId: Long) {
        scope.launch {
            val order = repository.getOrderById(orderId) ?: return@launch
            
            // Получаем список подходящих мастеров
            val availableMasters = repository.getAvailableMasters(order.deviceType)
            
            if (availableMasters.isEmpty()) {
                _assignmentEvents.value = AssignmentEvent.NoMastersAvailable(orderId)
                return@launch
            }
            
            // Начинаем последовательное оповещение
            assignToNextMaster(orderId, availableMasters, 0)
        }
    }
    
    /**
     * Назначить заказ следующему мастеру из списка
     */
    private suspend fun assignToNextMaster(
        orderId: Long,
        masters: List<Master>,
        currentIndex: Int
    ) {
        if (currentIndex >= masters.size) {
            // Все мастера отклонили или не ответили
            _assignmentEvents.value = AssignmentEvent.AllMastersRejected(orderId)
            return
        }
        
        val master = masters[currentIndex]
        
        // Создаём назначение
        val assignment = repository.createOrderAssignment(orderId, master.id)
        
        // Отправляем уведомление мастеру
        _assignmentEvents.value = AssignmentEvent.MasterNotified(
            orderId = orderId,
            masterId = master.id,
            masterName = master.name,
            assignmentId = assignment.id
        )
        
        // Запускаем таймер ожидания ответа
        startExpirationTimer(assignment, masters, currentIndex)
    }
    
    /**
     * Запустить таймер истечения времени ожидания ответа
     */
    private fun startExpirationTimer(
        assignment: OrderAssignment,
        masters: List<Master>,
        currentIndex: Int
    ) {
        // Отменяем предыдущий таймер, если есть
        activeTimers[assignment.orderId]?.cancel()
        
        // Создаём новый таймер
        val timer = scope.launch {
            val now = Date()
            val delay = assignment.expiresAt.time - now.time
            
            if (delay > 0) {
                delay(delay)
            }
            
            // Проверяем, не ответил ли мастер
            val currentAssignment = repository.getActiveAssignmentForOrder(assignment.orderId)
            if (currentAssignment?.id == assignment.id && 
                currentAssignment.status == AssignmentStatus.PENDING) {
                
                // Время истекло, помечаем как истекшее
                repository.updateAssignmentStatus(assignment.id, AssignmentStatus.EXPIRED)
                
                _assignmentEvents.value = AssignmentEvent.AssignmentExpired(
                    orderId = assignment.orderId,
                    masterId = assignment.masterId
                )
                
                // Назначаем следующему мастеру
                assignToNextMaster(assignment.orderId, masters, currentIndex + 1)
            }
        }
        
        activeTimers[assignment.orderId] = timer
    }
    
    /**
     * Мастер принял заказ
     */
    fun acceptOrder(assignmentId: Long, masterId: Long) {
        scope.launch {
            val assignment = repository.getActiveAssignmentForOrder(
                repository.orderAssignments.value.find { it.id == assignmentId }?.orderId ?: return@launch
            ) ?: return@launch
            
            if (assignment.id != assignmentId) {
                // Это назначение уже не активно
                return@launch
            }
            
            // Отменяем таймер
            activeTimers[assignment.orderId]?.cancel()
            activeTimers.remove(assignment.orderId)
            
            // Обновляем статус назначения
            repository.updateAssignmentStatus(assignmentId, AssignmentStatus.ACCEPTED)
            
            // Обновляем статус мастера
            repository.updateMasterStatus(masterId, MasterStatus.BUSY)
            
            // Обновляем статус заказа
            val order = repository.getOrderById(assignment.orderId)
            order?.let {
                repository.updateOrder(it.copy(status = RepairStatus.IN_PROGRESS))
            }
            
            _assignmentEvents.value = AssignmentEvent.OrderAccepted(
                orderId = assignment.orderId,
                masterId = masterId
            )
        }
    }
    
    /**
     * Мастер отклонил заказ
     */
    fun rejectOrder(assignmentId: Long, masterId: Long, reason: String? = null) {
        scope.launch {
            val allAssignments = repository.orderAssignments.value
            val assignment = allAssignments.find { it.id == assignmentId } ?: return@launch
            
            // Отменяем таймер
            activeTimers[assignment.orderId]?.cancel()
            activeTimers.remove(assignment.orderId)
            
            // Обновляем статус назначения
            repository.updateAssignmentStatus(assignmentId, AssignmentStatus.REJECTED)
            
            _assignmentEvents.value = AssignmentEvent.OrderRejected(
                orderId = assignment.orderId,
                masterId = masterId
            )
            
            // Получаем порядковый номер мастера
            val order = repository.getOrderById(assignment.orderId) ?: return@launch
            val availableMasters = repository.getAvailableMasters(order.deviceType)
            val currentIndex = availableMasters.indexOfFirst { it.id == masterId }
            
            if (currentIndex >= 0) {
                // Назначаем следующему мастеру
                assignToNextMaster(assignment.orderId, availableMasters, currentIndex + 1)
            }
        }
    }
    
    /**
     * Отменить все активные назначения для заказа
     */
    fun cancelOrderAssignments(orderId: Long) {
        activeTimers[orderId]?.cancel()
        activeTimers.remove(orderId)
    }
    
    /**
     * Очистить событие (после обработки в UI)
     */
    fun clearEvent() {
        _assignmentEvents.value = null
    }
}

/**
 * События процесса назначения заказов
 */
sealed class AssignmentEvent {
    data class MasterNotified(
        val orderId: Long,
        val masterId: Long,
        val masterName: String,
        val assignmentId: Long
    ) : AssignmentEvent()
    
    data class OrderAccepted(
        val orderId: Long,
        val masterId: Long
    ) : AssignmentEvent()
    
    data class OrderRejected(
        val orderId: Long,
        val masterId: Long
    ) : AssignmentEvent()
    
    data class AssignmentExpired(
        val orderId: Long,
        val masterId: Long
    ) : AssignmentEvent()
    
    data class NoMastersAvailable(
        val orderId: Long
    ) : AssignmentEvent()
    
    data class AllMastersRejected(
        val orderId: Long
    ) : AssignmentEvent()
}







