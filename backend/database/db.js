import initSqlJs from 'sql.js';
import { readFileSync, writeFileSync, existsSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import { config } from '../config.js';

// Импорт PostgreSQL (если используется)
let postgresQuery = null;

// Асинхронная инициализация PostgreSQL
async function initPostgreSQLIfNeeded() {
  if (config.databaseType === 'postgresql') {
    try {
      const postgresDb = await import('./postgres-db.js');
      postgresDb.initPostgreSQL();
      postgresQuery = postgresDb.query;
      console.log('✅ PostgreSQL инициализирован');
    } catch (error) {
      console.warn('⚠️ PostgreSQL модуль не найден, используем SQLite:', error.message);
    }
  }
}

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

let SQL;
let db;

// Инициализация sql.js
async function initSQL() {
  if (!SQL) {
    SQL = await initSqlJs();
  }
  return SQL;
}

// Загрузка или создание базы данных
async function loadDatabase() {
  await initSQL();
  
  if (existsSync(config.databasePath)) {
    const buffer = readFileSync(config.databasePath);
    db = new SQL.Database(buffer);
    console.log('✅ База данных загружена из файла');
  } else {
    db = new SQL.Database();
    console.log('✅ Создана новая база данных');
  }
  
  // Включаем поддержку внешних ключей
  db.run('PRAGMA foreign_keys = ON');
  
  return db;
}

// Сохранение базы данных в файл
export function saveDatabase() {
  if (db) {
    const data = db.export();
    const buffer = Buffer.from(data);
    writeFileSync(config.databasePath, buffer);
  }
}

// Перезагрузка базы данных
export async function reloadDatabase() {
  await loadDatabase();
}

// Функция для инициализации схемы БД
export async function initDatabase() {
  if (!db) {
    await loadDatabase();
  }
  
  const schemaPath = join(__dirname, 'schema.sql');
  const schema = readFileSync(schemaPath, 'utf-8');
  
  // Разделяем SQL-запросы и выполняем их по очереди
  const statements = schema.split(';').filter(stmt => stmt.trim());
  
  for (const statement of statements) {
    if (statement.trim()) {
      try {
        db.run(statement);
      } catch (error) {
        // Игнорируем ошибки "table already exists" и "index already exists"
        if (error.message.includes('already exists') || error.message.includes('duplicate')) {
          continue;
        }
        
        // Обрабатываем ошибку "no such column" для существующих таблиц
        if (error.message.includes('no such column')) {
          const columnMatch = error.message.match(/no such column: (\w+)/);
          if (columnMatch) {
            const columnName = columnMatch[1];
            // Определяем таблицу из запроса
            let tableName = null;
            
            // Автоматическое добавление поля inn в таблицу masters
            if (columnName === 'inn' && (statement.includes('masters') || statement.toUpperCase().includes('FROM masters') || statement.toUpperCase().includes('UPDATE masters') || statement.toUpperCase().includes('INTO masters'))) {
              try {
                // Проверяем, существует ли уже поле
                const stmt = db.prepare("PRAGMA table_info(masters)");
                const tableInfo = [];
                while (stmt.step()) {
                  tableInfo.push(stmt.getAsObject());
                }
                stmt.free();
                const hasInn = tableInfo && tableInfo.some(col => col.name === 'inn');
                
                if (!hasInn) {
                  db.run('ALTER TABLE masters ADD COLUMN inn TEXT');
                  console.log('✅ Автоматически добавлено поле inn в таблицу masters');
                  // Повторяем запрос после добавления поля
                  try {
                    db.run(statement);
                    continue;
                  } catch (retryError) {
                    if (retryError.message.includes('already exists') || retryError.message.includes('duplicate')) {
                      continue;
                    }
                    throw retryError;
                  }
                }
              } catch (alterError) {
                if (!alterError.message.includes('duplicate column') && !alterError.message.includes('already exists')) {
                  console.error('Ошибка добавления поля inn:', alterError);
                }
              }
            }
            
            // Автоматическое добавление поля sponsor_id в таблицу users
            if (columnName === 'sponsor_id' && (statement.includes('users') || statement.toUpperCase().includes('FROM users') || statement.toUpperCase().includes('UPDATE users') || statement.toUpperCase().includes('INTO users') || statement.toUpperCase().includes('idx_users_sponsor_id'))) {
              try {
                // Проверяем, существует ли уже поле
                const stmt = db.prepare("PRAGMA table_info(users)");
                const tableInfo = [];
                while (stmt.step()) {
                  tableInfo.push(stmt.getAsObject());
                }
                stmt.free();
                const hasSponsorId = tableInfo && tableInfo.some(col => col.name === 'sponsor_id');
                
                if (!hasSponsorId) {
                  db.run('ALTER TABLE users ADD COLUMN sponsor_id INTEGER');
                  console.log('✅ Автоматически добавлено поле sponsor_id в таблицу users');
                  // Повторяем запрос после добавления поля
                  try {
                    db.run(statement);
                    continue;
                  } catch (retryError) {
                    if (retryError.message.includes('already exists') || retryError.message.includes('duplicate')) {
                      continue;
                    }
                    throw retryError;
                  }
                }
              } catch (alterError) {
                if (!alterError.message.includes('duplicate column') && !alterError.message.includes('already exists')) {
                  console.error('Ошибка добавления поля sponsor_id:', alterError);
                }
              }
            }
            
            if (statement.includes('loyalty_points')) {
              tableName = 'loyalty_points';
            } else if (statement.includes('clients')) {
              tableName = 'clients';
            }
            
            if (tableName) {
              try {
                // Проверяем, существует ли таблица
                const tableInfo = db.exec(`PRAGMA table_info(${tableName})`);
                if (tableInfo.length > 0) {
                  // Проверяем, есть ли уже эта колонка
                  const columns = tableInfo[0].values.map(row => row[1]);
                  if (!columns.includes(columnName)) {
                    // Таблица существует, но колонки нет - добавляем
                    let alterStatement = '';
                    if (columnName === 'used') {
                      alterStatement = `ALTER TABLE ${tableName} ADD COLUMN ${columnName} INTEGER DEFAULT 0`;
                    } else if (columnName === 'total_loyalty_points') {
                      alterStatement = `ALTER TABLE ${tableName} ADD COLUMN ${columnName} INTEGER DEFAULT 0`;
                    }
                    
                    if (alterStatement) {
                      db.run(alterStatement);
                      console.log(`✅ Добавлена колонка ${columnName} в таблицу ${tableName}`);
                      // Повторно выполняем исходный запрос (создание индекса)
                      try {
                        db.run(statement);
                        continue;
                      } catch (retryError) {
                        // Если это был индекс и он уже существует, игнорируем
                        if (retryError.message.includes('already exists') || retryError.message.includes('duplicate')) {
                          continue;
                        }
                        throw retryError;
                      }
                    }
                  }
                }
              } catch (alterError) {
                console.error(`Ошибка добавления колонки ${columnName}:`, alterError.message);
              }
            }
          }
        }
        
        // Если ошибка не обработана, выводим её
        console.error('Ошибка выполнения SQL:', statement.substring(0, 100));
        console.error('Детали ошибки:', error.message);
        // Не бросаем ошибку для индексов, которые могут уже существовать
        if (!statement.trim().toUpperCase().startsWith('CREATE INDEX')) {
          throw error;
        }
      }
    }
  }
  
  saveDatabase();
  console.log('✅ База данных инициализирована');
}

// Функция для автоматического добавления поля inn при ошибке
function addInnColumnIfNeeded(sql) {
  try {
    // Проверяем, относится ли запрос к таблице masters
    const sqlUpper = sql.toUpperCase().replace(/\s+/g, ' ');
    const isMastersTable = sqlUpper.includes(' MASTERS') || 
                           sqlUpper.includes('FROM MASTERS') || 
                           sqlUpper.includes('UPDATE MASTERS') || 
                           sqlUpper.includes('INTO MASTERS') || 
                           sqlUpper.includes('JOIN MASTERS');
    
    if (!isMastersTable) {
      return false; // Не относится к таблице masters
    }
    
    // Проверяем, существует ли уже поле inn
    try {
      const stmt = db.prepare("PRAGMA table_info(masters)");
      const tableInfo = [];
      while (stmt.step()) {
        const row = stmt.getAsObject();
        if (row && row.name === 'inn') {
          stmt.free();
          return false; // Поле уже существует
        }
        tableInfo.push(row);
      }
      stmt.free();
    } catch (checkError) {
      console.error('Ошибка проверки структуры таблицы masters:', checkError.message);
      // Продолжаем, если проверка не удалась - попробуем добавить поле
    }
    
    // Пытаемся добавить поле inn (безопасно, если уже существует - просто проигнорируем ошибку)
    try {
      db.run('ALTER TABLE masters ADD COLUMN inn TEXT');
      console.log('✅ Автоматически добавлено поле inn в таблицу masters');
      saveDatabase();
      return true; // Поле добавлено
    } catch (alterError) {
      const errorMsg = alterError.message || '';
      if (errorMsg.includes('duplicate column') || 
          errorMsg.includes('already exists') ||
          errorMsg.includes('UNIQUE constraint')) {
        // Поле уже существует, это нормально
        console.log('ℹ️ Поле inn уже существует в таблице masters');
        return false;
      }
      // Если другая ошибка - выводим её, но не прерываем выполнение
      console.error('Ошибка добавления поля inn:', errorMsg);
      return false;
    }
  } catch (e) {
    console.error('Ошибка в addInnColumnIfNeeded:', e.message);
    return false;
  }
}

// Утилита для выполнения запросов
// Если используется PostgreSQL, делегируем запросы туда
export const query = config.databaseType === 'postgresql' && postgresQuery ? postgresQuery : {
  get: (sql, params = []) => {
    try {
      const stmt = db.prepare(sql);
      stmt.bind(params);
      const result = stmt.step() ? stmt.getAsObject() : null;
      stmt.free();
      return result;
    } catch (error) {
      // Обрабатываем ошибку "no such column: inn"
      if (error.message.includes('no such column: inn')) {
        if (addInnColumnIfNeeded(sql)) {
          // Повторяем запрос после добавления поля
          const stmt = db.prepare(sql);
          stmt.bind(params);
          const result = stmt.step() ? stmt.getAsObject() : null;
          stmt.free();
          return result;
        }
      }
      throw error;
    }
  },
  
  all: (sql, params = []) => {
    try {
      const stmt = db.prepare(sql);
      stmt.bind(params);
      const results = [];
      while (stmt.step()) {
        results.push(stmt.getAsObject());
      }
      stmt.free();
      return results;
    } catch (error) {
      // Обрабатываем ошибку "no such column: inn"
      if (error.message.includes('no such column: inn')) {
        if (addInnColumnIfNeeded(sql)) {
          // Повторяем запрос после добавления поля
          const stmt = db.prepare(sql);
          stmt.bind(params);
          const results = [];
          while (stmt.step()) {
            results.push(stmt.getAsObject());
          }
          stmt.free();
          return results;
        }
      }
      throw error;
    }
  },
  
  run: (sql, params = []) => {
    try {
      const stmt = db.prepare(sql);
      stmt.bind(params);
      stmt.step();
      stmt.free();
      
      // Получаем lastInsertRowid ДО сохранения
      const result = db.exec('SELECT last_insert_rowid() as id');
      const lastInsertRowid = result[0]?.values[0]?.[0] || 0;
      
      // Сохраняем после каждого изменения
      saveDatabase();
      
      return {
        lastInsertRowid: lastInsertRowid,
        changes: db.getRowsModified()
      };
    } catch (error) {
      // Обрабатываем ошибку "no such column: inn"
      if (error.message.includes('no such column: inn')) {
        if (addInnColumnIfNeeded(sql)) {
          // Повторяем запрос после добавления поля
          const stmt = db.prepare(sql);
          stmt.bind(params);
          stmt.step();
          stmt.free();
          
          const result = db.exec('SELECT last_insert_rowid() as id');
          const lastInsertRowid = result[0]?.values[0]?.[0] || 0;
          saveDatabase();
          
          return {
            lastInsertRowid: lastInsertRowid,
            changes: db.getRowsModified()
          };
        }
      }
      throw error;
    }
  },
  
  transaction: (fn) => {
    db.run('BEGIN TRANSACTION');
    try {
      const result = fn();
      db.run('COMMIT');
      saveDatabase();
      return result;
    } catch (error) {
      db.run('ROLLBACK');
      throw error;
    }
  }
};

// Инициализация при импорте
await loadDatabase();

export default db;
