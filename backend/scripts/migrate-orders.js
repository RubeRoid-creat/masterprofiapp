import { query, reloadDatabase } from '../database/db.js';
import { existsSync } from 'fs';
import { config } from '../config.js';

console.log('🔄 Миграция базы данных для расширенной структуры заявок...');

// Проверяем наличие базы данных
if (!existsSync(config.databasePath)) {
  console.log('❌ База данных не найдена. Запустите init-db.js для создания новой БД.');
  process.exit(1);
}

await reloadDatabase();

try {
  // Проверяем, есть ли уже новые поля
  const orderColumns = query.all("PRAGMA table_info(orders)");
  const columnNames = orderColumns.map(col => col.name);
  
  console.log('📋 Проверка существующих полей...');
  
  // Добавляем новые поля в таблицу orders
  const newFields = [
    { name: 'order_number', sql: 'TEXT', isUnique: true },
    { name: 'request_status', sql: "TEXT NOT NULL CHECK(request_status IN ('new', 'accepted', 'in_progress', 'completed', 'cancelled')) DEFAULT 'new'" },
    { name: 'priority', sql: "TEXT NOT NULL CHECK(priority IN ('emergency', 'urgent', 'regular', 'planned')) DEFAULT 'regular'" },
    { name: 'order_source', sql: "TEXT CHECK(order_source IN ('app', 'website', 'phone')) DEFAULT 'app'" },
    { name: 'device_category', sql: "TEXT CHECK(device_category IN ('large', 'small', 'builtin'))" },
    { name: 'device_serial_number', sql: 'TEXT' },
    { name: 'device_year', sql: 'INTEGER' },
    { name: 'warranty_status', sql: "TEXT CHECK(warranty_status IN ('warranty', 'post_warranty'))" },
    { name: 'problem_short_description', sql: 'TEXT' },
    { name: 'problem_when_started', sql: 'TEXT' },
    { name: 'problem_conditions', sql: 'TEXT' },
    { name: 'problem_error_codes', sql: 'TEXT' },
    { name: 'problem_attempted_fixes', sql: 'TEXT' },
    { name: 'address_street', sql: 'TEXT' },
    { name: 'address_building', sql: 'TEXT' },
    { name: 'address_apartment', sql: 'TEXT' },
    { name: 'address_floor', sql: 'INTEGER' },
    { name: 'address_entrance_code', sql: 'TEXT' },
    { name: 'address_landmark', sql: 'TEXT' },
    { name: 'desired_repair_date', sql: 'DATE' },
    { name: 'urgency', sql: "TEXT CHECK(urgency IN ('emergency', 'urgent', 'planned')) DEFAULT 'planned'" },
    { name: 'client_budget', sql: 'REAL' },
    { name: 'payment_type', sql: "TEXT CHECK(payment_type IN ('cash', 'card', 'online', 'installment'))" },
    { name: 'visit_cost', sql: 'REAL' },
    { name: 'max_cost_without_approval', sql: 'REAL' },
    { name: 'intercom_working', sql: 'INTEGER DEFAULT 1' },
    { name: 'needs_pass', sql: 'INTEGER DEFAULT 0' },
    { name: 'parking_available', sql: 'INTEGER DEFAULT 1' },
    { name: 'has_pets', sql: 'INTEGER DEFAULT 0' },
    { name: 'has_small_children', sql: 'INTEGER DEFAULT 0' },
    { name: 'needs_shoe_covers', sql: 'INTEGER DEFAULT 0' },
    { name: 'preferred_contact_method', sql: "TEXT CHECK(preferred_contact_method IN ('call', 'sms', 'chat')) DEFAULT 'call'" },
    { name: 'master_gender_preference', sql: "TEXT CHECK(master_gender_preference IN ('male', 'female', 'any')) DEFAULT 'any'" },
    { name: 'master_min_experience', sql: 'INTEGER' },
    { name: 'preferred_master_id', sql: 'INTEGER' },
    { name: 'assignment_date', sql: 'DATETIME' },
    { name: 'preliminary_diagnosis', sql: 'TEXT' },
    { name: 'required_parts', sql: 'TEXT' },
    { name: 'special_equipment', sql: 'TEXT' },
    { name: 'repair_complexity', sql: "TEXT CHECK(repair_complexity IN ('simple', 'medium', 'complex'))" },
    { name: 'estimated_repair_time', sql: 'INTEGER' },
    { name: 'problem_tags', sql: 'TEXT' },
    { name: 'problem_category', sql: "TEXT CHECK(problem_category IN ('electrical', 'mechanical', 'electronic', 'software'))" },
    { name: 'problem_seasonality', sql: "TEXT CHECK(problem_seasonality IN ('seasonal', 'permanent')) DEFAULT 'permanent'" },
    { name: 'related_order_id', sql: 'INTEGER' }
  ];
  
  let addedCount = 0;
  const uniqueFields = [];
  
  for (const field of newFields) {
    if (!columnNames.includes(field.name)) {
      try {
        // Для UNIQUE полей сначала добавляем без ограничения
        const sqlToRun = field.isUnique 
          ? `ALTER TABLE orders ADD COLUMN ${field.name} TEXT`
          : `ALTER TABLE orders ADD COLUMN ${field.name} ${field.sql}`;
        
        query.run(sqlToRun);
        console.log(`  ✅ Добавлено поле: ${field.name}`);
        addedCount++;
        
        // Запоминаем UNIQUE поля для обработки позже
        if (field.isUnique) {
          uniqueFields.push(field.name);
        }
      } catch (error) {
        console.error(`  ❌ Ошибка добавления поля ${field.name}:`, error.message);
      }
    } else {
      console.log(`  ⏭️  Поле ${field.name} уже существует`);
    }
  }
  
  // Генерируем order_number для существующих заказов, если поле было добавлено
  if (uniqueFields.includes('order_number')) {
    console.log('\n📋 Генерация номеров заявок для существующих заказов...');
    const ordersWithoutNumber = query.all("SELECT id FROM orders WHERE order_number IS NULL OR order_number = ''");
    
    for (const order of ordersWithoutNumber) {
      const year = new Date().getFullYear().toString().slice(-2);
      const paddedId = order.id.toString().padStart(4, '0');
      const orderNumber = `#${paddedId}-КЛ`;
      query.run("UPDATE orders SET order_number = ? WHERE id = ?", [orderNumber, order.id]);
    }
    
    if (ordersWithoutNumber.length > 0) {
      console.log(`  ✅ Сгенерировано ${ordersWithoutNumber.length} номеров заявок`);
    }
  }
  
  if (addedCount > 0) {
    console.log(`\n✅ Добавлено ${addedCount} новых полей в таблицу orders`);
  } else {
    console.log('\n✅ Все поля уже существуют');
  }
  
  // Создаем новые таблицы
  console.log('\n📋 Создание новых таблиц...');
  
  const newTables = {
    order_media: `
      CREATE TABLE IF NOT EXISTS order_media (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        order_id INTEGER NOT NULL,
        media_type TEXT NOT NULL CHECK(media_type IN ('photo', 'video', 'document', 'audio')),
        file_path TEXT NOT NULL,
        file_url TEXT,
        file_name TEXT,
        file_size INTEGER,
        mime_type TEXT,
        description TEXT,
        thumbnail_url TEXT,
        duration INTEGER,
        upload_order INTEGER DEFAULT 0,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
      )
    `,
    client_order_history: `
      CREATE TABLE IF NOT EXISTS client_order_history (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        client_id INTEGER NOT NULL,
        order_id INTEGER NOT NULL,
        related_device_type TEXT,
        related_device_model TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
      )
    `,
    device_repair_history: `
      CREATE TABLE IF NOT EXISTS device_repair_history (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        order_id INTEGER NOT NULL,
        device_type TEXT NOT NULL,
        device_brand TEXT,
        device_model TEXT,
        device_serial_number TEXT,
        repair_date DATETIME NOT NULL,
        repair_description TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
      )
    `,
    diagnostic_checklists: `
      CREATE TABLE IF NOT EXISTS diagnostic_checklists (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        device_type TEXT NOT NULL,
        device_brand TEXT,
        device_model TEXT,
        checklist_item TEXT NOT NULL,
        item_order INTEGER DEFAULT 0,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
      )
    `,
    common_problems: `
      CREATE TABLE IF NOT EXISTS common_problems (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        device_type TEXT NOT NULL,
        device_brand TEXT,
        device_model TEXT,
        problem_description TEXT NOT NULL,
        problem_code TEXT,
        solution_hint TEXT,
        frequency_rating INTEGER DEFAULT 0,
        average_repair_time INTEGER,
        average_repair_cost REAL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
      )
    `
  };
  
  let tablesCreated = 0;
  for (const [tableName, createSql] of Object.entries(newTables)) {
    try {
      query.run(createSql);
      console.log(`  ✅ Таблица ${tableName} создана`);
      tablesCreated++;
    } catch (error) {
      if (error.message.includes('already exists')) {
        console.log(`  ⏭️  Таблица ${tableName} уже существует`);
      } else {
        console.error(`  ❌ Ошибка создания таблицы ${tableName}:`, error.message);
      }
    }
  }
  
  // Создаем индексы
  console.log('\n📋 Создание индексов...');
  const indexes = [
    { sql: "CREATE INDEX IF NOT EXISTS idx_orders_request_status ON orders(request_status)", name: "request_status" },
    { sql: "CREATE INDEX IF NOT EXISTS idx_orders_order_number ON orders(order_number)", name: "order_number" },
    { sql: "CREATE INDEX IF NOT EXISTS idx_orders_device_type ON orders(device_type)", name: "device_type" },
    { sql: "CREATE INDEX IF NOT EXISTS idx_orders_priority ON orders(priority)", name: "priority" },
    { sql: "CREATE INDEX IF NOT EXISTS idx_order_media_order_id ON order_media(order_id)", name: "order_media_order_id" },
    { sql: "CREATE INDEX IF NOT EXISTS idx_order_media_type ON order_media(media_type)", name: "order_media_type" },
    { sql: "CREATE INDEX IF NOT EXISTS idx_client_order_history_client_id ON client_order_history(client_id)", name: "client_order_history_client_id" },
    { sql: "CREATE INDEX IF NOT EXISTS idx_device_repair_history_order_id ON device_repair_history(order_id)", name: "device_repair_history_order_id" },
    { sql: "CREATE INDEX IF NOT EXISTS idx_device_repair_history_serial ON device_repair_history(device_serial_number)", name: "device_repair_history_serial" },
    { sql: "CREATE INDEX IF NOT EXISTS idx_common_problems_device ON common_problems(device_type, device_brand, device_model)", name: "common_problems_device" }
  ];
  
  for (const index of indexes) {
    try {
      query.run(index.sql);
    } catch (error) {
      // Игнорируем ошибку, если колонка еще не существует
      if (!error.message.includes('no such column')) {
        console.error(`  ❌ Ошибка создания индекса ${index.name}:`, error.message);
      }
    }
  }
  console.log('  ✅ Индексы созданы');
  
  // Генерируем order_number для существующих заказов, если его еще нет
  if (!uniqueFields.includes('order_number')) {
    console.log('\n📋 Генерация номеров заявок для существующих заказов...');
    const ordersWithoutNumber = query.all("SELECT id FROM orders WHERE order_number IS NULL OR order_number = ''");
    
    if (ordersWithoutNumber.length > 0) {
      for (const order of ordersWithoutNumber) {
        const year = new Date().getFullYear().toString().slice(-2);
        const paddedId = order.id.toString().padStart(4, '0');
        const orderNumber = `#${paddedId}-КЛ`;
        query.run("UPDATE orders SET order_number = ? WHERE id = ?", [orderNumber, order.id]);
      }
      console.log(`  ✅ Сгенерировано ${ordersWithoutNumber.length} номеров заявок`);
    } else {
      console.log('  ✅ Все заказы уже имеют номера');
    }
  }
  
  // Устанавливаем request_status = 'new' для заказов без него
  query.run("UPDATE orders SET request_status = 'new' WHERE request_status IS NULL");
  
  console.log('\n🎉 Миграция успешно завершена!');
  process.exit(0);
  
} catch (error) {
  console.error('❌ Ошибка миграции:', error);
  process.exit(1);
}

