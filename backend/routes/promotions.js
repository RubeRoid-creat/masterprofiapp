import express from 'express';
import { query } from '../database/db.js';
import { authenticate, authorize } from '../middleware/auth.js';
import {
  getPromotionInfo,
  createPromotion,
  cancelPromotion,
  checkExpiredPromotions,
  PROMOTION_TYPES
} from '../services/promotion-service.js';
import { createPayment } from '../services/payment-service.js';

const router = express.Router();

// Получить информацию о продвижениях (мастер)
router.get('/my', authenticate, authorize('master'), (req, res) => {
  try {
    console.log(`[PROMOTIONS] GET /my - User: ${req.user.id}, Role: ${req.user.role}`);
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      console.log(`[PROMOTIONS] Master profile not found for user ${req.user.id}`);
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    console.log(`[PROMOTIONS] Master found: id=${master.id}`);
    const info = getPromotionInfo(master.id);
    console.log(`[PROMOTIONS] Promotion info retrieved: active=${info.activePromotions.length}`);
    
    res.json(info);
  } catch (error) {
    console.error('Ошибка получения продвижений:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Купить продвижение (мастер)
router.post('/purchase', authenticate, authorize('master'), (req, res) => {
  try {
    const { promotionType } = req.body;
    
    if (!promotionType || !PROMOTION_TYPES[promotionType]) {
      return res.status(400).json({ 
        error: 'Необходимо указать promotionType',
        availableTypes: Object.keys(PROMOTION_TYPES)
      });
    }
    
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const promotionData = PROMOTION_TYPES[promotionType];
    
    // Проверяем, нет ли уже активного продвижения этого типа
    const existing = query.get(`
      SELECT id FROM master_promotions 
      WHERE master_id = ? 
      AND promotion_type = ?
      AND status = 'active' 
      AND expires_at > datetime('now')
    `, [master.id, promotionType]);
    
    if (existing) {
      return res.status(400).json({ error: 'У вас уже есть активное продвижение этого типа' });
    }
    
    // Создаем платеж (для будущей интеграции с платежной системой)
    // TODO: Интегрировать с реальной платежной системой
    const payment = createPayment({
      orderId: null, // Продвижение не связано с заказом
      clientId: null,
      amount: promotionData.price,
      paymentMethod: 'online',
      paymentProvider: 'manual' // Пока ручная обработка
    });
    
    // Создаем продвижение
    const promotion = createPromotion(master.id, promotionType, payment.id);
    
    res.json({
      message: 'Продвижение успешно активировано',
      promotion,
      payment: {
        id: payment.id,
        amount: payment.amount,
        status: payment.payment_status
      },
      note: 'Для продакшена требуется интеграция с платежной системой'
    });
  } catch (error) {
    console.error('Ошибка покупки продвижения:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Отменить продвижение (мастер)
router.post('/:id/cancel', authenticate, authorize('master'), (req, res) => {
  try {
    const { id } = req.params;
    
    const promotion = query.get('SELECT * FROM master_promotions WHERE id = ?', [id]);
    if (!promotion) {
      return res.status(404).json({ error: 'Продвижение не найдено' });
    }
    
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (promotion.master_id !== master.id) {
      return res.status(403).json({ error: 'Нет доступа к этому продвижению' });
    }
    
    cancelPromotion(id);
    
    res.json({ message: 'Продвижение отменено' });
  } catch (error) {
    console.error('Ошибка отмены продвижения:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Получить доступные типы продвижений
router.get('/types', (req, res) => {
  res.json(PROMOTION_TYPES);
});

// Админ: получить все продвижения
router.get('/admin/all', authenticate, authorize('admin'), (req, res) => {
  try {
    const { status, promotionType } = req.query;
    
    let sql = `
      SELECT 
        mp.*,
        m.id as master_id,
        u.name as master_name,
        u.email as master_email
      FROM master_promotions mp
      JOIN masters m ON mp.master_id = m.id
      JOIN users u ON m.user_id = u.id
      WHERE 1=1
    `;
    const params = [];
    
    if (status) {
      sql += ' AND mp.status = ?';
      params.push(status);
    }
    
    if (promotionType) {
      sql += ' AND mp.promotion_type = ?';
      params.push(promotionType);
    }
    
    sql += ' ORDER BY mp.created_at DESC';
    
    const promotions = query.all(sql, params);
    
    res.json(promotions);
  } catch (error) {
    console.error('Ошибка получения продвижений:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Проверка истекших продвижений (для cron job)
router.post('/admin/check-expired', authenticate, authorize('admin'), (req, res) => {
  try {
    const count = checkExpiredPromotions();
    res.json({ message: `Проверено истекших продвижений: ${count}` });
  } catch (error) {
    console.error('Ошибка проверки продвижений:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

export default router;

