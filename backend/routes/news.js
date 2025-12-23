import express from 'express';
import { query } from '../database/db.js';
import { authenticateToken, isAdmin } from '../middleware/auth.js';

const router = express.Router();

/**
 * @swagger
 * /api/news:
 *   get:
 *     summary: Получить список всех новостей
 *     tags: [News]
 */
router.get('/', async (req, res) => {
  try {
    const news = query.all(
      'SELECT * FROM news WHERE is_active = 1 ORDER BY published_at DESC'
    );
    console.log(`[NEWS] Получено ${news.length} активных новостей`);
    if (news.length > 0) {
      console.log(`[NEWS] Первая новость: id=${news[0].id}, title=${news[0].title}, is_active=${news[0].is_active}`);
    }
    res.json(news);
  } catch (error) {
    console.error('Ошибка при получении новостей:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

/**
 * @swagger
 * /api/news/{id}:
 *   get:
 *     summary: Получить новость по ID
 *     tags: [News]
 */
router.get('/:id', async (req, res) => {
  try {
    const item = query.get('SELECT * FROM news WHERE id = ?', [req.params.id]);
    if (!item) {
      return res.status(404).json({ error: 'Новость не найдена' });
    }
    res.json(item);
  } catch (error) {
    console.error('Ошибка при получении новости:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Админские эндпоинты
/**
 * @swagger
 * /api/news:
 *   post:
 *     summary: Создать новую новость (только админ)
 *     tags: [News]
 */
router.post('/', authenticateToken, isAdmin, async (req, res) => {
  try {
    const { title, summary, content, image_url, category, is_active } = req.body;
    
    // Валидация обязательных полей
    if (!title || typeof title !== 'string' || title.trim().length === 0) {
      return res.status(400).json({ error: 'Заголовок обязателен и не может быть пустым' });
    }
    
    if (!content || typeof content !== 'string' || content.trim().length === 0) {
      return res.status(400).json({ error: 'Содержание обязательно и не может быть пустым' });
    }

    // Подготовка данных для вставки
    const newsCategory = category || 'general';
    const newsImageUrl = image_url || null;
    const newsSummary = summary || null;
    const activeStatus = is_active !== undefined ? (is_active ? 1 : 0) : 1;

    const result = query.run(
      'INSERT INTO news (title, summary, content, image_url, category, is_active) VALUES (?, ?, ?, ?, ?, ?)',
      [title.trim(), newsSummary, content.trim(), newsImageUrl, newsCategory, activeStatus]
    );
    
    const newItem = query.get('SELECT * FROM news WHERE id = ?', [result.lastInsertRowid]);
    console.log(`[NEWS] Создана новость: id=${newItem.id}, title=${newItem.title}, is_active=${newItem.is_active}`);
    res.status(201).json(newItem);
  } catch (error) {
    console.error('Ошибка при создании новости:', error);
    console.error('Request body:', req.body);
    res.status(500).json({ error: 'Ошибка сервера: ' + error.message });
  }
});

/**
 * @swagger
 * /api/news/{id}:
 *   put:
 *     summary: Обновить новость (только админ)
 *     tags: [News]
 */
router.put('/:id', authenticateToken, isAdmin, async (req, res) => {
  try {
    const { title, summary, content, image_url, category, is_active } = req.body;
    
    const existing = query.get('SELECT id FROM news WHERE id = ?', [req.params.id]);
    if (!existing) {
      return res.status(404).json({ error: 'Новость не найдена' });
    }

    // Подготовка данных для обновления
    const updateTitle = title !== undefined ? title.trim() : null;
    const updateContent = content !== undefined ? content.trim() : null;
    const updateSummary = summary !== undefined ? (summary.trim() || null) : null;
    const updateImageUrl = image_url !== undefined ? (image_url.trim() || null) : null;
    const updateCategory = category !== undefined ? category : null;
    const updateIsActive = is_active !== undefined ? (is_active ? 1 : 0) : null;

    query.run(
      `UPDATE news SET 
        title = COALESCE(?, title), 
        summary = COALESCE(?, summary), 
        content = COALESCE(?, content), 
        image_url = COALESCE(?, image_url), 
        category = COALESCE(?, category), 
        is_active = COALESCE(?, is_active),
        updated_at = CURRENT_TIMESTAMP
      WHERE id = ?`,
      [updateTitle, updateSummary, updateContent, updateImageUrl, updateCategory, updateIsActive, req.params.id]
    );
    
    const updated = query.get('SELECT * FROM news WHERE id = ?', [req.params.id]);
    res.json(updated);
  } catch (error) {
    console.error('Ошибка при обновлении новости:', error);
    console.error('Request body:', req.body);
    res.status(500).json({ error: 'Ошибка сервера: ' + error.message });
  }
});

/**
 * @swagger
 * /api/news/{id}:
 *   delete:
 *     summary: Удалить новость (только админ)
 *     tags: [News]
 */
router.delete('/:id', authenticateToken, isAdmin, async (req, res) => {
  try {
    const result = query.run('DELETE FROM news WHERE id = ?', [req.params.id]);
    if (result.changes === 0) {
      return res.status(404).json({ error: 'Новость не найдена' });
    }
    res.json({ message: 'Новость удалена' });
  } catch (error) {
    console.error('Ошибка при удалении новости:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

export default router;
