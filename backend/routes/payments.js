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
import yooKassaService from '../services/yookassa-service.js';

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
      paymentProvider: paymentMethod === 'cash' ? 'manual' : 'yookassa'
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

// Создать платеж через ЮKassa (клиент)
router.post('/create-yookassa', authenticate, async (req, res) => {
  try {
    const { orderId, amount, returnUrl } = req.body;
    
    if (!orderId || !amount) {
      return res.status(400).json({ error: 'Необходимо указать orderId и amount' });
    }
    
    // Проверяем, что заказ принадлежит клиенту
    const order = query.get(`
      SELECT o.*, c.id as client_id, u.name as client_name, u.email as client_email
      FROM orders o
      JOIN clients c ON o.client_id = c.id
      JOIN users u ON c.user_id = u.id
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
    
    // Проверяем, есть ли уже pending платеж через ЮKassa
    let payment = query.get(
      'SELECT * FROM payments WHERE order_id = ? AND payment_provider = ? AND payment_status IN (?, ?)',
      [orderId, 'yookassa', 'pending', 'processing']
    );
    
    // Если нет pending платежа, создаем новый
    if (!payment) {
      payment = createPayment({
        orderId,
        clientId: order.client_id,
        amount,
        paymentMethod: 'online',
        paymentProvider: 'yookassa'
      });
    }
    
    // Создаем платеж в ЮKassa
    try {
      const yooKassaPayment = await yooKassaService.createPayment({
        amount: parseFloat(amount),
        description: `Оплата заказа #${order.order_number || orderId}`,
        orderId: orderId,
        returnUrl: returnUrl || `${process.env.APP_URL || 'http://localhost:3000'}/payment/success?orderId=${orderId}`,
        metadata: {
          payment_id: payment.id,
          order_number: order.order_number || orderId.toString()
        }
      });
      
      // Обновляем платеж с ID от ЮKassa
      updatePaymentStatus(payment.id, 'processing', {
        provider_payment_id: yooKassaPayment.id
      });
      
      res.status(201).json({
        message: 'Платеж создан',
        payment: {
          id: payment.id,
          orderId: payment.order_id,
          amount: payment.amount,
          status: 'processing'
        },
        yooKassa: {
          paymentId: yooKassaPayment.id,
          confirmationUrl: yooKassaPayment.confirmationUrl,
          status: yooKassaPayment.status
        }
      });
    } catch (yooKassaError) {
      console.error('Ошибка создания платежа в ЮKassa:', yooKassaError);
      
      // Если ЮKassa не настроен, возвращаем ошибку
      if (yooKassaError.message.includes('не настроен')) {
        return res.status(503).json({ 
          error: 'Платежная система временно недоступна',
          details: 'ЮKassa не настроен. Обратитесь к администратору.'
        });
      }
      
      // Обновляем статус платежа на failed
      updatePaymentStatus(payment.id, 'failed');
      
      return res.status(500).json({ 
        error: 'Ошибка создания платежа',
        details: yooKassaError.message 
      });
    }
  } catch (error) {
    console.error('Ошибка создания платежа через ЮKassa:', error);
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

// Проверить статус платежа в ЮKassa
router.get('/:id/yookassa-status', authenticate, async (req, res) => {
  try {
    const { id } = req.params;
    
    // Получаем платеж
    const payment = query.get('SELECT * FROM payments WHERE id = ?', [id]);
    if (!payment) {
      return res.status(404).json({ error: 'Платеж не найден' });
    }
    
    // Проверяем доступ
    const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
    if (req.user.role === 'client' && (!client || payment.client_id !== client.id)) {
      return res.status(403).json({ error: 'Нет доступа к этому платежу' });
    }
    
    // Если платеж не через ЮKassa, возвращаем текущий статус
    if (payment.payment_provider !== 'yookassa' || !payment.provider_payment_id) {
      return res.json({
        paymentId: payment.id,
        status: payment.payment_status,
        provider: payment.payment_provider
      });
    }
    
    // Получаем актуальный статус из ЮKassa
    try {
      const yooKassaPayment = await yooKassaService.getPayment(payment.provider_payment_id);
      
      // Обновляем статус в БД, если изменился
      const yooKassaStatus = yooKassaPayment.status;
      let dbStatus = payment.payment_status;
      
      if (yooKassaStatus === 'succeeded' && dbStatus !== 'completed') {
        updatePaymentStatus(payment.id, 'completed', {
          provider_response: yooKassaPayment
        });
        processPaymentSuccess(payment.id);
        dbStatus = 'completed';
      } else if (yooKassaStatus === 'canceled' && dbStatus !== 'cancelled') {
        updatePaymentStatus(payment.id, 'cancelled', {
          provider_response: yooKassaPayment
        });
        dbStatus = 'cancelled';
      }
      
      res.json({
        paymentId: payment.id,
        yooKassaPaymentId: payment.provider_payment_id,
        status: dbStatus,
        yooKassaStatus: yooKassaStatus,
        amount: yooKassaPayment.amount?.value,
        currency: yooKassaPayment.amount?.currency
      });
    } catch (yooKassaError) {
      console.error('Ошибка проверки статуса в ЮKassa:', yooKassaError);
      // Возвращаем статус из БД в случае ошибки
      res.json({
        paymentId: payment.id,
        status: payment.payment_status,
        error: 'Не удалось проверить статус в ЮKassa'
      });
    }
  } catch (error) {
    console.error('Ошибка проверки статуса платежа:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Webhook от ЮKassa
router.post('/webhook/yookassa', express.raw({ type: 'application/json' }), async (req, res) => {
  try {
    const webhookData = typeof req.body === 'string' ? JSON.parse(req.body) : req.body;
    
    console.log('📥 Webhook от ЮKassa:', JSON.stringify(webhookData, null, 2));
    
    // Обрабатываем webhook через сервис
    const result = await yooKassaService.handleWebhook(webhookData);
    
    if (result.event === 'payment.succeeded') {
      // Ищем платеж по ID от ЮKassa
      const payment = query.get(
        'SELECT * FROM payments WHERE provider_payment_id = ?',
        [result.paymentId]
      );
      
      if (payment && payment.payment_status !== 'completed') {
        console.log(`✅ Платеж успешен: payment_id=${payment.id}, order_id=${payment.order_id}`);
        
        // Обновляем статус платежа
        updatePaymentStatus(payment.id, 'completed', {
          provider_response: webhookData
        });
        
        // Обрабатываем успешную оплату (начисление мастеру, комиссии и т.д.)
        processPaymentSuccess(payment.id);
        
        // TODO: Отправить уведомление клиенту и мастеру о успешной оплате
      } else if (!payment) {
        console.warn(`⚠️ Платеж не найден для ЮKassa payment_id: ${result.paymentId}`);
      }
    } else if (result.event === 'payment.canceled') {
      // Ищем и отменяем платеж
      const payment = query.get(
        'SELECT * FROM payments WHERE provider_payment_id = ?',
        [result.paymentId]
      );
      
      if (payment && payment.payment_status !== 'cancelled') {
        updatePaymentStatus(payment.id, 'cancelled', {
          provider_response: webhookData
        });
        console.log(`❌ Платеж отменен: payment_id=${payment.id}`);
      }
    }
    
    // ЮKassa ожидает ответ 200 OK
    res.status(200).json({ received: true });
  } catch (error) {
    console.error('❌ Ошибка обработки webhook от ЮKassa:', error);
    // Все равно возвращаем 200, чтобы ЮKassa не повторял запрос
    res.status(200).json({ received: true, error: error.message });
  }
});

// Webhook от других платежных систем (общий endpoint)
router.post('/webhook/:provider', (req, res) => {
  try {
    const { provider } = req.params;
    console.log(`Webhook от ${provider}:`, req.body);
    
    // Здесь будет логика обработки webhook от других систем
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

