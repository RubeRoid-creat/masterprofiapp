// Скрипт для ручного создания бэкапа базы данных
import { createBackup, listBackups } from '../services/backup-service.js';
import { initDatabase } from '../database/db.js';

(async () => {
  try {
    console.log('🔄 Инициализация базы данных...');
    await initDatabase();
    
    console.log('💾 Создание резервной копии...');
    const backup = createBackup();
    
    console.log('');
    console.log('✅ Бэкап успешно создан!');
    console.log(`   Файл: ${backup.fileName}`);
    console.log(`   Размер: ${(backup.fileSize / 1024 / 1024).toFixed(2)} MB`);
    console.log(`   Дата: ${backup.createdAt}`);
    console.log('');
    
    // Показываем список всех бэкапов
    const backups = listBackups();
    console.log(`📦 Всего бэкапов: ${backups.length}`);
    if (backups.length > 0) {
      console.log('   Последние 5 бэкапов:');
      backups.slice(0, 5).forEach((b, i) => {
        console.log(`   ${i + 1}. ${b.fileName} (${(b.fileSize / 1024 / 1024).toFixed(2)} MB)`);
      });
    }
    
    process.exit(0);
  } catch (error) {
    console.error('❌ Ошибка создания бэкапа:', error);
    process.exit(1);
  }
})();

