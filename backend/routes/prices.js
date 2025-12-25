import express from 'express';
import { query } from '../database/db.js';
import { authenticateToken, isAdmin } from '../middleware/auth.js';

const router = express.Router();

// Получить весь прайс-лист
router.get('/', (req, res) => {
  try {
    const { category, type } = req.query;
    
    let sql = 'SELECT * FROM prices WHERE 1=1';
    const params = [];
    
    if (category) {
      sql += ' AND category = ?';
      params.push(category);
    }
    
    if (type) {
      sql += ' AND type = ?';
      params.push(type);
    }
    
    sql += ' ORDER BY category, name';
    
    const prices = query.all(sql, params);
    
    res.json(prices);
  } catch (error) {
    console.error('Ошибка при получении прайс-листа:', error);
    res.status(500).json({ error: 'Ошибка при получении прайс-листа' });
  }
});

// Получить работы (type = 'service')
router.get('/services', (req, res) => {
  try {
    const { category } = req.query;
    
    let sql = "SELECT * FROM prices WHERE type = 'service'";
    const params = [];
    
    if (category) {
      sql += ' AND category = ?';
      params.push(category);
    }
    
    sql += ' ORDER BY category, name';
    
    const services = query.all(sql, params);
    
    res.json(services);
  } catch (error) {
    console.error('Ошибка при получении работ:', error);
    res.status(500).json({ error: 'Ошибка при получении работ' });
  }
});

// Получить запчасти (type = 'part')
router.get('/parts', (req, res) => {
  try {
    const { category } = req.query;
    
    let sql = "SELECT * FROM prices WHERE type = 'part'";
    const params = [];
    
    if (category) {
      sql += ' AND category = ?';
      params.push(category);
    }
    
    sql += ' ORDER BY category, name';
    
    const parts = query.all(sql, params);
    
    res.json(parts);
  } catch (error) {
    console.error('Ошибка при получении запчастей:', error);
    res.status(500).json({ error: 'Ошибка при получении запчастей' });
  }
});

// Получить категории
router.get('/categories', (req, res) => {
  try {
    const { type } = req.query;
    
    let sql = 'SELECT DISTINCT category FROM prices WHERE 1=1';
    const params = [];
    
    if (type) {
      sql += ' AND type = ?';
      params.push(type);
    }
    
    sql += ' ORDER BY category';
    
    const categories = query.all(sql, params).map(row => row.category);
    
    res.json(categories);
  } catch (error) {
    console.error('Ошибка при получении категорий:', error);
    res.status(500).json({ error: 'Ошибка при получении категорий' });
  }
});

// Админские эндпоинты

/**
 * Создать новую позицию прайса (только админ)
 */
router.post('/', authenticateToken, isAdmin, async (req, res) => {
  try {
    const { category, name, price, type, description, unit } = req.body;
    
    // Валидация обязательных полей
    if (!category || typeof category !== 'string' || category.trim().length === 0) {
      return res.status(400).json({ error: 'Категория обязательна и не может быть пустой' });
    }
    
    if (!name || typeof name !== 'string' || name.trim().length === 0) {
      return res.status(400).json({ error: 'Название обязательно и не может быть пустым' });
    }
    
    if (!price || typeof price !== 'number' || price <= 0) {
      return res.status(400).json({ error: 'Цена обязательна и должна быть положительным числом' });
    }
    
    if (!type || !['service', 'part'].includes(type)) {
      return res.status(400).json({ error: 'Тип обязателен и должен быть "service" или "part"' });
    }

    console.log(`[PRICES] Создание позиции: name="${name.trim()}", category="${category.trim()}", type="${type}", price=${price}`);
    
    const result = query.run(
      'INSERT INTO prices (category, name, price, type, description, unit) VALUES (?, ?, ?, ?, ?, ?)',
      [category.trim(), name.trim(), price, type, description?.trim() || null, unit || 'шт']
    );
    
    const newItem = query.get('SELECT * FROM prices WHERE id = ?', [result.lastInsertRowid]);
    if (newItem) {
      console.log(`[PRICES] ✅ Создана позиция: id=${newItem.id}, name="${newItem.name}"`);
    }
    res.status(201).json(newItem);
  } catch (error) {
    console.error('Ошибка при создании позиции прайса:', error);
    console.error('Request body:', req.body);
    res.status(500).json({ error: 'Ошибка сервера: ' + error.message });
  }
});

/**
 * Обновить позицию прайса (только админ)
 */
router.put('/:id', authenticateToken, isAdmin, async (req, res) => {
  try {
    const { category, name, price, type, description, unit } = req.body;
    
    const existing = query.get('SELECT id FROM prices WHERE id = ?', [req.params.id]);
    if (!existing) {
      return res.status(404).json({ error: 'Позиция прайса не найдена' });
    }

    // Подготовка данных для обновления
    const updateCategory = category !== undefined ? category.trim() : null;
    const updateName = name !== undefined ? name.trim() : null;
    const updatePrice = price !== undefined ? price : null;
    const updateType = type !== undefined ? type : null;
    const updateDescription = description !== undefined ? (description.trim() || null) : null;
    const updateUnit = unit !== undefined ? (unit.trim() || 'шт') : null;

    // Валидация типа, если он обновляется
    if (updateType && !['service', 'part'].includes(updateType)) {
      return res.status(400).json({ error: 'Тип должен быть "service" или "part"' });
    }

    // Валидация цены, если она обновляется
    if (updatePrice !== null && (typeof updatePrice !== 'number' || updatePrice <= 0)) {
      return res.status(400).json({ error: 'Цена должна быть положительным числом' });
    }

    query.run(
      `UPDATE prices SET 
        category = COALESCE(?, category), 
        name = COALESCE(?, name), 
        price = COALESCE(?, price), 
        type = COALESCE(?, type), 
        description = COALESCE(?, description), 
        unit = COALESCE(?, unit),
        updated_at = CURRENT_TIMESTAMP
      WHERE id = ?`,
      [updateCategory, updateName, updatePrice, updateType, updateDescription, updateUnit, req.params.id]
    );
    
    const updated = query.get('SELECT * FROM prices WHERE id = ?', [req.params.id]);
    res.json(updated);
  } catch (error) {
    console.error('Ошибка при обновлении позиции прайса:', error);
    console.error('Request body:', req.body);
    res.status(500).json({ error: 'Ошибка сервера: ' + error.message });
  }
});

/**
 * Удалить позицию прайса (только админ)
 */
router.delete('/:id', authenticateToken, isAdmin, async (req, res) => {
  try {
    const result = query.run('DELETE FROM prices WHERE id = ?', [req.params.id]);
    if (result.changes === 0) {
      return res.status(404).json({ error: 'Позиция прайса не найдена' });
    }
    res.json({ message: 'Позиция прайса удалена' });
  } catch (error) {
    console.error('Ошибка при удалении позиции прайса:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

export default router;
