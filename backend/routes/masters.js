import express from 'express';
import { query } from '../database/db.js';
import { authenticate, authorize } from '../middleware/auth.js';
import { upload, handleUploadError } from '../middleware/upload.js';
import multer from 'multer';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';
import { existsSync } from 'fs';
import { withCache, CacheKeys, invalidateMastersCache } from '../services/cache-service.js';
import { findNearestMasters, formatDistance, formatArrivalTime } from '../services/distance-service.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const router = express.Router();

// Получить всех мастеров
router.get('/', authenticate, async (req, res) => {
  try {
    const { specialization, status, isOnShift, latitude, longitude, radius, limit, offset } = req.query;
    
    // Создаем ключ кэша на основе параметров запроса
    const cacheKey = `masters:${JSON.stringify({ specialization, status, isOnShift, latitude, longitude, radius, limit, offset })}`;
    
    // Пытаемся получить из кэша (только для базовых запросов без координат)
    const cached = latitude && longitude ? null : await withCache(cacheKey, async () => {
      let sql = `
        SELECT 
          m.*,
          u.name, u.phone, u.email
        FROM masters m
        JOIN users u ON m.user_id = u.id
        WHERE 1=1
      `;
      const params = [];
      
      if (status) {
        sql += ' AND m.status = ?';
        params.push(status);
      }
      
      if (isOnShift !== undefined) {
        sql += ' AND m.is_on_shift = ?';
        params.push(isOnShift === 'true' ? 1 : 0);
      }
      
      // Фильтр по наличию координат для поиска ближайших
      if (latitude && longitude) {
        sql += ' AND m.latitude IS NOT NULL AND m.longitude IS NOT NULL';
      }
      
      sql += ' ORDER BY m.rating DESC';
      
      // Пагинация
      if (limit) {
        sql += ' LIMIT ?';
        params.push(parseInt(limit));
        if (offset) {
          sql += ' OFFSET ?';
          params.push(parseInt(offset));
        }
      }
      
      return query.all(sql, params);
    }, 300); // Кэш на 5 минут
    
    let masters = cached || (() => {
      let sql = `
        SELECT 
          m.*,
          u.name, u.phone, u.email
        FROM masters m
        JOIN users u ON m.user_id = u.id
        WHERE 1=1
      `;
      const params = [];
      
      if (status) {
        sql += ' AND m.status = ?';
        params.push(status);
      }
      
      if (isOnShift !== undefined) {
        sql += ' AND m.is_on_shift = ?';
        params.push(isOnShift === 'true' ? 1 : 0);
      }
      
      // Фильтр по наличию координат для поиска ближайших
      if (latitude && longitude) {
        sql += ' AND m.latitude IS NOT NULL AND m.longitude IS NOT NULL';
      }
      
      sql += ' ORDER BY m.rating DESC';
      
      // Пагинация
      if (limit) {
        sql += ' LIMIT ?';
        params.push(parseInt(limit));
        if (offset) {
          sql += ' OFFSET ?';
          params.push(parseInt(offset));
        }
      }
      
      return query.all(sql, params);
    })();
    
    // Парсим JSON specialization
    masters = masters.map(m => ({
      ...m,
      specialization: JSON.parse(m.specialization || '[]')
    }));
    
    // Фильтр по специализации (после парсинга JSON)
    if (specialization) {
      masters = masters.filter(m => m.specialization.includes(specialization));
    }
    
    // Вычисляем расстояние и фильтруем по радиусу
    if (latitude && longitude && masters.length > 0) {
      const clientLat = parseFloat(latitude);
      const clientLon = parseFloat(longitude);
      const maxRadius = radius ? parseFloat(radius) : 20000; // По умолчанию 20км
      
      // Используем сервис для поиска ближайших мастеров
      const nearestMasters = findNearestMasters(masters, clientLat, clientLon, maxRadius, limit ? parseInt(limit) : 10);
      
      // Добавляем форматированные значения для удобства
      const mastersWithInfo = nearestMasters.map(m => ({
        ...m,
        distanceFormatted: formatDistance(m.distance),
        arrivalTimeFormatted: formatArrivalTime(m.estimatedArrivalTime)
      }));
      
      res.json(mastersWithInfo);
    } else {
      res.json(masters);
    }
  } catch (error) {
    console.error('Ошибка получения мастеров:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Функция для вычисления расстояния между двумя точками (формула гаверсинуса)
function calculateDistance(lat1, lon1, lat2, lon2) {
  const R = 6371000; // Радиус Земли в метрах
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function toRad(degrees) {
  return degrees * (Math.PI / 180);
}

// ============= Кошелек мастера =============
// ВАЖНО: Эти роуты должны быть ПЕРЕД роутом /:id, иначе /:id перехватит их

// Получить баланс и информацию о кошельке
router.get('/wallet', authenticate, (req, res) => {
  console.log(`[WALLET] ✅ Route handler called! user_id=${req.user?.id}, role=${req.user?.role}`);
  if (!req.user || req.user.role !== 'master') {
    console.log(`[WALLET] Access denied: user=${req.user?.id}, role=${req.user?.role}`);
    return res.status(403).json({ error: 'Недостаточно прав доступа' });
  }
  try {
    const master = query.get('SELECT id, balance FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      console.log(`[WALLET] Master profile not found for user_id=${req.user.id}`);
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    console.log(`[WALLET] Master found: id=${master.id}, balance=${master.balance}`);
    
    // Получаем статистику транзакций (с проверкой на существование таблицы)
    let pendingPayouts = { total: 0 };
    let totalEarned = { total: 0 };
    let totalPayouts = { total: 0 };
    
    try {
      const pendingResult = query.get(`
        SELECT COALESCE(SUM(amount), 0) as total 
        FROM master_transactions 
        WHERE master_id = ? AND transaction_type = 'payout' AND status = 'pending'
      `, [master.id]);
      pendingPayouts = pendingResult || { total: 0 };
      
      const earnedResult = query.get(`
        SELECT COALESCE(SUM(amount), 0) as total 
        FROM master_transactions 
        WHERE master_id = ? AND transaction_type = 'income' AND status = 'completed'
      `, [master.id]);
      totalEarned = earnedResult || { total: 0 };
      
      const payoutsResult = query.get(`
        SELECT COALESCE(SUM(amount), 0) as total 
        FROM master_transactions 
        WHERE master_id = ? AND transaction_type = 'payout' AND status = 'completed'
      `, [master.id]);
      totalPayouts = payoutsResult || { total: 0 };
    } catch (tableError) {
      // Таблица может не существовать, используем значения по умолчанию
      console.log('Таблица master_transactions не найдена, используем значения по умолчанию');
    }
    
    res.json({
      balance: master.balance || 0,
      pendingPayouts: pendingPayouts.total || 0,
      totalEarned: totalEarned.total || 0,
      totalPayouts: totalPayouts.total || 0,
      availableForPayout: (master.balance || 0) - (pendingPayouts.total || 0)
    });
  } catch (error) {
    console.error('Ошибка получения кошелька:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Получить статистику мастера
router.get('/stats/me', authenticate, authorize('master'), (req, res) => {
  try {
    console.log(`[GET /api/masters/stats/me] User ID: ${req.user.id}, Role: ${req.user.role}`);
    
    // Получаем данные мастера с информацией о пользователе
    const master = query.get(`
      SELECT 
        m.*,
        u.name, u.phone, u.email, u.email_verified, u.phone_verified
      FROM masters m
      JOIN users u ON m.user_id = u.id
      WHERE m.user_id = ?
    `, [req.user.id]);
    
    if (!master) {
      console.error(`[GET /api/masters/stats/me] Master not found for user_id: ${req.user.id}`);
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    console.log(`[GET /api/masters/stats/me] Master found: id=${master.id}, name=${master.name}`);
    
    // Получаем статистику заказов с обработкой ошибок
    let totalOrders = null;
    let completedOrders = null;
    let inProgressOrders = null;
    let avgRating = null;
    
    try {
      totalOrders = query.get(
        'SELECT COUNT(*) as count FROM orders WHERE assigned_master_id = ?',
        [master.id]
      );
    } catch (e) {
      console.error(`[GET /api/masters/stats/me] Error getting totalOrders:`, e);
      totalOrders = { count: 0 };
    }
    
    try {
      completedOrders = query.get(
        'SELECT COUNT(*) as count FROM orders WHERE assigned_master_id = ? AND repair_status = ?',
        [master.id, 'completed']
      );
    } catch (e) {
      console.error(`[GET /api/masters/stats/me] Error getting completedOrders:`, e);
      completedOrders = { count: 0 };
    }
    
    try {
      inProgressOrders = query.get(
        'SELECT COUNT(*) as count FROM orders WHERE assigned_master_id = ? AND repair_status = ?',
        [master.id, 'in_progress']
      );
    } catch (e) {
      console.error(`[GET /api/masters/stats/me] Error getting inProgressOrders:`, e);
      inProgressOrders = { count: 0 };
    }
    
    // Получаем средний рейтинг из отзывов
    try {
      avgRating = query.get(
        'SELECT AVG(rating) as avg, COUNT(*) as count FROM reviews WHERE master_id = ?',
        [master.id]
      );
    } catch (e) {
      console.error(`[GET /api/masters/stats/me] Error getting avgRating:`, e);
      avgRating = { avg: 0, count: 0 };
    }
    
    // Получаем статистику за сегодня
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const todayStart = today.toISOString();
    
    let todayOrders = null;
    let todayRevenue = null;
    
    try {
      todayOrders = query.get(
        `SELECT COUNT(*) as count FROM orders 
         WHERE assigned_master_id = ? 
         AND (created_at >= ? OR updated_at >= ?)`,
        [master.id, todayStart, todayStart]
      );
    } catch (e) {
      console.error(`[GET /api/masters/stats/me] Error getting todayOrders:`, e);
      todayOrders = { count: 0 };
    }
    
    try {
      todayRevenue = query.get(
        `SELECT COALESCE(SUM(final_cost), 0) as total FROM orders 
         WHERE assigned_master_id = ? 
         AND repair_status = 'completed' 
         AND completed_at >= ?`,
        [master.id, todayStart]
      );
    } catch (e) {
      console.error(`[GET /api/masters/stats/me] Error getting todayRevenue:`, e);
      todayRevenue = { total: 0 };
    }
    
    // Получаем доходы за последние 7 дней
    const weeklyRevenue = [];
    for (let i = 6; i >= 0; i--) {
      try {
        const date = new Date(today);
        date.setDate(date.getDate() - i);
        const dayStart = new Date(date.setHours(0, 0, 0, 0)).toISOString();
        const dayEnd = new Date(date.setHours(23, 59, 59, 999)).toISOString();
        
        const dayRevenue = query.get(
          `SELECT COALESCE(SUM(final_cost), 0) as total FROM orders 
           WHERE assigned_master_id = ? 
           AND repair_status = 'completed' 
           AND completed_at >= ? AND completed_at <= ?`,
          [master.id, dayStart, dayEnd]
        );
        
        weeklyRevenue.push((dayRevenue && dayRevenue.total != null) ? dayRevenue.total : 0);
      } catch (e) {
        console.error(`[GET /api/masters/stats/me] Error getting revenue for day ${i}:`, e);
        weeklyRevenue.push(0);
      }
    }
    
    // Получаем новые заказы (не назначенные)
    let newOrders = null;
    let uniqueClients = null;
    let monthlyRevenue = null;
    
    try {
      newOrders = query.get(
        'SELECT COUNT(*) as count FROM orders WHERE request_status = ?',
        ['new']
      );
    } catch (e) {
      console.error(`[GET /api/masters/stats/me] Error getting newOrders:`, e);
      newOrders = { count: 0 };
    }
    
    // Получаем количество уникальных клиентов
    try {
      uniqueClients = query.get(
        'SELECT COUNT(DISTINCT client_id) as count FROM orders WHERE assigned_master_id = ?',
        [master.id]
      );
    } catch (e) {
      console.error(`[GET /api/masters/stats/me] Error getting uniqueClients:`, e);
      uniqueClients = { count: 0 };
    }
    
    // Получаем доход за текущий месяц
    try {
      const monthStart = new Date(today.getFullYear(), today.getMonth(), 1).toISOString();
      monthlyRevenue = query.get(
        `SELECT COALESCE(SUM(final_cost), 0) as total FROM orders 
         WHERE assigned_master_id = ? 
         AND repair_status = 'completed' 
         AND completed_at >= ?`,
        [master.id, monthStart]
      );
    } catch (e) {
      console.error(`[GET /api/masters/stats/me] Error getting monthlyRevenue:`, e);
      monthlyRevenue = { total: 0 };
    }
    
    // Парсим специализацию
    let specialization = [];
    try {
      specialization = JSON.parse(master.specialization || '[]');
    } catch (e) {
      specialization = [];
    }
    
    res.json({
      master: {
        id: master.id,
        name: master.name || '',
        email: master.email || '',
        phone: master.phone || '',
        rating: master.rating || 0,
        completedOrders: master.completed_orders || 0,
        isOnShift: master.is_on_shift === 1,
        status: master.status || 'offline',
        specialization: specialization,
        verificationStatus: master.verification_status || 'not_verified',
        photoUrl: master.photo_url || null,
        emailVerified: master.email_verified === 1,
        phoneVerified: master.phone_verified === 1
      },
      stats: {
        totalOrders: (totalOrders && totalOrders.count) ? totalOrders.count : 0,
        completedOrders: (completedOrders && completedOrders.count) ? completedOrders.count : 0,
        inProgressOrders: (inProgressOrders && inProgressOrders.count) ? inProgressOrders.count : 0,
        averageRating: (avgRating && avgRating.avg != null) ? avgRating.avg : 0,
        reviewsCount: (avgRating && avgRating.count) ? avgRating.count : 0,
        todayOrders: (todayOrders && todayOrders.count) ? todayOrders.count : 0,
        todayRevenue: (todayRevenue && todayRevenue.total != null) ? todayRevenue.total : 0,
        weeklyRevenue: weeklyRevenue,
        newOrders: (newOrders && newOrders.count) ? newOrders.count : 0,
        clientsCount: (uniqueClients && uniqueClients.count) ? uniqueClients.count : 0,
        monthlyRevenue: (monthlyRevenue && monthlyRevenue.total != null) ? monthlyRevenue.total : 0
      }
    });
  } catch (error) {
    console.error('[GET /api/masters/stats/me] Ошибка получения статистики:', error);
    console.error('[GET /api/masters/stats/me] Stack trace:', error.stack);
    console.error('[GET /api/masters/stats/me] User ID:', req.user?.id);
    
    // Более детальная обработка ошибок
    let errorMessage = 'Ошибка сервера при получении статистики';
    let statusCode = 500;
    
    if (error.message && error.message.includes('no such column')) {
      errorMessage = `Ошибка базы данных: ${error.message}. Возможно, требуется миграция.`;
      statusCode = 500;
    } else if (error.message && error.message.includes('SQLITE')) {
      errorMessage = 'Ошибка базы данных. Попробуйте позже.';
      statusCode = 500;
    }
    
    res.status(statusCode).json({ 
      error: errorMessage,
      details: error.message,
      stack: process.env.NODE_ENV === 'development' ? error.stack : undefined
    });
  }
});

// ============= Расписание мастера =============
// ВАЖНО: Эти роуты должны быть ПЕРЕД роутом /:id, иначе /:id перехватит их

// Получить расписание мастера
router.get('/schedule', authenticate, authorize('master'), (req, res) => {
  try {
    const { startDate, endDate } = req.query; // Формат: YYYY-MM-DD
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    let sql = 'SELECT * FROM master_schedule WHERE master_id = ?';
    const params = [master.id];
    
    if (startDate && endDate) {
      sql += ' AND date >= ? AND date <= ?';
      params.push(startDate, endDate);
    } else if (startDate) {
      sql += ' AND date >= ?';
      params.push(startDate);
    }
    
    sql += ' ORDER BY date ASC';
    
    const schedule = query.all(sql, params);
    
    res.json({
      schedule: schedule.map(item => ({
        id: item.id,
        date: item.date,
        startTime: item.start_time,
        endTime: item.end_time,
        isAvailable: item.is_available === 1,
        note: item.note
      }))
    });
  } catch (error) {
    console.error('Ошибка получения расписания:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Создать или обновить расписание на дату
router.post('/schedule', authenticate, authorize('master'), (req, res) => {
  try {
    const { date, startTime, endTime, isAvailable, note } = req.body;
    
    if (!date) {
      return res.status(400).json({ error: 'Необходимо указать дату' });
    }
    
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    // Проверяем, существует ли уже запись на эту дату
    const existing = query.get(
      'SELECT id FROM master_schedule WHERE master_id = ? AND date = ?',
      [master.id, date]
    );
    
    if (existing) {
      // Обновляем существующую запись
      query.run(`
        UPDATE master_schedule 
        SET start_time = ?, end_time = ?, is_available = ?, note = ?, updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
      `, [
        startTime || null,
        endTime || null,
        isAvailable !== undefined ? (isAvailable ? 1 : 0) : 1,
        note || null,
        existing.id
      ]);
      
      const updated = query.get('SELECT * FROM master_schedule WHERE id = ?', [existing.id]);
      
      res.json({
        message: 'Расписание обновлено',
        schedule: {
          id: updated.id,
          date: updated.date,
          startTime: updated.start_time,
          endTime: updated.end_time,
          isAvailable: updated.is_available === 1,
          note: updated.note
        }
      });
    } else {
      // Создаем новую запись
      const result = query.run(`
        INSERT INTO master_schedule (master_id, date, start_time, end_time, is_available, note)
        VALUES (?, ?, ?, ?, ?, ?)
      `, [
        master.id,
        date,
        startTime || null,
        endTime || null,
        isAvailable !== undefined ? (isAvailable ? 1 : 0) : 1,
        note || null
      ]);
      
      const created = query.get('SELECT * FROM master_schedule WHERE id = ?', [result.lastInsertRowid]);
      
      res.status(201).json({
        message: 'Расписание создано',
        schedule: {
          id: created.id,
          date: created.date,
          startTime: created.start_time,
          endTime: created.end_time,
          isAvailable: created.is_available === 1,
          note: created.note
        }
      });
    }
  } catch (error) {
    console.error('Ошибка создания/обновления расписания:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Удалить расписание на дату
router.delete('/schedule/:date', authenticate, authorize('master'), (req, res) => {
  try {
    const { date } = req.params;
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    query.run(
      'DELETE FROM master_schedule WHERE master_id = ? AND date = ?',
      [master.id, date]
    );
    
    res.json({ message: 'Расписание удалено' });
  } catch (error) {
    console.error('Ошибка удаления расписания:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Массовое создание расписания (на неделю/месяц)
router.post('/schedule/batch', authenticate, authorize('master'), (req, res) => {
  try {
    const { startDate, endDate, startTime, endTime, isAvailable, daysOfWeek } = req.body;
    // daysOfWeek: [0,1,2,3,4,5,6] где 0=воскресенье, 1=понедельник и т.д.
    
    if (!startDate || !endDate) {
      return res.status(400).json({ error: 'Необходимо указать начальную и конечную дату' });
    }
    
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const created = [];
    const updated = [];
    
    // Генерируем даты в диапазоне
    const start = new Date(startDate);
    const end = new Date(endDate);
    
    for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
      const dateStr = d.toISOString().split('T')[0];
      const dayOfWeek = d.getDay();
      
      // Если указаны дни недели, пропускаем дни, которые не в списке
      if (daysOfWeek && daysOfWeek.length > 0 && !daysOfWeek.includes(dayOfWeek)) {
        continue;
      }
      
      const existing = query.get(
        'SELECT id FROM master_schedule WHERE master_id = ? AND date = ?',
        [master.id, dateStr]
      );
      
      if (existing) {
        query.run(`
          UPDATE master_schedule 
          SET start_time = ?, end_time = ?, is_available = ?, updated_at = CURRENT_TIMESTAMP
          WHERE id = ?
        `, [
          startTime || null,
          endTime || null,
          isAvailable !== undefined ? (isAvailable ? 1 : 0) : 1,
          existing.id
        ]);
        updated.push(dateStr);
      } else {
        query.run(`
          INSERT INTO master_schedule (master_id, date, start_time, end_time, is_available)
          VALUES (?, ?, ?, ?, ?)
        `, [
          master.id,
          dateStr,
          startTime || null,
          endTime || null,
          isAvailable !== undefined ? (isAvailable ? 1 : 0) : 1
        ]);
        created.push(dateStr);
      }
    }
    
    res.json({
      message: `Расписание создано/обновлено для ${created.length + updated.length} дней`,
      created: created.length,
      updated: updated.length
    });
  } catch (error) {
    console.error('Ошибка массового создания расписания:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Получить мастера по ID
router.get('/:id', authenticate, (req, res) => {
  try {
    const { id } = req.params;
    
    const master = query.get(`
      SELECT 
        m.*,
        u.name, u.phone, u.email
      FROM masters m
      JOIN users u ON m.user_id = u.id
      WHERE m.id = ?
    `, [id]);
    
    if (!master) {
      return res.status(404).json({ error: 'Мастер не найден' });
    }
    
    master.specialization = JSON.parse(master.specialization || '[]');
    
    // Получаем портфолио и сертификаты
    const portfolio = query.all('SELECT * FROM master_portfolio WHERE master_id = ? ORDER BY order_index ASC', [id]);
    const certificates = query.all('SELECT * FROM master_certificates WHERE master_id = ? ORDER BY order_index ASC', [id]);
    
    res.json({
      ...master,
      portfolio: portfolio,
      certificates: certificates
    });
  } catch (error) {
    console.error('Ошибка получения мастера:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Обновить профиль мастера
router.put('/profile', authenticate, authorize('master'), (req, res) => {
  try {
    const { specialization, latitude, longitude, bio, experience_years, photo_url } = req.body;
    
    console.log(`[PUT /api/masters/profile] user_id=${req.user.id}`);
    console.log(`[PUT /api/masters/profile] specialization=`, specialization);
    
    // Получаем ID мастера
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const updateFields = [];
    const updateValues = [];
    
    if (specialization !== undefined) {
      const specsJson = JSON.stringify(specialization);
      updateFields.push('specialization = ?');
      updateValues.push(specsJson);
      console.log(`[PUT /api/masters/profile] Saving specialization for master #${master.id}:`, specsJson);
    }
    
    if (latitude !== undefined) {
      updateFields.push('latitude = ?');
      updateValues.push(latitude);
    }
    
    if (longitude !== undefined) {
      updateFields.push('longitude = ?');
      updateValues.push(longitude);
    }
    
    if (bio !== undefined) {
      updateFields.push('bio = ?');
      updateValues.push(bio);
    }
    
    if (experience_years !== undefined) {
      updateFields.push('experience_years = ?');
      updateValues.push(experience_years);
    }
    
    if (photo_url !== undefined) {
      updateFields.push('photo_url = ?');
      updateValues.push(photo_url);
    }
    
    if (updateFields.length === 0) {
      return res.status(400).json({ error: 'Нет полей для обновления' });
    }
    
    updateFields.push('updated_at = CURRENT_TIMESTAMP');
    updateValues.push(master.id);
    
    query.run(
      `UPDATE masters SET ${updateFields.join(', ')} WHERE id = ?`,
      updateValues
    );
    
    // Получаем обновленные данные
    const updated = query.get('SELECT * FROM masters WHERE id = ?', [master.id]);
    updated.specialization = JSON.parse(updated.specialization || '[]');
    
    res.json({
      message: 'Профиль успешно обновлен',
      master: updated
    });
  } catch (error) {
    console.error('Ошибка обновления профиля мастера:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Загрузить фото профиля мастера
router.post('/profile/photo', authenticate, authorize('master'), (req, res, next) => {
  console.log('[POST /api/masters/profile/photo] Запрос на загрузку фото профиля');
  console.log('[POST /api/masters/profile/photo] Headers:', JSON.stringify(req.headers, null, 2));
  console.log('[POST /api/masters/profile/photo] Content-Type:', req.headers['content-type']);
  next();
}, upload.single('photo'), (err, req, res, next) => {
  if (err) {
    console.error('[POST /api/masters/profile/photo] Ошибка multer:', err);
    if (err instanceof multer.MulterError) {
      if (err.code === 'LIMIT_FILE_SIZE') {
        return res.status(400).json({ error: 'Файл слишком большой. Максимум 50MB' });
      }
      if (err.code === 'LIMIT_FILE_COUNT') {
        return res.status(400).json({ error: 'Слишком много файлов. Максимум 5' });
      }
      return res.status(400).json({ error: `Ошибка загрузки: ${err.message}`, code: err.code });
    }
    return res.status(400).json({ error: err.message || 'Ошибка загрузки файла' });
  }
  next();
}, (req, res) => {
  try {
    console.log('[POST /api/masters/profile/photo] Файл получен:', req.file ? `filename=${req.file.filename}, size=${req.file.size}, mimetype=${req.file.mimetype}` : 'null');
    
    if (!req.file) {
      console.error('[POST /api/masters/profile/photo] Файл не загружен. Request body keys:', Object.keys(req.body || {}));
      return res.status(400).json({ error: 'Файл не загружен. Убедитесь, что поле называется "photo"' });
    }
    
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      console.error('[POST /api/masters/profile/photo] Профиль мастера не найден для user_id:', req.user.id);
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const fileUrl = `/uploads/${req.file.filename}`;
    
    console.log('[POST /api/masters/profile/photo] Обновление photo_url для master_id:', master.id, 'fileUrl:', fileUrl);
    
    query.run(
      'UPDATE masters SET photo_url = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      [fileUrl, master.id]
    );
    
    console.log('[POST /api/masters/profile/photo] Фото профиля успешно загружено');
    
    res.json({
      message: 'Фото профиля загружено',
      photo_url: fileUrl
    });
  } catch (error) {
    console.error('[POST /api/masters/profile/photo] Ошибка загрузки фото профиля:', error);
    console.error('[POST /api/masters/profile/photo] Stack trace:', error.stack);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Получить портфолио мастера
router.get('/:id/portfolio', authenticate, (req, res) => {
  try {
    const { id } = req.params;
    
    const portfolio = query.all(`
      SELECT * FROM master_portfolio 
      WHERE master_id = ? 
      ORDER BY order_index ASC, created_at DESC
    `, [id]);
    
    res.json(portfolio);
  } catch (error) {
    console.error('Ошибка получения портфолио:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Добавить фото в портфолио
router.post('/portfolio', authenticate, authorize('master'), upload.single('image'), handleUploadError, (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'Файл не загружен' });
    }
    
    const { description, category } = req.body;
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const fileUrl = `/uploads/${req.file.filename}`;
    
    // Получаем максимальный order_index
    const maxOrder = query.get(
      'SELECT MAX(order_index) as max_order FROM master_portfolio WHERE master_id = ?',
      [master.id]
    );
    const nextOrder = (maxOrder?.max_order || 0) + 1;
    
    const result = query.run(`
      INSERT INTO master_portfolio (master_id, image_url, description, category, order_index)
      VALUES (?, ?, ?, ?, ?)
    `, [master.id, fileUrl, description || null, category || null, nextOrder]);
    
    const portfolioItem = query.get('SELECT * FROM master_portfolio WHERE id = ?', [result.lastInsertRowid]);
    
    res.status(201).json({
      message: 'Фото добавлено в портфолио',
      item: portfolioItem
    });
  } catch (error) {
    console.error('Ошибка добавления в портфолио:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Удалить фото из портфолио
router.delete('/portfolio/:id', authenticate, authorize('master'), (req, res) => {
  try {
    const { id } = req.params;
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const portfolioItem = query.get('SELECT * FROM master_portfolio WHERE id = ? AND master_id = ?', [id, master.id]);
    if (!portfolioItem) {
      return res.status(404).json({ error: 'Элемент портфолио не найден' });
    }
    
    query.run('DELETE FROM master_portfolio WHERE id = ?', [id]);
    
    res.json({ message: 'Элемент портфолио удален' });
  } catch (error) {
    console.error('Ошибка удаления из портфолио:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить сертификаты мастера
router.get('/:id/certificates', authenticate, (req, res) => {
  try {
    const { id } = req.params;
    
    const certificates = query.all(`
      SELECT * FROM master_certificates 
      WHERE master_id = ? 
      ORDER BY order_index ASC, issue_date DESC
    `, [id]);
    
    res.json(certificates);
  } catch (error) {
    console.error('Ошибка получения сертификатов:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Добавить сертификат
router.post('/certificates', authenticate, authorize('master'), upload.single('certificate'), handleUploadError, (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'Файл не загружен' });
    }
    
    const { title, issuer, issue_date, expiry_date, description } = req.body;
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const fileUrl = `/uploads/${req.file.filename}`;
    
    // Получаем максимальный order_index
    const maxOrder = query.get(
      'SELECT MAX(order_index) as max_order FROM master_certificates WHERE master_id = ?',
      [master.id]
    );
    const nextOrder = (maxOrder?.max_order || 0) + 1;
    
    const result = query.run(`
      INSERT INTO master_certificates 
      (master_id, title, issuer, issue_date, expiry_date, certificate_url, description, order_index)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `, [master.id, title, issuer || null, issue_date || null, expiry_date || null, fileUrl, description || null, nextOrder]);
    
    const certificate = query.get('SELECT * FROM master_certificates WHERE id = ?', [result.lastInsertRowid]);
    
    res.status(201).json({
      message: 'Сертификат добавлен',
      certificate: certificate
    });
  } catch (error) {
    console.error('Ошибка добавления сертификата:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Удалить сертификат
router.delete('/certificates/:id', authenticate, authorize('master'), (req, res) => {
  try {
    const { id } = req.params;
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const certificate = query.get('SELECT * FROM master_certificates WHERE id = ? AND master_id = ?', [id, master.id]);
    if (!certificate) {
      return res.status(404).json({ error: 'Сертификат не найден' });
    }
    
    query.run('DELETE FROM master_certificates WHERE id = ?', [id]);
    
    res.json({ message: 'Сертификат удален' });
  } catch (error) {
    console.error('Ошибка удаления сертификата:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Начать смену
router.post('/shift/start', authenticate, authorize('master'), (req, res) => {
  try {
    const { latitude, longitude } = req.body;
    
    if (!latitude || !longitude) {
      return res.status(400).json({ error: 'Необходимо передать координаты' });
    }
    
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    query.run(
      'UPDATE masters SET is_on_shift = 1, status = ?, latitude = ?, longitude = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      ['available', latitude, longitude, master.id]
    );
    
    res.json({ message: 'Смена начата', isOnShift: true });
  } catch (error) {
    console.error('Ошибка начала смены:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Завершить смену
router.post('/shift/end', authenticate, authorize('master'), (req, res) => {
  try {
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    query.run(
      'UPDATE masters SET is_on_shift = 0, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      ['offline', master.id]
    );
    
    res.json({ message: 'Смена завершена', isOnShift: false });
  } catch (error) {
    console.error('Ошибка завершения смены:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить статистику мастера
router.get('/stats/me', authenticate, authorize('master'), (req, res) => {
  try {
    const { period } = req.query; // 'day', 'week', 'month', 'all'
    const master = query.get('SELECT * FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    // Определяем период для фильтрации
    let dateFilter = '';
    const params = [master.id];
    
    if (period === 'day') {
      dateFilter = " AND DATE(o.created_at) = DATE('now')";
    } else if (period === 'week') {
      dateFilter = " AND o.created_at >= datetime('now', '-7 days')";
    } else if (period === 'month') {
      dateFilter = " AND o.created_at >= datetime('now', '-30 days')";
    }
    // 'all' или отсутствие period - без фильтра по дате
    
    // Получаем статистику заказов
    const totalOrders = query.get(
      `SELECT COUNT(*) as count FROM orders o WHERE o.assigned_master_id = ?${dateFilter}`,
      params
    );
    
    const completedOrders = query.get(
      `SELECT COUNT(*) as count FROM orders o WHERE o.assigned_master_id = ? AND o.repair_status = 'completed'${dateFilter}`,
      params
    );
    
    const inProgressOrders = query.get(
      `SELECT COUNT(*) as count FROM orders o WHERE o.assigned_master_id = ? AND o.repair_status = 'in_progress'${dateFilter}`,
      params
    );
    
    // Получаем средний рейтинг из отзывов
    const avgRating = query.get(
      'SELECT AVG(rating) as avg, COUNT(*) as count FROM reviews WHERE master_id = ?',
      [master.id]
    );
    
    // Получаем доходы за период
    let incomeStats = { total: 0, chartData: [] };
    try {
      if (period === 'day') {
        // По часам за день
        const hourlyIncome = query.all(`
          SELECT 
            strftime('%H', mt.created_at) as hour,
            COALESCE(SUM(mt.amount), 0) as total
          FROM master_transactions mt
          WHERE mt.master_id = ? 
            AND mt.transaction_type = 'income' 
            AND mt.status = 'completed'
            AND DATE(mt.created_at) = DATE('now')
          GROUP BY strftime('%H', mt.created_at)
          ORDER BY hour
        `, [master.id]);
        incomeStats.chartData = hourlyIncome.map(row => ({
          label: `${row.hour}:00`,
          value: row.total || 0
        }));
      } else if (period === 'week') {
        // По дням за неделю
        const dailyIncome = query.all(`
          SELECT 
            DATE(mt.created_at) as date,
            COALESCE(SUM(mt.amount), 0) as total
          FROM master_transactions mt
          WHERE mt.master_id = ? 
            AND mt.transaction_type = 'income' 
            AND mt.status = 'completed'
            AND mt.created_at >= datetime('now', '-7 days')
          GROUP BY DATE(mt.created_at)
          ORDER BY date
        `, [master.id]);
        incomeStats.chartData = dailyIncome.map(row => ({
          label: row.date,
          value: row.total || 0
        }));
      } else if (period === 'month') {
        // По дням за месяц
        const dailyIncome = query.all(`
          SELECT 
            DATE(mt.created_at) as date,
            COALESCE(SUM(mt.amount), 0) as total
          FROM master_transactions mt
          WHERE mt.master_id = ? 
            AND mt.transaction_type = 'income' 
            AND mt.status = 'completed'
            AND mt.created_at >= datetime('now', '-30 days')
          GROUP BY DATE(mt.created_at)
          ORDER BY date
        `, [master.id]);
        incomeStats.chartData = dailyIncome.map(row => ({
          label: row.date,
          value: row.total || 0
        }));
      }
      
      // Общий доход за период
      let incomeDateFilter = '';
      if (period === 'day') {
        incomeDateFilter = " AND DATE(mt.created_at) = DATE('now')";
      } else if (period === 'week') {
        incomeDateFilter = " AND mt.created_at >= datetime('now', '-7 days')";
      } else if (period === 'month') {
        incomeDateFilter = " AND mt.created_at >= datetime('now', '-30 days')";
      }
      
      const totalIncome = query.get(`
        SELECT COALESCE(SUM(amount), 0) as total 
        FROM master_transactions mt
        WHERE mt.master_id = ? 
          AND mt.transaction_type = 'income' 
          AND mt.status = 'completed'
          ${incomeDateFilter}
      `, [master.id]);
      incomeStats.total = totalIncome?.total || 0;
    } catch (incomeError) {
      console.log('Ошибка получения доходов (таблица может не существовать):', incomeError.message);
    }
    
    // Статистика по заказам для графика
    let ordersChartData = [];
    try {
      if (period === 'day') {
        const hourlyOrders = query.all(`
          SELECT 
            strftime('%H', o.created_at) as hour,
            COUNT(*) as count
          FROM orders o
          WHERE o.assigned_master_id = ? 
            AND DATE(o.created_at) = DATE('now')
          GROUP BY strftime('%H', o.created_at)
          ORDER BY hour
        `, [master.id]);
        ordersChartData = hourlyOrders.map(row => ({
          label: `${row.hour}:00`,
          value: row.count || 0
        }));
      } else if (period === 'week' || period === 'month') {
        const dailyOrders = query.all(`
          SELECT 
            DATE(o.created_at) as date,
            COUNT(*) as count
          FROM orders o
          WHERE o.assigned_master_id = ? 
            ${dateFilter}
          GROUP BY DATE(o.created_at)
          ORDER BY date
        `, params);
        ordersChartData = dailyOrders.map(row => ({
          label: row.date,
          value: row.count || 0
        }));
      }
    } catch (ordersError) {
      console.log('Ошибка получения статистики заказов:', ordersError.message);
    }
    
    res.json({
      master: {
        id: master.id,
        rating: master.rating,
        completedOrders: master.completed_orders,
        isOnShift: master.is_on_shift === 1,
        status: master.status
      },
      stats: {
        totalOrders: totalOrders.count,
        completedOrders: completedOrders.count,
        inProgressOrders: inProgressOrders.count,
        averageRating: avgRating.avg || 0,
        reviewsCount: avgRating.count,
        totalIncome: incomeStats.total,
        incomeChartData: incomeStats.chartData,
        ordersChartData: ordersChartData
      },
      period: period || 'all'
    });
  } catch (error) {
    console.error('Ошибка получения статистики:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Получить историю транзакций
router.get('/wallet/transactions', authenticate, authorize('master'), (req, res) => {
  try {
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const { limit = 50, offset = 0, type, status } = req.query;
    
    let sql = `
      SELECT 
        mt.*,
        o.order_number,
        o.final_cost
      FROM master_transactions mt
      LEFT JOIN orders o ON mt.order_id = o.id
      WHERE mt.master_id = ?
    `;
    const params = [master.id];
    
    if (type) {
      sql += ' AND mt.transaction_type = ?';
      params.push(type);
    }
    
    if (status) {
      sql += ' AND mt.status = ?';
      params.push(status);
    }
    
    sql += ' ORDER BY mt.created_at DESC LIMIT ? OFFSET ?';
    params.push(parseInt(limit), parseInt(offset));
    
    const transactions = query.all(sql, params);
    
    res.json(transactions);
  } catch (error) {
    console.error('Ошибка получения транзакций:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Запросить выплату
router.post('/wallet/payout', authenticate, authorize('master'), async (req, res) => {
  try {
    const { amount, payoutMethod, payoutDetails } = req.body;
    
    if (!amount || amount <= 0) {
      return res.status(400).json({ error: 'Укажите сумму для выплаты' });
    }
    
    const master = query.get('SELECT id, balance FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const availableBalance = master.balance || 0;
    
    // Проверяем pending выплаты
    const pendingPayouts = query.get(`
      SELECT COALESCE(SUM(amount), 0) as total 
      FROM master_transactions 
      WHERE master_id = ? AND transaction_type = 'payout' AND status IN ('pending', 'processing')
    `, [master.id]);
    
    const availableForPayout = availableBalance - (pendingPayouts.total || 0);
    
    if (amount > availableForPayout) {
      return res.status(400).json({ 
        error: `Недостаточно средств. Доступно для выплаты: ${availableForPayout.toFixed(2)} ₽` 
      });
    }
    
    const effectivePayoutMethod = payoutMethod || 'bank';
    const details = payoutDetails || {};
    
    // Если выплата через ЮMoney
    if (effectivePayoutMethod === 'yoomoney' && details.yoomoneyWallet) {
      try {
        // Импортируем сервис динамически, чтобы избежать ошибок если он не настроен
        const { yooMoneyService } = await import('../services/yoomoney-service.js');
        
        // Создаем транзакцию со статусом processing
        const transactionResult = query.run(`
          INSERT INTO master_transactions 
          (master_id, transaction_type, amount, status, payout_method, payout_details)
          VALUES (?, 'payout', ?, 'processing', ?, ?)
        `, [master.id, amount, 'yoomoney', JSON.stringify(details)]);
        
        const transactionId = transactionResult.lastInsertRowid;
        
        // Выполняем выплату через ЮMoney
        const payoutResult = await yooMoneyService.payoutToWallet({
          to: details.yoomoneyWallet,
          amount: amount,
          label: `Выплата мастеру #${master.id}`,
          masterId: master.id.toString()
        });
        
        // Обновляем транзакцию с данными от ЮMoney
        query.run(`
          UPDATE master_transactions 
          SET status = 'completed', 
              payout_details = ?,
              completed_at = CURRENT_TIMESTAMP
          WHERE id = ?
        `, [
          JSON.stringify({
            ...details,
            yoomoney_request_id: payoutResult.requestId,
            yoomoney_status: payoutResult.status
          }),
          transactionId
        ]);
        
        // Списываем средства с баланса мастера
        query.run(`
          UPDATE masters 
          SET balance = COALESCE(balance, 0) - ?, updated_at = CURRENT_TIMESTAMP 
          WHERE id = ?
        `, [amount, master.id]);
        
        const transaction = query.get('SELECT * FROM master_transactions WHERE id = ?', [transactionId]);
        
        console.log(`✅ Выплата через ЮMoney выполнена: master_id=${master.id}, amount=${amount}, request_id=${payoutResult.requestId}`);
        
        res.status(201).json({
          message: 'Выплата успешно выполнена через ЮMoney',
          transaction,
          yooMoney: {
            requestId: payoutResult.requestId,
            status: payoutResult.status
          }
        });
        return;
      } catch (yooMoneyError) {
        console.error('Ошибка выплаты через ЮMoney:', yooMoneyError);
        
        // Если ошибка, создаем транзакцию со статусом failed
        const failedTransactionResult = query.run(`
          INSERT INTO master_transactions 
          (master_id, transaction_type, amount, status, payout_method, payout_details)
          VALUES (?, 'payout', ?, 'failed', ?, ?)
        `, [master.id, amount, 'yoomoney', JSON.stringify({
          ...details,
          error: yooMoneyError.message
        })]);
        
        if (yooMoneyError.message.includes('не настроен')) {
          return res.status(503).json({
            error: 'Выплаты через ЮMoney временно недоступны',
            details: 'ЮMoney не настроен. Обратитесь к администратору.'
          });
        }
        
        return res.status(500).json({
          error: 'Ошибка выплаты через ЮMoney',
          details: yooMoneyError.message
        });
      }
    }
    
    // Для других методов выплат (bank, sbp) - создаем запрос на выплату
    const result = query.run(`
      INSERT INTO master_transactions 
      (master_id, transaction_type, amount, status, payout_method, payout_details)
      VALUES (?, 'payout', ?, 'pending', ?, ?)
    `, [master.id, amount, effectivePayoutMethod, details ? JSON.stringify(details) : null]);
    
    const transaction = query.get('SELECT * FROM master_transactions WHERE id = ?', [result.lastInsertRowid]);
    
    console.log(`📝 Создан запрос на выплату: master_id=${master.id}, amount=${amount}, method=${effectivePayoutMethod}`);
    
    res.status(201).json({
      message: 'Запрос на выплату создан',
      transaction
    });
  } catch (error) {
    console.error('Ошибка создания запроса на выплату:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Пополнить кошелек
router.post('/wallet/topup', authenticate, authorize('master'), (req, res) => {
  try {
    const { amount, paymentMethod, description } = req.body;
    
    if (!amount || amount <= 0) {
      return res.status(400).json({ error: 'Укажите сумму для пополнения' });
    }
    
    if (amount > 100000) {
      return res.status(400).json({ error: 'Максимальная сумма пополнения: 100 000 ₽' });
    }
    
    const master = query.get('SELECT id, balance FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    // Создаем транзакцию пополнения
    // В реальной системе здесь должна быть интеграция с платежной системой
    // Для MVP сразу зачисляем средства со статусом 'completed'
    const transactionResult = query.run(`
      INSERT INTO master_transactions 
      (master_id, transaction_type, amount, status, description, payout_method)
      VALUES (?, 'income', ?, 'completed', ?, ?)
    `, [
      master.id, 
      amount, 
      description || `Пополнение кошелька через ${paymentMethod || 'карта'}`,
      paymentMethod || 'card'
    ]);
    
    // Обновляем баланс мастера
    query.run(`
      UPDATE masters 
      SET balance = COALESCE(balance, 0) + ?, updated_at = CURRENT_TIMESTAMP 
      WHERE id = ?
    `, [amount, master.id]);
    
    const transaction = query.get('SELECT * FROM master_transactions WHERE id = ?', [transactionResult.lastInsertRowid]);
    const updatedMaster = query.get('SELECT balance FROM masters WHERE id = ?', [master.id]);
    
    res.status(201).json({
      message: 'Кошелек успешно пополнен',
      transaction,
      newBalance: updatedMaster.balance || 0
    });
  } catch (error) {
    console.error('Ошибка пополнения кошелька:', error);
    res.status(500).json({ error: 'Ошибка сервера при пополнении кошелька' });
  }
});

export default router;



