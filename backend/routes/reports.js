import express from 'express';
import { query } from '../database/db.js';
import { authenticate, authorize } from '../middleware/auth.js';
import { upload, handleUploadError } from '../middleware/upload.js';

const router = express.Router();

// Получить отчеты (для мастера или клиента)
router.get('/', authenticate, (req, res) => {
  try {
    const { orderId, status } = req.query;
    
    let sql = '';
    const params = [];
    
    // Если пользователь - мастер
    if (req.user.role === 'master') {
      const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
      if (!master) {
        return res.status(404).json({ error: 'Профиль мастера не найден' });
      }
      
      sql = 'SELECT * FROM work_reports WHERE master_id = ?';
      params.push(master.id);
    } 
    // Если пользователь - клиент
    else if (req.user.role === 'client') {
      const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
      if (!client) {
        return res.status(404).json({ error: 'Профиль клиента не найден' });
      }
      
      sql = 'SELECT * FROM work_reports WHERE client_id = ?';
      params.push(client.id);
    } else {
      return res.status(403).json({ error: 'Доступ запрещен' });
    }
    
    if (orderId) {
      sql += ' AND order_id = ?';
      params.push(orderId);
    }
    
    if (status) {
      sql += ' AND status = ?';
      params.push(status);
    }
    
    sql += ' ORDER BY created_at DESC';
    
    const reports = query.all(sql, params);
    
    // Возвращаем массив отчетов напрямую (для совместимости с клиентским приложением)
    res.json(reports.map(report => ({
      id: report.id,
      orderId: report.order_id,
      masterId: report.master_id,
      clientId: report.client_id,
      reportType: report.report_type,
      workDescription: report.work_description,
      partsUsed: report.parts_used ? JSON.parse(report.parts_used) : [],
      workDuration: report.work_duration,
      totalCost: report.total_cost,
      partsCost: report.parts_cost,
      laborCost: report.labor_cost,
      beforePhotos: report.before_photos ? JSON.parse(report.before_photos) : [],
      afterPhotos: report.after_photos ? JSON.parse(report.after_photos) : [],
      clientSignature: report.client_signature,
      clientSignedAt: report.client_signed_at,
      masterSignedAt: report.master_signed_at,
      status: report.status,
      templateId: report.template_id,
      createdAt: report.created_at,
      updatedAt: report.updated_at
    })));
  } catch (error) {
    console.error('Ошибка получения отчетов:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Получить отчет по ID
router.get('/:id', authenticate, (req, res) => {
  try {
    const { id } = req.params;
    const report = query.get('SELECT * FROM work_reports WHERE id = ?', [id]);
    
    if (!report) {
      return res.status(404).json({ error: 'Отчет не найден' });
    }
    
    // Проверяем права доступа
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    const isMaster = master && master.id === report.master_id;
    
    // Для клиента проверяем через таблицу clients
    let isClient = false;
    if (req.user.role === 'client') {
      const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
      isClient = client && client.id === report.client_id;
    }
    
    if (!isMaster && !isClient) {
      return res.status(403).json({ error: 'Недостаточно прав доступа' });
    }
    
    res.json({
      id: report.id,
      orderId: report.order_id,
      masterId: report.master_id,
      clientId: report.client_id,
      reportType: report.report_type,
      workDescription: report.work_description,
      partsUsed: report.parts_used ? JSON.parse(report.parts_used) : [],
      workDuration: report.work_duration,
      totalCost: report.total_cost,
      partsCost: report.parts_cost,
      laborCost: report.labor_cost,
      beforePhotos: report.before_photos ? JSON.parse(report.before_photos) : [],
      afterPhotos: report.after_photos ? JSON.parse(report.after_photos) : [],
      clientSignature: report.client_signature,
      clientSignedAt: report.client_signed_at,
      masterSignedAt: report.master_signed_at,
      status: report.status,
      templateId: report.template_id,
      createdAt: report.created_at,
      updatedAt: report.updated_at
    });
  } catch (error) {
    console.error('Ошибка получения отчета:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Создать отчет
router.post('/', authenticate, authorize('master'), (req, res) => {
  try {
    // Поддерживаем как JSON, так и multipart/form-data
    let orderId, reportType, workDescription, partsUsed, workDuration, totalCost, partsCost, laborCost, beforePhotos, afterPhotos, templateId;
    
    if (req.headers['content-type']?.includes('multipart/form-data')) {
      // Multipart form data
      orderId = req.body.orderId;
      reportType = req.body.reportType;
      workDescription = req.body.workDescription;
      partsUsed = req.body.partsUsed;
      workDuration = req.body.workDuration;
      totalCost = req.body.totalCost;
      partsCost = req.body.partsCost;
      laborCost = req.body.laborCost;
      beforePhotos = req.body.beforePhotos;
      afterPhotos = req.body.afterPhotos;
      templateId = req.body.templateId;
    } else {
      // JSON
      orderId = req.body.order_id || req.body.orderId;
      reportType = req.body.report_type || req.body.reportType;
      workDescription = req.body.work_description || req.body.workDescription;
      partsUsed = req.body.parts_used || req.body.partsUsed;
      workDuration = req.body.work_duration || req.body.workDuration;
      totalCost = req.body.total_cost || req.body.totalCost;
      partsCost = req.body.parts_cost || req.body.partsCost;
      laborCost = req.body.labor_cost || req.body.laborCost;
      beforePhotos = req.body.before_photos || req.body.beforePhotos;
      afterPhotos = req.body.after_photos || req.body.afterPhotos;
      templateId = req.body.template_id || req.body.templateId;
    }
    
    if (!orderId || !workDescription || totalCost === undefined || totalCost === null) {
      return res.status(400).json({ error: 'Необходимо указать orderId (или order_id), workDescription (или work_description) и totalCost (или total_cost)' });
    }
    
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    // Получаем информацию о заказе
    const order = query.get('SELECT client_id FROM orders WHERE id = ?', [orderId]);
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден' });
    }
    
    // Обрабатываем загруженные фото (если multipart)
    const uploadedPhotos = req.files || [];
    let beforePhotosList = [];
    let afterPhotosList = [];
    
    if (Array.isArray(beforePhotos)) {
      beforePhotosList = beforePhotos;
    } else if (typeof beforePhotos === 'string') {
      try {
        beforePhotosList = JSON.parse(beforePhotos);
      } catch (e) {
        beforePhotosList = [];
      }
    }
    
    if (Array.isArray(afterPhotos)) {
      afterPhotosList = afterPhotos;
    } else if (typeof afterPhotos === 'string') {
      try {
        afterPhotosList = JSON.parse(afterPhotos);
      } catch (e) {
        afterPhotosList = [];
      }
    }
    
    // Добавляем загруженные файлы (если multipart)
    uploadedPhotos.forEach(file => {
      const photoUrl = `/uploads/${file.filename}`;
      if (file.fieldname === 'before') {
        beforePhotosList.push(photoUrl);
      } else if (file.fieldname === 'after') {
        afterPhotosList.push(photoUrl);
      }
    });
    
    const result = query.run(`
      INSERT INTO work_reports 
      (order_id, master_id, client_id, report_type, work_description, parts_used, 
       work_duration, total_cost, parts_cost, labor_cost, before_photos, after_photos, 
       template_id, status)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `, [
      orderId,
      master.id,
      order.client_id,
      reportType || 'standard',
      workDescription,
      partsUsed ? (Array.isArray(partsUsed) ? JSON.stringify(partsUsed) : (typeof partsUsed === 'string' ? partsUsed : JSON.stringify(partsUsed))) : null,
      workDuration || null,
      totalCost,
      partsCost || 0,
      laborCost || 0,
      beforePhotosList.length > 0 ? JSON.stringify(beforePhotosList) : null,
      afterPhotosList.length > 0 ? JSON.stringify(afterPhotosList) : null,
      templateId || null,
      'pending_signature'
    ]);
    
    const created = query.get('SELECT * FROM work_reports WHERE id = ?', [result.lastInsertRowid]);
    
    res.status(201).json({
      message: 'Отчет создан',
      report: {
        id: created.id,
        orderId: created.order_id,
        status: created.status
      }
    });
  } catch (error) {
    console.error('Ошибка создания отчета:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Обновить отчет (только мастер, до подписания клиентом)
router.put('/:id', authenticate, authorize('master'), (req, res) => {
  try {
    const { id } = req.params;
    const { workDescription, partsUsed, workDuration, totalCost, partsCost, laborCost } = req.body;
    
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const report = query.get('SELECT * FROM work_reports WHERE id = ? AND master_id = ?', [id, master.id]);
    if (!report) {
      return res.status(404).json({ error: 'Отчет не найден' });
    }
    
    if (report.status !== 'draft' && report.status !== 'pending_signature') {
      return res.status(400).json({ error: 'Отчет уже подписан, нельзя изменить' });
    }
    
    query.run(`
      UPDATE work_reports 
      SET work_description = COALESCE(?, work_description),
          parts_used = COALESCE(?, parts_used),
          work_duration = COALESCE(?, work_duration),
          total_cost = COALESCE(?, total_cost),
          parts_cost = COALESCE(?, parts_cost),
          labor_cost = COALESCE(?, labor_cost),
          updated_at = CURRENT_TIMESTAMP
      WHERE id = ?
    `, [
      workDescription || null,
      partsUsed ? (typeof partsUsed === 'string' ? partsUsed : JSON.stringify(partsUsed)) : null,
      workDuration || null,
      totalCost || null,
      partsCost || null,
      laborCost || null,
      id
    ]);
    
    const updated = query.get('SELECT * FROM work_reports WHERE id = ?', [id]);
    
    res.json({
      message: 'Отчет обновлен',
      report: {
        id: updated.id,
        status: updated.status
      }
    });
  } catch (error) {
    console.error('Ошибка обновления отчета:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Подписать отчет клиентом
router.post('/:id/sign', authenticate, (req, res) => {
  try {
    const { id } = req.params;
    const { signature } = req.body; // Base64 подпись
    
    if (!signature) {
      return res.status(400).json({ error: 'Необходимо предоставить подпись' });
    }
    
    const report = query.get('SELECT * FROM work_reports WHERE id = ?', [id]);
    if (!report) {
      return res.status(404).json({ error: 'Отчет не найден' });
    }
    
    // Проверяем, что это клиент
    if (req.user.id !== report.client_id) {
      return res.status(403).json({ error: 'Только клиент может подписать отчет' });
    }
    
    if (report.status === 'signed' || report.status === 'completed') {
      return res.status(400).json({ error: 'Отчет уже подписан' });
    }
    
    query.run(`
      UPDATE work_reports 
      SET client_signature = ?,
          client_signed_at = CURRENT_TIMESTAMP,
          status = 'signed',
          updated_at = CURRENT_TIMESTAMP
      WHERE id = ?
    `, [signature, id]);
    
    // Обновляем статус заказа на "завершен"
    query.run(
      'UPDATE orders SET repair_status = ?, request_status = ? WHERE id = ?',
      ['completed', 'completed', report.order_id]
    );
    
    res.json({ message: 'Отчет подписан' });
  } catch (error) {
    console.error('Ошибка подписания отчета:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Получить шаблоны отчетов
router.get('/templates', authenticate, authorize('master'), (req, res) => {
  try {
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    // Получаем шаблоны мастера и общие шаблоны
    const templates = query.all(`
      SELECT * FROM report_templates 
      WHERE (master_id = ? OR master_id IS NULL) AND is_active = 1
      ORDER BY master_id DESC, created_at DESC
    `, [master.id]);
    
    res.json({
      templates: templates.map(template => ({
        id: template.id,
        masterId: template.master_id,
        name: template.name,
        description: template.description,
        workDescriptionTemplate: template.work_description_template,
        defaultParts: template.default_parts ? JSON.parse(template.default_parts) : [],
        defaultLaborCost: template.default_labor_cost
      }))
    });
  } catch (error) {
    console.error('Ошибка получения шаблонов:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

// Создать шаблон отчета
router.post('/templates', authenticate, authorize('master'), (req, res) => {
  try {
    const { name, description, workDescriptionTemplate, defaultParts, defaultLaborCost } = req.body;
    
    if (!name || !workDescriptionTemplate) {
      return res.status(400).json({ error: 'Необходимо указать name и workDescriptionTemplate' });
    }
    
    const master = query.get('SELECT id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }
    
    const result = query.run(`
      INSERT INTO report_templates 
      (master_id, name, description, work_description_template, default_parts, default_labor_cost)
      VALUES (?, ?, ?, ?, ?, ?)
    `, [
      master.id,
      name,
      description || null,
      workDescriptionTemplate,
      defaultParts ? JSON.stringify(JSON.parse(defaultParts)) : null,
      defaultLaborCost || null
    ]);
    
    const created = query.get('SELECT * FROM report_templates WHERE id = ?', [result.lastInsertRowid]);
    
    res.status(201).json({
      message: 'Шаблон создан',
      template: {
        id: created.id,
        name: created.name
      }
    });
  } catch (error) {
    console.error('Ошибка создания шаблона:', error);
    res.status(500).json({ error: 'Ошибка сервера', details: error.message });
  }
});

export default router;

