import { query } from '../database/db.js';

console.log('📝 СОЗДАНИЕ ТЕСТОВОГО ЗАКАЗА\n');
console.log('='.repeat(80));

// Получаем первого клиента или создаем тестового
let client = query.get('SELECT id FROM clients LIMIT 1');

if (!client) {
  console.log('\n⚠️ Нет клиентов в системе. Создаем тестового клиента...\n');
  
  // Создаем тестового пользователя-клиента
  const userResult = query.run(`
    INSERT INTO users (email, password_hash, name, phone, role)
    VALUES (?, ?, ?, ?, ?)
  `, ['test_client@example.com', 'dummy_hash', 'Тестовый Клиент', '+79991234567', 'client']);
  
  const userId = userResult.lastInsertRowid;
  
  // Создаем профиль клиента
  const clientResult = query.run(`
    INSERT INTO clients (user_id, address, latitude, longitude)
    VALUES (?, ?, ?, ?)
  `, [userId, 'Тестовый адрес, д. 1', 55.7558, 37.6173]); // Москва
  
  client = { id: clientResult.lastInsertRowid };
  console.log(`✅ Создан тестовый клиент #${client.id}\n`);
} else {
  console.log(`✅ Используем существующего клиента #${client.id}\n`);
}

// Генерируем номер заказа
const year = new Date().getFullYear().toString().slice(-2);
const lastOrder = query.get('SELECT id FROM orders ORDER BY id DESC LIMIT 1');
const nextId = lastOrder ? lastOrder.id + 1 : 1;
const paddedId = nextId.toString().padStart(4, '0');
const orderNumber = `#${paddedId}-КЛ`;

// Создаем тестовый заказ
// Только типы, которые есть у мастеров в специализации
const deviceTypes = ['Холодильник', 'Стиральная машина', 'Посудомоечная машина', 'Духовой шкаф'];
const randomDeviceType = deviceTypes[Math.floor(Math.random() * deviceTypes.length)];

console.log(`Создаем заказ на устройство: ${randomDeviceType}\n`);

const orderResult = query.run(`
  INSERT INTO orders (
    order_number,
    client_id,
    request_status,
    repair_status,
    device_type,
    problem_description,
    address,
    latitude,
    longitude,
    priority,
    urgency
  ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
`, [
  orderNumber,
  client.id,
  'new',
  'new',
  randomDeviceType,
  'Тестовая заявка: устройство не работает',
  'Тестовый адрес, д. 1, кв. 1',
  55.7558, // Москва
  37.6173,
  'regular',
  'planned'
]);

const orderId = orderResult.lastInsertRowid;

console.log(`✅ Создан тестовый заказ #${orderId}`);
console.log(`   Номер: ${orderNumber}`);
console.log(`   Устройство: ${randomDeviceType}`);
console.log(`   Статус: new\n`);

// Импортируем функцию уведомления мастеров
import { notifyMasters } from '../services/assignment-service.js';

console.log('📨 Отправка уведомлений мастерам...\n');

try {
  notifyMasters(orderId, randomDeviceType, 55.7558, 37.6173);
  console.log('✅ Уведомления отправлены!\n');
} catch (error) {
  console.error('❌ Ошибка при отправке уведомлений:', error);
}

console.log('='.repeat(80));
console.log('\n✅ Тестовый заказ создан и мастерам отправлены уведомления!\n');
console.log('Проверьте приложение мастера - должна появиться новая заявка.\n');

process.exit(0);
