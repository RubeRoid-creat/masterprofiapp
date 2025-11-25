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

// Получить документы верификации мастера
router.get('/documents', authenticate, authorize('master'), (req, res) => {
  try {
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const documents = query.all(`
      SELECT 
        id, document_type, document_name, file_url, file_name, file_size,
        mime_type, status, rejection_reason, reviewed_at, created_at
      FROM master_verification_documents
      WHERE master_id = ?
      ORDER BY created_at DESC
    `, [master.id]);
    
    res.json(documents);
  } catch (error) {
    console.error('Ошибка получения документов:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Загрузить документ верификации
router.post('/documents', authenticate, authorize('master'), upload.single('document'), (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'Необходимо загрузить файл' });
    }
    
    const { documentType, documentName } = req.body;
    
    if (!documentType || !documentName) {
      // Удаляем загруженный файл, если нет обязательных полей
      if (req.file.path) {
        try {
          unlinkSync(req.file.path);
        } catch (e) {
          console.error('Ошибка удаления файла:', e);
        }
      }
      return res.status(400).json({ error: 'Необходимо указать documentType и documentName' });
    }
    
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      // Удаляем загруженный файл
      if (req.file.path) {
        try {
          unlinkSync(req.file.path);
        } catch (e) {
          console.error('Ошибка удаления файла:', e);
        }
      }
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    // Формируем URL файла
    const fileUrl = `/uploads/${req.file.filename}`;
    
    // Сохраняем информацию о документе
    const result = query.run(`
      INSERT INTO master_verification_documents 
      (master_id, document_type, document_name, file_url, file_name, file_size, mime_type, status)
      VALUES (?, ?, ?, ?, ?, ?, ?, 'pending')
    `, [
      master.id,
      documentType,
      documentName,
      fileUrl,
      req.file.originalname,
      req.file.size,
      req.file.mimetype
    ]);
    
    // Обновляем статус верификации мастера на "pending", если был "not_verified"
    const currentStatus = query.get(
      'SELECT verification_status FROM masters WHERE id = ?',
      [master.id]
    );
    
    if (currentStatus && currentStatus.verification_status === 'not_verified') {
      query.run(
        'UPDATE masters SET verification_status = ? WHERE id = ?',
        ['pending', master.id]
      );
    }
    
    const document = query.get(`
      SELECT * FROM master_verification_documents WHERE id = ?
    `, [result.lastInsertRowid]);
    
    res.status(201).json({
      message: 'Документ загружен',
      document: {
        id: document.id,
        documentType: document.document_type,
        documentName: document.document_name,
        fileUrl: document.file_url,
        status: document.status,
        createdAt: document.created_at
      }
    });
  } catch (error) {
    console.error('Ошибка загрузки документа:', error);
    handleUploadError(error, req, res);
  }
});

// Удалить документ верификации (только если статус pending)
router.delete('/documents/:id', authenticate, authorize('master'), (req, res) => {
  try {
    const { id } = req.params;
    
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const document = query.get(`
      SELECT * FROM master_verification_documents 
      WHERE id = ? AND master_id = ?
    `, [id, master.id]);
    
    if (!document) {
      return res.status(404).json({ error: 'Документ не найден' });
    }
    
    if (document.status !== 'pending') {
      return res.status(400).json({ error: 'Можно удалить только документы со статусом pending' });
    }
    
    // Удаляем файл
    if (document.file_url) {
      const filePath = join(__dirname, '..', document.file_url);
      if (existsSync(filePath)) {
        try {
          unlinkSync(filePath);
        } catch (e) {
          console.error('Ошибка удаления файла:', e);
        }
      }
    }
    
    // Удаляем запись из БД
    query.run('DELETE FROM master_verification_documents WHERE id = ?', [id]);
    
    res.json({ message: 'Документ удален' });
  } catch (error) {
    console.error('Ошибка удаления документа:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Админ: получить все документы на модерацию
router.get('/admin/documents', authenticate, authorize('admin'), (req, res) => {
  try {
    const { status } = req.query;
    
    let sql = `
      SELECT 
        d.*,
        m.id as master_id,
        u.name as master_name,
        u.email as master_email,
        u.phone as master_phone
      FROM master_verification_documents d
      JOIN masters m ON d.master_id = m.id
      JOIN users u ON m.user_id = u.id
      WHERE 1=1
    `;
    const params = [];
    
    if (status) {
      sql += ' AND d.status = ?';
      params.push(status);
    }
    
    sql += ' ORDER BY d.created_at DESC';
    
    const documents = query.all(sql, params);
    
    res.json(documents);
  } catch (error) {
    console.error('Ошибка получения документов:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Админ: одобрить документ
router.post('/admin/documents/:id/approve', authenticate, authorize('admin'), (req, res) => {
  try {
    const { id } = req.params;
    
    const document = query.get('SELECT * FROM master_verification_documents WHERE id = ?', [id]);
    if (!document) {
      return res.status(404).json({ error: 'Документ не найден' });
    }
    
    if (document.status !== 'pending') {
      return res.status(400).json({ error: 'Документ уже обработан' });
    }
    
    // Обновляем статус документа
    query.run(`
      UPDATE master_verification_documents 
      SET status = ?, reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP
      WHERE id = ?
    `, ['approved', req.user.id, id]);
    
    // Проверяем, все ли документы мастера одобрены
    const pendingDocuments = query.get(`
      SELECT COUNT(*) as count 
      FROM master_verification_documents 
      WHERE master_id = ? AND status = 'pending'
    `, [document.master_id]);
    
    // Если все документы одобрены, обновляем статус верификации мастера
    if (pendingDocuments.count === 0) {
      query.run(
        'UPDATE masters SET verification_status = ? WHERE id = ?',
        ['verified', document.master_id]
      );
    }
    
    res.json({ message: 'Документ одобрен' });
  } catch (error) {
    console.error('Ошибка одобрения документа:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Админ: отклонить документ
router.post('/admin/documents/:id/reject', authenticate, authorize('admin'), (req, res) => {
  try {
    const { id } = req.params;
    const { reason } = req.body;
    
    if (!reason) {
      return res.status(400).json({ error: 'Необходимо указать причину отклонения' });
    }
    
    const document = query.get('SELECT * FROM master_verification_documents WHERE id = ?', [id]);
    if (!document) {
      return res.status(404).json({ error: 'Документ не найден' });
    }
    
    if (document.status !== 'pending') {
      return res.status(400).json({ error: 'Документ уже обработан' });
    }
    
    // Обновляем статус документа
    query.run(`
      UPDATE master_verification_documents 
      SET status = ?, rejection_reason = ?, reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP
      WHERE id = ?
    `, ['rejected', reason, req.user.id, id]);
    
    // Обновляем статус верификации мастера
    query.run(
      'UPDATE masters SET verification_status = ? WHERE id = ?',
      ['rejected', document.master_id]
    );
    
    res.json({ message: 'Документ отклонен' });
  } catch (error) {
    console.error('Ошибка отклонения документа:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

export default router;

