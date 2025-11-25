import { query } from '../database/db.js';
import { config } from '../config.js';
import { broadcastToMaster } from '../websocket.js';
import { hasActivePromotion } from './promotion-service.js';

// Хранилище таймеров для назначений
const assignmentTimers = new Map();

// Вычисление расстояния между двумя точками (формула гаверсинуса)
function calculateDistance(lat1, lon1, lat2, lon2) {
  if (!lat1 || !lon1 || !lat2 || !lon2) return Infinity;
  
  const R = 6371000; // Радиус Земли в метрах
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = 
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c; // Расстояние в метрах
}

// Вычисление скоринга мастера для заказа (улучшенная версия с учетом подписок и продвижений)
function calculateMasterScore(master, orderLat, orderLon) {
  let score = 0;
  
  // 1. Рейтинг мастера (0-5) - вес 25%
  const ratingScore = (master.rating || 0) / 5 * 0.25; // Нормализуем к 0-1
  score += ratingScore;
  
  // 2. Опыт мастера (количество завершенных заказов) - вес 15%
  const completedOrders = master.completed_orders || 0;
  // Нормализуем: 0 заказов = 0, 100+ заказов = 1
  const experienceScore = Math.min(1, completedOrders / 100) * 0.15;
  score += experienceScore;
  
  // 3. Расстояние до клиента - вес 25%
  if (orderLat && orderLon && master.latitude && master.longitude) {
    const distance = calculateDistance(
      master.latitude, master.longitude,
      orderLat, orderLon
    );
    // Нормализуем расстояние: чем ближе, тем выше score
    // Максимальное расстояние 50 км = 0, минимальное 0 км = 1
    const maxDistance = 50000; // 50 км
    const distanceScore = Math.max(0, 1 - (distance / maxDistance)) * 0.25;
    score += distanceScore;
    master.distance = Math.round(distance); // Сохраняем расстояние для отладки
  } else {
    // Если нет координат, даем средний балл
    score += 0.125; // половина от 0.25
  }
  
  // 4. Загрузка мастера (количество активных заказов) - вес 15%
  // Чем меньше активных заказов, тем выше score
  const activeOrders = master.active_orders_count || 0;
  const maxActiveOrders = 5; // Максимальное количество активных заказов
  const loadScore = Math.max(0, 1 - (activeOrders / maxActiveOrders)) * 0.15;
  score += loadScore;
  
  // 5. Премиум подписка - бонус 10%
  if (master.subscription_type === 'premium') {
    score += 0.10;
    master.hasPremium = true;
  }
  
  // 6. Продвижения - бонусы
  let promotionBonus = 0;
  if (master.has_top_listing) {
    promotionBonus += 0.05; // Топ в выдаче +5%
  }
  if (master.has_highlighted) {
    promotionBonus += 0.03; // Выделенный профиль +3%
  }
  if (master.has_featured) {
    promotionBonus += 0.07; // Рекомендуемый мастер +7%
  }
  score += Math.min(promotionBonus, 0.10); // Максимальный бонус от продвижений 10%
  
  // Нормализуем score к максимуму 1.0 (хотя может быть больше из-за бонусов)
  return Math.min(score, 1.2); // Разрешаем до 1.2 для учета всех бонусов
}

// Находим подходящих мастеров для заказа с умным подбором
export function findAvailableMasters(deviceType, orderLat = null, orderLon = null) {
  try {
    // Получаем мастеров с информацией о загрузке, опыте, подписках и продвижениях
    const masters = query.all(`
      SELECT 
        m.id, m.user_id, m.specialization, m.latitude, m.longitude, m.rating, m.completed_orders,
        m.subscription_type,
        u.name, u.phone,
        COUNT(DISTINCT CASE 
          WHEN o.repair_status IN ('new', 'in_progress', 'diagnostics', 'waiting_parts') 
          THEN o.id 
        END) as active_orders_count,
        -- Проверяем активные продвижения
        MAX(CASE WHEN mp1.promotion_type = 'top_listing' AND mp1.status = 'active' AND mp1.expires_at > datetime('now') THEN 1 ELSE 0 END) as has_top_listing,
        MAX(CASE WHEN mp2.promotion_type = 'highlighted_profile' AND mp2.status = 'active' AND mp2.expires_at > datetime('now') THEN 1 ELSE 0 END) as has_highlighted,
        MAX(CASE WHEN mp3.promotion_type = 'featured' AND mp3.status = 'active' AND mp3.expires_at > datetime('now') THEN 1 ELSE 0 END) as has_featured
      FROM masters m
      JOIN users u ON m.user_id = u.id
      LEFT JOIN orders o ON o.assigned_master_id = m.id
      LEFT JOIN master_promotions mp1 ON mp1.master_id = m.id AND mp1.promotion_type = 'top_listing'
      LEFT JOIN master_promotions mp2 ON mp2.master_id = m.id AND mp2.promotion_type = 'highlighted_profile'
      LEFT JOIN master_promotions mp3 ON mp3.master_id = m.id AND mp3.promotion_type = 'featured'
      WHERE m.is_on_shift = 1 AND m.status = 'available'
      GROUP BY m.id, m.user_id, m.specialization, m.latitude, m.longitude, m.rating, m.completed_orders, m.subscription_type, u.name, u.phone
    `);
    
    // Фильтруем по специализации
    const filteredMasters = masters.filter(master => {
      const specializations = JSON.parse(master.specialization || '[]');
      return specializations.includes(deviceType);
    });
    
    // Вычисляем скоринг для каждого мастера
    const scoredMasters = filteredMasters.map(master => {
      const score = calculateMasterScore(master, orderLat, orderLon);
      
      // Формируем информацию о бонусах для логирования
      const bonuses = [];
      if (master.subscription_type === 'premium') bonuses.push('Premium');
      if (master.has_top_listing) bonuses.push('Top');
      if (master.has_highlighted) bonuses.push('Highlighted');
      if (master.has_featured) bonuses.push('Featured');
      
      return {
        ...master,
        score: score,
        bonuses: bonuses.join(', ') || 'None'
      };
    });
    
    // Сортируем по score (от большего к меньшему)
    scoredMasters.sort((a, b) => b.score - a.score);
    
    console.log(`📊 Найдено ${scoredMasters.length} подходящих мастеров для ${deviceType}`);
    if (scoredMasters.length > 0) {
      console.log(`🏆 Топ-3 мастера:`, scoredMasters.slice(0, 3).map(m => 
        `#${m.id} (score: ${m.score.toFixed(3)}, rating: ${m.rating}, completed: ${m.completed_orders || 0}, distance: ${m.distance || 'N/A'}м, active: ${m.active_orders_count}, bonuses: [${m.bonuses}])`
      ));
    }
    
    return scoredMasters;
  } catch (error) {
    console.error('Ошибка поиска мастеров:', error);
    return [];
  }
}

// Вычисляем таймаут на основе номера попытки
function calculateTimeout(attemptNumber) {
  const baseTimeout = 5 * 60 * 1000; // 5 минут
  switch (attemptNumber) {
    case 1:
      return baseTimeout; // 5 минут
    case 2:
      return 7 * 60 * 1000; // 7 минут
    case 3:
      return 10 * 60 * 1000; // 10 минут
    default:
      return 15 * 60 * 1000; // 15 минут для 4+ попыток
  }
}

// Создаем назначение для мастера
export function createAssignment(orderId, masterId, attemptNumber = 1) {
  try {
    const timeout = calculateTimeout(attemptNumber);
    const expiresAt = new Date(Date.now() + timeout);
    
    const result = query.run(`
      INSERT INTO order_assignments (order_id, master_id, status, expires_at, attempt_number)
      VALUES (?, ?, 'pending', ?, ?)
    `, [orderId, masterId, expiresAt.toISOString(), attemptNumber]);
    
    const assignmentId = result.lastInsertRowid;
    
    // Получаем полную информацию о назначении
    const assignment = query.get(`
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
      WHERE oa.id = ?
    `, [assignmentId]);
    
    // Отправляем уведомление мастеру через WebSocket
    const master = query.get('SELECT user_id FROM masters WHERE id = ?', [masterId]);
    if (master) {
      broadcastToMaster(master.user_id, {
        type: 'new_assignment',
        assignment
      });
    }
    
    // Устанавливаем таймер для истечения времени
    const timer = setTimeout(() => {
      handleAssignmentExpiration(assignmentId, orderId);
    }, timeout);
    
    assignmentTimers.set(assignmentId, timer);
    
    const timeoutMinutes = Math.round(timeout / 60000);
    console.log(`✅ Создано назначение #${assignmentId} для заказа #${orderId} мастеру #${masterId} (попытка ${attemptNumber}, таймаут ${timeoutMinutes} мин)`);
    
    return assignment;
  } catch (error) {
    console.error('Ошибка создания назначения:', error);
    return null;
  }
}

// Обработка истечения времени назначения
export function handleAssignmentExpiration(assignmentId, orderId) {
  try {
    // Проверяем статус назначения
    const assignment = query.get('SELECT status FROM order_assignments WHERE id = ?', [assignmentId]);
    
    if (!assignment) {
      console.log(`Назначение #${assignmentId} не найдено`);
      return;
    }
    
    if (assignment.status === 'pending') {
      // Время истекло, обновляем статус
      query.run(
        'UPDATE order_assignments SET status = ? WHERE id = ?',
        ['expired', assignmentId]
      );
      
      console.log(`⏱️ Время ответа истекло для назначения #${assignmentId}`);
      
      // Ищем следующего мастера
      findNextMaster(orderId);
    }
    
    // Удаляем таймер
    assignmentTimers.delete(assignmentId);
  } catch (error) {
    console.error('Ошибка обработки истечения назначения:', error);
  }
}

// Ищем следующего доступного мастера для заказа (с поддержкой попыток)
export function findNextMaster(orderId) {
  try {
    // Получаем заказ с координатами
    const order = query.get('SELECT * FROM orders WHERE id = ?', [orderId]);
    if (!order || order.repair_status !== 'new') {
      console.log(`Заказ #${orderId} уже не новый или не найден`);
      return;
    }
    
    // Получаем всех мастеров, которым уже отправляли этот заказ, и максимальный номер попытки
    const previousAssignments = query.all(
      'SELECT master_id, attempt_number FROM order_assignments WHERE order_id = ?',
      [orderId]
    );
    
    const excludedMasterIds = previousAssignments.map(a => a.master_id);
    const maxAttempt = previousAssignments.length > 0 
      ? Math.max(...previousAssignments.map(a => a.attempt_number || 1))
      : 0;
    
    // Вычисляем номер следующей попытки
    const nextAttemptNumber = maxAttempt + 1;
    
    // Находим доступных мастеров с учетом координат заказа
    const availableMasters = findAvailableMasters(
      order.device_type, 
      order.latitude, 
      order.longitude
    );
    
    // Фильтруем уже уведомленных мастеров
    const nextMasters = availableMasters.filter(m => !excludedMasterIds.includes(m.id));
    
    if (nextMasters.length === 0) {
      console.log(`❌ Нет доступных мастеров для заказа #${orderId} (попытка ${nextAttemptNumber})`);
      
      // Если это уже 5+ попытка, отменяем заказ
      if (nextAttemptNumber >= 5) {
        console.log(`🚫 Заказ #${orderId} отменен после ${nextAttemptNumber} попыток`);
        
        // Обновляем статус заказа
        query.run(
          'UPDATE orders SET repair_status = ? WHERE id = ?',
          ['cancelled', orderId]
        );
        
        query.run(
          'INSERT INTO order_status_history (order_id, old_status, new_status, note) VALUES (?, ?, ?, ?)',
          [orderId, 'new', 'cancelled', `Нет доступных мастеров после ${nextAttemptNumber} попыток`]
        );
      }
      
      return;
    }
    
    // Назначаем лучшему доступному мастеру (с наивысшим score)
    const nextMaster = nextMasters[0];
    console.log(`🔄 Назначаем заказ #${orderId} следующему мастеру #${nextMaster.id} (попытка ${nextAttemptNumber}, score: ${nextMaster.score.toFixed(3)})`);
    createAssignment(orderId, nextMaster.id, nextAttemptNumber);
  } catch (error) {
    console.error('Ошибка поиска следующего мастера:', error);
  }
}

// Запуск процесса назначения заказа мастерам
export function notifyMasters(orderId, deviceType, orderLat = null, orderLon = null) {
  try {
    console.log(`🚀 Запуск процесса назначения для заказа #${orderId} (${deviceType})`);
    
    // Находим доступных мастеров с учетом координат заказа
    const availableMasters = findAvailableMasters(deviceType, orderLat, orderLon);
    
    if (availableMasters.length === 0) {
      console.log(`❌ Нет доступных мастеров для заказа #${orderId}`);
      return;
    }
    
    // Назначаем лучшему мастеру из списка (с наивысшим score)
    const bestMaster = availableMasters[0];
    const bonusInfo = bestMaster.bonuses ? `, bonuses: [${bestMaster.bonuses}]` : '';
    console.log(`✅ Выбран мастер #${bestMaster.id} (score: ${bestMaster.score.toFixed(3)}${bonusInfo})`);
    createAssignment(orderId, bestMaster.id);
  } catch (error) {
    console.error('Ошибка уведомления мастеров:', error);
  }
}

// Отмена всех назначений для заказа
export function cancelAssignments(orderId) {
  try {
    // Получаем все pending назначения
    const pendingAssignments = query.all(
      'SELECT id FROM order_assignments WHERE order_id = ? AND status = ?',
      [orderId, 'pending']
    );
    
    // Отменяем все таймеры
    pendingAssignments.forEach(assignment => {
      const timer = assignmentTimers.get(assignment.id);
      if (timer) {
        clearTimeout(timer);
        assignmentTimers.delete(assignment.id);
      }
    });
    
    // Обновляем статус всех pending назначений
    query.run(
      'UPDATE order_assignments SET status = ? WHERE order_id = ? AND status = ?',
      ['expired', orderId, 'pending']
    );
    
    console.log(`🚫 Отменены все назначения для заказа #${orderId}`);
  } catch (error) {
    console.error('Ошибка отмены назначений:', error);
  }
}

export default {
  findAvailableMasters,
  createAssignment,
  handleAssignmentExpiration,
  findNextMaster,
  notifyMasters,
  cancelAssignments
};





