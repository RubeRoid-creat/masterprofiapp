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
    
    const { documentType, documentName, inn } = req.body;
    
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
    
    // ИНН обязателен для верификации
    if (!inn) {
      if (req.file.path) {
        try {
          unlinkSync(req.file.path);
        } catch (e) {
          console.error('Ошибка удаления файла:', e);
        }
      }
      return res.status(400).json({ error: 'ИНН обязателен для верификации. Пожалуйста, укажите ваш ИНН.' });
    }
    
    // Валидация ИНН
    const innRegex = /^\d{10}$|^\d{12}$/;
    if (!innRegex.test(inn)) {
      if (req.file.path) {
        try {
          unlinkSync(req.file.path);
        } catch (e) {
          console.error('Ошибка удаления файла:', e);
        }
      }
      return res.status(400).json({ error: 'ИНН должен содержать 10 или 12 цифр' });
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
    
    // Сохраняем ИНН (обязательное поле)
    // Убеждаемся, что поле inn существует в таблице masters
    try {
      query.run(
        'UPDATE masters SET inn = ? WHERE id = ?',
        [inn, master.id]
      );
    } catch (innError) {
      // Если ошибка "no such column: inn", добавляем поле и повторяем запрос
      if (innError.message && innError.message.includes('no such column: inn')) {
        try {
          query.run('ALTER TABLE masters ADD COLUMN inn TEXT');
          console.log('✅ Поле inn добавлено при сохранении документа');
          // Повторяем запрос
          query.run(
            'UPDATE masters SET inn = ? WHERE id = ?',
            [inn, master.id]
          );
        } catch (alterError) {
          if (!alterError.message.includes('duplicate column') && !alterError.message.includes('already exists')) {
            throw alterError;
          }
          // Если поле уже существует, повторяем UPDATE
          query.run(
            'UPDATE masters SET inn = ? WHERE id = ?',
            [inn, master.id]
          );
        }
      } else {
        throw innError;
      }
    }
    
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

// Админ: получить все документы на модерацию (сгруппированные по мастерам)
router.get('/admin/documents', authenticate, authorize('admin'), (req, res) => {
  try {
    const { status, groupByMaster } = req.query;
    
    let sql = `
      SELECT 
        d.*,
        m.id as master_id,
        m.inn as master_inn,
        m.verification_status as master_verification_status,
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
    
    // Если запрошена группировка по мастерам
    if (groupByMaster === 'true') {
      const grouped = {};
      documents.forEach(doc => {
        if (!grouped[doc.master_id]) {
          grouped[doc.master_id] = {
            master_id: doc.master_id,
            master_name: doc.master_name,
            master_email: doc.master_email,
            master_phone: doc.master_phone,
            master_inn: doc.master_inn,
            master_verification_status: doc.master_verification_status,
            documents: []
          };
        }
        grouped[doc.master_id].documents.push(doc);
      });
      res.json(Object.values(grouped));
    } else {
      res.json(documents);
    }
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
    
    // Проверяем статус всех документов мастера
    const allDocuments = query.all(`
      SELECT status 
      FROM master_verification_documents 
      WHERE master_id = ?
    `, [document.master_id]);
    
    // Проверяем, что у мастера есть ИНН и текущий статус
    const masterInfo = query.get('SELECT inn, verification_status FROM masters WHERE id = ?', [document.master_id]);
    
    if (!masterInfo) {
      return res.status(404).json({ error: 'Мастер не найден' });
    }
    
    // Подсчитываем документы по статусам
    const statusCounts = {
      pending: 0,
      approved: 0,
      rejected: 0
    };
    
    allDocuments.forEach(doc => {
      if (doc.status === 'pending') statusCounts.pending++;
      else if (doc.status === 'approved') statusCounts.approved++;
      else if (doc.status === 'rejected') statusCounts.rejected++;
    });
    
    // Если нет pending документов и есть хотя бы один одобренный - верифицируем мастера
    if (statusCounts.pending === 0 && statusCounts.approved > 0) {
      if (!masterInfo.inn) {
        return res.json({ 
          message: 'Документ одобрен. Для верификации мастера требуется указать ИНН.',
          needsInn: true
        });
      }
      
      // Обновляем статус верификации мастера
      query.run(
        'UPDATE masters SET verification_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
        ['verified', document.master_id]
      );
      
      console.log(`✅ Мастер #${document.master_id} автоматически верифицирован после одобрения всех документов`);
      
      res.json({ 
        message: 'Документ одобрен. Мастер автоматически верифицирован.',
        masterVerified: true
      });
    } else {
      res.json({ 
        message: 'Документ одобрен',
        pendingCount: statusCounts.pending,
        approvedCount: statusCounts.approved
      });
    }
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

// Админ: получить список мастеров на верификацию
router.get('/admin/masters', authenticate, authorize('admin'), (req, res) => {
  try {
    const { status } = req.query;
    
    let sql = `
      SELECT 
        m.*,
        u.name,
        u.email,
        u.phone,
        COUNT(d.id) as documents_count,
        SUM(CASE WHEN d.status = 'pending' THEN 1 ELSE 0 END) as pending_documents_count,
        SUM(CASE WHEN d.status = 'approved' THEN 1 ELSE 0 END) as approved_documents_count,
        SUM(CASE WHEN d.status = 'rejected' THEN 1 ELSE 0 END) as rejected_documents_count
      FROM masters m
      JOIN users u ON m.user_id = u.id
      LEFT JOIN master_verification_documents d ON m.id = d.master_id
      WHERE 1=1
    `;
    const params = [];
    
    if (status) {
      sql += ' AND m.verification_status = ?';
      params.push(status);
    }
    
    sql += ' GROUP BY m.id ORDER BY m.created_at DESC';
    
    const masters = query.all(sql, params);
    
    res.json(masters);
  } catch (error) {
    console.error('Ошибка получения мастеров:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Админ: получить все документы конкретного мастера
router.get('/admin/masters/:masterId/documents', authenticate, authorize('admin'), (req, res) => {
  try {
    const { masterId } = req.params;
    const { status } = req.query;
    
    let sql = `
      SELECT 
        d.*,
        m.inn as master_inn,
        m.verification_status as master_verification_status,
        u.name as master_name,
        u.email as master_email,
        u.phone as master_phone
      FROM master_verification_documents d
      JOIN masters m ON d.master_id = m.id
      JOIN users u ON m.user_id = u.id
      WHERE d.master_id = ?
    `;
    const params = [masterId];
    
    if (status) {
      sql += ' AND d.status = ?';
      params.push(status);
    }
    
    sql += ' ORDER BY d.created_at DESC';
    
    const documents = query.all(sql, params);
    
    res.json(documents);
  } catch (error) {
    console.error('Ошибка получения документов мастера:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Админ: верифицировать мастера целиком
router.post('/admin/masters/:masterId/verify', authenticate, authorize('admin'), (req, res) => {
  try {
    const { masterId } = req.params;
    
    // Проверяем мастера
    const master = query.get('SELECT * FROM masters WHERE id = ?', [masterId]);
    if (!master) {
      return res.status(404).json({ error: 'Мастер не найден' });
    }
    
    // Проверяем, что у мастера есть ИНН
    if (!master.inn) {
      return res.status(400).json({ error: 'У мастера не указан ИНН' });
    }
    
    // Проверяем, что все документы одобрены
    const pendingDocuments = query.get(`
      SELECT COUNT(*) as count 
      FROM master_verification_documents 
      WHERE master_id = ? AND status = 'pending'
    `, [masterId]);
    
    if (pendingDocuments.count > 0) {
      return res.status(400).json({ 
        error: 'Не все документы одобрены. Сначала одобрите все документы мастера.' 
      });
    }
    
    // Проверяем, что есть хотя бы один одобренный документ
    const approvedDocuments = query.get(`
      SELECT COUNT(*) as count 
      FROM master_verification_documents 
      WHERE master_id = ? AND status = 'approved'
    `, [masterId]);
    
    if (approvedDocuments.count === 0) {
      return res.status(400).json({ 
        error: 'Нет одобренных документов. Необходимо одобрить хотя бы один документ.' 
      });
    }
    
    // Верифицируем мастера
    query.run(
      'UPDATE masters SET verification_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      ['verified', masterId]
    );
    
    res.json({ message: 'Мастер успешно верифицирован' });
  } catch (error) {
    console.error('Ошибка верификации мастера:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Админ: отклонить верификацию мастера
router.post('/admin/masters/:masterId/reject', authenticate, authorize('admin'), (req, res) => {
  try {
    const { masterId } = req.params;
    const { reason } = req.body;
    
    if (!reason) {
      return res.status(400).json({ error: 'Необходимо указать причину отклонения' });
    }
    
    // Проверяем мастера
    const master = query.get('SELECT * FROM masters WHERE id = ?', [masterId]);
    if (!master) {
      return res.status(404).json({ error: 'Мастер не найден' });
    }
    
    // Отклоняем верификацию мастера
    query.run(
      'UPDATE masters SET verification_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      ['rejected', masterId]
    );
    
    // Отклоняем все pending документы мастера
    query.run(`
      UPDATE master_verification_documents 
      SET status = ?, rejection_reason = ?, reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP
      WHERE master_id = ? AND status = 'pending'
    `, ['rejected', reason, req.user.id, masterId]);
    
    res.json({ message: 'Верификация мастера отклонена' });
  } catch (error) {
    console.error('Ошибка отклонения верификации мастера:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

export default router;

