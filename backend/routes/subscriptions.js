import express from 'express';
import { query } from '../database/db.js';
import { authenticate, authorize } from '../middleware/auth.js';
import {
  getSubscriptionInfo,
  createOrUpdateSubscription,
  cancelSubscription,
  checkExpiredSubscriptions
} from '../services/subscription-service.js';
import { createPayment } from '../services/payment-service.js';

const router = express.Router();

// Получить информацию о подписке (мастер)
router.get('/my', authenticate, authorize('master'), (req, res) => {
  try {
    console.log(`[SUBSCRIPTIONS] GET /my - User: ${req.user.id}, Role: ${req.user.role}`);
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      console.log(`[SUBSCRIPTIONS] Master profile not found for user ${req.user.id}`);
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    console.log(`[SUBSCRIPTIONS] Master found: id=${master.id}`);
    const info = getSubscriptionInfo(master.id);
    console.log(`[SUBSCRIPTIONS] Subscription info retrieved: type=${info.currentType}`);
    
    res.json(info);
  } catch (error) {
    console.error('Ошибка получения подписки:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Активировать подписку (мастер)
router.post('/activate', authenticate, authorize('master'), (req, res) => {
  try {
    const { subscriptionType } = req.body;
    
    if (!subscriptionType || !['basic', 'premium'].includes(subscriptionType)) {
      return res.status(400).json({ error: 'Необходимо указать subscriptionType (basic или premium)' });
    }
    
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    // Если базовая подписка - активируем бесплатно
    if (subscriptionType === 'basic') {
      const subscription = createOrUpdateSubscription(master.id, 'basic');
      return res.json({
        message: 'Базовая подписка активирована',
        subscription
      });
    }
    
    // Для премиум подписки нужен платеж
    // TODO: Интегрировать с платежной системой
    // Пока создаем подписку без платежа (для тестирования)
    const subscription = createOrUpdateSubscription(master.id, 'premium');
    
    res.json({
      message: 'Премиум подписка активирована',
      subscription,
      note: 'Для продакшена требуется оплата'
    });
  } catch (error) {
    console.error('Ошибка активации подписки:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Отменить подписку (мастер)
router.post('/cancel', authenticate, authorize('master'), (req, res) => {
  try {
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    cancelSubscription(master.id);
    
    res.json({ message: 'Подписка отменена' });
  } catch (error) {
    console.error('Ошибка отмены подписки:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Админ: получить все подписки
router.get('/admin/all', authenticate, authorize('admin'), (req, res) => {
  try {
    const { status, subscriptionType } = req.query;
    
    let sql = `
      SELECT 
        ms.*,
        m.id as master_id,
        u.name as master_name,
        u.email as master_email
      FROM master_subscriptions ms
      JOIN masters m ON ms.master_id = m.id
      JOIN users u ON m.user_id = u.id
      WHERE 1=1
    `;
    const params = [];
    
    if (status) {
      sql += ' AND ms.status = ?';
      params.push(status);
    }
    
    if (subscriptionType) {
      sql += ' AND ms.subscription_type = ?';
      params.push(subscriptionType);
    }
    
    sql += ' ORDER BY ms.created_at DESC';
    
    const subscriptions = query.all(sql, params);
    
    res.json(subscriptions);
  } catch (error) {
    console.error('Ошибка получения подписок:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Проверка истекших подписок (для cron job)
router.post('/admin/check-expired', authenticate, authorize('admin'), (req, res) => {
  try {
    const count = checkExpiredSubscriptions();
    res.json({ message: `Проверено истекших подписок: ${count}` });
  } catch (error) {
    console.error('Ошибка проверки подписок:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

export default router;

