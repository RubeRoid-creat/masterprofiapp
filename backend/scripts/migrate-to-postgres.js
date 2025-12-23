import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import { initSQL } from 'sql.js';
import pg from 'pg';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const { Pool } = pg;

// Конфигурация PostgreSQL
const postgresConfig = {
  host: process.env.POSTGRES_HOST || 'localhost',
  port: parseInt(process.env.POSTGRES_PORT || '5432'),
  database: process.env.POSTGRES_DB || 'bestapp',
  user: process.env.POSTGRES_USER || 'bestapp_user',
  password: process.env.POSTGRES_PASSWORD || '',
};

// Путь к SQLite базе данных
const sqlitePath = process.env.DATABASE_PATH || join(__dirname, '..', 'database.sqlite');

async function migrateData() {
  console.log('🚀 Начало миграции данных из SQLite в PostgreSQL...\n');
  
  // Инициализация SQLite
  console.log('📦 Загрузка SQLite базы данных...');
  const SQL = await initSQL();
  const buffer = readFileSync(sqlitePath);
  const db = new SQL.Database(buffer);
  console.log('✅ SQLite база данных загружена\n');
  
  // Подключение к PostgreSQL
  console.log('📦 Подключение к PostgreSQL...');
  const pool = new Pool(postgresConfig);
  
  try {
    await pool.query('SELECT NOW()');
    console.log('✅ Подключение к PostgreSQL установлено\n');
  } catch (error) {
    console.error('❌ Ошибка подключения к PostgreSQL:', error.message);
    process.exit(1);
  }
  
  // Функция для выполнения запроса в SQLite
  function sqliteQuery(sql, params = []) {
    const stmt = db.prepare(sql);
    stmt.bind(params);
    const results = [];
    while (stmt.step()) {
      results.push(stmt.getAsObject());
    }
    stmt.free();
    return results;
  }
  
  // Функция для получения одной записи из SQLite
  function sqliteGet(sql, params = []) {
    const stmt = db.prepare(sql);
    stmt.bind(params);
    const result = stmt.step() ? stmt.getAsObject() : null;
    stmt.free();
    return result;
  }
  
  // Начинаем транзакцию
  const client = await pool.connect();
  
  try {
    await client.query('BEGIN');
    
    // Миграция пользователей
    console.log('👥 Миграция пользователей...');
    const users = sqliteQuery('SELECT * FROM users ORDER BY id');
    for (const user of users) {
      await client.query(`
        INSERT INTO users (id, email, password_hash, name, phone, role, created_at, updated_at)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
        ON CONFLICT (id) DO NOTHING
      `, [
        user.id, user.email, user.password_hash, user.name, user.phone, user.role,
        user.created_at, user.updated_at
      ]);
    }
    console.log(`✅ Мигрировано ${users.length} пользователей\n`);
    
    // Миграция мастеров
    console.log('🔧 Миграция мастеров...');
    const masters = sqliteQuery('SELECT * FROM masters ORDER BY id');
    for (const master of masters) {
      await client.query(`
        INSERT INTO masters (id, user_id, specialization, rating, completed_orders, status, 
                           latitude, longitude, is_on_shift, balance, verification_status, created_at, updated_at)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)
        ON CONFLICT (id) DO NOTHING
      `, [
        master.id, master.user_id, master.specialization, master.rating, master.completed_orders,
        master.status, master.latitude, master.longitude, master.is_on_shift === 1, master.balance,
        master.verification_status, master.created_at, master.updated_at
      ]);
    }
    console.log(`✅ Мигрировано ${masters.length} мастеров\n`);
    
    // Миграция клиентов
    console.log('👤 Миграция клиентов...');
    const clients = sqliteQuery('SELECT * FROM clients ORDER BY id');
    for (const client of clients) {
      await client.query(`
        INSERT INTO clients (id, user_id, address, latitude, longitude, total_loyalty_points, created_at, updated_at)
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
        ON CONFLICT (id) DO NOTHING
      `, [
        client.id, client.user_id, client.address, client.latitude, client.longitude,
        client.total_loyalty_points || 0, client.created_at, client.updated_at
      ]);
    }
    console.log(`✅ Мигрировано ${clients.length} клиентов\n`);
    
    // Миграция заказов
    console.log('📋 Миграция заказов...');
    const orders = sqliteQuery('SELECT * FROM orders ORDER BY id');
    for (const order of orders) {
      await client.query(`
        INSERT INTO orders (
          id, order_number, client_id, request_status, priority, order_source,
          device_type, device_category, device_brand, device_model, device_serial_number, device_year, warranty_status,
          problem_short_description, problem_description, problem_when_started, problem_conditions, 
          problem_error_codes, problem_attempted_fixes,
          address, address_street, address_building, address_apartment, address_floor, 
          address_entrance_code, address_landmark, latitude, longitude,
          arrival_time, desired_repair_date, urgency,
          estimated_cost, final_cost, client_budget, payment_type, visit_cost, max_cost_without_approval,
          intercom_working, needs_pass, parking_available, has_pets, has_small_children, 
          needs_shoe_covers, preferred_contact_method,
          master_gender_preference, master_min_experience, preferred_master_id, assigned_master_id, assignment_date,
          preliminary_diagnosis, required_parts, special_equipment, repair_complexity, estimated_repair_time,
          problem_tags, problem_category, problem_seasonality,
          related_order_id, order_type, repair_status, created_at, updated_at
        ) VALUES (
          $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19,
          $20, $21, $22, $23, $24, $25, $26, $27, $28, $29, $30, $31, $32, $33, $34, $35, $36, $37,
          $38, $39, $40, $41, $42, $43, $44, $45, $46, $47, $48, $49, $50, $51, $52, $53, $54, $55,
          $56, $57, $58, $59, $60, $61
        )
        ON CONFLICT (id) DO NOTHING
      `, [
        order.id, order.order_number, order.client_id, order.request_status || 'new',
        order.priority || 'regular', order.order_source || 'app',
        order.device_type, order.device_category, order.device_brand, order.device_model,
        order.device_serial_number, order.device_year, order.warranty_status,
        order.problem_short_description, order.problem_description, order.problem_when_started,
        order.problem_conditions, order.problem_error_codes, order.problem_attempted_fixes,
        order.address, order.address_street, order.address_building, order.address_apartment,
        order.address_floor, order.address_entrance_code, order.address_landmark,
        order.latitude, order.longitude,
        order.arrival_time, order.desired_repair_date, order.urgency || 'planned',
        order.estimated_cost, order.final_cost, order.client_budget, order.payment_type,
        order.visit_cost, order.max_cost_without_approval,
        order.intercom_working === 1, order.needs_pass === 1, order.parking_available === 1,
        order.has_pets === 1, order.has_small_children === 1, order.needs_shoe_covers === 1,
        order.preferred_contact_method || 'call',
        order.master_gender_preference || 'any', order.master_min_experience, order.preferred_master_id,
        order.assigned_master_id, order.assignment_date,
        order.preliminary_diagnosis, order.required_parts, order.special_equipment,
        order.repair_complexity, order.estimated_repair_time,
        order.problem_tags, order.problem_category, order.problem_seasonality || 'permanent',
        order.related_order_id, order.order_type || 'regular', order.repair_status || 'new',
        order.created_at, order.updated_at
      ]);
    }
    console.log(`✅ Мигрировано ${orders.length} заказов\n`);
    
    // Обновляем последовательности
    console.log('🔄 Обновление последовательностей...');
    await client.query(`SELECT setval('users_id_seq', (SELECT MAX(id) FROM users))`);
    await client.query(`SELECT setval('masters_id_seq', (SELECT MAX(id) FROM masters))`);
    await client.query(`SELECT setval('clients_id_seq', (SELECT MAX(id) FROM clients))`);
    await client.query(`SELECT setval('orders_id_seq', (SELECT MAX(id) FROM orders))`);
    console.log('✅ Последовательности обновлены\n');
    
    await client.query('COMMIT');
    console.log('✅ Миграция завершена успешно!');
    
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('❌ Ошибка миграции:', error);
    throw error;
  } finally {
    client.release();
    await pool.end();
    db.close();
  }
}

// Запуск миграции
migrateData().catch(error => {
  console.error('❌ Критическая ошибка миграции:', error);
  process.exit(1);
});







