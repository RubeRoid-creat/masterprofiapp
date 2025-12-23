import { query } from '../database/db.js';

console.log('🔍 ДИАГНОСТИКА СИСТЕМЫ НАЗНАЧЕНИЙ\n');
console.log('=' .repeat(60));

// 1. Проверка мастеров
console.log('\n1️⃣ МАСТЕРА:');
const masters = query.all(`
  SELECT 
    m.id, m.user_id, m.is_on_shift, m.status, m.verification_status, 
    m.specialization, m.rating, m.completed_orders,
    u.name, u.email
  FROM masters m
  JOIN users u ON m.user_id = u.id
`);

console.log(`   Всего мастеров: ${masters.length}`);

masters.forEach(m => {
  const specializations = JSON.parse(m.specialization || '[]');
  const onShift = m.is_on_shift ? '✅ НА СМЕНЕ' : '❌ Не на смене';
  const verified = m.verification_status === 'verified' ? '✅ Верифицирован' : `❌ ${m.verification_status}`;
  
  console.log(`\n   Мастер #${m.id} (user_id=${m.user_id}): ${m.name}`);
  console.log(`   Email: ${m.email}`);
  console.log(`   Статус: ${onShift}, ${verified}, ${m.status}`);
  console.log(`   Специализации: ${specializations.join(', ') || 'НЕТ'}`);
  console.log(`   Рейтинг: ${m.rating || 0}, Заказов: ${m.completed_orders || 0}`);
});

// 2. Проверка заказов
console.log('\n' + '='.repeat(60));
console.log('\n2️⃣ ЗАКАЗЫ:');
const orders = query.all(`
  SELECT 
    o.id, o.device_type, o.repair_status, o.created_at,
    o.latitude, o.longitude,
    u.name as client_name, u.email as client_email
  FROM orders o
  JOIN clients c ON o.client_id = c.id
  JOIN users u ON c.user_id = u.id
  ORDER BY o.created_at DESC
  LIMIT 10
`);

console.log(`   Всего заказов (последние 10): ${orders.length}`);

orders.forEach(o => {
  const status = o.repair_status === 'new' ? '🆕 НОВЫЙ' : o.repair_status;
  console.log(`\n   Заказ #${o.id}: ${o.device_type}`);
  console.log(`   Клиент: ${o.client_name} (${o.client_email})`);
  console.log(`   Статус: ${status}`);
  console.log(`   Создан: ${o.created_at}`);
  console.log(`   Координаты: ${o.latitude}, ${o.longitude}`);
});

// 3. Проверка назначений
console.log('\n' + '='.repeat(60));
console.log('\n3️⃣ НАЗНАЧЕНИЯ:');
const assignments = query.all(`
  SELECT 
    oa.id, oa.order_id, oa.master_id, oa.status, 
    oa.created_at, oa.expires_at, oa.attempt_number,
    m.user_id as master_user_id,
    u.name as master_name,
    o.device_type, o.repair_status as order_status
  FROM order_assignments oa
  JOIN masters m ON oa.master_id = m.id
  JOIN users u ON m.user_id = u.id
  JOIN orders o ON oa.order_id = o.id
  ORDER BY oa.created_at DESC
  LIMIT 20
`);

console.log(`   Всего назначений (последние 20): ${assignments.length}`);

const pendingAssignments = assignments.filter(a => a.status === 'pending');
console.log(`   📋 Pending: ${pendingAssignments.length}`);
console.log(`   ✅ Accepted: ${assignments.filter(a => a.status === 'accepted').length}`);
console.log(`   ❌ Rejected: ${assignments.filter(a => a.status === 'rejected').length}`);
console.log(`   ⏱️ Expired: ${assignments.filter(a => a.status === 'expired').length}`);

if (assignments.length > 0) {
  console.log('\n   Детали назначений:');
  assignments.slice(0, 5).forEach(a => {
    const now = new Date();
    const expiresAt = new Date(a.expires_at);
    const isExpired = expiresAt < now;
    const statusEmoji = {
      'pending': '📋',
      'accepted': '✅',
      'rejected': '❌',
      'expired': '⏱️'
    }[a.status] || '❓';
    
    console.log(`\n   ${statusEmoji} Назначение #${a.id}`);
    console.log(`      Заказ: #${a.order_id} (${a.device_type}, ${a.order_status})`);
    console.log(`      Мастер: #${a.master_id} - ${a.master_name} (user_id=${a.master_user_id})`);
    console.log(`      Статус: ${a.status}`);
    console.log(`      Создано: ${a.created_at}`);
    console.log(`      Истекает: ${a.expires_at} ${isExpired ? '(ИСТЕКЛО)' : '(активно)'}`);
    console.log(`      Попытка: ${a.attempt_number || 1}`);
  });
}

// 4. Проверка соответствия специализаций
console.log('\n' + '='.repeat(60));
console.log('\n4️⃣ ПРОВЕРКА СООТВЕТСТВИЯ:');

const newOrders = orders.filter(o => o.repair_status === 'new');
console.log(`   Новых заказов: ${newOrders.length}`);

newOrders.forEach(order => {
  console.log(`\n   📦 Заказ #${order.id}: ${order.device_type}`);
  
  const suitableMasters = masters.filter(m => {
    const specs = JSON.parse(m.specialization || '[]');
    return specs.includes(order.device_type) && 
           m.is_on_shift === 1 && 
           m.status === 'available' &&
           m.verification_status === 'verified';
  });
  
  console.log(`   ✅ Подходящих мастеров: ${suitableMasters.length}`);
  
  if (suitableMasters.length > 0) {
    suitableMasters.forEach(m => {
      console.log(`      - Мастер #${m.id}: ${m.name}`);
    });
  } else {
    console.log(`      ❌ НЕТ ПОДХОДЯЩИХ МАСТЕРОВ!`);
    
    // Детали проблемы
    const onShiftMasters = masters.filter(m => m.is_on_shift === 1);
    const verifiedMasters = masters.filter(m => m.verification_status === 'verified');
    const mastersWithSpec = masters.filter(m => {
      const specs = JSON.parse(m.specialization || '[]');
      return specs.includes(order.device_type);
    });
    
    console.log(`      - Мастеров на смене: ${onShiftMasters.length}`);
    console.log(`      - Верифицированных: ${verifiedMasters.length}`);
    console.log(`      - Со специализацией "${order.device_type}": ${mastersWithSpec.length}`);
  }
  
  // Проверка назначений для этого заказа
  const orderAssignments = assignments.filter(a => a.order_id === order.id);
  console.log(`   📋 Назначений для заказа: ${orderAssignments.length}`);
  if (orderAssignments.length > 0) {
    orderAssignments.forEach(a => {
      console.log(`      - Assignment #${a.id}: ${a.status} (мастер #${a.master_id})`);
    });
  }
});

// 5. Рекомендации
console.log('\n' + '='.repeat(60));
console.log('\n5️⃣ РЕКОМЕНДАЦИИ:\n');

if (masters.length === 0) {
  console.log('   ❌ В системе нет мастеров. Зарегистрируйте мастера.');
} else {
  const onShiftMasters = masters.filter(m => m.is_on_shift === 1);
  const verifiedMasters = masters.filter(m => m.verification_status === 'verified');
  
  if (onShiftMasters.length === 0) {
    console.log('   ⚠️  Все мастера НЕ НА СМЕНЕ. Включите смену в приложении мастера.');
  }
  
  if (verifiedMasters.length === 0) {
    console.log('   ⚠️  Все мастера НЕ ВЕРИФИЦИРОВАНЫ. Верифицируйте мастера в админ-панели.');
  }
  
  const mastersWithoutSpecs = masters.filter(m => {
    const specs = JSON.parse(m.specialization || '[]');
    return specs.length === 0;
  });
  
  if (mastersWithoutSpecs.length > 0) {
    console.log(`   ⚠️  ${mastersWithoutSpecs.length} мастеров без специализаций. Добавьте специализации.`);
  }
}

if (newOrders.length === 0) {
  console.log('   ℹ️  Нет новых заказов. Создайте тестовый заказ в клиентском приложении.');
}

console.log('\n' + '='.repeat(60));
console.log('\n✅ Диагностика завершена!\n');

process.exit(0);


