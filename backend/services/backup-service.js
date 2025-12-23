import { copyFileSync, mkdirSync, existsSync, readdirSync, statSync, unlinkSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';
import { config } from '../config.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// Путь к папке с бэкапами
const BACKUP_DIR = join(__dirname, '..', 'backups');
const MAX_BACKUPS = parseInt(process.env.MAX_BACKUPS || '30'); // Хранить последние 30 бэкапов

// Создаем папку для бэкапов, если её нет
if (!existsSync(BACKUP_DIR)) {
  mkdirSync(BACKUP_DIR, { recursive: true });
}

/**
 * Создает резервную копию базы данных
 * @returns {Object} Информация о созданном бэкапе
 */
export function createBackup() {
  try {
    const dbPath = config.databasePath.startsWith('./') 
      ? join(__dirname, '..', config.databasePath.substring(2))
      : config.databasePath;
    
    if (!existsSync(dbPath)) {
      throw new Error(`База данных не найдена: ${dbPath}`);
    }
    
    // Формируем имя файла бэкапа с датой и временем
    const now = new Date();
    const dateStr = now.toISOString().replace(/[:.]/g, '-').slice(0, -5); // YYYY-MM-DDTHH-MM-SS
    const backupFileName = `database-backup-${dateStr}.sqlite`;
    const backupPath = join(BACKUP_DIR, backupFileName);
    
    // Копируем файл базы данных
    copyFileSync(dbPath, backupPath);
    
    // Получаем размер файла
    const stats = statSync(backupPath);
    const fileSize = stats.size;
    
    console.log(`✅ Бэкап создан: ${backupFileName} (${(fileSize / 1024 / 1024).toFixed(2)} MB)`);
    
    // Удаляем старые бэкапы, если их больше MAX_BACKUPS
    cleanupOldBackups();
    
    return {
      success: true,
      fileName: backupFileName,
      filePath: backupPath,
      fileSize: fileSize,
      createdAt: now.toISOString()
    };
  } catch (error) {
    console.error('❌ Ошибка создания бэкапа:', error);
    throw error;
  }
}

/**
 * Удаляет старые бэкапы, оставляя только последние MAX_BACKUPS
 */
function cleanupOldBackups() {
  try {
    const files = readdirSync(BACKUP_DIR)
      .filter(file => file.startsWith('database-backup-') && file.endsWith('.sqlite'))
      .map(file => ({
        name: file,
        path: join(BACKUP_DIR, file),
        time: statSync(join(BACKUP_DIR, file)).mtime.getTime()
      }))
      .sort((a, b) => b.time - a.time); // Сортируем по времени (новые первыми)
    
    // Удаляем старые бэкапы
    if (files.length > MAX_BACKUPS) {
      const filesToDelete = files.slice(MAX_BACKUPS);
      filesToDelete.forEach(file => {
        try {
          unlinkSync(file.path);
          console.log(`🗑️ Удален старый бэкап: ${file.name}`);
        } catch (error) {
          console.error(`Ошибка удаления бэкапа ${file.name}:`, error);
        }
      });
    }
  } catch (error) {
    console.error('Ошибка очистки старых бэкапов:', error);
  }
}

/**
 * Получает список всех бэкапов
 * @returns {Array} Массив информации о бэкапах
 */
export function listBackups() {
  try {
    if (!existsSync(BACKUP_DIR)) {
      return [];
    }
    
    const files = readdirSync(BACKUP_DIR)
      .filter(file => file.startsWith('database-backup-') && file.endsWith('.sqlite'))
      .map(file => {
        const filePath = join(BACKUP_DIR, file);
        const stats = statSync(filePath);
        return {
          fileName: file,
          filePath: filePath,
          fileSize: stats.size,
          createdAt: stats.birthtime.toISOString(),
          modifiedAt: stats.mtime.toISOString()
        };
      })
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)); // Сортируем по дате (новые первыми)
    
    return files;
  } catch (error) {
    console.error('Ошибка получения списка бэкапов:', error);
    return [];
  }
}

/**
 * Восстанавливает базу данных из бэкапа
 * @param {string} backupFileName - Имя файла бэкапа
 * @returns {Object} Результат восстановления
 */
export function restoreBackup(backupFileName) {
  try {
    const backupPath = join(BACKUP_DIR, backupFileName);
    
    if (!existsSync(backupPath)) {
      throw new Error(`Бэкап не найден: ${backupFileName}`);
    }
    
    const dbPath = config.databasePath.startsWith('./') 
      ? join(__dirname, '..', config.databasePath.substring(2))
      : config.databasePath;
    
    // Создаем резервную копию текущей БД перед восстановлением
    const now = new Date();
    const preRestoreBackup = `pre-restore-${now.toISOString().replace(/[:.]/g, '-').slice(0, -5)}.sqlite`;
    const preRestorePath = join(BACKUP_DIR, preRestoreBackup);
    
    if (existsSync(dbPath)) {
      copyFileSync(dbPath, preRestorePath);
      console.log(`✅ Создана резервная копия перед восстановлением: ${preRestoreBackup}`);
    }
    
    // Восстанавливаем из бэкапа
    copyFileSync(backupPath, dbPath);
    
    console.log(`✅ База данных восстановлена из бэкапа: ${backupFileName}`);
    
    return {
      success: true,
      message: 'База данных успешно восстановлена',
      preRestoreBackup: preRestoreBackup
    };
  } catch (error) {
    console.error('❌ Ошибка восстановления бэкапа:', error);
    throw error;
  }
}

export default {
  createBackup,
  listBackups,
  restoreBackup
};

