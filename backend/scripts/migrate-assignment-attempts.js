// Миграция: добавление поля attempt_number в order_assignments
import { query } from '../database/db.js';

try {
  console.log('🔄 Миграция: добавление поля attempt_number в order_assignments...');
  
  // Проверяем, существует ли уже поле
  const tableInfo = query.all("PRAGMA table_info(order_assignments)");
  const hasAttemptNumber = tableInfo.some(col => col.name === 'attempt_number');
  
  if (!hasAttemptNumber) {
    // Добавляем поле attempt_number
    query.run(`
      ALTER TABLE order_assignments 
      ADD COLUMN attempt_number INTEGER DEFAULT 1
    `);
    
    // Устанавливаем attempt_number = 1 для всех существующих записей
    query.run(`
      UPDATE order_assignments 
      SET attempt_number = 1 
      WHERE attempt_number IS NULL
    `);
    
    console.log('✅ Поле attempt_number успешно добавлено');
  } else {
    console.log('ℹ️ Поле attempt_number уже существует');
  }
  
  console.log('✅ Миграция завершена');
} catch (error) {
  console.error('❌ Ошибка миграции:', error);
  throw error;
}

