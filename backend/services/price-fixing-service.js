import { query } from '../database/db.js';

/**
 * Фиксирует цену заказа после согласования с мастером
 * @param {number} orderId - ID заказа
 * @param {number} finalCost - Финальная стоимость
 * @param {string} reason - Причина изменения цены (опционально)
 */
export function fixOrderPrice(orderId, finalCost, reason = null) {
  try {
    const order = query.get('SELECT * FROM orders WHERE id = ?', [orderId]);
    if (!order) {
      throw new Error(`Заказ #${orderId} не найден`);
    }
    
    // Обновляем финальную стоимость
    query.run(
      'UPDATE orders SET final_cost = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      [finalCost, orderId]
    );
    
    // Записываем в историю изменения цены
    query.run(`
      INSERT INTO order_status_history (order_id, old_status, new_status, note)
      VALUES (?, ?, ?, ?)
    `, [
      orderId,
      order.repair_status,
      order.repair_status,
      reason 
        ? `Цена зафиксирована: ${finalCost} ₽. Причина: ${reason}`
        : `Цена зафиксирована: ${finalCost} ₽`
    ]);
    
    console.log(`✅ Цена заказа #${orderId} зафиксирована: ${finalCost} ₽`);
    
    return {
      orderId,
      estimatedCost: order.estimated_cost,
      finalCost,
      priceChange: finalCost - (order.estimated_cost || 0)
    };
  } catch (error) {
    console.error('Ошибка фиксации цены:', error);
    throw error;
  }
}

/**
 * Получает историю изменения цены заказа
 * @param {number} orderId - ID заказа
 * @returns {Array} История изменений
 */
export function getPriceHistory(orderId) {
  try {
    const history = query.all(`
      SELECT 
        old_status,
        new_status,
        note,
        created_at
      FROM order_status_history
      WHERE order_id = ? AND note LIKE '%Цена%'
      ORDER BY created_at DESC
    `, [orderId]);
    
    return history;
  } catch (error) {
    console.error('Ошибка получения истории цены:', error);
    return [];
  }
}

/**
 * Проверяет, зафиксирована ли цена заказа
 * @param {number} orderId - ID заказа
 * @returns {boolean}
 */
export function isPriceFixed(orderId) {
  try {
    const order = query.get('SELECT final_cost FROM orders WHERE id = ?', [orderId]);
    return order && order.final_cost !== null && order.final_cost > 0;
  } catch (error) {
    console.error('Ошибка проверки фиксации цены:', error);
    return false;
  }
}

/**
 * Генерирует данные для чека (для будущей генерации PDF)
 * @param {number} orderId - ID заказа
 * @param {number} paymentId - ID платежа
 * @returns {Object} Данные чека
 */
export function generateReceiptData(orderId, paymentId) {
  try {
    const order = query.get(`
      SELECT 
        o.*,
        u.name as client_name,
        u.email as client_email,
        u.phone as client_phone,
        m.id as master_id,
        mu.name as master_name,
        mu.phone as master_phone
      FROM orders o
      JOIN clients c ON o.client_id = c.id
      JOIN users u ON c.user_id = u.id
      LEFT JOIN masters m ON o.assigned_master_id = m.id
      LEFT JOIN users mu ON m.user_id = mu.id
      WHERE o.id = ?
    `, [orderId]);
    
    if (!order) {
      throw new Error(`Заказ #${orderId} не найден`);
    }
    
    const payment = query.get('SELECT * FROM payments WHERE id = ?', [paymentId]);
    if (!payment) {
      throw new Error(`Платеж #${paymentId} не найден`);
    }
    
    const commission = query.get(
      'SELECT * FROM platform_commissions WHERE order_id = ?',
      [orderId]
    );
    
    const receiptData = {
      receiptNumber: `REC-${orderId}-${Date.now()}`,
      date: new Date().toISOString(),
      order: {
        id: order.id,
        orderNumber: order.order_number,
        deviceType: order.device_type,
        deviceBrand: order.device_brand,
        deviceModel: order.device_model,
        problemDescription: order.problem_description
      },
      client: {
        name: order.client_name,
        email: order.client_email,
        phone: order.client_phone
      },
      master: order.master_id ? {
        id: order.master_id,
        name: order.master_name,
        phone: order.master_phone
      } : null,
      payment: {
        amount: payment.amount,
        method: payment.payment_method,
        status: payment.payment_status,
        paidAt: payment.paid_at
      },
      costs: {
        finalCost: order.final_cost || order.estimated_cost,
        commission: commission ? commission.commission_amount : 0,
        masterAmount: commission ? (payment.amount - commission.commission_amount) : payment.amount
      }
    };
    
    return receiptData;
  } catch (error) {
    console.error('Ошибка генерации данных чека:', error);
    throw error;
  }
}

export default {
  fixOrderPrice,
  getPriceHistory,
  isPriceFixed,
  generateReceiptData
};

