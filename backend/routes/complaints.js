import express from 'express';
import { query } from '../database/db.js';
import { authenticate, authorize } from '../middleware/auth.js';
import { upload, handleUploadError } from '../middleware/upload.js';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';
import { existsSync, unlinkSync } from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const router = express.Router();

// Создать жалобу
router.post('/', authenticate, upload.array('evidence', 5), (req, res) => {
  try {
    const { orderId, accusedUserId, complaintType, title, description } = req.body;
    
    if (!accusedUserId || !complaintType || !title || !description) {
      return res.status(400).json({ error: 'Необходимо указать accusedUserId, complaintType, title и description' });
    }
    
    // Определяем роль жалобщика
    const user = query.get('SELECT role FROM users WHERE id = ?', [req.user.id]);
    if (!user) {
      return res.status(404).json({ error: 'Пользователь не найден' });
    }
    
    const complainantRole = user.role; // 'client' или 'master'
    
    // Определяем роль обвиняемого
    const accusedUser = query.get('SELECT role FROM users WHERE id = ?', [accusedUserId]);
    if (!accusedUser) {
      return res.status(404).json({ error: 'Обвиняемый пользователь не найден' });
    }
    
    const accusedRole = accusedUser.role;
    
    // Проверяем, что жалобщик и обвиняемый разные пользователи
    if (req.user.id === parseInt(accusedUserId)) {
      return res.status(400).json({ error: 'Нельзя подать жалобу на самого себя' });
    }
    
    // Обрабатываем загруженные файлы
    const evidenceUrls = [];
    if (req.files && req.files.length > 0) {
      req.files.forEach(file => {
        evidenceUrls.push(`/uploads/${file.filename}`);
      });
    }
    
    // Создаем жалобу
    const result = query.run(`
      INSERT INTO complaints 
      (order_id, complainant_user_id, complainant_role, accused_user_id, accused_role,
       complaint_type, title, description, evidence_urls, status)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending')
    `, [
      orderId || null,
      req.user.id,
      complainantRole,
      accusedUserId,
      accusedRole,
      complaintType,
      title,
      description,
      JSON.stringify(evidenceUrls)
    ]);
    
    const complaint = query.get('SELECT * FROM complaints WHERE id = ?', [result.lastInsertRowid]);
    
    res.status(201).json({
      message: 'Жалоба создана',
      complaint: {
        id: complaint.id,
        orderId: complaint.order_id,
        complaintType: complaint.complaint_type,
        title: complaint.title,
        status: complaint.status,
        createdAt: complaint.created_at
      }
    });
  } catch (error) {
    console.error('Ошибка создания жалобы:', error);
    handleUploadError(error, req, res);
  }
});

// Получить мои жалобы (как жалобщик)
router.get('/my', authenticate, (req, res) => {
  try {
    const complaints = query.all(`
      SELECT 
        c.*,
        u1.name as complainant_name,
        u2.name as accused_name,
        o.order_number
      FROM complaints c
      JOIN users u1 ON c.complainant_user_id = u1.id
      JOIN users u2 ON c.accused_user_id = u2.id
      LEFT JOIN orders o ON c.order_id = o.id
      WHERE c.complainant_user_id = ?
      ORDER BY c.created_at DESC
    `, [req.user.id]);
    
    // Парсим JSON поля
    const parsedComplaints = complaints.map(c => ({
      ...c,
      evidenceUrls: c.evidence_urls ? JSON.parse(c.evidence_urls) : []
    }));
    
    res.json(parsedComplaints);
  } catch (error) {
    console.error('Ошибка получения жалоб:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Получить жалобы на меня (как обвиняемый)
router.get('/against-me', authenticate, (req, res) => {
  try {
    const complaints = query.all(`
      SELECT 
        c.*,
        u1.name as complainant_name,
        u2.name as accused_name,
        o.order_number
      FROM complaints c
      JOIN users u1 ON c.complainant_user_id = u1.id
      JOIN users u2 ON c.accused_user_id = u2.id
      LEFT JOIN orders o ON c.order_id = o.id
      WHERE c.accused_user_id = ?
      ORDER BY c.created_at DESC
    `, [req.user.id]);
    
    // Парсим JSON поля
    const parsedComplaints = complaints.map(c => ({
      ...c,
      evidenceUrls: c.evidence_urls ? JSON.parse(c.evidence_urls) : []
    }));
    
    res.json(parsedComplaints);
  } catch (error) {
    console.error('Ошибка получения жалоб:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Админ: получить все жалобы
router.get('/admin/all', authenticate, authorize('admin'), (req, res) => {
  try {
    const { status } = req.query;
    
    let sql = `
      SELECT 
        c.*,
        u1.name as complainant_name,
        u1.email as complainant_email,
        u2.name as accused_name,
        u2.email as accused_email,
        o.order_number,
        admin.name as resolved_by_name
      FROM complaints c
      JOIN users u1 ON c.complainant_user_id = u1.id
      JOIN users u2 ON c.accused_user_id = u2.id
      LEFT JOIN orders o ON c.order_id = o.id
      LEFT JOIN users admin ON c.resolved_by = admin.id
      WHERE 1=1
    `;
    const params = [];
    
    if (status) {
      sql += ' AND c.status = ?';
      params.push(status);
    }
    
    sql += ' ORDER BY c.created_at DESC';
    
    const complaints = query.all(sql, params);
    
    // Парсим JSON поля
    const parsedComplaints = complaints.map(c => ({
      ...c,
      evidenceUrls: c.evidence_urls ? JSON.parse(c.evidence_urls) : []
    }));
    
    res.json(parsedComplaints);
  } catch (error) {
    console.error('Ошибка получения жалоб:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Админ: получить жалобу по ID
router.get('/admin/:id', authenticate, authorize('admin'), (req, res) => {
  try {
    const { id } = req.params;
    
    const complaint = query.get(`
      SELECT 
        c.*,
        u1.name as complainant_name,
        u1.email as complainant_email,
        u1.phone as complainant_phone,
        u2.name as accused_name,
        u2.email as accused_email,
        u2.phone as accused_phone,
        o.*,
        admin.name as resolved_by_name
      FROM complaints c
      JOIN users u1 ON c.complainant_user_id = u1.id
      JOIN users u2 ON c.accused_user_id = u2.id
      LEFT JOIN orders o ON c.order_id = o.id
      LEFT JOIN users admin ON c.resolved_by = admin.id
      WHERE c.id = ?
    `, [id]);
    
    if (!complaint) {
      return res.status(404).json({ error: 'Жалоба не найдена' });
    }
    
    complaint.evidenceUrls = complaint.evidence_urls ? JSON.parse(complaint.evidence_urls) : [];
    
    res.json(complaint);
  } catch (error) {
    console.error('Ошибка получения жалобы:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Админ: разрешить жалобу
router.post('/admin/:id/resolve', authenticate, authorize('admin'), (req, res) => {
  try {
    const { id } = req.params;
    const { resolution, status } = req.body;
    
    if (!resolution || !status) {
      return res.status(400).json({ error: 'Необходимо указать resolution и status' });
    }
    
    if (!['resolved', 'rejected', 'dismissed'].includes(status)) {
      return res.status(400).json({ error: 'Некорректный статус' });
    }
    
    const complaint = query.get('SELECT * FROM complaints WHERE id = ?', [id]);
    if (!complaint) {
      return res.status(404).json({ error: 'Жалоба не найдена' });
    }
    
    if (complaint.status !== 'pending' && complaint.status !== 'reviewing') {
      return res.status(400).json({ error: 'Жалоба уже обработана' });
    }
    
    // Обновляем статус жалобы
    query.run(`
      UPDATE complaints 
      SET status = ?, resolution = ?, resolved_by = ?, resolved_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
      WHERE id = ?
    `, [status, resolution, req.user.id, id]);
    
    res.json({ message: 'Жалоба обработана' });
  } catch (error) {
    console.error('Ошибка обработки жалобы:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Админ: изменить статус жалобы (например, на "reviewing")
router.put('/admin/:id/status', authenticate, authorize('admin'), (req, res) => {
  try {
    const { id } = req.params;
    const { status } = req.body;
    
    if (!status) {
      return res.status(400).json({ error: 'Необходимо указать status' });
    }
    
    const complaint = query.get('SELECT * FROM complaints WHERE id = ?', [id]);
    if (!complaint) {
      return res.status(404).json({ error: 'Жалоба не найдена' });
    }
    
    query.run(`
      UPDATE complaints 
      SET status = ?, updated_at = CURRENT_TIMESTAMP
      WHERE id = ?
    `, [status, id]);
    
    res.json({ message: 'Статус обновлен' });
  } catch (error) {
    console.error('Ошибка обновления статуса:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

export default router;

