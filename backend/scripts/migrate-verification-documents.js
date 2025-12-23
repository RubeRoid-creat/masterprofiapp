// Миграция: создание таблицы master_verification_documents
import { query } from '../database/db.js';

try {
  console.log('🔄 Миграция: создание таблицы master_verification_documents...');
  
  query.run(`
    CREATE TABLE IF NOT EXISTS master_verification_documents (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      master_id INTEGER NOT NULL,
      document_type TEXT NOT NULL CHECK(document_type IN ('passport', 'certificate', 'diploma', 'license', 'other')),
      document_name TEXT NOT NULL,
      file_url TEXT NOT NULL,
      file_name TEXT,
      file_size INTEGER,
      mime_type TEXT,
      status TEXT NOT NULL CHECK(status IN ('pending', 'approved', 'rejected')) DEFAULT 'pending',
      rejection_reason TEXT,
      reviewed_by INTEGER, -- user_id админа
      reviewed_at DATETIME,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (master_id) REFERENCES masters(id) ON DELETE CASCADE,
      FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
    )
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_verification_documents_master_id 
    ON master_verification_documents(master_id)
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_verification_documents_status 
    ON master_verification_documents(status)
  `);
  
  // Добавляем поле verification_status в таблицу masters, если его нет
  const tableInfo = query.all("PRAGMA table_info(masters)");
  const hasVerificationStatus = tableInfo.some(col => col.name === 'verification_status');
  
  if (!hasVerificationStatus) {
    query.run(`
      ALTER TABLE masters 
      ADD COLUMN verification_status TEXT DEFAULT 'not_verified' 
      CHECK(verification_status IN ('not_verified', 'pending', 'verified', 'rejected'))
    `);
    
    console.log('✅ Поле verification_status добавлено в таблицу masters');
  }
  
  console.log('✅ Миграция завершена');
} catch (error) {
  console.error('❌ Ошибка миграции:', error);
  throw error;
}

