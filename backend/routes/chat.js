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
    const uploadDir = join(__dirname, '..', 'uploads', 'chat');
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

// Получить историю сообщений чата для заказа
router.get('/:orderId/messages', authenticate, async (req, res) => {
  try {
    const orderId = parseInt(req.params.orderId);
    const userId = req.user.id;
    
    // Проверяем доступ к заказу
    const order = query.get(`
      SELECT o.id, o.client_id, o.assigned_master_id, c.user_id as client_user_id, m.user_id as master_user_id
      FROM orders o
      LEFT JOIN clients c ON o.client_id = c.id
      LEFT JOIN masters m ON o.assigned_master_id = m.id
      WHERE o.id = ?
    `, [orderId]);
    
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    // Проверяем доступ: клиент или назначенный мастер
    const hasAccess = order.client_user_id === userId || 
                     (order.assigned_master_id && order.master_user_id === userId);
    
    if (!hasAccess) {
      return res.status(403).json({ error: 'Нет доступа к этому заказу' });
    }
    
    // Получаем сообщения
    const messages = query.all(`
      SELECT 
        cm.id,
        cm.order_id,
        cm.sender_id,
        cm.message_type,
        cm.message_text,
        cm.image_url,
        cm.image_thumbnail_url,
        cm.read_at,
        cm.created_at,
        u.name as sender_name,
        u.role as sender_role
      FROM chat_messages cm
      JOIN users u ON cm.sender_id = u.id
      WHERE cm.order_id = ?
      ORDER BY cm.created_at ASC
    `, [orderId]);
    
    // Отмечаем сообщения как прочитанные
    query.run(`
      UPDATE chat_messages
      SET read_at = CURRENT_TIMESTAMP
      WHERE order_id = ? AND sender_id != ? AND read_at IS NULL
    `, [orderId, userId]);
    
    res.json(messages);
  } catch (error) {
    console.error('Ошибка получения сообщений:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Отправить сообщение (текст)
router.post('/:orderId/messages', authenticate, async (req, res) => {
  try {
    const orderId = parseInt(req.params.orderId);
    const userId = req.user.id;
    const { message } = req.body;
    
    if (!message || message.trim().length === 0) {
      return res.status(400).json({ error: 'Сообщение не может быть пустым' });
    }
    
    // Проверяем доступ к заказу
    const order = query.get(`
      SELECT o.id, o.client_id, o.assigned_master_id, c.user_id as client_user_id, m.user_id as master_user_id
      FROM orders o
      LEFT JOIN clients c ON o.client_id = c.id
      LEFT JOIN masters m ON o.assigned_master_id = m.id
      WHERE o.id = ?
    `, [orderId]);
    
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    const hasAccess = order.client_user_id === userId || 
                     (order.assigned_master_id && order.master_user_id === userId);
    
    if (!hasAccess) {
      return res.status(403).json({ error: 'Нет доступа к этому заказу' });
    }
    
    // Сохраняем сообщение
    const result = query.run(`
      INSERT INTO chat_messages (order_id, sender_id, message_type, message_text)
      VALUES (?, ?, 'text', ?)
    `, [orderId, userId, message.trim()]);
    
    // Получаем созданное сообщение
    const createdMessage = query.get(`
      SELECT 
        cm.id,
        cm.order_id,
        cm.sender_id,
        cm.message_type,
        cm.message_text,
        cm.image_url,
        cm.image_thumbnail_url,
        cm.read_at,
        cm.created_at,
        u.name as sender_name,
        u.role as sender_role
      FROM chat_messages cm
      JOIN users u ON cm.sender_id = u.id
      WHERE cm.id = ?
    `, [result.lastInsertRowid]);
    
    res.status(201).json(createdMessage);
  } catch (error) {
    console.error('Ошибка отправки сообщения:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Отправить изображение
router.post('/:orderId/messages/image', authenticate, upload.single('image'), async (req, res) => {
  try {
    const orderId = parseInt(req.params.orderId);
    const userId = req.user.id;
    
    if (!req.file) {
      return res.status(400).json({ error: 'Изображение не загружено' });
    }
    
    // Проверяем доступ к заказу
    const order = query.get(`
      SELECT o.id, o.client_id, o.assigned_master_id, c.user_id as client_user_id, m.user_id as master_user_id
      FROM orders o
      LEFT JOIN clients c ON o.client_id = c.id
      LEFT JOIN masters m ON o.assigned_master_id = m.id
      WHERE o.id = ?
    `, [orderId]);
    
    if (!order) {
      // Удаляем загруженный файл
      fs.unlinkSync(req.file.path);
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    const hasAccess = order.client_user_id === userId || 
                     (order.assigned_master_id && order.master_user_id === userId);
    
    if (!hasAccess) {
      // Удаляем загруженный файл
      fs.unlinkSync(req.file.path);
      return res.status(403).json({ error: 'Нет доступа к этому заказу' });
    }
    
    const imageUrl = `/uploads/chat/${req.file.filename}`;
    
    // Сохраняем сообщение с изображением
    const result = query.run(`
      INSERT INTO chat_messages (order_id, sender_id, message_type, image_url, image_thumbnail_url)
      VALUES (?, ?, 'image', ?, ?)
    `, [orderId, userId, imageUrl, imageUrl]); // Для простоты используем одно и то же изображение
    
    // Получаем созданное сообщение
    const createdMessage = query.get(`
      SELECT 
        cm.id,
        cm.order_id,
        cm.sender_id,
        cm.message_type,
        cm.message_text,
        cm.image_url,
        cm.image_thumbnail_url,
        cm.read_at,
        cm.created_at,
        u.name as sender_name,
        u.role as sender_role
      FROM chat_messages cm
      JOIN users u ON cm.sender_id = u.id
      WHERE cm.id = ?
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

export default router;



