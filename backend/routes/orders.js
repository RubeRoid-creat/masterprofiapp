import express from 'express';
import { query } from '../database/db.js';
import { authenticate, authorize } from '../middleware/auth.js';
import { notifyMasters } from '../services/assignment-service.js';
import { fixOrderPrice, isPriceFixed, generateReceiptData } from '../services/price-fixing-service.js';
import { getCoordinatesForAddress } from '../services/geocoding-service.js';
import { broadcastToClient } from '../websocket.js';
import { notifyOrderStatusChange, notifyMasterAssigned } from '../services/push-notification-service.js';
import { upload, handleUploadError } from '../middleware/upload.js';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';
import { existsSync } from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const router = express.Router();

// Функция для вычисления расстояния между двумя точками (формула гаверсинуса)
function calculateDistance(lat1, lon1, lat2, lon2) {
  const R = 6371000; // Радиус Земли в метрах
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c; // Расстояние в метрах
}

function toRad(degrees) {
  return degrees * (Math.PI / 180);
}

// Генерация уникального номера заявки в формате #XXXX-КЛ
function generateOrderNumber() {
  const year = new Date().getFullYear().toString().slice(-2);
  const lastOrder = query.get('SELECT id FROM orders ORDER BY id DESC LIMIT 1');
  const nextId = lastOrder ? lastOrder.id + 1 : 1;
  const paddedId = nextId.toString().padStart(4, '0');
  return `#${paddedId}-КЛ`;
}

// Функция для проверки гарантийного случая
function checkWarrantyCase(order) {
  try {
    // Проверяем, был ли предыдущий заказ завершен недавно (в пределах гарантийного срока)
    const warrantyPeriodDays = 90; // 90 дней гарантии
    const warrantyDate = new Date();
    warrantyDate.setDate(warrantyDate.getDate() - warrantyPeriodDays);
    
    // Проверяем, был ли этот заказ завершен недавно
    const orderCompletedDate = new Date(order.updated_at || order.created_at);
    const isRecentlyCompleted = orderCompletedDate >= warrantyDate && 
                                (order.repair_status === 'completed' || order.request_status === 'completed');
    
    if (!isRecentlyCompleted) {
      return false;
    }
    
    // Проверяем, есть ли связанный заказ
    if (order.related_order_id) {
      const relatedOrder = query.get('SELECT * FROM orders WHERE id = ?', [order.related_order_id]);
      if (relatedOrder) {
        const relatedCompletedDate = new Date(relatedOrder.updated_at || relatedOrder.created_at);
        if (relatedCompletedDate >= warrantyDate && 
            (relatedOrder.repair_status === 'completed' || relatedOrder.request_status === 'completed')) {
          return true;
        }
      }
    }
    
    // Проверяем, был ли предыдущий заказ на ту же технику завершен недавно
    if (order.device_serial_number) {
      const recentOrder = query.get(`
        SELECT * FROM orders 
        WHERE device_serial_number = ? 
          AND id != ?
          AND (repair_status = 'completed' OR request_status = 'completed')
          AND updated_at >= ?
        ORDER BY updated_at DESC
        LIMIT 1
      `, [order.device_serial_number, order.id, warrantyDate.toISOString()]);
      
      if (recentOrder) {
        return true;
      }
    }
    
    // Если заказ завершен недавно и это повторный заказ, считаем гарантийным
    return isRecentlyCompleted;
  } catch (error) {
    console.error('Ошибка проверки гарантийного случая:', error);
    return false;
  }
}

// Получить все заказы (для мастеров - доступные заказы)
router.get('/', authenticate, (req, res) => {
  console.log('\n========================================');
  console.log('[GET /api/orders] Request received');
  console.log(`[GET /api/orders] Time: ${new Date().toISOString()}`);
  
  try {
    const { 
      status, 
      deviceType, 
      orderType, 
      urgency, 
      maxDistance, 
      minPrice, 
      maxPrice,
      sortBy, // 'distance', 'price', 'urgency', 'created_at'
      limit,
      offset
    } = req.query;
    
    // Получаем координаты мастера из query или из БД
    let masterLatitude = req.query.masterLatitude ? parseFloat(req.query.masterLatitude) : null;
    let masterLongitude = req.query.masterLongitude ? parseFloat(req.query.masterLongitude) : null;
    let master = null;
    
    console.log(`[GET /api/orders] User ID: ${req.user?.id}, Role: ${req.user?.role}`);
    console.log(`[GET /api/orders] Query params:`, req.query);
    
    let sql = `
      SELECT 
        o.*,
        u.name as client_name,
        u.phone as client_phone
      FROM orders o
      JOIN clients c ON o.client_id = c.id
      JOIN users u ON c.user_id = u.id
      WHERE 1=1
    `;
    const params = [];
    
    // Если мастер, показываем заказы в зависимости от статуса
    if (req.user.role === 'master') {
      // Используем индекс по user_id (уже есть idx_masters_user_id)
      master = query.get('SELECT id, latitude, longitude, verification_status, specialization FROM masters WHERE user_id = ? LIMIT 1', [req.user.id]);
      
      // Проверяем верификацию мастера
      if (!master) {
        return res.status(404).json({ error: 'Профиль мастера не найден' });
      }
      
      // Верификация НЕ требуется для просмотра заказов, только для принятия
      // Позволяем мастерам видеть заявки даже без верификации, чтобы они понимали систему
      
      if (status && (status === 'in_progress' || status === 'completed')) {
        // Для принятых заказов показываем только заказы мастера
        if (master) {
          sql += ' AND o.assigned_master_id = ? AND o.repair_status = ?';
          params.push(master.id, status);
          console.log(`[GET /api/orders] Master filter: assigned_master_id = ${master.id}, repair_status = '${status}'`);
        } else {
          // Если мастер не найден, возвращаем пустой список
          sql += ' AND 1=0';
          console.log(`[GET /api/orders] Master not found, returning empty list`);
        }
      } else {
        // Для новых заказов показываем все новые заказы
        sql += ' AND o.repair_status = ?';
        params.push('new');
        console.log(`[GET /api/orders] Master filter: repair_status = 'new'`);
        
        // Исключаем заказы, которые мастер уже отклонил
        if (master) {
          sql += ` AND o.id NOT IN (
            SELECT order_id FROM order_assignments 
            WHERE master_id = ? AND status = 'rejected'
          )`;
          params.push(master.id);
        }
      }
      
      // Используем координаты мастера из БД, если не переданы в запросе
      if (master && masterLatitude === null && masterLongitude === null && master.latitude && master.longitude) {
        masterLatitude = parseFloat(master.latitude);
        masterLongitude = parseFloat(master.longitude);
      }
    }
    
    // Если клиент, показываем только его заказы
    if (req.user.role === 'client') {
      // Используем индекс по user_id (нужно добавить, если его нет)
      const client = query.get('SELECT id FROM clients WHERE user_id = ? LIMIT 1', [req.user.id]);
      if (client) {
        sql += ' AND o.client_id = ?';
        params.push(client.id);
      }
    }
    
    // Фильтры
    if (status) {
      sql += ' AND o.repair_status = ?';
      params.push(status);
    }
    
    if (deviceType) {
      sql += ' AND o.device_type = ?';
      params.push(deviceType);
    }
    
    if (orderType) {
      sql += ' AND o.order_type = ?';
      params.push(orderType);
    }
    
    if (urgency) {
      sql += ' AND o.urgency = ?';
      params.push(urgency);
    }
    
    if (minPrice) {
      sql += ' AND (o.estimated_cost >= ? OR o.estimated_cost IS NULL)';
      params.push(parseFloat(minPrice));
    }
    
    if (maxPrice) {
      sql += ' AND (o.estimated_cost <= ? OR o.estimated_cost IS NULL)';
      params.push(parseFloat(maxPrice));
    }
    
    // Сортировка
    let orderBy = 'o.created_at DESC';
    if (sortBy === 'distance' && masterLatitude && masterLongitude) {
      // Сортировка по расстоянию (будет вычислена после получения данных)
      orderBy = 'o.created_at DESC';
    } else if (sortBy === 'price') {
      orderBy = 'o.estimated_cost DESC NULLS LAST, o.created_at DESC';
    } else if (sortBy === 'urgency') {
      orderBy = `CASE 
        WHEN o.urgency = 'emergency' THEN 1
        WHEN o.urgency = 'urgent' THEN 2
        WHEN o.urgency = 'planned' THEN 3
        ELSE 4
      END, o.created_at DESC`;
    } else if (sortBy === 'created_at') {
      orderBy = 'o.created_at DESC';
    }
    
    sql += ` ORDER BY ${orderBy}`;
    
    // Пагинация
    const limitValue = limit ? parseInt(limit) : null;
    const offsetValue = offset ? parseInt(offset) : null;
    
    if (limitValue) {
      sql += ' LIMIT ?';
      params.push(limitValue);
      if (offsetValue) {
        sql += ' OFFSET ?';
        params.push(offsetValue);
      }
    }
    
    console.log(`[GET /api/orders] SQL: ${sql}`);
    console.log(`[GET /api/orders] Params:`, params);
    
    let orders = query.all(sql, params);
    console.log(`[GET /api/orders] Found ${orders.length} orders`);

    // Для мастеров дополнительно фильтруем новые заказы по специализации
    if (req.user.role === 'master' && master) {
      try {
        const specializations = JSON.parse(master.specialization || '[]');
        if (Array.isArray(specializations) && specializations.length > 0) {
          const beforeCount = orders.length;
          orders = orders.filter(order => {
            // Для новых заказов применяем фильтр по специализации
            if (order.repair_status === 'new') {
              return !!order.device_type && specializations.includes(order.device_type);
            }
            // Для остальных статусов (in_progress, completed и т.д.) ничего не меняем
            return true;
          });
          const afterCount = orders.length;
          console.log(`[GET /api/orders] Master specialization filter applied: ${beforeCount} -> ${afterCount} orders`);
        } else {
          console.log('[GET /api/orders] Master has no specialization set, specialization filter skipped');
        }
      } catch (specError) {
        console.error('[GET /api/orders] Error parsing master specialization, filter skipped:', specError);
      }
    }
    
    // Вычисляем расстояние для каждого заказа (если есть координаты мастера)
    if (req.user.role === 'master' && masterLatitude && masterLongitude) {
      orders = orders.map(order => {
        if (order.latitude && order.longitude) {
          const distance = calculateDistance(
            parseFloat(masterLatitude),
            parseFloat(masterLongitude),
            parseFloat(order.latitude),
            parseFloat(order.longitude)
          );
          order.distance = distance;
        } else {
          order.distance = null;
        }
        return order;
      });
      
      // Фильтр по максимальному расстоянию
      if (maxDistance) {
        orders = orders.filter(order => 
          order.distance === null || order.distance <= parseFloat(maxDistance)
        );
      }
      
      // Сортировка по расстоянию (если запрошена)
      if (sortBy === 'distance') {
        orders.sort((a, b) => {
          if (a.distance === null && b.distance === null) return 0;
          if (a.distance === null) return 1;
          if (b.distance === null) return -1;
          return a.distance - b.distance;
        });
      }
    }
    
    // Для каждого заказа нормализуем problem_tags и получаем количество медиафайлов
    const ordersWithMediaCount = orders.map(order => {
      const mediaCount = query.get(
        'SELECT COUNT(*) as count FROM order_media WHERE order_id = ?',
        [order.id]
      );

      // Нормализуем problem_tags: в БД хранится TEXT (часто JSON-строка),
      // на фронт отправляем всегда как массив строк.
      let normalizedProblemTags = [];
      if (order.problem_tags != null) {
        try {
          const raw = order.problem_tags;
          let parsed = raw;

          if (typeof raw === 'string') {
            // Пытаемся распарсить как JSON
            try {
              parsed = JSON.parse(raw);
            } catch {
              // Если это не JSON, пробуем разделить по запятым
              normalizedProblemTags = raw
                .split(',')
                .map(t => t.trim())
                .filter(t => t.length > 0);
            }
          }

          if (Array.isArray(parsed)) {
            normalizedProblemTags = parsed
              .map(t => (typeof t === 'string' ? t : String(t)))
              .filter(t => t.length > 0);
          } else if (typeof parsed === 'string' && normalizedProblemTags.length === 0) {
            // Один тег строкой
            const trimmed = parsed.trim();
            if (trimmed.length > 0) {
              normalizedProblemTags = [trimmed];
            }
          }
        } catch (e) {
          console.warn('[GET /api/orders] Failed to normalize problem_tags for order', order.id, e.message);
          normalizedProblemTags = [];
        }
      }

      return {
        ...order,
        problem_tags: normalizedProblemTags,
        media_count: mediaCount ? mediaCount.count : 0
      };
    });
    
    console.log(`[GET /api/orders] Returning ${ordersWithMediaCount.length} orders`);
    if (ordersWithMediaCount.length > 0) {
      console.log(`[GET /api/orders] First order: id=${ordersWithMediaCount[0].id}, repair_status=${ordersWithMediaCount[0].repair_status}`);
    } else {
      console.log(`[GET /api/orders] ⚠️ NO ORDERS FOUND! Check database for orders with repair_status='new'`);
    }
    console.log('========================================');
    
    // Возвращаем результат с метаданными пагинации (если запрошена пагинация)
    if (limitValue) {
      const response = {
        data: ordersWithMediaCount,
        pagination: {
          limit: limitValue,
          offset: offsetValue || 0,
          count: ordersWithMediaCount.length,
          hasMore: ordersWithMediaCount.length === limitValue
        }
      };
      res.json(response);
    } else {
      // Без пагинации возвращаем просто массив (для обратной совместимости)
      res.json(ordersWithMediaCount);
    }
  } catch (error) {
    console.error('Ошибка получения заказов:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить историю всех заказов клиента
router.get('/history', authenticate, (req, res) => {
  try {
    // Проверяем, что пользователь - клиент
    if (req.user.role !== 'client') {
      return res.status(403).json({ error: 'Доступ только для клиентов' });
    }
    
    const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
    if (!client) {
      return res.status(404).json({ error: 'Профиль клиента не найден' });
    }
    
    // Получаем все завершенные или отмененные заказы
    const orders = query.all(`
      SELECT 
        o.*,
        u.name as client_name,
        u.phone as client_phone,
        mu.name as master_name
      FROM orders o
      JOIN clients c ON o.client_id = c.id
      JOIN users u ON c.user_id = u.id
      LEFT JOIN masters m ON o.assigned_master_id = m.id
      LEFT JOIN users mu ON m.user_id = mu.id
      WHERE o.client_id = ? 
        AND (o.repair_status = 'completed' OR o.repair_status = 'cancelled')
      ORDER BY o.created_at DESC
    `, [client.id]);
    
    // Для каждого заказа получаем количество медиафайлов
    const ordersWithMediaCount = orders.map(order => {
      const mediaCount = query.get(
        'SELECT COUNT(*) as count FROM order_media WHERE order_id = ?',
        [order.id]
      );
      return {
        ...order,
        media_count: mediaCount ? mediaCount.count : 0
      };
    });
    
    res.json(ordersWithMediaCount);
  } catch (error) {
    console.error('Ошибка получения истории заказов:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить историю техники клиента (для повторных заказов)
router.get('/client/devices', authenticate, authorize('client'), (req, res) => {
  try {
    const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
    if (!client) {
      return res.status(404).json({ error: 'Клиент не найден' });
    }
    
    // Получаем уникальные устройства из истории заказов
    const devices = query.all(`
      SELECT DISTINCT
        o.device_type,
        o.device_brand,
        o.device_model,
        o.device_serial_number,
        o.device_year,
        o.device_category,
        MAX(o.created_at) as last_order_date,
        COUNT(o.id) as order_count,
        MAX(o.id) as last_order_id
      FROM orders o
      WHERE o.client_id = ?
        AND o.device_type IS NOT NULL
        AND o.device_type != ''
      GROUP BY 
        o.device_type,
        COALESCE(o.device_brand, ''),
        COALESCE(o.device_model, ''),
        COALESCE(o.device_serial_number, '')
      ORDER BY last_order_date DESC
    `, [client.id]);
    
    res.json(devices);
  } catch (error) {
    console.error('Ошибка получения истории техники:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Создать повторный заказ на основе предыдущего
router.post('/reorder/:orderId', authenticate, authorize('client'), async (req, res) => {
  console.log(`[POST /api/orders/reorder/:orderId] Запрос на создание повторного заказа, orderId: ${req.params.orderId}`);
  try {
    const { orderId } = req.params;
    const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
    if (!client) {
      return res.status(404).json({ error: 'Клиент не найден' });
    }
    
    // Получаем исходный заказ
    const originalOrder = query.get(`
      SELECT * FROM orders 
      WHERE id = ? AND client_id = ?
    `, [orderId, client.id]);
    
    if (!originalOrder) {
      return res.status(404).json({ error: 'Заказ не найден или не принадлежит вам' });
    }
    
    // Проверяем, является ли это гарантийным случаем
    const isWarrantyCase = checkWarrantyCase(originalOrder);
    
    // Создаем новый заказ на основе старого
    const orderNumber = generateOrderNumber();
    const result = query.run(`
      INSERT INTO orders (
        order_number, client_id,
        request_status, priority, order_source,
        device_type, device_category, device_brand, device_model, 
        device_serial_number, device_year, warranty_status,
        problem_short_description, problem_description,
        problem_when_started, problem_conditions, problem_error_codes, problem_attempted_fixes,
        address, address_street, address_building, address_apartment,
        address_floor, address_entrance_code, address_landmark,
        latitude, longitude,
        arrival_time, desired_repair_date, urgency,
        client_budget, payment_type, visit_cost, max_cost_without_approval,
        intercom_working, needs_pass, parking_available,
        has_pets, has_small_children, needs_shoe_covers, preferred_contact_method,
        master_gender_preference, master_min_experience, preferred_master_id,
        problem_tags, problem_category, problem_seasonality,
        related_order_id,
        order_type, repair_status
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `, [
      orderNumber, client.id,
      'new', originalOrder.priority || 'regular', originalOrder.order_source || 'app',
      originalOrder.device_type, originalOrder.device_category || null, originalOrder.device_brand || null, originalOrder.device_model || null,
      originalOrder.device_serial_number || null, originalOrder.device_year || null, isWarrantyCase ? 'warranty' : (originalOrder.warranty_status || null),
      originalOrder.problem_short_description || 'Повторный заказ', originalOrder.problem_description || 'Повторный заказ на основе предыдущего',
      originalOrder.problem_when_started || null, originalOrder.problem_conditions || null, originalOrder.problem_error_codes || null, originalOrder.problem_attempted_fixes || null,
      originalOrder.address, originalOrder.address_street || null, originalOrder.address_building || null, originalOrder.address_apartment || null,
      originalOrder.address_floor || null, originalOrder.address_entrance_code || null, originalOrder.address_landmark || null,
      originalOrder.latitude, originalOrder.longitude,
      originalOrder.arrival_time || null, originalOrder.desired_repair_date || null, originalOrder.urgency || 'planned',
      originalOrder.client_budget || null, originalOrder.payment_type || null, originalOrder.visit_cost || null, originalOrder.max_cost_without_approval || null,
      originalOrder.intercom_working || 1, originalOrder.needs_pass || 0, originalOrder.parking_available || 1,
      originalOrder.has_pets || 0, originalOrder.has_small_children || 0, originalOrder.needs_shoe_covers || 0, originalOrder.preferred_contact_method || 'call',
      originalOrder.master_gender_preference || 'any', originalOrder.master_min_experience || null, originalOrder.preferred_master_id || null,
      originalOrder.problem_tags || null, originalOrder.problem_category || null, originalOrder.problem_seasonality || 'permanent',
      originalOrder.id, // related_order_id - Связь с предыдущим заказом
      originalOrder.order_type || 'regular', 'new' // repair_status
    ]);
    
    const newOrder = query.get('SELECT * FROM orders WHERE id = ?', [result.lastInsertRowid]);
    
    // Добавляем запись в историю
    query.run(`
      INSERT INTO client_order_history (client_id, order_id, related_device_type, related_device_model)
      VALUES (?, ?, ?, ?)
    `, [client.id, newOrder.id, newOrder.device_type, newOrder.device_model]);
    
    // Добавляем в историю ремонтов техники
    if (newOrder.device_serial_number) {
      query.run(`
        INSERT INTO device_repair_history (order_id, device_type, device_brand, device_model, device_serial_number, repair_date)
        VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
      `, [newOrder.id, newOrder.device_type, newOrder.device_brand, newOrder.device_model, newOrder.device_serial_number]);
    }
    
    res.status(201).json({
      message: isWarrantyCase ? 'Создан гарантийный заказ' : 'Создан повторный заказ',
      order: newOrder,
      isWarrantyCase: isWarrantyCase
    });
  } catch (error) {
    console.error('Ошибка создания повторного заказа:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Получить историю статусов заказа
router.get('/:id/status-history', authenticate, (req, res) => {
  try {
    const { id } = req.params;
    
    // Проверяем доступ к заказу
    const order = query.get('SELECT * FROM orders WHERE id = ?', [id]);
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    // Проверяем права доступа
    if (req.user.role === 'client') {
      const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
      if (!client || order.client_id !== client.id) {
        return res.status(403).json({ error: 'Нет доступа к этому заказу' });
      }
    } else if (req.user.role === 'master') {
      const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
      if (!master || order.assigned_master_id !== master.id) {
        return res.status(403).json({ error: 'Нет доступа к этому заказу' });
      }
    }
    
    const history = query.all(`
      SELECT 
        h.*,
        u.name as changed_by_name,
        u.role as changed_by_role
      FROM order_status_history h
      LEFT JOIN users u ON h.changed_by = u.id
      WHERE h.order_id = ?
      ORDER BY h.created_at ASC
    `, [id]);
    
    res.json(history);
  } catch (error) {
    console.error('Ошибка получения истории статусов:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить заказ по ID с медиафайлами и связанными данными
router.get('/:id', authenticate, (req, res) => {
  try {
    const { id } = req.params;
    
    const order = query.get(`
      SELECT 
        o.*,
        o.assigned_master_id as master_id,
        u.name as client_name,
        u.phone as client_phone,
        u.email as client_email,
        mu.name as master_name
      FROM orders o
      JOIN clients c ON o.client_id = c.id
      JOIN users u ON c.user_id = u.id
      LEFT JOIN masters m ON o.assigned_master_id = m.id
      LEFT JOIN users mu ON m.user_id = mu.id
      WHERE o.id = ?
    `, [id]);
    
    // Преобразуем assigned_master_id в master_id для совместимости с клиентом
    if (order && order.assigned_master_id) {
      order.master_id = order.assigned_master_id;
    }
    
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    // Проверка прав доступа
    if (req.user.role === 'client') {
      const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
      if (client && order.client_id !== client.id) {
        return res.status(403).json({ error: 'Доступ запрещен' });
      }
    } else if (req.user.role === 'master') {
      // Мастер может видеть детали заказа только если он его принял
      const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
      if (master) {
        // Проверяем, принял ли мастер этот заказ
        const acceptedAssignment = query.get(
          'SELECT * FROM order_assignments WHERE order_id = ? AND master_id = ? AND status = ?',
          [id, master.id, 'accepted']
        );
        
        // Также проверяем, является ли мастер назначенным мастером заказа
        const isAssignedMaster = order.assigned_master_id === master.id;
        
        if (!acceptedAssignment && !isAssignedMaster) {
          // Мастер не может видеть детали заказа, который еще не принял
          return res.status(403).json({ 
            error: 'Доступ запрещен',
            message: 'Мастер не может открыть детали заказа, пока не примет его'
          });
        }
      }
    }
    
    // Получаем медиафайлы
    const media = query.all(`
      SELECT * FROM order_media 
      WHERE order_id = ? 
      ORDER BY upload_order ASC, created_at ASC
    `, [id]);
    
    // Получаем историю обращений клиента
    const clientHistory = query.all(`
      SELECT order_id, created_at 
      FROM client_order_history 
      WHERE client_id = ? AND order_id != ?
      ORDER BY created_at DESC
      LIMIT 10
    `, [order.client_id, id]);
    
    // Получаем историю ремонтов этой техники (если есть серийный номер)
    let deviceHistory = [];
    if (order.device_serial_number) {
      deviceHistory = query.all(`
        SELECT order_id, repair_date, repair_description 
        FROM device_repair_history 
        WHERE device_serial_number = ? AND order_id != ?
        ORDER BY repair_date DESC
      `, [order.device_serial_number, id]);
    }
    
    // Получаем частые проблемы для данной модели (если указана)
    let commonProblems = [];
    if (order.device_type) {
      const problemParams = [order.device_type];
      let problemSql = `
        SELECT * FROM common_problems 
        WHERE device_type = ?
      `;
      
      if (order.device_brand) {
        problemSql += ' AND (device_brand = ? OR device_brand IS NULL)';
        problemParams.push(order.device_brand);
      }
      
      if (order.device_model) {
        problemSql += ' AND (device_model = ? OR device_model IS NULL)';
        problemParams.push(order.device_model);
      }
      
      problemSql += ' ORDER BY frequency_rating DESC LIMIT 5';
      
      commonProblems = query.all(problemSql, problemParams);
    }
    
    // Формируем ответ с правильными именами полей
    const response = {
      ...order,
      master_id: order.assigned_master_id || order.master_id || null,
      media,
      clientHistory,
      deviceHistory,
      commonProblems
    };
    
    // Удаляем assigned_master_id если есть master_id (чтобы избежать дублирования)
    delete response.assigned_master_id;
    
    res.json(response);
  } catch (error) {
    console.error('Ошибка получения заказа:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Создать заказ с сайта (без авторизации, автоматически создает пользователя и клиента)
router.post('/from-website', async (req, res) => {
  try {
    console.log('📦 Создание заказа с сайта. Тело запроса:', JSON.stringify(req.body, null, 2));
    
    const {
      // Данные клиента
      name,
      phone,
      email,
      
      // Данные заказа
      device_type: deviceType,
      device_brand: deviceBrand,
      device_model: deviceModel = '',
      problem_description: problemDescription,
      problem_short_description: problemShortDescription,
      address,
      latitude,
      longitude,
      desired_repair_date: desiredRepairDate,
      arrival_time: arrivalTime,
      urgency = 'planned',
      priority = 'regular',
      
      // Дополнительные поля
      device_category: deviceCategory = null,
      intercom_working: intercomWorking = true,
      parking_available: parkingAvailable = true,
      preferred_contact_method: preferredContactMethod = 'call',
    } = req.body;
    
    // Валидация обязательных полей
    if (!name || !phone || !email || !deviceType || !problemDescription || !address) {
      return res.status(400).json({ 
        error: 'Не все обязательные поля заполнены: name, phone, email, device_type, problem_description, address' 
      });
    }
    
    // Проверяем или создаем пользователя
    let user = query.get('SELECT * FROM users WHERE email = ?', [email]);
    let userId;
    
    if (!user) {
      // Создаем нового пользователя (без пароля, так как это заказ с сайта)
      // Генерируем случайный пароль, который пользователь не будет использовать
      const bcrypt = await import('bcryptjs');
      const randomPassword = Math.random().toString(36).slice(-12) + Date.now().toString(36);
      const passwordHash = await bcrypt.default.hash(randomPassword, 10);
      
      const userResult = query.run(
        'INSERT INTO users (email, password_hash, name, phone, role) VALUES (?, ?, ?, ?, ?)',
        [email, passwordHash, name, phone, 'client']
      );
      userId = userResult.lastInsertRowid;
      console.log(`[POST /api/orders/from-website] Создан новый пользователь: id=${userId}, email=${email}`);
    } else {
      userId = user.id;
      // Обновляем имя и телефон, если они изменились
      if (user.name !== name || user.phone !== phone) {
        query.run('UPDATE users SET name = ?, phone = ? WHERE id = ?', [name, phone, userId]);
      }
      console.log(`[POST /api/orders/from-website] Используется существующий пользователь: id=${userId}, email=${email}`);
    }
    
    // Проверяем или создаем клиента
    let client = query.get('SELECT id FROM clients WHERE user_id = ?', [userId]);
    if (!client) {
      const clientResult = query.run('INSERT INTO clients (user_id) VALUES (?)', [userId]);
      client = { id: clientResult.lastInsertRowid };
      console.log(`[POST /api/orders/from-website] Создан новый клиент: id=${client.id}`);
    }
    
    // Получаем координаты для адреса
    const coordinates = await getCoordinatesForAddress(address, latitude, longitude);
    if (!coordinates) {
      console.error('❌ Не удалось получить координаты для адреса:', address);
      return res.status(400).json({ 
        error: 'Не удалось определить координаты адреса. Проверьте правильность адреса.' 
      });
    }
    
    const finalLatitude = coordinates.latitude;
    const finalLongitude = coordinates.longitude;
    
    // Генерируем уникальный номер заявки
    const orderNumber = generateOrderNumber();
    
    // Создаем заказ
    const result = query.run(`
      INSERT INTO orders (
        order_number, client_id,
        request_status, priority, order_source,
        device_type, device_category, device_brand, device_model,
        problem_short_description, problem_description,
        address,
        latitude, longitude,
        arrival_time, desired_repair_date, urgency,
        intercom_working, parking_available, preferred_contact_method,
        order_type, repair_status
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `, [
      orderNumber, client.id,
      'new', priority, 'website',
      deviceType, deviceCategory, deviceBrand || null, deviceModel,
      problemShortDescription || problemDescription, problemDescription,
      address,
      finalLatitude, finalLongitude,
      arrivalTime || null, desiredRepairDate || null, urgency,
      intercomWorking ? 1 : 0, parkingAvailable ? 1 : 0, preferredContactMethod,
      'regular', 'new'
    ]);
    
    const orderId = result.lastInsertRowid;
    
    // Записываем в историю статусов
    query.run(
      'INSERT INTO order_status_history (order_id, new_status, changed_by) VALUES (?, ?, ?)',
      [orderId, 'new', userId]
    );
    
    // Получаем созданный заказ
    const newOrder = query.get('SELECT * FROM orders WHERE id = ?', [orderId]);
    
    // Запускаем процесс назначения мастеров (асинхронно)
    setImmediate(() => {
      notifyMasters(orderId, deviceType, finalLatitude, finalLongitude);
    });
    
    console.log(`[POST /api/orders/from-website] Заказ успешно создан: id=${orderId}, order_number=${orderNumber}`);
    
    res.status(201).json({
      message: 'Заказ успешно создан',
      order: newOrder
    });
  } catch (error) {
    console.error('Ошибка создания заказа с сайта:', error);
    res.status(500).json({ error: 'Ошибка сервера: ' + error.message });
  }
});

// Создать новый заказ (только клиенты)
router.post('/', authenticate, authorize('client', 'admin'), async (req, res) => {
  try {
    console.log('📦 Создание заказа. Тело запроса:', JSON.stringify(req.body, null, 2));
    console.log(`[POST /api/orders] Пользователь: id=${req.user.id}, role=${req.user.role}, email=${req.user.email}`);
    
    // Читаем поля в snake_case (как приходит из клиента)
    const {
      // Основная информация
      device_type: deviceType,
      device_category: deviceCategory,
      device_brand: deviceBrand,
      device_model: deviceModel,
      device_serial_number: deviceSerialNumber,
      device_year: deviceYear,
      warranty_status: warrantyStatus,
      
      // Описание проблемы
      problem_short_description: problemShortDescription,
      problem_description: problemDescription,
      problem_when_started: problemWhenStarted,
      problem_conditions: problemConditions,
      problem_error_codes: problemErrorCodes,
      problem_attempted_fixes: problemAttemptedFixes,
      
      // Адрес (детализированный)
      address,
      address_street: addressStreet,
      address_building: addressBuilding,
      address_apartment: addressApartment,
      address_floor: addressFloor,
      address_entrance_code: addressEntranceCode,
      address_landmark: addressLandmark,
      latitude,
      longitude,
      
      // Временные параметры
      arrival_time: arrivalTime,
      desired_repair_date: desiredRepairDate,
      urgency,
      
      // Приоритет и источник
      priority = 'regular',
      order_source: orderSource = 'app',
      order_type: orderType = 'regular', // Для совместимости
      
      // Финансовые параметры
      client_budget: clientBudget,
      payment_type: paymentType,
      visit_cost: visitCost,
      max_cost_without_approval: maxCostWithoutApproval,
      
      // Дополнительная информация
      intercom_working: intercomWorking = true,
      needs_pass: needsPass = false,
      parking_available: parkingAvailable = true,
      has_pets: hasPets = false,
      has_small_children: hasSmallChildren = false,
      needs_shoe_covers: needsShoeCovers = false,
      preferred_contact_method: preferredContactMethod = 'call',
      
      // Предпочтения по мастеру
      master_gender_preference: masterGenderPreference = 'any',
      master_min_experience: masterMinExperience,
      preferred_master_id: preferredMasterId,
      
      // Теги и категории
      problem_tags: problemTags,
      problem_category: problemCategory,
      problem_seasonality: problemSeasonality = 'permanent'
    } = req.body;
    
    // Валидация обязательных полей
    if (!deviceType || !problemDescription || !address) {
      return res.status(400).json({ error: 'Не все обязательные поля заполнены: device_type, problem_description, address' });
    }
    
    // Получаем координаты для адреса (геокодирование, если нужно)
    const coordinates = await getCoordinatesForAddress(address, latitude, longitude);
    
    if (!coordinates) {
      console.error('❌ Не удалось получить координаты для адреса:', address);
      return res.status(400).json({ 
        error: 'Не удалось определить координаты адреса. Проверьте правильность адреса.' 
      });
    }
    
    const finalLatitude = coordinates.latitude;
    const finalLongitude = coordinates.longitude;
    
    // Получаем ID клиента
    let client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
    if (!client) {
      // Если пользователь имеет роль 'client' или 'admin', но записи в таблице clients нет, создаем её
      if (req.user.role === 'client' || req.user.role === 'admin') {
        console.log(`[POST /api/orders] Создаем запись клиента для user_id=${req.user.id} (role=${req.user.role})`);
        const result = query.run('INSERT INTO clients (user_id) VALUES (?)', [req.user.id]);
        client = { id: result.lastInsertRowid };
        console.log(`[POST /api/orders] Запись клиента создана: id=${client.id}`);
      } else {
        console.error(`[POST /api/orders] Пользователь ${req.user.id} имеет роль '${req.user.role}', но пытается создать заказ`);
        return res.status(403).json({ 
          error: 'Недостаточно прав доступа. Для создания заказа требуется роль клиента.',
          userRole: req.user.role
        });
      }
    }
    
    // Генерируем уникальный номер заявки
    const orderNumber = generateOrderNumber();
    
    // Создаем заказ
    const result = query.run(`
      INSERT INTO orders (
        order_number, client_id,
        request_status, priority, order_source,
        device_type, device_category, device_brand, device_model, 
        device_serial_number, device_year, warranty_status,
        problem_short_description, problem_description,
        problem_when_started, problem_conditions, problem_error_codes, problem_attempted_fixes,
        address, address_street, address_building, address_apartment,
        address_floor, address_entrance_code, address_landmark,
        latitude, longitude,
        arrival_time, desired_repair_date, urgency,
        client_budget, payment_type, visit_cost, max_cost_without_approval,
        intercom_working, needs_pass, parking_available,
        has_pets, has_small_children, needs_shoe_covers, preferred_contact_method,
        master_gender_preference, master_min_experience, preferred_master_id,
        problem_tags, problem_category, problem_seasonality,
        order_type, repair_status
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `, [
      orderNumber, client.id,
      'new', priority, orderSource,
      deviceType, deviceCategory || null, deviceBrand || null, deviceModel || null,
      deviceSerialNumber || null, deviceYear || null, warrantyStatus || null,
      problemShortDescription || null, problemDescription,
      problemWhenStarted || null, problemConditions || null, problemErrorCodes || null, problemAttemptedFixes || null,
      address, addressStreet || null, addressBuilding || null, addressApartment || null,
      addressFloor || null, addressEntranceCode || null, addressLandmark || null,
      finalLatitude, finalLongitude,
      arrivalTime || null, desiredRepairDate || null, urgency || 'planned',
      clientBudget || null, paymentType || null, visitCost || null, maxCostWithoutApproval || null,
      intercomWorking ? 1 : 0, needsPass ? 1 : 0, parkingAvailable ? 1 : 0,
      hasPets ? 1 : 0, hasSmallChildren ? 1 : 0, needsShoeCovers ? 1 : 0, preferredContactMethod,
      masterGenderPreference, masterMinExperience || null, preferredMasterId || null,
      problemTags ? JSON.stringify(problemTags) : null, problemCategory || null, problemSeasonality,
      orderType, 'new'
    ]);
    
    const orderId = result.lastInsertRowid;
    
    // Записываем в историю статусов
    query.run(
      'INSERT INTO order_status_history (order_id, new_status, changed_by) VALUES (?, ?, ?)',
      [orderId, 'new', req.user.id]
    );
    
    // Записываем в историю обращений клиента
    query.run(
      'INSERT INTO client_order_history (client_id, order_id, related_device_type, related_device_model) VALUES (?, ?, ?, ?)',
      [client.id, orderId, deviceType, deviceModel || null]
    );
    
    // Если есть серийный номер, записываем в историю ремонтов техники
    if (deviceSerialNumber) {
      query.run(
        'INSERT INTO device_repair_history (order_id, device_type, device_brand, device_model, device_serial_number, repair_date) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)',
        [orderId, deviceType, deviceBrand || null, deviceModel || null, deviceSerialNumber]
      );
    }
    
    // Получаем созданный заказ
    const newOrder = query.get('SELECT * FROM orders WHERE id = ?', [orderId]);
    
    // Запускаем процесс назначения мастеров (асинхронно) с учетом координат
    setImmediate(() => {
      notifyMasters(orderId, deviceType, finalLatitude, finalLongitude);
    });
    
    res.status(201).json({
      message: 'Заказ успешно создан',
      order: newOrder
    });
  } catch (error) {
    console.error('Ошибка создания заказа:', error);
    res.status(500).json({ error: 'Ошибка сервера: ' + error.message });
  }
});

// Обновить заказ
router.put('/:id', authenticate, async (req, res) => {
  try {
    const { id } = req.params;
    const updates = req.body;
    
    // Проверяем существование заказа
    const order = query.get('SELECT * FROM orders WHERE id = ?', [id]);
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    // Проверка прав доступа
    if (req.user.role === 'client') {
      const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
      if (client && order.client_id !== client.id) {
        return res.status(403).json({ error: 'Доступ запрещен' });
      }
    }
    
    // Если обновляется адрес, проверяем и обновляем координаты
    if (updates.address !== undefined) {
      const newAddress = updates.address;
      const newLatitude = updates.latitude;
      const newLongitude = updates.longitude;
      
      // Получаем координаты для нового адреса
      const coordinates = await getCoordinatesForAddress(newAddress, newLatitude, newLongitude);
      
      if (coordinates) {
        updates.latitude = coordinates.latitude;
        updates.longitude = coordinates.longitude;
        console.log(`📍 Обновлены координаты для заказа ${id}: ${coordinates.latitude}, ${coordinates.longitude}`);
      } else {
        console.warn(`⚠️ Не удалось получить координаты для адреса: ${newAddress}`);
        // Не обновляем координаты, если геокодирование не удалось
      }
    }
    
    // Формируем запрос на обновление
    const allowedFields = [
      // Основная информация
      'device_type', 'device_category', 'device_brand', 'device_model',
      'device_serial_number', 'device_year', 'warranty_status',
      // Описание проблемы
      'problem_short_description', 'problem_description',
      'problem_when_started', 'problem_conditions', 'problem_error_codes', 'problem_attempted_fixes',
      // Адрес
      'address', 'address_street', 'address_building', 'address_apartment',
      'address_floor', 'address_entrance_code', 'address_landmark',
      'latitude', 'longitude',
      // Временные параметры
      'arrival_time', 'desired_repair_date', 'urgency',
      // Статус и приоритет
      'request_status', 'priority', 'order_source', 'order_type',
      'repair_status',
      // Финансовые параметры
      'estimated_cost', 'final_cost', 'client_budget', 'payment_type',
      'visit_cost', 'max_cost_without_approval',
      // Дополнительная информация
      'intercom_working', 'needs_pass', 'parking_available',
      'has_pets', 'has_small_children', 'needs_shoe_covers', 'preferred_contact_method',
      // Предпочтения по мастеру
      'master_gender_preference', 'master_min_experience', 'preferred_master_id',
      'assigned_master_id', 'assignment_date',
      // Служебная информация
      'preliminary_diagnosis', 'required_parts', 'special_equipment',
      'repair_complexity', 'estimated_repair_time',
      // Теги и категории
      'problem_tags', 'problem_category', 'problem_seasonality'
    ];
    
    const updateFields = [];
    const updateValues = [];
    
    for (const field of allowedFields) {
      if (updates[field] !== undefined) {
        let value = updates[field];
        
        // Преобразуем boolean в INTEGER для SQLite
        if (['intercom_working', 'needs_pass', 'parking_available', 
             'has_pets', 'has_small_children', 'needs_shoe_covers'].includes(field)) {
          value = value ? 1 : 0;
        }
        
        // Преобразуем массивы в JSON строки
        if (['problem_tags', 'required_parts'].includes(field) && Array.isArray(value)) {
          value = JSON.stringify(value);
        }
        
        updateFields.push(`${field} = ?`);
        updateValues.push(value);
      }
    }
    
    if (updateFields.length === 0) {
      return res.status(400).json({ error: 'Нет полей для обновления' });
    }
    
    updateFields.push('updated_at = CURRENT_TIMESTAMP');
    updateValues.push(id);
    
    // Обновляем заказ
    query.run(
      `UPDATE orders SET ${updateFields.join(', ')} WHERE id = ?`,
      updateValues
    );
    
    // Если изменился статус, записываем в историю
    if (updates.repair_status && updates.repair_status !== order.repair_status) {
      query.run(
        'INSERT INTO order_status_history (order_id, old_status, new_status, changed_by) VALUES (?, ?, ?, ?)',
        [id, order.repair_status, updates.repair_status, req.user.id]
      );
    }
    
    // Если изменился request_status, также записываем в историю
    if (updates.request_status && updates.request_status !== order.request_status) {
      query.run(
        'INSERT INTO order_status_history (order_id, old_status, new_status, changed_by) VALUES (?, ?, ?, ?)',
        [id, order.request_status, updates.request_status, req.user.id]
      );
    }
    
    // Получаем обновленный заказ
    const updatedOrder = query.get('SELECT * FROM orders WHERE id = ?', [id]);
    
    res.json({
      message: 'Заказ успешно обновлен',
      order: updatedOrder
    });
  } catch (error) {
    console.error('Ошибка обновления заказа:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Завершить заказ (для мастера)
router.put('/:id/complete', authenticate, authorize('master'), async (req, res) => {
  try {
    const { id } = req.params;
    const { final_cost: finalCost, repair_description: repairDescription } = req.body;
    
    // Получаем мастера
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    // Получаем заказ
    const order = query.get('SELECT * FROM orders WHERE id = ?', [id]);
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    // Проверяем, что заказ назначен этому мастеру
    if (order.assigned_master_id !== master.id) {
      return res.status(403).json({ error: 'Заказ не назначен вам' });
    }
    
    // Проверяем, что заказ в работе
    if (order.repair_status !== 'in_progress' && order.request_status !== 'in_progress') {
      return res.status(400).json({ error: 'Можно завершить только заказ в работе' });
    }
    
    const oldRepairStatus = order.repair_status;
    const oldRequestStatus = order.request_status;
    
    // Обновляем статус заказа
    const updateFields = ['repair_status = ?', 'request_status = ?', 'updated_at = CURRENT_TIMESTAMP'];
    const updateValues = ['completed', 'completed'];
    
    if (finalCost !== undefined) {
      updateFields.push('final_cost = ?');
      updateValues.push(finalCost);
    }
    
    updateValues.push(id);
    
    query.run(
      `UPDATE orders SET ${updateFields.join(', ')} WHERE id = ?`,
      updateValues
    );
    
    // Фиксируем цену, если указана
    if (finalCost !== undefined && finalCost > 0) {
      try {
        fixOrderPrice(id, finalCost, repairDescription || 'Цена согласована при завершении заказа');
      } catch (error) {
        console.error('Ошибка фиксации цены:', error);
        // Продолжаем выполнение, даже если фиксация цены не удалась
      }
    }
    
    // Начисляем баллы лояльности клиенту за завершенный заказ
    try {
      const { awardPointsForOrder } = await import('../services/loyalty-service.js');
      const finalAmount = finalCost || order.estimated_cost || 0;
      awardPointsForOrder(order.client_id, id, finalAmount);
    } catch (error) {
      console.error('Ошибка начисления баллов лояльности:', error);
      // Продолжаем выполнение, даже если начисление баллов не удалось
    }
    
    // Если есть описание ремонта, записываем в историю ремонтов
    if (repairDescription) {
      query.run(
        'INSERT INTO device_repair_history (order_id, device_type, device_brand, device_model, device_serial_number, repair_date, repair_description) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)',
        [id, order.device_type, order.device_brand, order.device_model, order.device_serial_number, repairDescription]
      );
    }
    
    // Записываем в историю статусов
    const masterName = req.user.name || 'Мастер';
    query.run(
      'INSERT INTO order_status_history (order_id, old_status, new_status, changed_by, note) VALUES (?, ?, ?, ?, ?)',
      [id, oldRepairStatus, 'completed', req.user.id, `Заказ завершен мастером ${masterName}${repairDescription ? ': ' + repairDescription : ''}`]
    );
    
    // Обновляем статистику мастера
    const completedCount = query.get(
      'SELECT COUNT(*) as count FROM orders WHERE assigned_master_id = ? AND repair_status = ?',
      [master.id, 'completed']
    );
    query.run(
      'UPDATE masters SET completed_orders = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      [completedCount.count, 'available', master.id]
    );
    
    // Начисляем средства мастеру (если указана финальная стоимость)
    // Используем сервис платежей для правильного расчета комиссии с учетом подписки
    if (finalCost && finalCost > 0) {
      try {
        const { calculatePlatformCommission } = await import('../services/payment-service.js');
        const commission = calculatePlatformCommission(finalCost, master.id);
        
        // Обновляем баланс мастера
        query.run(
          'UPDATE masters SET balance = COALESCE(balance, 0) + ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
          [commission.netAmount, master.id]
        );
        
        // Создаем транзакцию начисления
        query.run(`
          INSERT INTO master_transactions 
          (master_id, order_id, transaction_type, amount, status, commission_percentage, commission_amount, description)
          VALUES (?, ?, 'income', ?, 'completed', ?, ?, ?)
        `, [
          master.id, 
          id, 
          commission.netAmount, 
          commission.commissionPercentage, 
          commission.commissionAmount,
          `Начисление за выполнение заказа #${order.order_number || id}. Комиссия платформы: ${commission.commissionAmount.toFixed(2)} ₽ (${commission.commissionPercentage}%)`
        ]);
        
        console.log(`💰 Начислено мастеру #${master.id}: ${commission.netAmount.toFixed(2)} ₽ (комиссия: ${commission.commissionAmount.toFixed(2)} ₽, ${commission.commissionPercentage}%)`);
        
        // Рассчитываем и начисляем MLM комиссии спонсорам
        try {
          const { calculateMLMCommissions } = await import('../services/mlm-service.js');
          calculateMLMCommissions(id, master.id, finalCost);
        } catch (mlmError) {
          console.error('Ошибка расчета MLM комиссий:', mlmError);
          // Продолжаем выполнение, даже если MLM комиссии не удалось рассчитать
        }
      } catch (error) {
        console.error('Ошибка начисления средств мастеру:', error);
        // Продолжаем выполнение, даже если начисление не удалось
      }
    }
    
    // Отправляем уведомление клиенту через WebSocket и Push
    const client = query.get(`
      SELECT c.user_id 
      FROM clients c 
      JOIN orders o ON o.client_id = c.id 
      WHERE o.id = ?
    `, [id]);
    
    if (client && client.user_id) {
      // WebSocket уведомление
      broadcastToClient(client.user_id, {
        type: 'order_status_changed',
        orderId: id,
        status: 'completed',
        message: 'Ваш заказ завершен',
        masterName: masterName
      });
      
      // Push-уведомление
      await notifyOrderStatusChange(
        client.user_id,
        id,
        'completed',
        'Ваш заказ завершен',
        masterName
      );
      
      console.log(`📨 Уведомления отправлены клиенту #${client.user_id}`);
    }
    
    // Получаем обновленный заказ
    const updatedOrder = query.get('SELECT * FROM orders WHERE id = ?', [id]);
    
    res.json({
      message: 'Заказ успешно завершен',
      order: updatedOrder
    });
  } catch (error) {
    console.error('Ошибка завершения заказа:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Отменить заказ
router.delete('/:id', authenticate, async (req, res) => {
  try {
    const { id } = req.params;
    
    const order = query.get('SELECT * FROM orders WHERE id = ?', [id]);
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    // Проверка прав доступа
    if (req.user.role === 'client') {
      const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
      if (client && order.client_id !== client.id) {
        return res.status(403).json({ error: 'Доступ запрещен' });
      }
    }
    
    // Обновляем статус на "отменен"
    query.run('UPDATE orders SET repair_status = ?, request_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?', 
      ['cancelled', 'cancelled', id]);
    
    // Записываем в историю
    query.run(
      'INSERT INTO order_status_history (order_id, old_status, new_status, changed_by, note) VALUES (?, ?, ?, ?, ?)',
      [id, order.repair_status, 'cancelled', req.user.id, 'Заказ отменен']
    );
    
    // Отправляем уведомление клиенту (если отменяет не клиент)
    if (req.user.role !== 'client') {
      const client = query.get(`
        SELECT c.user_id 
        FROM clients c 
        JOIN orders o ON o.client_id = c.id 
        WHERE o.id = ?
      `, [id]);
      
      if (client && client.user_id) {
        // WebSocket уведомление
        broadcastToClient(client.user_id, {
          type: 'order_status_changed',
          orderId: id,
          status: 'cancelled',
          message: 'Заказ отменен'
        });
        
        // Push-уведомление
        await notifyOrderStatusChange(
          client.user_id,
          id,
          'cancelled',
          'Заказ отменен'
        );
      }
    }
    
    res.json({ message: 'Заказ успешно отменен' });
  } catch (error) {
    console.error('Ошибка отмены заказа:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить медиафайлы заказа
router.get('/:id/media', authenticate, (req, res) => {
  try {
    const { id } = req.params;
    
    // Проверяем существование заказа и права доступа
    const order = query.get('SELECT * FROM orders WHERE id = ?', [id]);
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    if (req.user.role === 'client') {
      const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
      if (client && order.client_id !== client.id) {
        return res.status(403).json({ error: 'Доступ запрещен' });
      }
    }
    
    const media = query.all(`
      SELECT * FROM order_media 
      WHERE order_id = ? 
      ORDER BY upload_order ASC, created_at ASC
    `, [id]);
    
    res.json(media);
  } catch (error) {
    console.error('Ошибка получения медиафайлов:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Загрузить медиафайлы к заказу (multipart/form-data)
router.post('/:id/media/upload', authenticate, upload.array('files', 5), handleUploadError, (req, res) => {
  try {
    const { id } = req.params;
    const { description } = req.body;
    
    // Проверяем существование заказа и права доступа
    const order = query.get('SELECT * FROM orders WHERE id = ?', [id]);
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    if (req.user.role === 'client') {
      const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
      if (client && order.client_id !== client.id) {
        return res.status(403).json({ error: 'Доступ запрещен' });
      }
    }
    
    if (!req.files || req.files.length === 0) {
      return res.status(400).json({ error: 'Файлы не загружены' });
    }
    
    const uploadedMedia = [];
    
    for (let i = 0; i < req.files.length; i++) {
      const file = req.files[i];
      
      // Определяем тип медиа
      const isVideo = file.mimetype.startsWith('video/');
      const mediaType = isVideo ? 'video' : 'photo';
      
      // Проверка ограничений
      if (mediaType === 'photo') {
        const photoCount = query.get(
          'SELECT COUNT(*) as count FROM order_media WHERE order_id = ? AND media_type = ?',
          [id, 'photo']
        );
        if (photoCount && photoCount.count >= 5) {
          continue; // Пропускаем, если уже 5 фото
        }
      }
      
      if (mediaType === 'video') {
        const videoCount = query.get(
          'SELECT COUNT(*) as count FROM order_media WHERE order_id = ? AND media_type = ?',
          [id, 'video']
        );
        if (videoCount && videoCount.count >= 1) {
          continue; // Пропускаем, если уже есть видео
        }
      }
      
      // Формируем URL файла (для простоты используем относительный путь)
      const fileUrl = `/uploads/${file.filename}`;
      const filePath = join(__dirname, '..', 'uploads', file.filename);
      
      // Добавляем медиафайл в БД
      const result = query.run(`
        INSERT INTO order_media (
          order_id, media_type, file_path, file_url, file_name, file_size,
          mime_type, description, upload_order
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      `, [
        id,
        mediaType,
        filePath,
        fileUrl,
        file.originalname,
        file.size,
        file.mimetype,
        description || null,
        i
      ]);
      
      const mediaId = result.lastInsertRowid;
      const newMedia = query.get('SELECT * FROM order_media WHERE id = ?', [mediaId]);
      uploadedMedia.push(newMedia);
    }
    
    res.status(201).json({
      message: `Загружено файлов: ${uploadedMedia.length}`,
      media: uploadedMedia
    });
  } catch (error) {
    console.error('Ошибка загрузки медиафайлов:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Добавить медиафайл к заказу (только метаданные, для внешних URL)
router.post('/:id/media', authenticate, (req, res) => {
  try {
    const { id } = req.params;
    const {
      media_type: mediaType,
      file_path: filePath,
      file_url: fileUrl,
      file_name: fileName,
      file_size: fileSize,
      mime_type: mimeType,
      description,
      thumbnail_url: thumbnailUrl,
      duration,
      upload_order: uploadOrder = 0
    } = req.body;
    
    // Проверяем существование заказа и права доступа
    const order = query.get('SELECT * FROM orders WHERE id = ?', [id]);
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    if (req.user.role === 'client') {
      const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
      if (client && order.client_id !== client.id) {
        return res.status(403).json({ error: 'Доступ запрещен' });
      }
    }
    
    // Валидация
    if (!mediaType || (!filePath && !fileUrl)) {
      return res.status(400).json({ error: 'Не указаны media_type и file_path/file_url' });
    }
    
    // Проверка ограничений (максимум 5 фото, 1 видео до 60 секунд)
    if (mediaType === 'photo') {
      const photoCount = query.get(
        'SELECT COUNT(*) as count FROM order_media WHERE order_id = ? AND media_type = ?',
        [id, 'photo']
      );
      if (photoCount && photoCount.count >= 5) {
        return res.status(400).json({ error: 'Максимум 5 фотографий на заказ' });
      }
    }
    
    if (mediaType === 'video' && duration && duration > 60) {
      return res.status(400).json({ error: 'Видео должно быть не более 60 секунд' });
    }
    
    // Добавляем медиафайл
    const result = query.run(`
      INSERT INTO order_media (
        order_id, media_type, file_path, file_url, file_name, file_size,
        mime_type, description, thumbnail_url, duration, upload_order
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `, [
      id, mediaType, filePath || null, fileUrl || null, fileName || null,
      fileSize || null, mimeType || null, description || null,
      thumbnailUrl || null, duration || null, uploadOrder
    ]);
    
    const mediaId = result.lastInsertRowid;
    const newMedia = query.get('SELECT * FROM order_media WHERE id = ?', [mediaId]);
    
    res.status(201).json({
      message: 'Медиафайл успешно добавлен',
      media: newMedia
    });
  } catch (error) {
    console.error('Ошибка добавления медиафайла:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Удалить медиафайл
router.delete('/:id/media/:mediaId', authenticate, (req, res) => {
  try {
    const { id, mediaId } = req.params;
    
    // Проверяем существование заказа и права доступа
    const order = query.get('SELECT * FROM orders WHERE id = ?', [id]);
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    if (req.user.role === 'client') {
      const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
      if (client && order.client_id !== client.id) {
        return res.status(403).json({ error: 'Доступ запрещен' });
      }
    }
    
    const media = query.get('SELECT * FROM order_media WHERE id = ? AND order_id = ?', [mediaId, id]);
    if (!media) {
      return res.status(404).json({ error: 'Медиафайл не найден' });
    }
    
    query.run('DELETE FROM order_media WHERE id = ?', [mediaId]);
    
    res.json({ message: 'Медиафайл успешно удален' });
  } catch (error) {
    console.error('Ошибка удаления медиафайла:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить историю обращений клиента
router.get('/client/:clientId/history', authenticate, (req, res) => {
  try {
    const { clientId } = req.params;
    
    // Проверка прав доступа
    if (req.user.role === 'client') {
      const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
      if (!client || client.id !== parseInt(clientId)) {
        return res.status(403).json({ error: 'Доступ запрещен' });
      }
    }
    
    const history = query.all(`
      SELECT 
        h.*,
        o.order_number,
        o.device_type,
        o.device_brand,
        o.device_model,
        o.request_status,
        o.created_at as order_created_at
      FROM client_order_history h
      JOIN orders o ON h.order_id = o.id
      WHERE h.client_id = ?
      ORDER BY h.created_at DESC
    `, [clientId]);
    
    res.json(history);
  } catch (error) {
    console.error('Ошибка получения истории обращений:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить частые проблемы для типа техники
router.get('/common-problems/:deviceType', authenticate, (req, res) => {
  try {
    const { deviceType } = req.params;
    const { deviceBrand, deviceModel } = req.query;
    
    const params = [deviceType];
    let sql = `
      SELECT * FROM common_problems 
      WHERE device_type = ?
    `;
    
    if (deviceBrand) {
      sql += ' AND (device_brand = ? OR device_brand IS NULL)';
      params.push(deviceBrand);
    }
    
    if (deviceModel) {
      sql += ' AND (device_model = ? OR device_model IS NULL)';
      params.push(deviceModel);
    }
    
    sql += ' ORDER BY frequency_rating DESC LIMIT 10';
    
    const problems = query.all(sql, params);
    res.json(problems);
  } catch (error) {
    console.error('Ошибка получения частых проблем:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить мастера для заказа
router.get('/:id/master', authenticate, (req, res) => {
  try {
    const { id } = req.params;
    
    // Проверяем существование заказа и права доступа
    const order = query.get('SELECT * FROM orders WHERE id = ?', [id]);
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    if (req.user.role === 'client') {
      const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
      if (client && order.client_id !== client.id) {
        return res.status(403).json({ error: 'Доступ запрещен' });
      }
    }
    
    // Если мастер не назначен
    if (!order.assigned_master_id) {
      return res.status(404).json({ error: 'Мастер еще не назначен' });
    }
    
    // Получаем информацию о мастере
    const master = query.get(`
      SELECT 
        m.*,
        u.name, u.phone, u.email
      FROM masters m
      JOIN users u ON m.user_id = u.id
      WHERE m.id = ?
    `, [order.assigned_master_id]);
    
    if (!master) {
      return res.status(404).json({ error: 'Мастер не найден' });
    }
    
    master.specialization = JSON.parse(master.specialization || '[]');
    
    res.json(master);
  } catch (error) {
    console.error('Ошибка получения мастера для заказа:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Зафиксировать цену заказа (после согласования с мастером)
router.post('/:id/fix-price', authenticate, (req, res) => {
  try {
    const { id } = req.params;
    const { finalCost, reason } = req.body;
    
    if (!finalCost || finalCost <= 0) {
      return res.status(400).json({ error: 'Необходимо указать finalCost > 0' });
    }
    
    // Проверяем существование заказа и права доступа
    const order = query.get('SELECT * FROM orders WHERE id = ?', [id]);
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    // Проверка прав: клиент или мастер, назначенный на заказ
    if (req.user.role === 'client') {
      const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
      if (client && order.client_id !== client.id) {
        return res.status(403).json({ error: 'Доступ запрещен' });
      }
    } else if (req.user.role === 'master') {
      const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
      if (master && order.assigned_master_id !== master.id) {
        return res.status(403).json({ error: 'Доступ запрещен' });
      }
    }
    
    // Фиксируем цену
    const result = fixOrderPrice(id, finalCost, reason);
    
    res.json({
      message: 'Цена успешно зафиксирована',
      ...result
    });
  } catch (error) {
    console.error('Ошибка фиксации цены:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Получить данные чека для заказа
router.get('/:id/receipt', authenticate, (req, res) => {
  try {
    const { id } = req.params;
    const { paymentId } = req.query;
    
    // Проверяем существование заказа и права доступа
    const order = query.get('SELECT * FROM orders WHERE id = ?', [id]);
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    // Проверка прав: клиент или мастер, назначенный на заказ
    if (req.user.role === 'client') {
      const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
      if (client && order.client_id !== client.id) {
        return res.status(403).json({ error: 'Доступ запрещен' });
      }
    } else if (req.user.role === 'master') {
      const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
      if (master && order.assigned_master_id !== master.id) {
        return res.status(403).json({ error: 'Доступ запрещен' });
      }
    }
    
    // Если paymentId не указан, ищем последний успешный платеж
    let finalPaymentId = paymentId;
    if (!finalPaymentId) {
      const lastPayment = query.get(
        'SELECT id FROM payments WHERE order_id = ? AND payment_status = ? ORDER BY created_at DESC LIMIT 1',
        [id, 'completed']
      );
      if (lastPayment) {
        finalPaymentId = lastPayment.id;
      }
    }
    
    if (!finalPaymentId) {
      return res.status(404).json({ error: 'Платеж не найден для этого заказа' });
    }
    
    // Генерируем данные чека
    const receiptData = generateReceiptData(id, finalPaymentId);
    
    res.json(receiptData);
  } catch (error) {
    console.error('Ошибка генерации чека:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

export default router;

