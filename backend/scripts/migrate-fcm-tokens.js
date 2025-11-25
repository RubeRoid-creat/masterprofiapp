import { initDatabase, query } from '../database/db.js';

/**
 * Миграция: добавление таблицы fcm_tokens для push-уведомлений
 */
async function migrateFcmTokens() {
  try {
    console.log('🔄 Начало миграции: добавление таблицы fcm_tokens...');
    
    await initDatabase();
    
    // Проверяем, существует ли уже таблица
    const tableExists = query.get(`
      SELECT name FROM sqlite_master 
      WHERE type='table' AND name='fcm_tokens'
    `);
    
    if (tableExists) {
      console.log('✅ Таблица fcm_tokens уже существует');
      return;
    }
    
    // Создаем таблицу
    query.run(`
      CREATE TABLE IF NOT EXISTS fcm_tokens (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER NOT NULL,
        token TEXT NOT NULL UNIQUE,
        device_type TEXT,
        device_id TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
      )
    `);
    
    // Создаем индексы
    query.run('CREATE INDEX IF NOT EXISTS idx_fcm_tokens_user_id ON fcm_tokens(user_id)');
    query.run('CREATE INDEX IF NOT EXISTS idx_fcm_tokens_token ON fcm_tokens(token)');
    
    console.log('✅ Миграция завершена: таблица fcm_tokens создана');
  } catch (error) {
    console.error('❌ Ошибка миграции:', error);
    process.exit(1);
  }
}

migrateFcmTokens();




