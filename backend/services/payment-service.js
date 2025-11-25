import { query } from '../database/db.js';
import { config } from '../config.js';

// Конфигурация комиссии платформы
const PLATFORM_COMMISSION_BASIC = 15.0; // 15% для базовой подписки
const PLATFORM_COMMISSION_PREMIUM = 10.0; // 10% для премиум подписки

/**
 * Рассчитывает комиссию платформы
 * @param {number} amount - Сумма заказа
 * @param {number} masterId - ID мастера
 * @returns {Object} { commissionPercentage, commissionAmount, netAmount }
 */
export function calculatePlatformCommission(amount, masterId) {
  // Получаем тип подписки мастера
  const master = query.get('SELECT subscription_type FROM masters WHERE id = ?', [masterId]);
  const subscriptionType = master?.subscription_type || 'basic';
  
  const commissionPercentage = subscriptionType === 'premium' 
    ? PLATFORM_COMMISSION_PREMIUM 
    : PLATFORM_COMMISSION_BASIC;
  
  const commissionAmount = Math.round((amount * commissionPercentage / 100) * 100) / 100;
  const netAmount = amount - commissionAmount;
  
  return {
    commissionPercentage,
    commissionAmount,
    netAmount
  };
}

/**
 * Создает запись о платеже
 * @param {Object} paymentData - Данные платежа
 * @returns {Object} Созданный платеж
 */
export function createPayment(paymentData) {
  try {
    const {
      orderId,
      clientId,
      amount,
      paymentMethod,
      paymentProvider = 'manual',
      currency = 'RUB'
    } = paymentData;
    
    const result = query.run(`
      INSERT INTO payments 
      (order_id, client_id, amount, currency, payment_method, payment_provider, payment_status)
      VALUES (?, ?, ?, ?, ?, ?, 'pending')
    `, [orderId, clientId, amount, currency, paymentMethod, paymentProvider]);
    
    const payment = query.get('SELECT * FROM payments WHERE id = ?', [result.lastInsertRowid]);
    
    console.log(`✅ Создан платеж #${payment.id} для заказа #${orderId}, сумма: ${amount} ${currency}`);
    
    return payment;
  } catch (error) {
    console.error('Ошибка создания платежа:', error);
    throw error;
  }
}

/**
 * Обновляет статус платежа
 * @param {number} paymentId - ID платежа
 * @param {string} status - Новый статус
 * @param {Object} additionalData - Дополнительные данные (provider_payment_id, receipt_url и т.д.)
 */
export function updatePaymentStatus(paymentId, status, additionalData = {}) {
  try {
    const updates = ['payment_status = ?', 'updated_at = CURRENT_TIMESTAMP'];
    const params = [status];
    
    if (status === 'completed' && !additionalData.paid_at) {
      updates.push('paid_at = CURRENT_TIMESTAMP');
    }
    
    if (additionalData.provider_payment_id) {
      updates.push('provider_payment_id = ?');
      params.push(additionalData.provider_payment_id);
    }
    
    if (additionalData.provider_response) {
      updates.push('provider_response = ?');
      params.push(JSON.stringify(additionalData.provider_response));
    }
    
    if (additionalData.receipt_url) {
      updates.push('receipt_url = ?');
      params.push(additionalData.receipt_url);
    }
    
    if (additionalData.receipt_data) {
      updates.push('receipt_data = ?');
      params.push(JSON.stringify(additionalData.receipt_data));
    }
    
    params.push(paymentId);
    
    query.run(
      `UPDATE payments SET ${updates.join(', ')} WHERE id = ?`,
      params
    );
    
    console.log(`✅ Статус платежа #${paymentId} обновлен на: ${status}`);
  } catch (error) {
    console.error('Ошибка обновления статуса платежа:', error);
    throw error;
  }
}

/**
 * Создает запись о комиссии платформы
 * @param {number} orderId - ID заказа
 * @param {number} paymentId - ID платежа
 * @param {number} masterId - ID мастера
 * @param {number} orderAmount - Сумма заказа
 */
export function createPlatformCommission(orderId, paymentId, masterId, orderAmount) {
  try {
    const commission = calculatePlatformCommission(orderAmount, masterId);
    
    const result = query.run(`
      INSERT INTO platform_commissions 
      (order_id, payment_id, master_id, order_amount, commission_percentage, commission_amount, status)
      VALUES (?, ?, ?, ?, ?, ?, 'pending')
    `, [
      orderId,
      paymentId,
      masterId,
      orderAmount,
      commission.commissionPercentage,
      commission.commissionAmount
    ]);
    
    // Обновляем статус комиссии на 'collected', если платеж уже завершен
    const payment = query.get('SELECT payment_status FROM payments WHERE id = ?', [paymentId]);
    if (payment && payment.payment_status === 'completed') {
      query.run(
        'UPDATE platform_commissions SET status = ?, collected_at = CURRENT_TIMESTAMP WHERE id = ?',
        ['collected', result.lastInsertRowid]
      );
    }
    
    console.log(`✅ Создана комиссия платформы: ${commission.commissionAmount} ₽ (${commission.commissionPercentage}%)`);
    
    return {
      id: result.lastInsertRowid,
      ...commission
    };
  } catch (error) {
    console.error('Ошибка создания комиссии платформы:', error);
    throw error;
  }
}

/**
 * Обрабатывает успешную оплату заказа
 * @param {number} paymentId - ID платежа
 */
export function processPaymentSuccess(paymentId) {
  try {
    const payment = query.get('SELECT * FROM payments WHERE id = ?', [paymentId]);
    if (!payment) {
      throw new Error(`Платеж #${paymentId} не найден`);
    }
    
    // Обновляем статус платежа
    updatePaymentStatus(paymentId, 'completed', {
      paid_at: new Date().toISOString()
    });
    
    // Получаем заказ
    const order = query.get('SELECT * FROM orders WHERE id = ?', [payment.order_id]);
    if (!order) {
      throw new Error(`Заказ #${payment.order_id} не найден`);
    }
    
    // Если есть мастер, создаем комиссию и транзакцию мастера
    if (order.assigned_master_id) {
      // Создаем комиссию платформы
      const commission = createPlatformCommission(
        payment.order_id,
        paymentId,
        order.assigned_master_id,
        payment.amount
      );
      
      // Создаем транзакцию дохода для мастера
      const masterTransaction = query.run(`
        INSERT INTO master_transactions 
        (master_id, order_id, transaction_type, amount, description, status, commission_percentage, commission_amount)
        VALUES (?, ?, 'income', ?, ?, 'pending', ?, ?)
      `, [
        order.assigned_master_id,
        payment.order_id,
        commission.netAmount,
        `Оплата заказа #${payment.order_id}`,
        commission.commissionPercentage,
        commission.commissionAmount
      ]);
      
      // Обновляем баланс мастера
      query.run(
        'UPDATE masters SET balance = balance + ? WHERE id = ?',
        [commission.netAmount, order.assigned_master_id]
      );
      
      // Обновляем статус транзакции
      query.run(
        'UPDATE master_transactions SET status = ?, completed_at = CURRENT_TIMESTAMP WHERE id = ?',
        ['completed', masterTransaction.lastInsertRowid]
      );
      
      // Обновляем статус комиссии
      query.run(
        'UPDATE platform_commissions SET status = ?, collected_at = CURRENT_TIMESTAMP WHERE payment_id = ?',
        ['collected', paymentId]
      );
      
      console.log(`✅ Обработка платежа завершена: мастер получит ${commission.netAmount} ₽, комиссия: ${commission.commissionAmount} ₽`);
    }
    
    return payment;
  } catch (error) {
    console.error('Ошибка обработки успешной оплаты:', error);
    throw error;
  }
}

/**
 * Получает историю платежей клиента
 * @param {number} clientId - ID клиента
 * @returns {Array} Список платежей
 */
export function getClientPayments(clientId) {
  try {
    const payments = query.all(`
      SELECT 
        p.*,
        o.order_number,
        o.device_type,
        o.device_brand,
        o.device_model
      FROM payments p
      JOIN orders o ON p.order_id = o.id
      WHERE p.client_id = ?
      ORDER BY p.created_at DESC
    `, [clientId]);
    
    return payments;
  } catch (error) {
    console.error('Ошибка получения платежей клиента:', error);
    return [];
  }
}

/**
 * Получает статистику комиссий платформы
 * @param {Object} filters - Фильтры (startDate, endDate, status)
 * @returns {Object} Статистика
 */
export function getPlatformCommissionsStats(filters = {}) {
  try {
    let sql = `
      SELECT 
        COUNT(*) as total,
        SUM(commission_amount) as total_amount,
        AVG(commission_percentage) as avg_percentage
      FROM platform_commissions
      WHERE 1=1
    `;
    const params = [];
    
    if (filters.status) {
      sql += ' AND status = ?';
      params.push(filters.status);
    }
    
    if (filters.startDate) {
      sql += ' AND created_at >= ?';
      params.push(filters.startDate);
    }
    
    if (filters.endDate) {
      sql += ' AND created_at <= ?';
      params.push(filters.endDate);
    }
    
    const stats = query.get(sql, params);
    
    return {
      total: stats.total || 0,
      totalAmount: stats.total_amount || 0,
      avgPercentage: stats.avg_percentage || 0
    };
  } catch (error) {
    console.error('Ошибка получения статистики комиссий:', error);
    return { total: 0, totalAmount: 0, avgPercentage: 0 };
  }
}

export default {
  calculatePlatformCommission,
  createPayment,
  updatePaymentStatus,
  createPlatformCommission,
  processPaymentSuccess,
  getClientPayments,
  getPlatformCommissionsStats
};

