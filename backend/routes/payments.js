import express from 'express';
import { query } from '../database/db.js';
import { authenticate, authorize } from '../middleware/auth.js';
import {
  createPayment,
  updatePaymentStatus,
  processPaymentSuccess,
  getClientPayments,
  calculatePlatformCommission
} from '../services/payment-service.js';

const router = express.Router();

// Создать платеж (клиент)
router.post('/', authenticate, (req, res) => {
  try {
    const { orderId, amount, paymentMethod } = req.body;
    
    if (!orderId || !amount || !paymentMethod) {
      return res.status(400).json({ error: 'Необходимо указать orderId, amount и paymentMethod' });
    }
    
    // Проверяем, что заказ принадлежит клиенту
    const order = query.get(`
      SELECT o.*, c.id as client_id 
      FROM orders o
      JOIN clients c ON o.client_id = c.id
      WHERE o.id = ? AND c.user_id = ?
    `, [orderId, req.user.id]);
    
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден или не принадлежит вам' });
    }
    
    // Проверяем, не оплачен ли уже заказ
    const existingPayment = query.get(
      'SELECT * FROM payments WHERE order_id = ? AND payment_status = ?',
      [orderId, 'completed']
    );
    
    if (existingPayment) {
      return res.status(400).json({ error: 'Заказ уже оплачен' });
    }
    
    // Создаем платеж
    const payment = createPayment({
      orderId,
      clientId: order.client_id,
      amount,
      paymentMethod,
      paymentProvider: paymentMethod === 'cash' ? 'manual' : 'yookassa' // TODO: настроить провайдер
    });
    
    // Если оплата наличными, сразу помечаем как завершенную
    if (paymentMethod === 'cash') {
      // Для наличных - платеж будет подтвержден мастером
      // Пока оставляем pending
    }
    
    res.status(201).json({
      message: 'Платеж создан',
      payment: {
        id: payment.id,
        orderId: payment.order_id,
        amount: payment.amount,
        paymentMethod: payment.payment_method,
        status: payment.payment_status
      }
    });
  } catch (error) {
    console.error('Ошибка создания платежа:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Подтвердить оплату наличными (мастер)
router.post('/:id/confirm-cash', authenticate, authorize('master'), (req, res) => {
  try {
    const { id } = req.params;
    
    const payment = query.get('SELECT * FROM payments WHERE id = ?', [id]);
    if (!payment) {
      return res.status(404).json({ error: 'Платеж не найден' });
    }
    
    if (payment.payment_method !== 'cash') {
      return res.status(400).json({ error: 'Этот платеж не наличными' });
    }
    
    // Проверяем, что заказ назначен этому мастеру
    const order = query.get('SELECT * FROM orders WHERE id = ?', [payment.order_id]);
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    
    if (!order || order.assigned_master_id !== master.id) {
      return res.status(403).json({ error: 'Нет доступа к этому заказу' });
    }
    
    // Обрабатываем успешную оплату
    processPaymentSuccess(id);
    
    res.json({ message: 'Оплата наличными подтверждена' });
  } catch (error) {
    console.error('Ошибка подтверждения оплаты:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Получить мои платежи (клиент)
router.get('/my', authenticate, (req, res) => {
  try {
    const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
    if (!client) {
      return res.status(404).json({ error: 'Профиль клиента не найден' });
    }
    
    const payments = getClientPayments(client.id);
    
    res.json(payments);
  } catch (error) {
    console.error('Ошибка получения платежей:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Получить платеж по ID
router.get('/:id', authenticate, (req, res) => {
  try {
    const { id } = req.params;
    
    const payment = query.get(`
      SELECT 
        p.*,
        o.order_number,
        o.device_type,
        o.device_brand,
        o.device_model,
        u.name as client_name
      FROM payments p
      JOIN orders o ON p.order_id = o.id
      JOIN clients c ON p.client_id = c.id
      JOIN users u ON c.user_id = u.id
      WHERE p.id = ?
    `, [id]);
    
    if (!payment) {
      return res.status(404).json({ error: 'Платеж не найден' });
    }
    
    // Проверяем доступ
    const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
    if (req.user.role === 'client' && (!client || payment.client_id !== client.id)) {
      return res.status(403).json({ error: 'Нет доступа к этому платежу' });
    }
    
    res.json(payment);
  } catch (error) {
    console.error('Ошибка получения платежа:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Webhook от платежной системы (для будущей интеграции)
router.post('/webhook/:provider', (req, res) => {
  try {
    const { provider } = req.params;
    // TODO: Реализовать обработку webhook от платежной системы
    console.log(`Webhook от ${provider}:`, req.body);
    
    // Здесь будет логика обработки webhook
    // Пока просто возвращаем успех
    res.json({ message: 'Webhook получен' });
  } catch (error) {
    console.error('Ошибка обработки webhook:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Админ: получить все платежи
router.get('/admin/all', authenticate, authorize('admin'), (req, res) => {
  try {
    const { status, paymentMethod, startDate, endDate } = req.query;
    
    let sql = `
      SELECT 
        p.*,
        o.order_number,
        u.name as client_name,
        u.email as client_email
      FROM payments p
      JOIN orders o ON p.order_id = o.id
      JOIN clients c ON p.client_id = c.id
      JOIN users u ON c.user_id = u.id
      WHERE 1=1
    `;
    const params = [];
    
    if (status) {
      sql += ' AND p.payment_status = ?';
      params.push(status);
    }
    
    if (paymentMethod) {
      sql += ' AND p.payment_method = ?';
      params.push(paymentMethod);
    }
    
    if (startDate) {
      sql += ' AND p.created_at >= ?';
      params.push(startDate);
    }
    
    if (endDate) {
      sql += ' AND p.created_at <= ?';
      params.push(endDate);
    }
    
    sql += ' ORDER BY p.created_at DESC LIMIT 100';
    
    const payments = query.all(sql, params);
    
    res.json(payments);
  } catch (error) {
    console.error('Ошибка получения платежей:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

export default router;

