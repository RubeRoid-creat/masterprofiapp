import { query } from '../database/db.js';

// Конфигурация продвижений
export const PROMOTION_TYPES = {
  top_listing: {
    name: 'Топ в выдаче',
    description: 'Показ в топе результатов поиска',
    price: 499, // рублей
    duration: 7 // дней
  },
  highlighted_profile: {
    name: 'Выделенный профиль',
    description: 'Выделение профиля в списке мастеров',
    price: 299, // рублей
    duration: 7 // дней
  },
  featured: {
    name: 'Рекомендуемый мастер',
    description: 'Показ в разделе рекомендуемых мастеров',
    price: 799, // рублей
    duration: 14 // дней
  }
};

/**
 * Получает активные продвижения мастера
 * @param {number} masterId - ID мастера
 * @returns {Array} Список активных продвижений
 */
export function getActivePromotions(masterId) {
  try {
    const promotions = query.all(`
      SELECT * FROM master_promotions 
      WHERE master_id = ? 
      AND status = 'active' 
      AND expires_at > datetime('now')
      ORDER BY expires_at DESC
    `, [masterId]);
    
    return promotions;
  } catch (error) {
    console.error('Ошибка получения продвижений мастера:', error);
    return [];
  }
}

/**
 * Проверяет, есть ли у мастера активное продвижение определенного типа
 * @param {number} masterId - ID мастера
 * @param {string} promotionType - Тип продвижения
 * @returns {boolean}
 */
export function hasActivePromotion(masterId, promotionType) {
  try {
    const promotion = query.get(`
      SELECT id FROM master_promotions 
      WHERE master_id = ? 
      AND promotion_type = ?
      AND status = 'active' 
      AND expires_at > datetime('now')
    `, [masterId, promotionType]);
    
    return !!promotion;
  } catch (error) {
    console.error('Ошибка проверки продвижения:', error);
    return false;
  }
}

/**
 * Создает продвижение для мастера
 * @param {number} masterId - ID мастера
 * @param {string} promotionType - Тип продвижения
 * @param {number} paymentId - ID платежа
 * @returns {Object} Созданное продвижение
 */
export function createPromotion(masterId, promotionType, paymentId) {
  try {
    const promotionData = PROMOTION_TYPES[promotionType];
    if (!promotionData) {
      throw new Error(`Неизвестный тип продвижения: ${promotionType}`);
    }
    
    // Вычисляем дату окончания
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + promotionData.duration);
    
    const result = query.run(`
      INSERT INTO master_promotions 
      (master_id, promotion_type, status, started_at, expires_at, payment_id)
      VALUES (?, ?, 'active', CURRENT_TIMESTAMP, ?, ?)
    `, [masterId, promotionType, expiresAt.toISOString(), paymentId]);
    
    const promotion = query.get('SELECT * FROM master_promotions WHERE id = ?', [result.lastInsertRowid]);
    
    console.log(`✅ Создано продвижение "${promotionData.name}" для мастера #${masterId}, действует до ${expiresAt.toISOString()}`);
    
    return promotion;
  } catch (error) {
    console.error('Ошибка создания продвижения:', error);
    throw error;
  }
}

/**
 * Проверяет и обновляет истекшие продвижения
 */
export function checkExpiredPromotions() {
  try {
    const expired = query.all(`
      SELECT * FROM master_promotions 
      WHERE status = 'active' 
      AND expires_at <= datetime('now')
    `);
    
    for (const promotion of expired) {
      query.run(`
        UPDATE master_promotions 
        SET status = 'expired', updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
      `, [promotion.id]);
      
      console.log(`⚠️ Продвижение мастера #${promotion.master_id} истекло`);
    }
    
    return expired.length;
  } catch (error) {
    console.error('Ошибка проверки истекших продвижений:', error);
    return 0;
  }
}

/**
 * Отменяет продвижение
 * @param {number} promotionId - ID продвижения
 */
export function cancelPromotion(promotionId) {
  try {
    query.run(`
      UPDATE master_promotions 
      SET status = 'cancelled', updated_at = CURRENT_TIMESTAMP
      WHERE id = ?
    `, [promotionId]);
    
    console.log(`✅ Продвижение #${promotionId} отменено`);
  } catch (error) {
    console.error('Ошибка отмены продвижения:', error);
    throw error;
  }
}

/**
 * Получает информацию о продвижениях для мастера
 * @param {number} masterId - ID мастера
 * @returns {Object} Информация о продвижениях
 */
export function getPromotionInfo(masterId) {
  try {
    const activePromotions = getActivePromotions(masterId);
    
    return {
      activePromotions,
      availableTypes: PROMOTION_TYPES,
      hasTopListing: hasActivePromotion(masterId, 'top_listing'),
      hasHighlighted: hasActivePromotion(masterId, 'highlighted_profile'),
      hasFeatured: hasActivePromotion(masterId, 'featured')
    };
  } catch (error) {
    console.error('Ошибка получения информации о продвижениях:', error);
    return {
      activePromotions: [],
      availableTypes: PROMOTION_TYPES,
      hasTopListing: false,
      hasHighlighted: false,
      hasFeatured: false
    };
  }
}

export default {
  getActivePromotions,
  hasActivePromotion,
  createPromotion,
  checkExpiredPromotions,
  cancelPromotion,
  getPromotionInfo,
  PROMOTION_TYPES
};

