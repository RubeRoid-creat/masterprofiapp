import { initDatabase, query } from '../database/db.js';

/**
 * Миграция: добавление таблицы chat_messages для чата по заказам
 */
async function migrateChat() {
  try {
    console.log('🔄 Начало миграции: добавление таблицы chat_messages...');
    
    await initDatabase();
    
    // Проверяем, существует ли уже таблица
    const tableExists = query.get(`
      SELECT name FROM sqlite_master 
      WHERE type='table' AND name='chat_messages'
    `);
    
    if (tableExists) {
      console.log('✅ Таблица chat_messages уже существует');
      return;
    }
    
    // Создаем таблицу
    query.run(`
      CREATE TABLE IF NOT EXISTS chat_messages (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        order_id INTEGER NOT NULL,
        sender_id INTEGER NOT NULL,
        message_type TEXT NOT NULL CHECK(message_type IN ('text', 'image', 'system')) DEFAULT 'text',
        message_text TEXT,
        image_url TEXT,
        image_thumbnail_url TEXT,
        read_at DATETIME,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
        FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
      )
    `);
    
    // Создаем индексы
    query.run('CREATE INDEX IF NOT EXISTS idx_chat_messages_order_id ON chat_messages(order_id)');
    query.run('CREATE INDEX IF NOT EXISTS idx_chat_messages_sender_id ON chat_messages(sender_id)');
    query.run('CREATE INDEX IF NOT EXISTS idx_chat_messages_created_at ON chat_messages(created_at)');
    
    console.log('✅ Миграция завершена: таблица chat_messages создана');
  } catch (error) {
    console.error('❌ Ошибка миграции:', error);
    process.exit(1);
  }
}

migrateChat();



