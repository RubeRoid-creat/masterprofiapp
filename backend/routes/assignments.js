import express from 'express';
import { query } from '../database/db.js';
import { authenticate, authorize } from '../middleware/auth.js';
import { broadcastToMaster, broadcastToClient } from '../websocket.js';
import { notifyMasterAssigned, notifyOrderStatusChange } from '../services/push-notification-service.js';
import { createAssignment } from '../services/assignment-service.js';

const router = express.Router();

// Получить историю отклоненных заказов мастера
router.get('/rejected', authenticate, authorize('master'), (req, res) => {
  try {
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const assignments = query.all(`
      SELECT 
        oa.*,
        o.id as order_id,
        o.device_type,
        o.device_brand,
        o.device_model,
        o.problem_description,
        o.client_address,
        o.latitude,
        o.longitude,
        o.estimated_cost,
        o.urgency,
        o.repair_status,
        o.created_at as order_created_at,
        u.name as client_name,
        u.phone as client_phone,
        c.user_id as client_user_id
      FROM order_assignments oa
      JOIN orders o ON oa.order_id = o.id
      JOIN clients c ON o.client_id = c.id
      JOIN users u ON c.user_id = u.id
      WHERE oa.master_id = ? AND oa.status = 'rejected'
      ORDER BY oa.responded_at DESC
      LIMIT 50
    `, [master.id]);
    
    const result = assignments.map(assignment => ({
      id: assignment.id,
      orderId: assignment.order_id,
      status: assignment.status,
      rejectedAt: assignment.responded_at,
      rejectionReason: assignment.rejection_reason,
      order: {
        id: assignment.order_id,
        deviceType: assignment.device_type,
        deviceBrand: assignment.device_brand,
        deviceModel: assignment.device_model,
        problemDescription: assignment.problem_description,
        clientAddress: assignment.client_address,
        latitude: assignment.latitude,
        longitude: assignment.longitude,
        estimatedCost: assignment.estimated_cost,
        urgency: assignment.urgency,
        createdAt: assignment.order_created_at,
        repairStatus: assignment.repair_status,
        client: {
          name: assignment.client_name,
          phone: assignment.client_phone
        }
      }
    }));
    
    res.json(result);
  } catch (error) {
    console.error('Ошибка получения истории отклонений:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить назначения для мастера
router.get('/my', authenticate, authorize('master'), (req, res) => {
  try {
    const { status } = req.query;
    
    // Получаем ID мастера
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    let sql = `
      SELECT 
        oa.*,
        o.device_type, o.device_brand, o.device_model,
        o.problem_description, o.address, o.latitude, o.longitude,
        o.arrival_time, o.order_type, o.estimated_cost,
        u.name as client_name, u.phone as client_phone
      FROM order_assignments oa
      JOIN orders o ON oa.order_id = o.id
      JOIN clients c ON o.client_id = c.id
      JOIN users u ON c.user_id = u.id
      WHERE oa.master_id = ?
    `;
    const params = [master.id];
    
    if (status) {
      sql += ' AND oa.status = ?';
      params.push(status);
    }
    
    sql += ' ORDER BY oa.created_at DESC';
    
    const assignments = query.all(sql, params);
    res.json(assignments);
  } catch (error) {
    console.error('Ошибка получения назначений:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить активное (pending) назначение для заказа
// Если назначения нет, но мастер на смене и заказ новый - создаем назначение автоматически
router.get('/order/:orderId/active', authenticate, (req, res) => {
  try {
    const { orderId } = req.params;
    
    // Проверяем существующее назначение
    let assignment = query.get(`
      SELECT 
        oa.*,
        m.user_id as master_user_id,
        u.name as master_name
      FROM order_assignments oa
      JOIN masters m ON oa.master_id = m.id
      JOIN users u ON m.user_id = u.id
      WHERE oa.order_id = ? AND oa.status = ?
      ORDER BY oa.created_at DESC
      LIMIT 1
    `, [orderId, 'pending']);
    
    // Если назначения нет, и мастер на смене - создаем его автоматически
    if (!assignment && req.user.role === 'master') {
      console.log(`[GET /api/assignments/order/${orderId}/active] Назначение не найдено, проверяем возможность создания`);
      
      // Получаем информацию о мастере
      const master = query.get('SELECT id, is_on_shift, status FROM masters WHERE user_id = ?', [req.user.id]);
      
      if (master && master.is_on_shift === 1 && master.status === 'available') {
        console.log(`[GET /api/assignments/order/${orderId}/active] Мастер на смене, проверяем заказ`);
        
        // Проверяем заказ
        const order = query.get('SELECT repair_status, device_type FROM orders WHERE id = ?', [orderId]);
        
        if (order && order.repair_status === 'new') {
          console.log(`[GET /api/assignments/order/${orderId}/active] Заказ новый, создаем назначение для мастера #${master.id}`);
          
          // Проверяем, нет ли уже назначения для этого мастера (даже не pending)
          const existingAssignment = query.get(
            'SELECT * FROM order_assignments WHERE order_id = ? AND master_id = ?',
            [orderId, master.id]
          );
          
          if (!existingAssignment) {
            // Создаем назначение
            const newAssignment = createAssignment(orderId, master.id);
            
            if (newAssignment) {
              assignment = query.get(`
                SELECT 
                  oa.*,
                  m.user_id as master_user_id,
                  u.name as master_name
                FROM order_assignments oa
                JOIN masters m ON oa.master_id = m.id
                JOIN users u ON m.user_id = u.id
                WHERE oa.id = ?
              `, [newAssignment.id]);
              
              console.log(`[GET /api/assignments/order/${orderId}/active] Назначение создано: #${newAssignment.id}`);
            } else {
              console.log(`[GET /api/assignments/order/${orderId}/active] Не удалось создать назначение`);
            }
          } else {
            console.log(`[GET /api/assignments/order/${orderId}/active] Назначение уже существует для этого мастера, но не pending`);
          }
        } else {
          console.log(`[GET /api/assignments/order/${orderId}/active] Заказ не новый (status: ${order?.repair_status})`);
        }
      } else {
        console.log(`[GET /api/assignments/order/${orderId}/active] Мастер не на смене или недоступен`);
      }
    }
    
    if (!assignment) {
      return res.status(404).json({ error: 'Активное назначение не найдено' });
    }
    
    res.json(assignment);
  } catch (error) {
    console.error('Ошибка получения назначения:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Принять несколько назначений одновременно (батчинг)
router.post('/batch/accept', authenticate, authorize('master'), async (req, res) => {
  try {
    const { assignmentIds } = req.body;
    
    if (!Array.isArray(assignmentIds) || assignmentIds.length === 0) {
      return res.status(400).json({ error: 'Необходимо указать массив assignmentIds' });
    }
    
    if (assignmentIds.length > 5) {
      return res.status(400).json({ error: 'Можно принять максимум 5 заказов одновременно' });
    }
    
    // Получаем ID мастера
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const results = [];
    const errors = [];
    
    for (const assignmentId of assignmentIds) {
      try {
        const assignment = query.get(`
          SELECT oa.*, o.repair_status 
          FROM order_assignments oa
          JOIN orders o ON oa.order_id = o.id
          WHERE oa.id = ? AND oa.master_id = ? AND oa.status = ?
        `, [assignmentId, master.id, 'pending']);
        
        if (!assignment) {
          errors.push({ assignmentId, error: 'Назначение не найдено или уже обработано' });
          continue;
        }
        
        // Проверяем, не истекло ли время
        const expiresAt = new Date(assignment.expires_at);
        if (expiresAt < new Date()) {
          errors.push({ assignmentId, error: 'Время на ответ истекло' });
          continue;
        }
        
        // Проверяем, что заказ еще новый
        if (assignment.repair_status !== 'new') {
          errors.push({ assignmentId, error: 'Заказ уже обработан' });
          continue;
        }
        
        // Принимаем назначение
        const oldStatus = assignment.repair_status || 'new';
        const masterName = req.user.name || 'Мастер';
        
        // Обновляем назначение
        query.run(
          'UPDATE order_assignments SET status = ?, responded_at = CURRENT_TIMESTAMP WHERE id = ?',
          ['accepted', assignmentId]
        );
        
        // Обновляем статус заказа
        query.run(
          'UPDATE orders SET repair_status = ?, assigned_master_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
          ['in_progress', master.id, assignment.order_id]
        );
        
        // Записываем в историю
        query.run(
          'INSERT INTO order_status_history (order_id, old_status, new_status, changed_by, note) VALUES (?, ?, ?, ?, ?)',
          [assignment.order_id, oldStatus, 'in_progress', req.user.id, `Заказ принят мастером ${masterName}`]
        );
        
        // Отклоняем все остальные pending-назначения для этого заказа
        query.run(
          'UPDATE order_assignments SET status = ? WHERE order_id = ? AND id != ? AND status = ?',
          ['expired', assignment.order_id, assignmentId, 'pending']
        );
        
        // Отправляем уведомления клиенту
        const order = query.get(`
          SELECT o.*, c.user_id as client_user_id 
          FROM orders o 
          JOIN clients c ON o.client_id = c.id 
          WHERE o.id = ?
        `, [assignment.order_id]);
        
        if (order && order.client_user_id) {
          broadcastToClient(order.client_user_id, {
            type: 'order_status_changed',
            orderId: assignment.order_id,
            status: 'in_progress',
            message: 'Мастер принял ваш заказ',
            masterName: masterName
          });
          
          await notifyMasterAssigned(
            order.client_user_id,
            assignment.order_id,
            masterName
          );
          
          await notifyOrderStatusChange(
            order.client_user_id,
            assignment.order_id,
            'in_progress',
            'Мастер принял ваш заказ',
            masterName
          );
        }
        
        results.push({ assignmentId, orderId: assignment.order_id, success: true });
      } catch (error) {
        console.error(`Ошибка принятия назначения ${assignmentId}:`, error);
        errors.push({ assignmentId, error: error.message });
      }
    }
    
    // Обновляем статус мастера (если принял хотя бы один заказ)
    if (results.length > 0) {
      query.run(
        'UPDATE masters SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
        ['busy', master.id]
      );
    }
    
    res.json({
      message: `Принято заказов: ${results.length}, ошибок: ${errors.length}`,
      accepted: results,
      errors: errors
    });
  } catch (error) {
    console.error('Ошибка батчинга назначений:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Принять заказ
router.post('/:id/accept', authenticate, authorize('master'), async (req, res) => {
  try {
    const { id } = req.params;
    const assignmentId = parseInt(id);
    
    if (isNaN(assignmentId)) {
      console.log(`❌ Неверный формат assignmentId: ${id}`);
      return res.status(400).json({ error: 'Неверный формат ID назначения' });
    }
    
    console.log(`📥 Принятие заказа: assignmentId=${assignmentId}, userId=${req.user.id}`);
    
    // Получаем ID мастера
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      console.log(`❌ Профиль мастера не найден для userId=${req.user.id}`);
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    console.log(`✅ Мастер найден: masterId=${master.id}`);
    
    // Получаем назначение
    const assignment = query.get(
      'SELECT * FROM order_assignments WHERE id = ? AND master_id = ?',
      [assignmentId, master.id]
    );
    
    if (!assignment) {
      // Проверяем, существует ли назначение вообще
      const anyAssignment = query.get(
        'SELECT * FROM order_assignments WHERE id = ?',
        [assignmentId]
      );
      
      if (!anyAssignment) {
        console.log(`❌ Назначение с id=${assignmentId} не существует`);
        return res.status(404).json({ error: 'Назначение не найдено' });
      } else {
        console.log(`❌ Назначение id=${assignmentId} принадлежит другому мастеру (masterId=${anyAssignment.master_id}, текущий=${master.id})`);
        return res.status(403).json({ error: 'Назначение не принадлежит вам' });
      }
    }
    
    console.log(`✅ Назначение найдено: status=${assignment.status}, expires_at=${assignment.expires_at}`);
    
    if (assignment.status !== 'pending') {
      console.log(`⚠️ Назначение уже обработано: status=${assignment.status}`);
      return res.status(400).json({ 
        error: 'Это назначение уже обработано',
        currentStatus: assignment.status
      });
    }
    
    // Проверяем, не истекло ли время
    const now = new Date();
    const expiresAt = new Date(assignment.expires_at);
    console.log(`⏰ Проверка времени: now=${now.toISOString()}, expires=${expiresAt.toISOString()}`);
    
    if (now > expiresAt) {
      console.log(`⏰ Время истекло`);
      query.run(
        'UPDATE order_assignments SET status = ? WHERE id = ?',
        ['expired', assignmentId]
      );
      return res.status(400).json({ 
        error: 'Время на ответ истекло',
        expiredAt: assignment.expires_at
      });
    }
    
    // Получаем текущий статус заказа для истории
    const currentOrder = query.get('SELECT repair_status FROM orders WHERE id = ?', [assignment.order_id]);
    const oldStatus = currentOrder?.repair_status || 'new';
    
    console.log(`📝 Обновление назначения и заказа: orderId=${assignment.order_id}, oldStatus=${oldStatus}`);
    
    // Обновляем назначение
    query.run(
      'UPDATE order_assignments SET status = ?, responded_at = CURRENT_TIMESTAMP WHERE id = ?',
      ['accepted', assignmentId]
    );
    console.log(`✅ Назначение обновлено: id=${assignmentId}, status=accepted`);
    
    // Обновляем статус заказа
    query.run(
      'UPDATE orders SET repair_status = ?, assigned_master_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      ['in_progress', master.id, assignment.order_id]
    );
    console.log(`✅ Заказ обновлен: orderId=${assignment.order_id}, status=in_progress`);
    
    // Обновляем статус мастера
    query.run(
      'UPDATE masters SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      ['busy', master.id]
    );
    console.log(`✅ Мастер обновлен: masterId=${master.id}, status=busy`);
    
    // Записываем в историю
    const masterName = req.user.name || 'Мастер';
    query.run(
      'INSERT INTO order_status_history (order_id, old_status, new_status, changed_by, note) VALUES (?, ?, ?, ?, ?)',
      [assignment.order_id, oldStatus, 'in_progress', req.user.id, `Заказ принят мастером ${masterName}`]
    );
    console.log(`✅ История обновлена`);
    
    // Отклоняем все остальные pending-назначения для этого заказа
    query.run(
      'UPDATE order_assignments SET status = ? WHERE order_id = ? AND id != ? AND status = ?',
      ['expired', assignment.order_id, assignmentId, 'pending']
    );
    
    // Отправляем уведомление клиенту через WebSocket
    const order = query.get(`
      SELECT o.*, c.user_id as client_user_id 
      FROM orders o 
      JOIN clients c ON o.client_id = c.id 
      WHERE o.id = ?
    `, [assignment.order_id]);
    
    if (order && order.client_user_id) {
      // WebSocket уведомление
      broadcastToClient(order.client_user_id, {
        type: 'order_status_changed',
        orderId: assignment.order_id,
        status: 'in_progress',
        message: 'Мастер принял ваш заказ',
        masterName: masterName
      });
      
      // Push-уведомление
      await notifyMasterAssigned(
        order.client_user_id,
        assignment.order_id,
        masterName
      );
      
      // Также отправляем уведомление об изменении статуса
      await notifyOrderStatusChange(
        order.client_user_id,
        assignment.order_id,
        'in_progress',
        'Мастер принял ваш заказ',
        masterName
      );
      
      console.log(`📨 Уведомления отправлены клиенту #${order.client_user_id}`);
    }
    
    console.log(`✅ Заказ успешно принят: assignmentId=${assignmentId}, orderId=${assignment.order_id}`);
    res.json({ 
      message: 'Заказ успешно принят',
      assignmentId: assignmentId,
      orderId: assignment.order_id
    });
  } catch (error) {
    console.error('Ошибка принятия заказа:', error);
    console.error('Детали ошибки:', {
      message: error.message,
      stack: error.stack,
      assignmentId: assignmentId,
      userId: req.user?.id
    });
    res.status(500).json({ 
      error: 'Ошибка сервера',
      details: process.env.NODE_ENV === 'development' ? error.message : undefined
    });
  }
});

// Отклонить заказ
router.post('/:id/reject', authenticate, authorize('master'), (req, res) => {
  try {
    const { id } = req.params;
    const { reason } = req.body;
    
    // Получаем ID мастера
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    // Получаем назначение
    const assignment = query.get(
      'SELECT * FROM order_assignments WHERE id = ? AND master_id = ?',
      [id, master.id]
    );
    
    if (!assignment) {
      return res.status(404).json({ error: 'Назначение не найдено' });
    }
    
    if (assignment.status !== 'pending') {
      return res.status(400).json({ error: 'Это назначение уже обработано' });
    }
    
    // Обновляем назначение
    query.run(
      'UPDATE order_assignments SET status = ?, responded_at = CURRENT_TIMESTAMP, rejection_reason = ? WHERE id = ?',
      ['rejected', reason || 'Не указана', id]
    );
    
    // Обновляем статус мастера (если не занят другими заказами)
    const activeMasterOrders = query.get(
      'SELECT COUNT(*) as count FROM orders WHERE assigned_master_id = ? AND repair_status = ?',
      [master.id, 'in_progress']
    );
    
    if (activeMasterOrders.count === 0) {
      query.run(
        'UPDATE masters SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
        ['available', master.id]
      );
    }
    
    res.json({ message: 'Заказ отклонен' });
    
    // Асинхронно ищем следующего мастера
    setImmediate(() => {
      const { findNextMaster } = require('../services/assignment-service.js');
      findNextMaster(assignment.order_id);
    });
  } catch (error) {
    console.error('Ошибка отклонения заказа:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить историю назначений для заказа
router.get('/order/:orderId/history', authenticate, (req, res) => {
  try {
    const { orderId } = req.params;
    
    const assignments = query.all(`
      SELECT 
        oa.*,
        u.name as master_name, u.phone as master_phone
      FROM order_assignments oa
      JOIN masters m ON oa.master_id = m.id
      JOIN users u ON m.user_id = u.id
      WHERE oa.order_id = ?
      ORDER BY oa.created_at DESC
    `, [orderId]);
    
    res.json(assignments);
  } catch (error) {
    console.error('Ошибка получения истории назначений:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

export default router;


