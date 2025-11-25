import express from 'express';
import { query } from '../database/db.js';
import { authenticate, authorize } from '../middleware/auth.js';
import { upload, handleUploadError } from '../middleware/upload.js';
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
    const master = query.get('SELECT * FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    // Получаем статистику заказов
    const totalOrders = query.get(
      'SELECT COUNT(*) as count FROM orders WHERE assigned_master_id = ?',
      [master.id]
    );
    
    const completedOrders = query.get(
      'SELECT COUNT(*) as count FROM orders WHERE assigned_master_id = ? AND repair_status = ?',
      [master.id, 'completed']
    );
    
    const inProgressOrders = query.get(
      'SELECT COUNT(*) as count FROM orders WHERE assigned_master_id = ? AND repair_status = ?',
      [master.id, 'in_progress']
    );
    
    // Получаем средний рейтинг из отзывов
    const avgRating = query.get(
      'SELECT AVG(rating) as avg, COUNT(*) as count FROM reviews WHERE master_id = ?',
      [master.id]
    );
    
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
        reviewsCount: avgRating.count
      }
    });
  } catch (error) {
    console.error('Ошибка получения статистики:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
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
    
    // Получаем ID мастера
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const updateFields = [];
    const updateValues = [];
    
    if (specialization !== undefined) {
      updateFields.push('specialization = ?');
      updateValues.push(JSON.stringify(specialization));
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
router.post('/profile/photo', authenticate, authorize('master'), upload.single('photo'), handleUploadError, (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'Файл не загружен' });
    }
    
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const fileUrl = `/uploads/${req.file.filename}`;
    
    query.run(
      'UPDATE masters SET photo_url = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      [fileUrl, master.id]
    );
    
    res.json({
      message: 'Фото профиля загружено',
      photo_url: fileUrl
    });
  } catch (error) {
    console.error('Ошибка загрузки фото профиля:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
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
router.post('/wallet/payout', authenticate, authorize('master'), (req, res) => {
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
      WHERE master_id = ? AND transaction_type = 'payout' AND status = 'pending'
    `, [master.id]);
    
    const availableForPayout = availableBalance - (pendingPayouts.total || 0);
    
    if (amount > availableForPayout) {
      return res.status(400).json({ 
        error: `Недостаточно средств. Доступно для выплаты: ${availableForPayout.toFixed(2)} ₽` 
      });
    }
    
    // Создаем транзакцию на выплату
    const result = query.run(`
      INSERT INTO master_transactions 
      (master_id, transaction_type, amount, status, payout_method, payout_details)
      VALUES (?, 'payout', ?, 'pending', ?, ?)
    `, [master.id, amount, payoutMethod || 'bank', payoutDetails ? JSON.stringify(payoutDetails) : null]);
    
    const transaction = query.get('SELECT * FROM master_transactions WHERE id = ?', [result.lastInsertRowid]);
    
    res.status(201).json({
      message: 'Запрос на выплату создан',
      transaction
    });
  } catch (error) {
    console.error('Ошибка создания запроса на выплату:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

export default router;



