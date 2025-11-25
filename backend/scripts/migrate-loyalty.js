import { query } from '../database/db.js';
import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

async function migrateLoyalty() {
  try {
    console.log('🔄 Начало миграции: Программа лояльности...');
    
    // Создаем таблицу loyalty_points
    query.run(`
      CREATE TABLE IF NOT EXISTS loyalty_points (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        client_id INTEGER NOT NULL,
        points INTEGER NOT NULL DEFAULT 0,
        source_type TEXT NOT NULL CHECK(source_type IN ('order', 'review', 'referral', 'bonus', 'spent')),
        source_id INTEGER, -- ID заказа, отзыва и т.д.
        description TEXT,
        expires_at DATETIME, -- Дата истечения баллов (опционально)
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
      )
    `);
    
    // Создаем таблицу для истории использования баллов
    query.run(`
      CREATE TABLE IF NOT EXISTS loyalty_transactions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        client_id INTEGER NOT NULL,
        points_used INTEGER NOT NULL,
        order_id INTEGER,
        description TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL
      )
    `);
    
    // Создаем индексы
    query.run(`
      CREATE INDEX IF NOT EXISTS idx_loyalty_points_client_id 
      ON loyalty_points(client_id)
    `);
    
    query.run(`
      CREATE INDEX IF NOT EXISTS idx_loyalty_points_expires_at 
      ON loyalty_points(expires_at)
    `);
    
    query.run(`
      CREATE INDEX IF NOT EXISTS idx_loyalty_transactions_client_id 
      ON loyalty_transactions(client_id)
    `);
    
    // Добавляем поле total_points в таблицу clients для быстрого доступа
    try {
      query.run(`
        ALTER TABLE clients ADD COLUMN total_loyalty_points INTEGER DEFAULT 0
      `);
    } catch (e) {
      // Колонка уже существует, игнорируем ошибку
      if (!e.message.includes('duplicate column')) {
        throw e;
      }
    }
    
    console.log('✅ Миграция программы лояльности завершена');
  } catch (error) {
    console.error('❌ Ошибка миграции:', error);
    throw error;
  }
}

migrateLoyalty();

