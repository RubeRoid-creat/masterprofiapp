/**
 * Миграция: добавление таблиц для чата с администрацией и обратной связи
 */

import { initDatabase, query } from '../database/db.js';

export async function migrateAdminChatAndFeedback() {
  try {
    console.log('🔄 Начало миграции: добавление таблиц для чата с администрацией и обратной связи...');
    
    await initDatabase();

    // Проверяем, существует ли таблица admin_chat_messages
    const adminChatExists = query.get(`
      SELECT name FROM sqlite_master 
      WHERE type='table' AND name='admin_chat_messages'
    `);

    if (adminChatExists) {
      console.log('✅ Таблица admin_chat_messages уже существует');
    } else {
      // Создаем таблицу сообщений чата с администрацией
      query.run(`
        CREATE TABLE IF NOT EXISTS admin_chat_messages (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          user_id INTEGER NOT NULL,
          sender_id INTEGER NOT NULL, -- ID отправителя (user_id или admin_id)
          sender_role TEXT NOT NULL CHECK(sender_role IN ('user', 'admin')),
          message_type TEXT NOT NULL CHECK(message_type IN ('text', 'image', 'file')) DEFAULT 'text',
          message_text TEXT,
          image_url TEXT,
          image_thumbnail_url TEXT,
          file_url TEXT,
          file_name TEXT,
          read_at DATETIME,
          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
          FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
          FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
        )
      `);

      query.run('CREATE INDEX IF NOT EXISTS idx_admin_chat_messages_user_id ON admin_chat_messages(user_id)');
      query.run('CREATE INDEX IF NOT EXISTS idx_admin_chat_messages_sender_id ON admin_chat_messages(sender_id)');
      query.run('CREATE INDEX IF NOT EXISTS idx_admin_chat_messages_created_at ON admin_chat_messages(created_at)');
      query.run('CREATE INDEX IF NOT EXISTS idx_admin_chat_messages_read_at ON admin_chat_messages(read_at)');

      console.log('✅ Таблица admin_chat_messages создана');
    }

    // Проверяем, существует ли таблица feedback
    const feedbackExists = query.get(`
      SELECT name FROM sqlite_master 
      WHERE type='table' AND name='feedback'
    `);

    if (feedbackExists) {
      console.log('✅ Таблица feedback уже существует');
    } else {
      // Создаем таблицу обратной связи
      query.run(`
        CREATE TABLE IF NOT EXISTS feedback (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          user_id INTEGER NOT NULL,
          feedback_type TEXT NOT NULL CHECK(feedback_type IN ('suggestion', 'bug_report', 'complaint', 'praise', 'other')),
          subject TEXT NOT NULL,
          message TEXT NOT NULL,
          attachments TEXT, -- JSON массив URL файлов
          status TEXT NOT NULL CHECK(status IN ('new', 'in_progress', 'resolved', 'closed')) DEFAULT 'new',
          admin_response TEXT,
          responded_by INTEGER, -- user_id админа
          responded_at DATETIME,
          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
          FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
          FOREIGN KEY (responded_by) REFERENCES users(id) ON DELETE SET NULL
        )
      `);

      query.run('CREATE INDEX IF NOT EXISTS idx_feedback_user_id ON feedback(user_id)');
      query.run('CREATE INDEX IF NOT EXISTS idx_feedback_type ON feedback(feedback_type)');
      query.run('CREATE INDEX IF NOT EXISTS idx_feedback_status ON feedback(status)');
      query.run('CREATE INDEX IF NOT EXISTS idx_feedback_created_at ON feedback(created_at)');

      console.log('✅ Таблица feedback создана');
    }

    console.log('✅ Миграция завершена: таблицы для чата с администрацией и обратной связи созданы');
  } catch (error) {
    console.error('❌ Ошибка миграции:', error);
    throw error;
  }
}

// Если скрипт запущен напрямую
if (import.meta.url === `file://${process.argv[1]}`) {
  migrateAdminChatAndFeedback()
    .then(() => {
      console.log('✅ Миграция успешно завершена');
      process.exit(0);
    })
    .catch((error) => {
      console.error('❌ Ошибка миграции:', error);
      process.exit(1);
    });
}
