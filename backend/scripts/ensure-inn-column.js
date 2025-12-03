import { initDatabase, query } from '../database/db.js';

/**
 * Скрипт для гарантированного добавления поля inn
 * Выполняет миграцию и проверяет результат
 */
async function ensureInnColumn() {
  try {
    console.log('🔄 Проверка и добавление поля inn в таблицу masters...');
    
    await initDatabase();
    
    // Проверяем структуру таблицы
    const tableInfo = query.all("PRAGMA table_info(masters)");
    console.log('📋 Текущие колонки таблицы masters:');
    tableInfo.forEach(col => {
      console.log(`   - ${col.name} (${col.type})`);
    });
    
    const hasInn = tableInfo.some(col => col.name === 'inn');
    
    if (hasInn) {
      console.log('✅ Поле inn уже существует в таблице masters');
      
      // Проверяем, что поле работает
      try {
        const testMaster = query.get('SELECT id FROM masters LIMIT 1');
        if (testMaster) {
          query.run('UPDATE masters SET inn = ? WHERE id = ?', [null, testMaster.id]);
          console.log('✅ Тест обновления поля inn успешен');
        }
      } catch (testError) {
        console.error('❌ Ошибка при тестировании поля inn:', testError.message);
        // Пытаемся пересоздать поле
        console.log('🔄 Попытка пересоздания поля...');
        try {
          // SQLite не поддерживает DROP COLUMN напрямую, поэтому просто добавляем заново
          query.run('ALTER TABLE masters ADD COLUMN inn TEXT');
          console.log('✅ Поле inn пересоздано');
        } catch (e) {
          console.error('❌ Не удалось пересоздать поле:', e.message);
        }
      }
      
      return;
    }
    
    // Добавляем поле inn
    console.log('📝 Добавление поля inn в таблицу masters...');
    try {
      query.run('ALTER TABLE masters ADD COLUMN inn TEXT');
      console.log('✅ Поле inn успешно добавлено');
      
      // Проверяем, что поле добавлено
      const newTableInfo = query.all("PRAGMA table_info(masters)");
      const hasInnNow = newTableInfo.some(col => col.name === 'inn');
      
      if (hasInnNow) {
        console.log('✅ Подтверждено: поле inn присутствует в таблице');
      } else {
        console.error('❌ Ошибка: поле inn не было добавлено');
        process.exit(1);
      }
      
    } catch (e) {
      if (e.message.includes('duplicate column') || e.message.includes('already exists')) {
        console.log('✅ Поле inn уже существует');
      } else {
        console.error('❌ Ошибка добавления поля inn:', e.message);
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
ensureInnColumn();



