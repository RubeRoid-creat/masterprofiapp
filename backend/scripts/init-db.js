import bcrypt from 'bcryptjs';
import { initDatabase, query, reloadDatabase } from '../database/db.js';
import { unlinkSync, existsSync } from 'fs';
import { config } from '../config.js';

console.log('🚀 Инициализация базы данных...');

// Удаляем старую базу данных если она существует
if (existsSync(config.databasePath)) {
  unlinkSync(config.databasePath);
  console.log('🗑️  Удалена старая база данных');
  // Перезагружаем базу
  await reloadDatabase();
}

// Инициализируем схему
await initDatabase();

// Данные уже очищены, так как мы создали новую БД

// Хешируем пароль
const hashPassword = (password) => bcrypt.hashSync(password, 10);

// Создаем тестовых пользователей
console.log('👥 Создание пользователей...');

// Клиенты
const clients = [
  { email: 'ivanov@example.com', password: 'password123', name: 'Иванов Иван', phone: '79991234567' },
  { email: 'petrova@example.com', password: 'password123', name: 'Петрова Мария', phone: '79997654321' },
  { email: 'sidorov@example.com', password: 'password123', name: 'Сидоров Петр', phone: '79995556677' },
];

const clientIds = [];
for (const client of clients) {
  const result = query.run(
    'INSERT INTO users (email, password_hash, name, phone, role) VALUES (?, ?, ?, ?, ?)',
    [client.email, hashPassword(client.password), client.name, client.phone, 'client']
  );
  clientIds.push(result.lastInsertRowid);
  
  // Создаем запись клиента
  query.run(
    'INSERT INTO clients (user_id, address, latitude, longitude) VALUES (?, ?, ?, ?)',
    [result.lastInsertRowid, 'Тверь, ул. Советская, д. 34, кв. 15', 56.859611, 35.911896]
  );
}

// Мастера
const masters = [
  {
    email: 'master@test.com',
    password: '123456',
    name: 'Тестовый Мастер',
    phone: '+79991234567',
    specialization: ['Стиральная машина', 'Холодильник', 'Посудомоечная машина'],
    rating: 5.0,
    completedOrders: 0,
    latitude: 56.859611,
    longitude: 35.911896
  },
  {
    email: 'smirnov@example.com',
    password: 'password123',
    name: 'Алексей Смирнов',
    phone: '79161234567',
    specialization: ['Стиральная машина', 'Посудомоечная машина', 'Холодильник'],
    rating: 4.8,
    completedOrders: 145,
    latitude: 56.859611,
    longitude: 35.911896
  },
  {
    email: 'kuznetsov@example.com',
    password: 'password123',
    name: 'Дмитрий Кузнецов',
    phone: '79167654321',
    specialization: ['Кондиционер', 'Водонагреватель', 'Духовой шкаф'],
    rating: 4.9,
    completedOrders: 203,
    latitude: 56.858506,
    longitude: 35.900775
  },
  {
    email: 'popov@example.com',
    password: 'password123',
    name: 'Сергей Попов',
    phone: '79165556677',
    specialization: ['Ноутбук', 'Десктоп', 'Кофемашина'],
    rating: 4.7,
    completedOrders: 98,
    latitude: 56.857422,
    longitude: 35.917034
  },
];

const masterIds = [];
for (const master of masters) {
  const result = query.run(
    'INSERT INTO users (email, password_hash, name, phone, role) VALUES (?, ?, ?, ?, ?)',
    [master.email, hashPassword(master.password), master.name, master.phone, 'master']
  );
  
  // Создаем запись мастера
  const masterResult = query.run(
    'INSERT INTO masters (user_id, specialization, rating, completed_orders, status, latitude, longitude, is_on_shift) VALUES (?, ?, ?, ?, ?, ?, ?, ?)',
    [
      result.lastInsertRowid,
      JSON.stringify(master.specialization),
      master.rating,
      master.completedOrders,
      'available',
      master.latitude,
      master.longitude,
      1 // на смене
    ]
  );
  masterIds.push(masterResult.lastInsertRowid);
}

console.log(`✅ Создано ${clientIds.length} клиентов и ${masterIds.length} мастеров`);

// Создаем тестовые заказы
console.log('📦 Создание заказов...');

const testOrders = [
  {
    clientId: 1,
    deviceType: 'Стиральная машина',
    deviceBrand: 'Samsung',
    deviceModel: 'WW80R42',
    problemDescription: 'Не сливает воду',
    address: 'Тверь, ул. Советская, д. 34, кв. 15',
    latitude: 56.859611,
    longitude: 35.911896,
    arrivalTime: '14:00 - 16:00',
    orderType: 'urgent',
    estimatedCost: 5000
  },
  {
    clientId: 2,
    deviceType: 'Холодильник',
    deviceBrand: 'Bosch',
    deviceModel: 'KGN39VI21R',
    problemDescription: 'Не морозит морозильная камера',
    address: 'Тверь, пр-т Калинина, д. 1, кв. 45',
    latitude: 56.858506,
    longitude: 35.900775,
    arrivalTime: '10:00 - 12:00',
    orderType: 'regular',
    estimatedCost: 12000
  },
  {
    clientId: 3,
    deviceType: 'Посудомоечная машина',
    deviceBrand: 'Electrolux',
    deviceModel: 'ESF9552LOX',
    problemDescription: 'Не включается, мигает индикатор',
    address: 'Тверь, ул. Трёхсвятская, д. 28, кв. 7',
    latitude: 56.857422,
    longitude: 35.917034,
    arrivalTime: '16:00 - 18:00',
    orderType: 'regular',
    estimatedCost: 7500
  },
  {
    clientId: 1,
    deviceType: 'Духовой шкаф',
    deviceBrand: 'Gorenje',
    deviceModel: 'BO635E11X',
    problemDescription: 'Не работает гриль',
    address: 'Тверь, ул. Желябова, д. 41, кв. 12',
    latitude: 56.862000,
    longitude: 35.906000,
    arrivalTime: '09:00 - 11:00',
    orderType: 'regular',
    estimatedCost: 6000
  },
  {
    clientId: 2,
    deviceType: 'Кондиционер',
    deviceBrand: 'LG',
    deviceModel: 'S09EQ',
    problemDescription: 'Течёт конденсат',
    address: 'Тверь, ул. Вагжанова, д. 7, кв. 3',
    latitude: 56.856111,
    longitude: 35.924444,
    arrivalTime: '13:00 - 15:00',
    orderType: 'urgent',
    estimatedCost: 4500
  },
  {
    clientId: 3,
    deviceType: 'Ноутбук',
    deviceBrand: 'ASUS',
    deviceModel: 'VivoBook 15',
    problemDescription: 'Не заряжается батарея',
    address: 'Тверь, ул. Радищева, д. 49, кв. 89',
    latitude: 56.854333,
    longitude: 35.912778,
    arrivalTime: '11:00 - 13:00',
    orderType: 'regular',
    estimatedCost: 3500
  },
  {
    clientId: 1,
    deviceType: 'Кофемашина',
    deviceBrand: 'DeLonghi',
    deviceModel: 'ECAM 22.110',
    problemDescription: 'Слабый напор воды',
    address: 'Тверь, Петербургское шоссе, д. 105, кв. 234',
    latitude: 56.868333,
    longitude: 35.890000,
    arrivalTime: '15:00 - 17:00',
    orderType: 'regular',
    estimatedCost: 4000
  }
];

// Функция генерации номера заявки
function generateOrderNumber(orderId) {
  const year = new Date().getFullYear().toString().slice(-2);
  const paddedId = orderId.toString().padStart(4, '0');
  return `#${paddedId}-КЛ`;
}

for (const order of testOrders) {
  const result = query.run(
    `INSERT INTO orders (
      client_id, order_number, device_type, device_category, device_brand, device_model, 
      problem_short_description, problem_description,
      address, latitude, longitude, 
      arrival_time, desired_repair_date, urgency,
      order_type, order_source, priority,
      request_status, repair_status, 
      estimated_cost, client_budget, payment_type,
      intercom_working, parking_available, preferred_contact_method
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    [
      order.clientId,
      null, // order_number будет сгенерирован после получения ID
      order.deviceType,
      'large', // Категория по умолчанию
      order.deviceBrand,
      order.deviceModel,
      order.problemDescription.substring(0, 100), // Краткое описание
      order.problemDescription,
      order.address,
      order.latitude,
      order.longitude,
      order.arrivalTime,
      null, // desired_repair_date
      order.orderType === 'urgent' ? 'urgent' : 'planned',
      order.orderType,
      'app',
      order.orderType === 'urgent' ? 'urgent' : 'regular',
      'new',
      'new',
      order.estimatedCost,
      order.estimatedCost * 1.2, // Бюджет немного выше
      'card',
      1, // intercom_working
      1, // parking_available
      'call' // preferred_contact_method
    ]
  );
  
  // Генерируем номер заявки
  const orderNumber = generateOrderNumber(result.lastInsertRowid);
  query.run('UPDATE orders SET order_number = ? WHERE id = ?', [orderNumber, result.lastInsertRowid]);
  
  // Добавляем в историю обращений клиента
  query.run(
    'INSERT INTO client_order_history (client_id, order_id, related_device_type, related_device_model) VALUES (?, ?, ?, ?)',
    [order.clientId, result.lastInsertRowid, order.deviceType, order.deviceModel]
  );
}

console.log(`✅ Создано ${testOrders.length} заказов`);
console.log('');
console.log('🎉 База данных успешно инициализирована!');
console.log('');
console.log('📝 Тестовые учетные данные:');
console.log('   Клиенты:');
console.log('   - Email: ivanov@example.com, Пароль: password123');
console.log('   - Email: petrova@example.com, Пароль: password123');
console.log('   Мастера:');
console.log('   - Email: smirnov@example.com, Пароль: password123');
console.log('   - Email: kuznetsov@example.com, Пароль: password123');
console.log('   - Email: popov@example.com, Пароль: password123');
console.log('');

process.exit(0);

