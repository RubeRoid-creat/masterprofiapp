import { query } from '../database/db.js';
import { notifyMasters } from '../services/assignment-service.js';

const deviceTypes = ['Холодильник', 'Стиральная машина', 'Посудомоечная машина', 'Духовой шкаф'];
const randomDeviceType = deviceTypes[Math.floor(Math.random() * deviceTypes.length)];

console.log('📝 СОЗДАНИЕ ТЕСТОВОГО ЗАКАЗА\n');
console.log('='.repeat(80));

// Получаем или создаем клиента
let client = query.get('SELECT id FROM clients LIMIT 1');
if (!client) {
  // Создаем тестового пользователя для клиента
  query.run(
    'INSERT INTO users (email, password_hash, name, role, phone) VALUES (?, ?, ?, ?, ?)',
    ['client@test.com', '$2b$10$test', 'Тестовый Клиент', 'client', '+79991234567']
  );
  const userId = query.lastInsertRowid;
  
  query.run(
    'INSERT INTO clients (user_id) VALUES (?)',
    [userId]
  );
  client = { id: query.lastInsertRowid };
  console.log('✅ Создан тестовый клиент');
} else {
  console.log('✅ Используем существующего клиента #' + client.id);
}

// Создаем заказ
const orderNumber = String(Date.now()).slice(-4);
query.run(
  `INSERT INTO orders (
    client_id, order_number, device_type, problem_description,
    address, latitude, longitude, repair_status, request_status,
    created_at, updated_at
  ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))`,
  [
    client.id,
    `#${orderNumber}-КЛ`,
    randomDeviceType,
    'Тестовая заявка: устройство не работает',
    'Тестовый адрес, д. 1, кв. 1',
    55.7558,
    37.6173,
    'new',
    'new'
  ]
);

const orderId = query.lastInsertRowid;
const order = query.get('SELECT * FROM orders WHERE id = ?', [orderId]);

console.log(`\n✅ Создан тестовый заказ #${orderId}`);
console.log(`   Номер: ${order.order_number}`);
console.log(`   Устройство: ${order.device_type}`);
console.log(`   Статус: ${order.repair_status}\n`);

console.log('📨 Отправка уведомлений мастерам...\n');
notifyMasters(orderId, order.device_type, order.latitude, order.longitude);

console.log('\n' + '='.repeat(80));
console.log('\n✅ Тестовый заказ создан и мастерам отправлены уведомления!\n');

process.exit(0);


