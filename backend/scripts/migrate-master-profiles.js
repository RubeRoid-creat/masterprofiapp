import { readFileSync, existsSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import initSqlJs from 'sql.js';
import { config } from '../config.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

async function migrate() {
  try {
    console.log('🔄 Начало миграции профилей мастеров...');
    
    // Инициализация SQL.js
    const SQL = await initSqlJs();
    
    // Загрузка базы данных
    if (!existsSync(config.databasePath)) {
      console.error('❌ База данных не найдена!');
      process.exit(1);
    }
    
    const buffer = readFileSync(config.databasePath);
    const db = new SQL.Database(buffer);
    console.log('✅ База данных загружена');
    
    // Добавляем поля в таблицу masters
    console.log('📝 Добавление полей в таблицу masters...');
    
    try {
      db.run(`
        ALTER TABLE masters 
        ADD COLUMN photo_url TEXT
      `);
      console.log('  ✅ Добавлено поле photo_url');
    } catch (e) {
      if (!e.message.includes('duplicate column')) {
        console.log('  ⚠️ Поле photo_url уже существует или ошибка:', e.message);
      }
    }
    
    try {
      db.run(`
        ALTER TABLE masters 
        ADD COLUMN bio TEXT
      `);
      console.log('  ✅ Добавлено поле bio');
    } catch (e) {
      if (!e.message.includes('duplicate column')) {
        console.log('  ⚠️ Поле bio уже существует или ошибка:', e.message);
      }
    }
    
    try {
      db.run(`
        ALTER TABLE masters 
        ADD COLUMN experience_years INTEGER DEFAULT 0
      `);
      console.log('  ✅ Добавлено поле experience_years');
    } catch (e) {
      if (!e.message.includes('duplicate column')) {
        console.log('  ⚠️ Поле experience_years уже существует или ошибка:', e.message);
      }
    }
    
    // Создаем таблицу для портфолио
    console.log('📝 Создание таблицы master_portfolio...');
    db.run(`
      CREATE TABLE IF NOT EXISTS master_portfolio (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        master_id INTEGER NOT NULL,
        image_url TEXT NOT NULL,
        description TEXT,
        category TEXT,
        order_index INTEGER DEFAULT 0,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (master_id) REFERENCES masters(id) ON DELETE CASCADE
      )
    `);
    console.log('  ✅ Таблица master_portfolio создана');
    
    // Создаем таблицу для сертификатов
    console.log('📝 Создание таблицы master_certificates...');
    db.run(`
      CREATE TABLE IF NOT EXISTS master_certificates (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        master_id INTEGER NOT NULL,
        title TEXT NOT NULL,
        issuer TEXT,
        issue_date DATE,
        expiry_date DATE,
        certificate_url TEXT NOT NULL,
        description TEXT,
        order_index INTEGER DEFAULT 0,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (master_id) REFERENCES masters(id) ON DELETE CASCADE
      )
    `);
    console.log('  ✅ Таблица master_certificates создана');
    
    // Сохраняем базу данных
    const data = db.export();
    const fs = await import('fs');
    fs.writeFileSync(config.databasePath, Buffer.from(data));
    console.log('✅ База данных сохранена');
    
    console.log('✅ Миграция завершена успешно!');
    process.exit(0);
  } catch (error) {
    console.error('❌ Ошибка миграции:', error);
    process.exit(1);
  }
}

migrate();





