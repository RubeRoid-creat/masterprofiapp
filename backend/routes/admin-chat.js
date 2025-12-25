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

// Настройка multer для загрузки изображений
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadDir = join(__dirname, '..', 'uploads', 'admin-chat');
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
  },
  fileFilter: (req, file, cb) => {
    if (file.mimetype.startsWith('image/')) {
      cb(null, true);
    } else {
      cb(new Error('Разрешены только изображения'));
    }
  }
});

/**
 * GET /api/admin-chat/messages
 * Получить историю сообщений чата с администрацией
 */
router.get('/messages', authenticate, async (req, res) => {
  try {
    const userId = req.user.id;
    
    // Получаем сообщения для пользователя
    const messages = query.all(`
      SELECT 
        acm.id,
        acm.user_id,
        acm.sender_id,
        acm.sender_role,
        acm.message_type,
        acm.message_text,
        acm.image_url,
        acm.image_thumbnail_url,
        acm.file_url,
        acm.file_name,
        acm.read_at,
        acm.created_at,
        u.name as sender_name,
        u.role as sender_user_role
      FROM admin_chat_messages acm
      JOIN users u ON acm.sender_id = u.id
      WHERE acm.user_id = ?
      ORDER BY acm.created_at ASC
    `, [userId]);
    
    // Отмечаем сообщения от администрации как прочитанные
    query.run(`
      UPDATE admin_chat_messages
      SET read_at = CURRENT_TIMESTAMP
      WHERE user_id = ? AND sender_role = 'admin' AND read_at IS NULL
    `, [userId]);
    
    res.json(messages);
  } catch (error) {
    console.error('Ошибка получения сообщений:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

/**
 * POST /api/admin-chat/messages
 * Отправить сообщение администрации
 */
router.post('/messages', authenticate, async (req, res) => {
  try {
    const userId = req.user.id;
    const { message } = req.body;
    
    if (!message || message.trim().length === 0) {
      return res.status(400).json({ error: 'Сообщение не может быть пустым' });
    }
    
    // Сохраняем сообщение
    const result = query.run(`
      INSERT INTO admin_chat_messages (user_id, sender_id, sender_role, message_type, message_text)
      VALUES (?, ?, 'user', 'text', ?)
    `, [userId, userId, message.trim()]);
    
    // Получаем созданное сообщение
    const createdMessage = query.get(`
      SELECT 
        acm.id,
        acm.user_id,
        acm.sender_id,
        acm.sender_role,
        acm.message_type,
        acm.message_text,
        acm.image_url,
        acm.image_thumbnail_url,
        acm.file_url,
        acm.file_name,
        acm.read_at,
        acm.created_at,
        u.name as sender_name,
        u.role as sender_user_role
      FROM admin_chat_messages acm
      JOIN users u ON acm.sender_id = u.id
      WHERE acm.id = ?
    `, [result.lastInsertRowid]);
    
    res.status(201).json(createdMessage);
  } catch (error) {
    console.error('Ошибка отправки сообщения:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

/**
 * POST /api/admin-chat/messages/image
 * Отправить изображение в чат с администрацией
 */
router.post('/messages/image', authenticate, upload.single('image'), async (req, res) => {
  try {
    const userId = req.user.id;
    
    if (!req.file) {
      return res.status(400).json({ error: 'Изображение не загружено' });
    }
    
    const imageUrl = `/uploads/admin-chat/${req.file.filename}`;
    
    // Сохраняем сообщение с изображением
    const result = query.run(`
      INSERT INTO admin_chat_messages (user_id, sender_id, sender_role, message_type, image_url, image_thumbnail_url)
      VALUES (?, ?, 'user', 'image', ?, ?)
    `, [userId, userId, imageUrl, imageUrl]);
    
    // Получаем созданное сообщение
    const createdMessage = query.get(`
      SELECT 
        acm.id,
        acm.user_id,
        acm.sender_id,
        acm.sender_role,
        acm.message_type,
        acm.message_text,
        acm.image_url,
        acm.image_thumbnail_url,
        acm.file_url,
        acm.file_name,
        acm.read_at,
        acm.created_at,
        u.name as sender_name,
        u.role as sender_user_role
      FROM admin_chat_messages acm
      JOIN users u ON acm.sender_id = u.id
      WHERE acm.id = ?
    `, [result.lastInsertRowid]);
    
    res.status(201).json(createdMessage);
  } catch (error) {
    console.error('Ошибка загрузки изображения:', error);
    if (req.file) {
      fs.unlinkSync(req.file.path);
    }
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

/**
 * GET /api/admin-chat/unread-count
 * Получить количество непрочитанных сообщений
 */
router.get('/unread-count', authenticate, async (req, res) => {
  try {
    const userId = req.user.id;
    
    const result = query.get(`
      SELECT COUNT(*) as count
      FROM admin_chat_messages
      WHERE user_id = ? AND sender_role = 'admin' AND read_at IS NULL
    `, [userId]);
    
    res.json({ unreadCount: result?.count || 0 });
  } catch (error) {
    console.error('Ошибка получения количества непрочитанных сообщений:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

export default router;
