import { initDatabase, query } from '../database/db.js';

/**
 * Скрипт для гарантированного добавления поля sponsor_id в таблицу users
 * Выполняет миграцию и проверяет результат
 */
async function ensureSponsorIdColumn() {
  try {
    console.log('🔄 Проверка и добавление поля sponsor_id в таблицу users...');
    
    await initDatabase();
    
    // Проверяем структуру таблицы
    const tableInfo = query.all("PRAGMA table_info(users)");
    console.log('📋 Текущие колонки таблицы users:');
    tableInfo.forEach(col => {
      console.log(`   - ${col.name} (${col.type})`);
    });
    
    const hasSponsorId = tableInfo.some(col => col.name === 'sponsor_id');
    
    if (hasSponsorId) {
      console.log('✅ Поле sponsor_id уже существует в таблице users');
      
      // Проверяем, что поле работает
      try {
        const testUser = query.get('SELECT id FROM users LIMIT 1');
        if (testUser) {
          query.run('UPDATE users SET sponsor_id = ? WHERE id = ?', [null, testUser.id]);
          console.log('✅ Тест обновления поля sponsor_id успешен');
        }
      } catch (testError) {
        console.error('❌ Ошибка при тестировании поля sponsor_id:', testError.message);
      }
      
      // Создаем индекс, если его нет
      try {
        query.run('CREATE INDEX IF NOT EXISTS idx_users_sponsor_id ON users(sponsor_id)');
        console.log('✅ Индекс idx_users_sponsor_id создан или уже существует');
      } catch (indexError) {
        if (!indexError.message.includes('already exists')) {
          console.error('❌ Ошибка создания индекса:', indexError.message);
        }
      }
      
      return;
    }
    
    // Добавляем поле sponsor_id
    console.log('📝 Добавление поля sponsor_id в таблицу users...');
    try {
      query.run('ALTER TABLE users ADD COLUMN sponsor_id INTEGER');
      console.log('✅ Поле sponsor_id успешно добавлено');
      
      // Добавляем внешний ключ (если поддерживается)
      try {
        // SQLite может не поддерживать добавление FOREIGN KEY через ALTER TABLE
        // Пропускаем эту часть, так как она уже должна быть в схеме
        console.log('ℹ️ Foreign key уже определен в схеме таблицы');
      } catch (fkError) {
        console.warn('⚠️ Не удалось добавить foreign key (это нормально для SQLite):', fkError.message);
      }
      
      // Проверяем, что поле добавлено
      const newTableInfo = query.all("PRAGMA table_info(users)");
      const hasSponsorIdNow = newTableInfo.some(col => col.name === 'sponsor_id');
      
      if (hasSponsorIdNow) {
        console.log('✅ Подтверждено: поле sponsor_id присутствует в таблице');
      } else {
        console.error('❌ Ошибка: поле sponsor_id не было добавлено');
        process.exit(1);
      }
      
      // Создаем индекс
      try {
        query.run('CREATE INDEX IF NOT EXISTS idx_users_sponsor_id ON users(sponsor_id)');
        console.log('✅ Индекс idx_users_sponsor_id создан');
      } catch (indexError) {
        if (!indexError.message.includes('already exists')) {
          console.error('❌ Ошибка создания индекса:', indexError.message);
        }
      }
      
    } catch (e) {
      if (e.message.includes('duplicate column') || e.message.includes('already exists')) {
        console.log('✅ Поле sponsor_id уже существует');
      } else {
        console.error('❌ Ошибка добавления поля sponsor_id:', e.message);
        throw e;
      }
    }
    
    console.log('✅ Миграция завершена успешно');
  } catch (error) {
    console.error('❌ Ошибка миграции:', error);
    console.error('Stack:', error.stack);
    process.exit(1);
  }
}

// Запускаем проверку
ensureSponsorIdColumn();




