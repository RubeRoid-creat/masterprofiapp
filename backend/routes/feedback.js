import express from 'express';
import { authenticate } from '../middleware/auth.js';
import { query } from '../database/db.js';
import multer from 'multer';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import fs from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const router = express.Router();

// Настройка multer для загрузки файлов
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = join(__dirname, '..', 'uploads', 'feedback');
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, uniqueSuffix + '-' + file.originalname);
  }
});

const upload = multer({
  storage: storage,
  limits: {
    fileSize: 10 * 1024 * 1024 // 10MB
  }
});

/**
 * POST /api/feedback
 * Создать обратную связь
 */
router.post('/', authenticate, upload.array('attachments', 5), async (req, res) => {
  try {
    const userId = req.user.id;
    const { feedback_type, subject, message } = req.body;
    
    if (!feedback_type || !subject || !message) {
      return res.status(400).json({ error: 'Заполните все обязательные поля' });
    }
    
    // Обрабатываем загруженные файлы
    let attachments = null;
    if (req.files && req.files.length > 0) {
      attachments = JSON.stringify(
        req.files.map(file => ({
          url: `/uploads/feedback/${file.filename}`,
          name: file.originalname,
          size: file.size,
          mimeType: file.mimetype
        }))
      );
    }
    
    // Сохраняем обратную связь
    const result = query.run(`
      INSERT INTO feedback (user_id, feedback_type, subject, message, attachments, status)
      VALUES (?, ?, ?, ?, ?, 'new')
    `, [userId, feedback_type, subject, message, attachments]);
    
    // Получаем созданную обратную связь
    const createdFeedback = query.get(`
      SELECT 
        f.id,
        f.user_id,
        f.feedback_type,
        f.subject,
        f.message,
        f.attachments,
        f.status,
        f.admin_response,
        f.responded_by,
        f.responded_at,
        f.created_at,
        f.updated_at,
        u.name as user_name,
        u.email as user_email
      FROM feedback f
      JOIN users u ON f.user_id = u.id
      WHERE f.id = ?
    `, [result.lastInsertRowid]);
    
    res.status(201).json(createdFeedback);
  } catch (error) {
    console.error('Ошибка создания обратной связи:', error);
    // Удаляем загруженные файлы в случае ошибки
    if (req.files) {
      req.files.forEach(file => {
        try {
          fs.unlinkSync(file.path);
        } catch (e) {
          console.error('Ошибка удаления файла:', e);
        }
      });
    }
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

/**
 * GET /api/feedback
 * Получить список обратной связи пользователя
 */
router.get('/', authenticate, async (req, res) => {
  try {
    const userId = req.user.id;
    
    const feedbackList = query.all(`
      SELECT 
        f.id,
        f.feedback_type,
        f.subject,
        f.message,
        f.attachments,
        f.status,
        f.admin_response,
        f.responded_at,
        f.created_at,
        f.updated_at
      FROM feedback f
      WHERE f.user_id = ?
      ORDER BY f.created_at DESC
    `, [userId]);
    
    res.json(feedbackList);
  } catch (error) {
    console.error('Ошибка получения обратной связи:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

/**
 * GET /api/feedback/:id
 * Получить детали обратной связи
 */
router.get('/:id', authenticate, async (req, res) => {
  try {
    const feedbackId = parseInt(req.params.id);
    const userId = req.user.id;
    
    const feedback = query.get(`
      SELECT 
        f.id,
        f.user_id,
        f.feedback_type,
        f.subject,
        f.message,
        f.attachments,
        f.status,
        f.admin_response,
        f.responded_by,
        f.responded_at,
        f.created_at,
        f.updated_at,
        u.name as user_name,
        u.email as user_email
      FROM feedback f
      JOIN users u ON f.user_id = u.id
      WHERE f.id = ? AND f.user_id = ?
    `, [feedbackId, userId]);
    
    if (!feedback) {
      return res.status(404).json({ error: 'Обратная связь не найдена' });
    }
    
    res.json(feedback);
  } catch (error) {
    console.error('Ошибка получения обратной связи:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

export default router;
