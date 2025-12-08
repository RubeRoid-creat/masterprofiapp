import Database from 'better-sqlite3';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const dbPath = join(__dirname, '..', 'database', 'bestapp.db');
const db = new Database(dbPath);

console.log('🔍 Проверка поля photo_url в таблице masters...');

try {
  // Получаем информацию о таблице
  const tableInfo = db.prepare("PRAGMA table_info(masters)").all();
  
  const hasPhotoUrl = tableInfo && Array.isArray(tableInfo) && tableInfo.some(col => col && col.name === 'photo_url');
  
  if (!hasPhotoUrl) {
    console.log('📝 Добавление поля photo_url в таблицу masters...');
    try {
      db.prepare('ALTER TABLE masters ADD COLUMN photo_url TEXT').run();
      console.log('✅ Поле photo_url успешно добавлено в таблицу masters');
    } catch (e) {
      if (e.message.includes('duplicate column') || e.message.includes('already exists')) {
        console.log('ℹ️ Поле photo_url уже существует');
      } else {
        console.error('⚠️ Ошибка добавления поля photo_url:', e.message);
        process.exit(1);
      }
    }
  } else {
    console.log('✅ Поле photo_url уже присутствует в таблице masters');
  }
  
  // Проверяем структуру таблицы
  console.log('\n📋 Структура таблицы masters:');
  tableInfo.forEach(col => {
    console.log(`  - ${col.name} (${col.type})`);
  });
  
  db.close();
  console.log('\n✅ Проверка завершена');
} catch (error) {
  console.error('❌ Ошибка:', error.message);
  db.close();
  process.exit(1);
}
