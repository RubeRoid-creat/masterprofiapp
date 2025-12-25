import express from 'express';
import { query } from '../database/db.js';
import { authenticate, authorize } from '../middleware/auth.js';
import { createBackup, listBackups, restoreBackup } from '../services/backup-service.js';
import { notifyMasters } from '../services/assignment-service.js';
import { verifySMSService, checkSMSRuBalance } from '../services/sms-service.js';
import { verifyEmailService } from '../services/email-service.js';
import { getRateLimitStats, unblockIP, resetIPCounter } from '../middleware/rate-limiter.js';

const router = express.Router();

// Все маршруты требуют авторизации и роли admin
router.use(authenticate);
router.use(authorize('admin'));

// ============= Статистика =============

// Общая статистика для дашборда
router.get('/stats', (req, res) => {
  try {
    // Статистика заказов
    const ordersStats = {
      total: query.get('SELECT COUNT(*) as count FROM orders').count,
      new: query.get('SELECT COUNT(*) as count FROM orders WHERE repair_status = ?', ['new']).count,
      inProgress: query.get('SELECT COUNT(*) as count FROM orders WHERE repair_status = ?', ['in_progress']).count,
      completed: query.get('SELECT COUNT(*) as count FROM orders WHERE repair_status = ?', ['completed']).count,
      cancelled: query.get('SELECT COUNT(*) as count FROM orders WHERE repair_status = ?', ['cancelled']).count,
      today: query.get(`
        SELECT COUNT(*) as count FROM orders 
        WHERE DATE(created_at) = DATE('now')
      `).count,
      thisWeek: query.get(`
        SELECT COUNT(*) as count FROM orders 
        WHERE created_at >= datetime('now', '-7 days')
      `).count,
      thisMonth: query.get(`
        SELECT COUNT(*) as count FROM orders 
        WHERE created_at >= datetime('now', '-30 days')
      `).count
    };
    
    // Статистика мастеров
    const mastersStats = {
      total: query.get('SELECT COUNT(*) as count FROM masters').count,
      verified: query.get('SELECT COUNT(*) as count FROM masters WHERE verification_status = ?', ['verified']).count,
      pending: query.get('SELECT COUNT(*) as count FROM masters WHERE verification_status = ?', ['pending']).count,
      onShift: query.get('SELECT COUNT(*) as count FROM masters WHERE is_on_shift = 1').count,
      available: query.get('SELECT COUNT(*) as count FROM masters WHERE status = ?', ['available']).count
    };
    
    // Статистика клиентов
    const clientsStats = {
      total: query.get('SELECT COUNT(*) as count FROM clients').count,
      active: query.get(`
        SELECT COUNT(DISTINCT client_id) as count 
        FROM orders 
        WHERE created_at >= datetime('now', '-30 days')
      `).count
    };
    
    // Статистика доходов платформы
    const revenueStats = {
      total: query.get(`
        SELECT COALESCE(SUM(commission_amount), 0) as total 
        FROM master_transactions 
        WHERE transaction_type = 'commission' AND status = 'completed'
      `).total || 0,
      thisMonth: query.get(`
        SELECT COALESCE(SUM(commission_amount), 0) as total 
        FROM master_transactions 
        WHERE transaction_type = 'commission' 
        AND status = 'completed'
        AND created_at >= datetime('now', '-30 days')
      `).total || 0,
      today: query.get(`
        SELECT COALESCE(SUM(commission_amount), 0) as total 
        FROM master_transactions 
        WHERE transaction_type = 'commission' 
        AND status = 'completed'
        AND DATE(created_at) = DATE('now')
      `).total || 0
    };
    
    // Статистика жалоб
    const complaintsStats = {
      total: query.get('SELECT COUNT(*) as count FROM complaints').count,
      pending: query.get('SELECT COUNT(*) as count FROM complaints WHERE status = ?', ['pending']).count,
      resolved: query.get('SELECT COUNT(*) as count FROM complaints WHERE status = ?', ['resolved']).count
    };
    
    // Статистика документов верификации
    const verificationStats = {
      pending: query.get('SELECT COUNT(*) as count FROM master_verification_documents WHERE status = ?', ['pending']).count,
      approved: query.get('SELECT COUNT(*) as count FROM master_verification_documents WHERE status = ?', ['approved']).count,
      rejected: query.get('SELECT COUNT(*) as count FROM master_verification_documents WHERE status = ?', ['rejected']).count
    };
    
    res.json({
      orders: ordersStats,
      masters: mastersStats,
      clients: clientsStats,
      revenue: revenueStats,
      complaints: complaintsStats,
      verification: verificationStats,
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    console.error('Ошибка получения статистики:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Статистика по заказам с детализацией
router.get('/stats/orders', (req, res) => {
  try {
    const { period = 'all' } = req.query; // 'day', 'week', 'month', 'all'
    
    let dateFilter = '';
    if (period === 'day') {
      dateFilter = "WHERE DATE(created_at) = DATE('now')";
    } else if (period === 'week') {
      dateFilter = "WHERE created_at >= datetime('now', '-7 days')";
    } else if (period === 'month') {
      dateFilter = "WHERE created_at >= datetime('now', '-30 days')";
    }
    
    const stats = query.all(`
      SELECT 
        DATE(created_at) as date,
        COUNT(*) as count,
        SUM(CASE WHEN repair_status = 'completed' THEN 1 ELSE 0 END) as completed,
        SUM(CASE WHEN repair_status = 'cancelled' THEN 1 ELSE 0 END) as cancelled,
        AVG(estimated_cost) as avg_estimated_cost,
        AVG(final_cost) as avg_final_cost
      FROM orders
      ${dateFilter}
      GROUP BY DATE(created_at)
      ORDER BY date DESC
      LIMIT 30
    `);
    
    res.json(stats);
  } catch (error) {
    console.error('Ошибка получения статистики заказов:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// ============= Управление заказами =============

// Ручное назначение заказа мастеру
router.post('/orders/:orderId/assign', (req, res) => {
  try {
    const { orderId } = req.params;
    const { masterId } = req.body;
    
    if (!masterId) {
      return res.status(400).json({ error: 'Необходимо указать masterId' });
    }
    
    // Проверяем заказ
    const order = query.get('SELECT * FROM orders WHERE id = ?', [orderId]);
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    if (order.repair_status !== 'new') {
      return res.status(400).json({ error: 'Заказ уже обработан' });
    }
    
    // Проверяем мастера
    const master = query.get('SELECT * FROM masters WHERE id = ?', [masterId]);
    if (!master) {
      return res.status(404).json({ error: 'Мастер не найден' });
    }
    
    // Назначаем заказ мастеру
    query.run(
      'UPDATE orders SET repair_status = ?, assigned_master_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      ['in_progress', masterId, orderId]
    );
    
    // Записываем в историю
    query.run(
      'INSERT INTO order_status_history (order_id, old_status, new_status, changed_by, note) VALUES (?, ?, ?, ?, ?)',
      [orderId, 'new', 'in_progress', req.user.id, `Заказ назначен вручную администратором`]
    );
    
    res.json({ message: 'Заказ успешно назначен мастеру' });
  } catch (error) {
    console.error('Ошибка назначения заказа:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Отменить заказ
router.post('/orders/:orderId/cancel', (req, res) => {
  try {
    const { orderId } = req.params;
    const { reason } = req.body;
    
    const order = query.get('SELECT * FROM orders WHERE id = ?', [orderId]);
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    if (order.repair_status === 'cancelled') {
      return res.status(400).json({ error: 'Заказ уже отменен' });
    }
    
    const oldStatus = order.repair_status;
    
    // Отменяем заказ
    query.run(
      'UPDATE orders SET repair_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      ['cancelled', orderId]
    );
    
    // Записываем в историю
    query.run(
      'INSERT INTO order_status_history (order_id, old_status, new_status, changed_by, note) VALUES (?, ?, ?, ?, ?)',
      [orderId, oldStatus, 'cancelled', req.user.id, reason || 'Заказ отменен администратором']
    );
    
    res.json({ message: 'Заказ отменен' });
  } catch (error) {
    console.error('Ошибка отмены заказа:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// ============= Управление пользователями =============

// Блокировать/разблокировать пользователя
router.post('/users/:userId/block', (req, res) => {
  try {
    const { userId } = req.params;
    const { blocked, reason } = req.body;
    
    // Проверяем, есть ли поле is_blocked в таблице users
    const tableInfo = query.all("PRAGMA table_info(users)");
    const hasIsBlocked = tableInfo.some(col => col.name === 'is_blocked');
    
    if (!hasIsBlocked) {
      // Добавляем поле, если его нет
      query.run('ALTER TABLE users ADD COLUMN is_blocked INTEGER DEFAULT 0');
      query.run('ALTER TABLE users ADD COLUMN block_reason TEXT');
      query.run('ALTER TABLE users ADD COLUMN blocked_at DATETIME');
      query.run('ALTER TABLE users ADD COLUMN blocked_by INTEGER');
    }
    
    // Обновляем статус блокировки
    if (blocked) {
      query.run(
        'UPDATE users SET is_blocked = 1, block_reason = ?, blocked_at = CURRENT_TIMESTAMP, blocked_by = ? WHERE id = ?',
        [reason || 'Заблокирован администратором', req.user.id, userId]
      );
      
      // Если это мастер, обновляем его статус
      const master = query.get('SELECT id FROM masters WHERE user_id = ?', [userId]);
      if (master) {
        query.run('UPDATE masters SET status = ? WHERE id = ?', ['offline', master.id]);
      }
    } else {
      query.run(
        'UPDATE users SET is_blocked = 0, block_reason = NULL, blocked_at = NULL, blocked_by = NULL WHERE id = ?',
        [userId]
      );
    }
    
    res.json({ 
      message: blocked ? 'Пользователь заблокирован' : 'Пользователь разблокирован' 
    });
  } catch (error) {
    console.error('Ошибка блокировки пользователя:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Получить список всех пользователей
router.get('/users', (req, res) => {
  try {
    const { role, blocked } = req.query;
    
    let sql = `
      SELECT 
        u.*,
        CASE WHEN m.id IS NOT NULL THEN m.id ELSE NULL END as master_id,
        CASE WHEN c.id IS NOT NULL THEN c.id ELSE NULL END as client_id,
        m.verification_status,
        m.rating,
        m.completed_orders
      FROM users u
      LEFT JOIN masters m ON u.id = m.user_id
      LEFT JOIN clients c ON u.id = c.user_id
      WHERE 1=1
    `;
    const params = [];
    
    if (role) {
      sql += ' AND u.role = ?';
      params.push(role);
    }
    
    if (blocked !== undefined) {
      const tableInfo = query.all("PRAGMA table_info(users)");
      const hasIsBlocked = tableInfo.some(col => col.name === 'is_blocked');
      if (hasIsBlocked) {
        sql += ' AND u.is_blocked = ?';
        params.push(blocked === 'true' ? 1 : 0);
      }
    }
    
    sql += ' ORDER BY u.created_at DESC';
    
    const users = query.all(sql, params);
    
    res.json(users);
  } catch (error) {
    console.error('Ошибка получения пользователей:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Удалить аккаунт мастера
router.delete('/masters/:masterId', (req, res) => {
  try {
    const { masterId } = req.params;
    
    // Проверяем существование мастера
    const master = query.get('SELECT id, user_id FROM masters WHERE id = ?', [masterId]);
    if (!master) {
      return res.status(404).json({ error: 'Мастер не найден' });
    }
    
    // Проверяем, нет ли активных заказов у мастера
    const activeOrders = query.all(`
      SELECT COUNT(*) as count 
      FROM orders 
      WHERE assigned_master_id = ? 
        AND repair_status IN ('new', 'in_progress', 'diagnostics', 'waiting_parts')
    `, [masterId]);
    
    if (activeOrders.length > 0 && activeOrders[0].count > 0) {
      return res.status(400).json({ 
        error: 'Невозможно удалить мастера с активными заказами',
        activeOrdersCount: activeOrders[0].count
      });
    }
    
    // Получаем информацию о мастере для логирования
    const masterInfo = query.get(`
      SELECT m.*, u.name, u.email 
      FROM masters m 
      JOIN users u ON m.user_id = u.id 
      WHERE m.id = ?
    `, [masterId]);
    
    // Удаляем мастера (CASCADE удалит связанные записи)
    // Сначала удаляем мастера, потом пользователя
    // Это безопаснее, так как многие таблицы ссылаются на masters.id
    
    // Удаляем назначения мастера
    query.run('DELETE FROM order_assignments WHERE master_id = ?', [masterId]);
    
    // Обнуляем назначения в заказах
    query.run('UPDATE orders SET assigned_master_id = NULL WHERE assigned_master_id = ?', [masterId]);
    query.run('UPDATE orders SET preferred_master_id = NULL WHERE preferred_master_id = ?', [masterId]);
    
    // Удаляем мастера
    query.run('DELETE FROM masters WHERE id = ?', [masterId]);
    
    // Удаляем пользователя (CASCADE удалит связанные записи)
    query.run('DELETE FROM users WHERE id = ?', [master.user_id]);
    
    console.log(`🗑️ Администратор ${req.user.id} удалил мастера #${masterId} (${masterInfo.name}, ${masterInfo.email})`);
    
    res.json({ 
      message: 'Аккаунт мастера успешно удален',
      deletedMaster: {
        id: masterId,
        name: masterInfo.name,
        email: masterInfo.email
      }
    });
  } catch (error) {
    console.error('Ошибка удаления мастера:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// ============= Резервное копирование =============

// Создать бэкап
router.post('/backup/create', (req, res) => {
  try {
    const backup = createBackup();
    res.json({
      message: 'Бэкап успешно создан',
      backup: backup
    });
  } catch (error) {
    console.error('Ошибка создания бэкапа:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Получить список бэкапов
router.get('/backup/list', (req, res) => {
  try {
    const backups = listBackups();
    res.json(backups);
  } catch (error) {
    console.error('Ошибка получения списка бэкапов:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Восстановить из бэкапа
router.post('/backup/restore', (req, res) => {
  try {
    const { fileName } = req.body;
    
    if (!fileName) {
      return res.status(400).json({ error: 'Необходимо указать fileName' });
    }
    
    const result = restoreBackup(fileName);
    res.json({
      message: 'База данных успешно восстановлена',
      result: result
    });
  } catch (error) {
    console.error('Ошибка восстановления бэкапа:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// ============= Проверка сервисов =============

// Проверка SMS сервиса
router.get('/services/sms/status', async (req, res) => {
  try {
    const status = await verifySMSService();
    res.json(status);
  } catch (error) {
    console.error('Ошибка проверки SMS сервиса:', error);
    res.status(500).json({ 
      success: false,
      error: 'Ошибка сервера', 
      details: error.message 
    });
  }
});

// Проверка баланса SMS.ru
router.get('/services/sms/balance', async (req, res) => {
  try {
    const balance = await checkSMSRuBalance();
    res.json(balance);
  } catch (error) {
    console.error('Ошибка проверки баланса SMS:', error);
    res.status(500).json({ 
      success: false,
      error: 'Ошибка сервера', 
      details: error.message 
    });
  }
});

// Проверка Email сервиса
router.get('/services/email/status', async (req, res) => {
  try {
    const status = await verifyEmailService();
    res.json(status);
  } catch (error) {
    console.error('Ошибка проверки Email сервиса:', error);
    res.status(500).json({ 
      success: false,
      error: 'Ошибка сервера', 
      details: error.message 
    });
  }
});

// Проверка всех сервисов сразу
router.get('/services/health', async (req, res) => {
  try {
    const [smsStatus, emailStatus] = await Promise.all([
      verifySMSService().catch(e => ({ success: false, error: e.message })),
      verifyEmailService().catch(e => ({ success: false, error: e.message }))
    ]);
    
    let smsBalance = null;
    if (smsStatus.success && smsStatus.provider === 'smsru') {
      try {
        smsBalance = await checkSMSRuBalance();
      } catch (e) {
        console.warn('Не удалось получить баланс SMS:', e.message);
      }
    }
    
    res.json({
      services: {
        sms: {
          ...smsStatus,
          balance: smsBalance
        },
        email: emailStatus
      },
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    console.error('Ошибка проверки сервисов:', error);
    res.status(500).json({ 
      error: 'Ошибка сервера', 
      details: error.message 
    });
  }
});

// ============= WebSocket мониторинг =============
router.get('/websocket/stats', async (req, res) => {
  try {
    const { getMasterSubscriptionsStats, getConnectedClientsCount } = await import('../websocket.js');
    
    const stats = getMasterSubscriptionsStats();
    const totalConnected = getConnectedClientsCount();
    
    res.json({
      ...stats,
      totalConnected
    });
  } catch (error) {
    console.error('Ошибка получения статистики WebSocket:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// ============= Rate Limiting управление =============

// Получить статистику rate limiting
router.get('/rate-limit/stats', (req, res) => {
  try {
    const stats = getRateLimitStats();
    res.json(stats);
  } catch (error) {
    console.error('Ошибка получения статистики rate limiting:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Разблокировать IP адрес
router.post('/rate-limit/unblock', (req, res) => {
  try {
    const { ip } = req.body;
    
    if (!ip) {
      return res.status(400).json({ error: 'Необходимо указать IP адрес' });
    }
    
    const unblocked = unblockIP(ip);
    
    if (unblocked) {
      res.json({ 
        message: `IP ${ip} успешно разблокирован`,
        ip: ip
      });
    } else {
      res.status(404).json({ 
        error: `IP ${ip} не найден в списке заблокированных` 
      });
    }
  } catch (error) {
    console.error('Ошибка разблокировки IP:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Сбросить счетчик запросов для IP
router.post('/rate-limit/reset', (req, res) => {
  try {
    const { ip } = req.body;
    
    if (!ip) {
      return res.status(400).json({ error: 'Необходимо указать IP адрес' });
    }
    
    resetIPCounter(ip);
    
    res.json({ 
      message: `Счетчик запросов для IP ${ip} сброшен`,
      ip: ip
    });
  } catch (error) {
    console.error('Ошибка сброса счетчика IP:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

export default router;

