// Миграция: создание таблицы complaints
import { query } from '../database/db.js';

try {
  console.log('🔄 Миграция: создание таблицы complaints...');
  
  query.run(`
    CREATE TABLE IF NOT EXISTS complaints (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      order_id INTEGER,
      complainant_user_id INTEGER NOT NULL, -- ID пользователя, который подал жалобу
      complainant_role TEXT NOT NULL CHECK(complainant_role IN ('client', 'master')),
      accused_user_id INTEGER NOT NULL, -- ID пользователя, на которого подали жалобу
      accused_role TEXT NOT NULL CHECK(accused_role IN ('client', 'master')),
      complaint_type TEXT NOT NULL CHECK(complaint_type IN ('quality', 'behavior', 'payment', 'other')),
      title TEXT NOT NULL,
      description TEXT NOT NULL,
      evidence_urls TEXT, -- JSON массив URL файлов (скриншоты, фото)
      status TEXT NOT NULL CHECK(status IN ('pending', 'reviewing', 'resolved', 'rejected', 'dismissed')) DEFAULT 'pending',
      resolution TEXT, -- Решение админа
      resolved_by INTEGER, -- user_id админа
      resolved_at DATETIME,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL,
      FOREIGN KEY (complainant_user_id) REFERENCES users(id) ON DELETE CASCADE,
      FOREIGN KEY (accused_user_id) REFERENCES users(id) ON DELETE CASCADE,
      FOREIGN KEY (resolved_by) REFERENCES users(id) ON DELETE SET NULL
    )
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_complaints_order_id 
    ON complaints(order_id)
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_complaints_complainant 
    ON complaints(complainant_user_id)
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_complaints_accused 
    ON complaints(accused_user_id)
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_complaints_status 
    ON complaints(status)
  `);
  
  console.log('✅ Миграция завершена');
} catch (error) {
  console.error('❌ Ошибка миграции:', error);
  throw error;
}

