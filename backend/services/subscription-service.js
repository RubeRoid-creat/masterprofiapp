import { query } from '../database/db.js';

// Конфигурация подписок
const SUBSCRIPTION_TYPES = {
  basic: {
    name: 'Базовый',
    price: 0,
    commission: 15.0,
    features: ['Базовый доступ к заказам', 'Комиссия 15%']
  },
  premium: {
    name: 'Премиум',
    price: 999, // рублей в месяц
    commission: 10.0,
    features: [
      'Приоритет в получении заказов',
      'Комиссия 10%',
      'Расширенная статистика',
      'Больше информации о клиенте',
      'Выделенный профиль'
    ]
  }
};

/**
 * Получает активную подписку мастера
 * @param {number} masterId - ID мастера
 * @returns {Object|null} Подписка или null
 */
export function getMasterSubscription(masterId) {
  try {
    const subscription = query.get(`
      SELECT * FROM master_subscriptions 
      WHERE master_id = ? AND status = 'active' AND (expires_at IS NULL OR expires_at > datetime('now'))
      ORDER BY created_at DESC
      LIMIT 1
    `, [masterId]);
    
    return subscription;
  } catch (error) {
    console.error('Ошибка получения подписки мастера:', error);
    return null;
  }
}

/**
 * Создает или обновляет подписку мастера
 * @param {number} masterId - ID мастера
 * @param {string} subscriptionType - Тип подписки ('basic' или 'premium')
 * @param {number} paymentId - ID платежа (опционально)
 * @returns {Object} Созданная подписка
 */
export function createOrUpdateSubscription(masterId, subscriptionType, paymentId = null) {
  try {
    // Отменяем предыдущие активные подписки
    query.run(`
      UPDATE master_subscriptions 
      SET status = 'cancelled', updated_at = CURRENT_TIMESTAMP
      WHERE master_id = ? AND status = 'active'
    `, [masterId]);
    
    // Если базовая подписка - создаем бесплатную
    if (subscriptionType === 'basic') {
      const result = query.run(`
        INSERT INTO master_subscriptions 
        (master_id, subscription_type, status, started_at, expires_at, auto_renew)
        VALUES (?, 'basic', 'active', CURRENT_TIMESTAMP, NULL, 1)
      `, [masterId]);
      
      // Обновляем тип подписки в таблице masters
      query.run('UPDATE masters SET subscription_type = ? WHERE id = ?', ['basic', masterId]);
      
      const subscription = query.get('SELECT * FROM master_subscriptions WHERE id = ?', [result.lastInsertRowid]);
      return subscription;
    }
    
    // Для премиум подписки - создаем на 30 дней
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + 30);
    
    const result = query.run(`
      INSERT INTO master_subscriptions 
      (master_id, subscription_type, status, started_at, expires_at, auto_renew, payment_id)
      VALUES (?, 'premium', 'active', CURRENT_TIMESTAMP, ?, 1, ?)
    `, [masterId, expiresAt.toISOString(), paymentId]);
    
    // Обновляем тип подписки в таблице masters
    query.run('UPDATE masters SET subscription_type = ? WHERE id = ?', ['premium', masterId]);
    
    const subscription = query.get('SELECT * FROM master_subscriptions WHERE id = ?', [result.lastInsertRowid]);
    
    console.log(`✅ Создана премиум подписка для мастера #${masterId}, действует до ${expiresAt.toISOString()}`);
    
    return subscription;
  } catch (error) {
    console.error('Ошибка создания подписки:', error);
    throw error;
  }
}

/**
 * Проверяет и обновляет истекшие подписки
 */
export function checkExpiredSubscriptions() {
  try {
    const expired = query.all(`
      SELECT * FROM master_subscriptions 
      WHERE status = 'active' 
      AND expires_at IS NOT NULL 
      AND expires_at <= datetime('now')
    `);
    
    for (const subscription of expired) {
      // Если автопродление - создаем новую подписку (если есть платеж)
      if (subscription.auto_renew === 1) {
        // TODO: Проверить наличие активного платежа для продления
        // Пока просто отменяем
        query.run(`
          UPDATE master_subscriptions 
          SET status = 'expired', updated_at = CURRENT_TIMESTAMP
          WHERE id = ?
        `, [subscription.id]);
        
        // Возвращаем мастера на базовую подписку
        query.run('UPDATE masters SET subscription_type = ? WHERE id = ?', ['basic', subscription.master_id]);
        
        console.log(`⚠️ Подписка мастера #${subscription.master_id} истекла, переведен на базовый тариф`);
      } else {
        query.run(`
          UPDATE master_subscriptions 
          SET status = 'expired', updated_at = CURRENT_TIMESTAMP
          WHERE id = ?
        `, [subscription.id]);
        
        query.run('UPDATE masters SET subscription_type = ? WHERE id = ?', ['basic', subscription.master_id]);
      }
    }
    
    return expired.length;
  } catch (error) {
    console.error('Ошибка проверки истекших подписок:', error);
    return 0;
  }
}

/**
 * Отменяет подписку мастера
 * @param {number} masterId - ID мастера
 */
export function cancelSubscription(masterId) {
  try {
    query.run(`
      UPDATE master_subscriptions 
      SET status = 'cancelled', auto_renew = 0, updated_at = CURRENT_TIMESTAMP
      WHERE master_id = ? AND status = 'active'
    `, [masterId]);
    
    // Возвращаем мастера на базовую подписку
    query.run('UPDATE masters SET subscription_type = ? WHERE id = ?', ['basic', masterId]);
    
    console.log(`✅ Подписка мастера #${masterId} отменена`);
  } catch (error) {
    console.error('Ошибка отмены подписки:', error);
    throw error;
  }
}

/**
 * Получает информацию о подписке для мастера
 * @param {number} masterId - ID мастера
 * @returns {Object} Информация о подписке
 */
export function getSubscriptionInfo(masterId) {
  try {
    const subscription = getMasterSubscription(masterId);
    const master = query.get('SELECT subscription_type FROM masters WHERE id = ?', [masterId]);
    
    const currentType = master?.subscription_type || 'basic';
    const subscriptionData = SUBSCRIPTION_TYPES[currentType];
    
    return {
      currentType,
      currentSubscription: subscription,
      subscriptionData,
      allTypes: SUBSCRIPTION_TYPES
    };
  } catch (error) {
    console.error('Ошибка получения информации о подписке:', error);
    return {
      currentType: 'basic',
      currentSubscription: null,
      subscriptionData: SUBSCRIPTION_TYPES.basic,
      allTypes: SUBSCRIPTION_TYPES
    };
  }
}

export default {
  getMasterSubscription,
  createOrUpdateSubscription,
  checkExpiredSubscriptions,
  cancelSubscription,
  getSubscriptionInfo,
  SUBSCRIPTION_TYPES
};

