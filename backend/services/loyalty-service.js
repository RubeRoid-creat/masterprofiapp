import { query } from '../database/db.js';

// Конфигурация программы лояльности
const LOYALTY_CONFIG = {
  // Начисление баллов
  POINTS_PER_ORDER: 10, // Баллов за каждый заказ
  POINTS_PER_REVIEW: 5, // Баллов за отзыв
  POINTS_PER_REFERRAL: 50, // Баллов за приглашение друга
  POINTS_PER_RUB: 1, // 1 балл за каждый рубль потраченный (опционально)
  
  // Использование баллов
  RUBLES_PER_POINT: 0.1, // 1 балл = 0.1 рубля скидки
  MIN_POINTS_TO_USE: 100, // Минимум баллов для использования
  
  // Срок действия баллов (в днях)
  POINTS_EXPIRY_DAYS: 365, // Баллы действуют 1 год
};

/**
 * Начислить баллы клиенту
 * @param {number} clientId - ID клиента
 * @param {number} points - Количество баллов
 * @param {string} sourceType - Тип источника ('order', 'review', 'referral', 'bonus')
 * @param {number} sourceId - ID источника (заказа, отзыва и т.д.)
 * @param {string} description - Описание начисления
 * @returns {Promise<number>} - ID созданной записи
 */
export function addLoyaltyPoints(clientId, points, sourceType, sourceId = null, description = null) {
  try {
    // Вычисляем дату истечения
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + LOYALTY_CONFIG.POINTS_EXPIRY_DAYS);
    
    // Добавляем запись о начислении
    const result = query.run(`
      INSERT INTO loyalty_points 
      (client_id, points, source_type, source_id, description, expires_at)
      VALUES (?, ?, ?, ?, ?, ?)
    `, [clientId, points, sourceType, sourceId, description, expiresAt.toISOString()]);
    
    // Обновляем общее количество баллов у клиента
    updateClientTotalPoints(clientId);
    
    return result.lastInsertRowid;
  } catch (error) {
    console.error('Ошибка начисления баллов:', error);
    throw error;
  }
}

/**
 * Получить текущий баланс баллов клиента
 * @param {number} clientId - ID клиента
 * @returns {Promise<number>} - Текущий баланс баллов
 */
export function getClientLoyaltyBalance(clientId) {
  try {
    const now = new Date().toISOString();
    
    // Считаем только не истекшие баллы
    const result = query.get(`
      SELECT COALESCE(SUM(points), 0) as total_points
      FROM loyalty_points
      WHERE client_id = ? 
        AND (expires_at IS NULL OR expires_at > ?)
    `, [clientId, now]);
    
    return result ? result.total_points : 0;
  } catch (error) {
    console.error('Ошибка получения баланса баллов:', error);
    return 0;
  }
}

/**
 * Использовать баллы для скидки
 * @param {number} clientId - ID клиента
 * @param {number} pointsToUse - Количество баллов для использования
 * @param {number} orderId - ID заказа (опционально)
 * @param {string} description - Описание использования
 * @returns {Promise<{success: boolean, discount: number, message?: string}>}
 */
export function useLoyaltyPoints(clientId, pointsToUse, orderId = null, description = null) {
  try {
    // Проверяем минимальное количество баллов
    if (pointsToUse < LOYALTY_CONFIG.MIN_POINTS_TO_USE) {
      return {
        success: false,
        discount: 0,
        message: `Минимум ${LOYALTY_CONFIG.MIN_POINTS_TO_USE} баллов для использования`
      };
    }
    
    // Проверяем баланс
    const balance = getClientLoyaltyBalance(clientId);
    if (balance < pointsToUse) {
      return {
        success: false,
        discount: 0,
        message: 'Недостаточно баллов'
      };
    }
    
    // Вычисляем скидку
    const discount = pointsToUse * LOYALTY_CONFIG.RUBLES_PER_POINT;
    
    // Используем баллы (FIFO - первые начисленные первыми используются)
    const now = new Date().toISOString();
    let remainingPoints = pointsToUse;
    
    const availablePoints = query.all(`
      SELECT id, points
      FROM loyalty_points
      WHERE client_id = ? 
        AND (expires_at IS NULL OR expires_at > ?)
        AND points > 0
      ORDER BY created_at ASC
    `, [clientId, now]);
    
    for (const pointRecord of availablePoints) {
      if (remainingPoints <= 0) break;
      
      const pointsToDeduct = Math.min(remainingPoints, pointRecord.points);
      
      // Уменьшаем баллы в записи
      query.run(`
        UPDATE loyalty_points
        SET points = points - ?
        WHERE id = ?
      `, [pointsToDeduct, pointRecord.id]);
      
      // Добавляем запись об использовании
      query.run(`
        INSERT INTO loyalty_transactions 
        (client_id, points_used, order_id, description)
        VALUES (?, ?, ?, ?)
      `, [clientId, pointsToDeduct, orderId, description || `Использовано ${pointsToDeduct} баллов`]);
      
      remainingPoints -= pointsToDeduct;
    }
    
    // Обновляем общее количество баллов
    updateClientTotalPoints(clientId);
    
    return {
      success: true,
      discount: discount,
      message: `Использовано ${pointsToUse} баллов, скидка ${discount.toFixed(2)} ₽`
    };
  } catch (error) {
    console.error('Ошибка использования баллов:', error);
    return {
      success: false,
      discount: 0,
      message: 'Ошибка использования баллов'
    };
  }
}

/**
 * Обновить общее количество баллов у клиента
 * @param {number} clientId - ID клиента
 */
function updateClientTotalPoints(clientId) {
  try {
    const balance = getClientLoyaltyBalance(clientId);
    query.run(`
      UPDATE clients
      SET total_loyalty_points = ?
      WHERE id = ?
    `, [balance, clientId]);
  } catch (error) {
    console.error('Ошибка обновления общего количества баллов:', error);
  }
}

/**
 * Получить историю баллов клиента
 * @param {number} clientId - ID клиента
 * @param {number} limit - Лимит записей
 * @returns {Promise<Array>}
 */
export function getLoyaltyHistory(clientId, limit = 50) {
  try {
    const points = query.all(`
      SELECT 
        id,
        points,
        source_type,
        source_id,
        description,
        expires_at,
        created_at
      FROM loyalty_points
      WHERE client_id = ?
      ORDER BY created_at DESC
      LIMIT ?
    `, [clientId, limit]);
    
    const transactions = query.all(`
      SELECT 
        id,
        points_used,
        order_id,
        description,
        created_at
      FROM loyalty_transactions
      WHERE client_id = ?
      ORDER BY created_at DESC
      LIMIT ?
    `, [clientId, limit]);
    
    return {
      points: points,
      transactions: transactions,
      balance: getClientLoyaltyBalance(clientId)
    };
  } catch (error) {
    console.error('Ошибка получения истории баллов:', error);
    return { points: [], transactions: [], balance: 0 };
  }
}

/**
 * Начислить баллы за заказ
 * @param {number} clientId - ID клиента
 * @param {number} orderId - ID заказа
 * @param {number} orderAmount - Сумма заказа
 */
export function awardPointsForOrder(clientId, orderId, orderAmount) {
  // Начисляем базовые баллы за заказ
  addLoyaltyPoints(
    clientId,
    LOYALTY_CONFIG.POINTS_PER_ORDER,
    'order',
    orderId,
    `Баллы за заказ #${orderId}`
  );
  
  // Дополнительно начисляем баллы за сумму (опционально)
  // const pointsFromAmount = Math.floor(orderAmount * LOYALTY_CONFIG.POINTS_PER_RUB);
  // if (pointsFromAmount > 0) {
  //   addLoyaltyPoints(
  //     clientId,
  //     pointsFromAmount,
  //     'order',
  //     orderId,
  //     `Бонусные баллы за сумму заказа`
  //   );
  // }
}

/**
 * Начислить баллы за отзыв
 * @param {number} clientId - ID клиента
 * @param {number} reviewId - ID отзыва
 */
export function awardPointsForReview(clientId, reviewId) {
  addLoyaltyPoints(
    clientId,
    LOYALTY_CONFIG.POINTS_PER_REVIEW,
    'review',
    reviewId,
    'Баллы за оставленный отзыв'
  );
}

/**
 * Очистить истекшие баллы (вызывать периодически)
 */
export function cleanExpiredPoints() {
  try {
    const now = new Date().toISOString();
    const result = query.run(`
      DELETE FROM loyalty_points
      WHERE expires_at IS NOT NULL 
        AND expires_at < ?
        AND points = 0
    `, [now]);
    
    return result.changes;
  } catch (error) {
    console.error('Ошибка очистки истекших баллов:', error);
    return 0;
  }
}

export { LOYALTY_CONFIG };

