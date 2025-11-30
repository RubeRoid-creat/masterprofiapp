import { initDatabase, query } from '../database/db.js';

/**
 * Миграция: добавление поля ИНН в таблицу masters
 */
async function migrateInnToMasters() {
  try {
    console.log('🔄 Начало миграции: добавление поля ИНН в таблицу masters...');
    
    await initDatabase();
    
    // Проверяем, существует ли уже поле inn
    const tableInfo = query.all("PRAGMA table_info(masters)");
    const hasInn = tableInfo.some(col => col.name === 'inn');
    
    if (hasInn) {
      console.log('✅ Поле inn уже существует в таблице masters');
      return;
    }
    
    // Добавляем поле inn
    console.log('📝 Добавление поля inn в таблицу masters...');
    try {
      query.run('ALTER TABLE masters ADD COLUMN inn TEXT');
      console.log('✅ Поле inn успешно добавлено в таблицу masters');
    } catch (e) {
      if (e.message.includes('duplicate column')) {
        console.log('✅ Поле inn уже существует');
      } else {
        console.error('❌ Ошибка добавления поля inn:', e.message);
        throw e;
      }
    }
    
    console.log('✅ Миграция завершена успешно');
  } catch (error) {
    console.error('❌ Ошибка миграции:', error);
    process.exit(1);
  }
}

// Запускаем миграцию
migrateInnToMasters();


