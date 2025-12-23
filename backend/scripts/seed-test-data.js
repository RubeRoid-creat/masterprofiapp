import { query } from '../database/db.js';
import bcrypt from 'bcryptjs';

// Генерация уникального номера заявки
function generateOrderNumber() {
  const prefix = 'ORD';
  const timestamp = Date.now().toString().slice(-8);
  const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0');
  return `${prefix}-${timestamp}-${random}`;
}

// Получение координат для адреса (упрощенная версия)
function getCoordinatesForAddress(address) {
  // Для тестовых данных используем координаты Москвы с небольшими смещениями
  const baseLat = 55.7558;
  const baseLon = 37.6173;
  
  // Генерируем случайные координаты в радиусе ~10 км от центра
  const offsetLat = (Math.random() - 0.5) * 0.1;
  const offsetLon = (Math.random() - 0.5) * 0.15;
  
  return {
    latitude: baseLat + offsetLat,
    longitude: baseLon + offsetLon
  };
}

async function createTestClients() {
  console.log('📝 Создание тестовых клиентов...');
  
  const clients = [
    {
      email: 'client1@test.com',
      password: '123456',
      name: 'Иван Петров',
      phone: '+79991234567',
      address: 'ул. Ленина, д. 10, кв. 5'
    },
    {
      email: 'client2@test.com',
      password: '123456',
      name: 'Мария Сидорова',
      phone: '+79991234568',
      address: 'пр. Мира, д. 25, кв. 12'
    },
    {
      email: 'client3@test.com',
      password: '123456',
      name: 'Алексей Иванов',
      phone: '+79991234569',
      address: 'ул. Пушкина, д. 3, кв. 8'
    }
  ];
  
  const createdClients = [];
  
  for (const clientData of clients) {
    // Проверяем, существует ли уже пользователь
    const existingUser = query.get('SELECT id FROM users WHERE email = ?', [clientData.email]);
    if (existingUser) {
      console.log(`   ⚠️  Клиент ${clientData.email} уже существует`);
      const client = query.get('SELECT c.* FROM clients c JOIN users u ON c.user_id = u.id WHERE u.email = ?', [clientData.email]);
      if (client) {
        createdClients.push(client);
      }
      continue;
    }
    
    // Хешируем пароль
    const passwordHash = await bcrypt.hash(clientData.password, 10);
    
    // Создаем пользователя
    const userResult = query.run(
      'INSERT INTO users (email, password_hash, name, phone, role) VALUES (?, ?, ?, ?, ?)',
      [clientData.email, passwordHash, clientData.name, clientData.phone, 'client']
    );
    
    const userId = userResult.lastInsertRowid;
    console.log(`   ✅ Создан пользователь: ${clientData.name} (${clientData.email})`);
    
    // Получаем координаты для адреса
    const coords = getCoordinatesForAddress(clientData.address);
    
    // Создаем клиента
    query.run(
      'INSERT INTO clients (user_id, address, latitude, longitude) VALUES (?, ?, ?, ?)',
      [userId, clientData.address, coords.latitude, coords.longitude]
    );
    
    const client = query.get('SELECT * FROM clients WHERE user_id = ?', [userId]);
    createdClients.push(client);
    console.log(`   ✅ Создан клиент: ID=${client.id}`);
  }
  
  return createdClients;
}

async function createTestOrders(clients) {
  console.log('\n📦 Создание тестовых заказов...');
  
  if (clients.length === 0) {
    console.log('   ⚠️  Нет клиентов для создания заказов');
    return;
  }
  
  const ordersData = [
    {
      device_type: 'Стиральная машина',
      device_category: 'large',
      device_brand: 'Samsung',
      device_model: 'WF8590NLW',
      device_year: 2022,
      warranty_status: 'post_warranty',
      problem_short_description: 'Не включается',
      problem_description: 'Стиральная машина не включается после отключения электричества. Индикаторы не загораются, кнопки не реагируют.',
      problem_when_started: 'Вчера вечером',
      problem_conditions: 'При включении в розетку',
      problem_error_codes: null,
      problem_attempted_fixes: 'Пробовали перезагрузить, отключить и включить снова',
      problem_category: 'electrical',
      problem_tags: ['не включается', 'электрика', 'срочно'],
      address: 'ул. Ленина, д. 10, кв. 5',
      address_building: '10',
      address_apartment: '5',
      address_floor: 3,
      urgency: 'urgent',
      priority: 'urgent',
      client_budget: 5000,
      payment_type: 'cash',
      estimated_cost: 3500,
      intercom_working: 1,
      parking_available: 1,
      has_pets: 0,
      has_small_children: 0,
      preferred_contact_method: 'call'
    },
    {
      device_type: 'Холодильник',
      device_category: 'large',
      device_brand: 'LG',
      device_model: 'GA-B509SLZL',
      device_year: 2021,
      warranty_status: 'post_warranty',
      problem_short_description: 'Не морозит',
      problem_description: 'Холодильник перестал морозить. В морозильной камере температура поднялась до +5 градусов. Компрессор работает, но не охлаждает.',
      problem_when_started: 'Сегодня утром',
      problem_conditions: 'Постоянно',
      problem_error_codes: 'E2',
      problem_attempted_fixes: 'Размораживали, но не помогло',
      problem_category: 'mechanical',
      problem_tags: ['не морозит', 'компрессор', 'холодильник'],
      address: 'пр. Мира, д. 25, кв. 12',
      address_building: '25',
      address_apartment: '12',
      address_floor: 5,
      urgency: 'emergency',
      priority: 'emergency',
      client_budget: 8000,
      payment_type: 'card',
      estimated_cost: 6000,
      intercom_working: 1,
      parking_available: 1,
      has_pets: 1,
      has_small_children: 0,
      preferred_contact_method: 'call'
    },
    {
      device_type: 'Посудомоечная машина',
      device_category: 'builtin',
      device_brand: 'Bosch',
      device_model: 'SMS2HKI45',
      device_year: 2023,
      warranty_status: 'warranty',
      problem_short_description: 'Течет вода',
      problem_description: 'Посудомоечная машина течет во время работы. Вода капает из-под дверцы на пол. Проблема началась недавно.',
      problem_when_started: 'Неделю назад',
      problem_conditions: 'Во время мойки',
      problem_error_codes: null,
      problem_attempted_fixes: 'Проверяли уплотнитель, но визуально все в порядке',
      problem_category: 'mechanical',
      problem_tags: ['течет', 'вода', 'уплотнитель'],
      address: 'ул. Пушкина, д. 3, кв. 8',
      address_building: '3',
      address_apartment: '8',
      address_floor: 2,
      urgency: 'planned',
      priority: 'regular',
      client_budget: 4000,
      payment_type: 'online',
      estimated_cost: 2500,
      intercom_working: 0,
      parking_available: 0,
      has_pets: 0,
      has_small_children: 1,
      preferred_contact_method: 'sms'
    },
    {
      device_type: 'Ноутбук',
      device_category: 'small',
      device_brand: 'Lenovo',
      device_model: 'ThinkPad E14',
      device_year: 2020,
      warranty_status: 'post_warranty',
      problem_short_description: 'Не включается экран',
      problem_description: 'Ноутбук включается, но экран не работает. Слышен звук загрузки системы, индикаторы горят, но изображения нет.',
      problem_when_started: 'После падения',
      problem_conditions: 'При включении',
      problem_error_codes: null,
      problem_attempted_fixes: 'Пробовали подключить внешний монитор - работает',
      problem_category: 'electronic',
      problem_tags: ['экран', 'не работает', 'ноутбук'],
      address: 'ул. Ленина, д. 10, кв. 5',
      address_building: '10',
      address_apartment: '5',
      address_floor: 3,
      urgency: 'urgent',
      priority: 'urgent',
      client_budget: 7000,
      payment_type: 'card',
      estimated_cost: 5000,
      intercom_working: 1,
      parking_available: 1,
      has_pets: 0,
      has_small_children: 0,
      preferred_contact_method: 'call'
    },
    {
      device_type: 'Микроволновая печь',
      device_category: 'small',
      device_brand: 'Panasonic',
      device_model: 'NN-ST45KW',
      device_year: 2021,
      warranty_status: 'post_warranty',
      problem_short_description: 'Не греет',
      problem_description: 'Микроволновка включается, таймер работает, но не греет еду. Магнетрон не работает.',
      problem_when_started: 'Три дня назад',
      problem_conditions: 'При включении',
      problem_error_codes: 'F6',
      problem_attempted_fixes: 'Проверяли настройки мощности',
      problem_category: 'electronic',
      problem_tags: ['не греет', 'магнетрон'],
      address: 'пр. Мира, д. 25, кв. 12',
      address_building: '25',
      address_apartment: '12',
      address_floor: 5,
      urgency: 'planned',
      priority: 'regular',
      client_budget: 3000,
      payment_type: 'cash',
      estimated_cost: 2000,
      intercom_working: 1,
      parking_available: 1,
      has_pets: 1,
      has_small_children: 0,
      preferred_contact_method: 'call'
    }
  ];
  
  const createdOrders = [];
  
  for (let i = 0; i < ordersData.length; i++) {
    const orderData = ordersData[i];
    const client = clients[i % clients.length]; // Распределяем заказы между клиентами
    
    // Получаем координаты для адреса
    const coords = getCoordinatesForAddress(orderData.address);
    
    // Генерируем номер заявки
    const orderNumber = generateOrderNumber();
    
    // Создаем заказ
    const result = query.run(`
      INSERT INTO orders (
        order_number, client_id,
        request_status, priority, order_source,
        device_type, device_category, device_brand, device_model,
        device_year, warranty_status,
        problem_short_description, problem_description,
        problem_when_started, problem_conditions, problem_error_codes, problem_attempted_fixes,
        problem_category, problem_tags,
        address, address_building, address_apartment, address_floor,
        latitude, longitude,
        urgency,
        client_budget, payment_type, estimated_cost,
        intercom_working, parking_available,
        has_pets, has_small_children, preferred_contact_method,
        order_type, repair_status
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `, [
      orderNumber, client.id,
      'new', orderData.priority, 'app',
      orderData.device_type, orderData.device_category, orderData.device_brand, orderData.device_model,
      orderData.device_year, orderData.warranty_status,
      orderData.problem_short_description, orderData.problem_description,
      orderData.problem_when_started, orderData.problem_conditions, orderData.problem_error_codes, orderData.problem_attempted_fixes,
      orderData.problem_category, JSON.stringify(orderData.problem_tags),
      orderData.address, orderData.address_building, orderData.address_apartment, orderData.address_floor,
      coords.latitude, coords.longitude,
      orderData.urgency,
      orderData.client_budget, orderData.payment_type, orderData.estimated_cost,
      orderData.intercom_working, orderData.parking_available,
      orderData.has_pets, orderData.has_small_children, orderData.preferred_contact_method,
      'regular', 'new'
    ]);
    
    const orderId = result.lastInsertRowid;
    
    // Записываем в историю статусов
    query.run(
      'INSERT INTO order_status_history (order_id, new_status) VALUES (?, ?)',
      [orderId, 'new']
    );
    
    // Записываем в историю обращений клиента
    query.run(
      'INSERT INTO client_order_history (client_id, order_id, related_device_type, related_device_model) VALUES (?, ?, ?, ?)',
      [client.id, orderId, orderData.device_type, orderData.device_model]
    );
    
    const order = query.get('SELECT * FROM orders WHERE id = ?', [orderId]);
    createdOrders.push(order);
    
    console.log(`   ✅ Создан заказ: ${orderNumber} (${orderData.device_type} ${orderData.device_brand})`);
  }
  
  return createdOrders;
}

async function main() {
  try {
    console.log('🚀 Начало загрузки тестовых данных...\n');
    
    // Создаем клиентов
    const clients = await createTestClients();
    
    // Создаем заказы
    const orders = await createTestOrders(clients);
    
    console.log('\n✅ Тестовые данные успешно загружены!');
    console.log(`\n📊 Статистика:`);
    console.log(`   Клиентов: ${clients.length}`);
    console.log(`   Заказов: ${orders.length}`);
    console.log(`\n📝 Учетные данные клиентов:`);
    console.log(`   client1@test.com / 123456`);
    console.log(`   client2@test.com / 123456`);
    console.log(`   client3@test.com / 123456`);
    
  } catch (error) {
    console.error('❌ Ошибка загрузки тестовых данных:', error);
    throw error;
  }
}

main().then(() => {
  process.exit(0);
}).catch(error => {
  console.error(error);
  process.exit(1);
});

