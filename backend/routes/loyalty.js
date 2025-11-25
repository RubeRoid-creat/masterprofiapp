import express from 'express';
import { query } from '../database/db.js';
import { authenticate } from '../middleware/auth.js';
import {
  getClientLoyaltyBalance,
  getLoyaltyHistory,
  useLoyaltyPoints,
  LOYALTY_CONFIG
} from '../services/loyalty-service.js';

const router = express.Router();

// Получить баланс баллов клиента
router.get('/balance', authenticate, (req, res) => {
  try {
    if (req.user.role !== 'client') {
      return res.status(403).json({ error: 'Доступ только для клиентов' });
    }
    
    const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
    if (!client) {
      return res.status(404).json({ error: 'Профиль клиента не найден' });
    }
    
    const balance = getClientLoyaltyBalance(client.id);
    
    res.json({
      balance: balance,
      config: {
        rublesPerPoint: LOYALTY_CONFIG.RUBLES_PER_POINT,
        minPointsToUse: LOYALTY_CONFIG.MIN_POINTS_TO_USE,
        maxDiscount: balance * LOYALTY_CONFIG.RUBLES_PER_POINT
      }
    });
  } catch (error) {
    console.error('Ошибка получения баланса баллов:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Получить историю баллов
router.get('/history', authenticate, (req, res) => {
  try {
    if (req.user.role !== 'client') {
      return res.status(403).json({ error: 'Доступ только для клиентов' });
    }
    
    const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
    if (!client) {
      return res.status(404).json({ error: 'Профиль клиента не найден' });
    }
    
    const limit = parseInt(req.query.limit) || 50;
    const history = getLoyaltyHistory(client.id, limit);
    
    res.json(history);
  } catch (error) {
    console.error('Ошибка получения истории баллов:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Использовать баллы для скидки
router.post('/use', authenticate, (req, res) => {
  try {
    if (req.user.role !== 'client') {
      return res.status(403).json({ error: 'Доступ только для клиентов' });
    }
    
    const { points, orderId, description } = req.body;
    
    if (!points || points <= 0) {
      return res.status(400).json({ error: 'Необходимо указать количество баллов' });
    }
    
    const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
    if (!client) {
      return res.status(404).json({ error: 'Профиль клиента не найден' });
    }
    
    const result = useLoyaltyPoints(client.id, points, orderId, description);
    
    if (!result.success) {
      return res.status(400).json({ error: result.message });
    }
    
    res.json({
      message: result.message,
      discount: result.discount,
      pointsUsed: points,
      newBalance: getClientLoyaltyBalance(client.id)
    });
  } catch (error) {
    console.error('Ошибка использования баллов:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Получить информацию о программе лояльности
router.get('/info', authenticate, (req, res) => {
  try {
    res.json({
      config: {
        pointsPerOrder: LOYALTY_CONFIG.POINTS_PER_ORDER,
        pointsPerReview: LOYALTY_CONFIG.POINTS_PER_REVIEW,
        pointsPerReferral: LOYALTY_CONFIG.POINTS_PER_REFERRAL,
        rublesPerPoint: LOYALTY_CONFIG.RUBLES_PER_POINT,
        minPointsToUse: LOYALTY_CONFIG.MIN_POINTS_TO_USE,
        pointsExpiryDays: LOYALTY_CONFIG.POINTS_EXPIRY_DAYS
      },
      description: 'Программа лояльности позволяет накапливать баллы за заказы и отзывы, и использовать их для получения скидок.'
    });
  } catch (error) {
    console.error('Ошибка получения информации о программе:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

export default router;

