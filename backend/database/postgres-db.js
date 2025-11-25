import pg from 'pg';
import { config } from '../config.js';

const { Pool } = pg;

let pool = null;

// Инициализация пула подключений PostgreSQL
export function initPostgreSQL() {
  try {
    const postgresConfig = {
      host: process.env.POSTGRES_HOST || 'localhost',
      port: parseInt(process.env.POSTGRES_PORT || '5432'),
      database: process.env.POSTGRES_DB || 'bestapp',
      user: process.env.POSTGRES_USER || 'bestapp_user',
      password: process.env.POSTGRES_PASSWORD || '',
      max: 20, // Максимальное количество клиентов в пуле
      idleTimeoutMillis: 30000,
      connectionTimeoutMillis: 2000,
    };

    pool = new Pool(postgresConfig);

    pool.on('error', (err) => {
      console.error('Unexpected error on idle PostgreSQL client', err);
    });

    // Проверяем подключение (асинхронно)
    pool.query('SELECT NOW()')
      .then(() => {
        console.log('✅ PostgreSQL подключен');
      })
      .catch((err) => {
        console.error('❌ Ошибка подключения к PostgreSQL:', err.message);
      });

    return pool;
  } catch (error) {
    console.error('❌ Ошибка инициализации PostgreSQL:', error);
    return null;
  }
}

// Утилита для выполнения запросов (аналог query из db.js)
export const query = {
  // Получить одну запись
  get: async (sql, params = []) => {
    if (!pool) {
      throw new Error('PostgreSQL pool не инициализирован');
    }
    
    try {
      const result = await pool.query(sql, params);
      return result.rows[0] || null;
    } catch (error) {
      console.error('Ошибка выполнения запроса (get):', error);
      throw error;
    }
  },
  
  // Получить все записи
  all: async (sql, params = []) => {
    if (!pool) {
      throw new Error('PostgreSQL pool не инициализирован');
    }
    
    try {
      // Конвертируем SQLite синтаксис в PostgreSQL
      const pgSql = sql.replace(/\?/g, (match, offset) => {
        const paramIndex = sql.substring(0, offset).split('?').length;
        return `$${paramIndex}`;
      });
      const result = await pool.query(pgSql, params);
      return result.rows;
    } catch (error) {
      console.error('Ошибка выполнения запроса (all):', error);
      throw error;
    }
  },
  
  // Выполнить запрос (INSERT, UPDATE, DELETE)
  run: async (sql, params = []) => {
    if (!pool) {
      throw new Error('PostgreSQL pool не инициализирован');
    }
    
    try {
      // Конвертируем SQLite синтаксис в PostgreSQL
      let pgSql = sql.replace(/\?/g, (match, offset) => {
        const paramIndex = sql.substring(0, offset).split('?').length;
        return `$${paramIndex}`;
      });
      
      // Для INSERT добавляем RETURNING id
      if (pgSql.trim().toUpperCase().startsWith('INSERT')) {
        if (!pgSql.toUpperCase().includes('RETURNING')) {
          pgSql += ' RETURNING id';
        }
      }
      
      const result = await pool.query(pgSql, params);
      return {
        lastInsertRowid: result.rows[0]?.id || null,
        changes: result.rowCount || 0
      };
    } catch (error) {
      console.error('Ошибка выполнения запроса (run):', error);
      throw error;
    }
  },
  
  // Транзакция
  transaction: async (fn) => {
    if (!pool) {
      throw new Error('PostgreSQL pool не инициализирован');
    }
    
    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      const result = await fn(client);
      await client.query('COMMIT');
      return result;
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }
};

// Инициализация схемы БД
export async function initPostgreSQLSchema() {
  if (!pool) {
    throw new Error('PostgreSQL pool не инициализирован');
  }
  
  try {
    const schemaPath = new URL('./postgres-schema.sql', import.meta.url);
    const schema = await import('fs').then(fs => 
      fs.promises.readFile(schemaPath, 'utf-8')
    );
    
    // Выполняем схему
    await pool.query(schema);
    console.log('✅ Схема PostgreSQL инициализирована');
  } catch (error) {
    console.error('Ошибка инициализации схемы PostgreSQL:', error);
    throw error;
  }
}

export default pool;

