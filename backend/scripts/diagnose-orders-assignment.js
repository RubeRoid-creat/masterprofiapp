import { query } from '../database/db.js';

console.log('🔍 ДИАГНОСТИКА ПРОБЛЕМЫ С НАЗНАЧЕНИЕМ ЗАКАЗОВ\n');
console.log('='.repeat(80));

// 1. Проверяем новых заказов
console.log('\n📋 1. ПРОВЕРКА ЗАКАЗОВ:');
console.log('-'.repeat(80));

const newOrders = query.all(`
  SELECT 
    id, 
    order_number,
    device_type,
    repair_status,
    request_status,
    created_at,
    latitude,
    longitude
  FROM orders
  WHERE repair_status = 'new'
  ORDER BY created_at DESC
  LIMIT 10
`);

console.log(`\nНайдено новых заказов: ${newOrders.length}\n`);

if (newOrders.length > 0) {
  newOrders.forEach(order => {
    console.log(`  Заказ #${order.id} (${order.order_number || 'без номера'}):`);
    console.log(`    Тип устройства: "${order.device_type}"`);
    console.log(`    Статус: ${order.repair_status} / ${order.request_status}`);
    console.log(`    Координаты: ${order.latitude}, ${order.longitude}`);
    console.log(`    Создан: ${order.created_at}`);
    console.log('');
  });
} else {
  console.log('  ❌ Нет новых заказов!\n');
}

// 2. Проверяем мастеров
console.log('\n👨‍🔧 2. ПРОВЕРКА МАСТЕРОВ:');
console.log('-'.repeat(80));

const allMasters = query.all(`
  SELECT 
    m.id,
    m.user_id,
    m.specialization,
    m.is_on_shift,
    m.status,
    m.verification_status,
    m.latitude,
    m.longitude,
    u.name,
    u.email
  FROM masters m
  JOIN users u ON m.user_id = u.id
  ORDER BY m.id
`);

console.log(`\nВсего мастеров: ${allMasters.length}\n`);

allMasters.forEach(master => {
  console.log(`  Мастер #${master.id}: ${master.name} (${master.email})`);
  console.log(`    На смене: ${master.is_on_shift === 1 ? '✅ ДА' : '❌ НЕТ'}`);
  console.log(`    Статус: ${master.status}`);
  console.log(`    Верификация: ${master.verification_status}`);
  console.log(`    Координаты: ${master.latitude || 'НЕТ'}, ${master.longitude || 'НЕТ'}`);
  
  let specs = [];
  try {
    specs = JSON.parse(master.specialization || '[]');
  } catch (e) {
    console.log(`    ❌ Ошибка парсинга специализации: ${e.message}`);
    console.log(`    Текущее значение: "${master.specialization}"`);
  }
  
  if (specs.length === 0) {
    console.log(`    ❌ Специализация: ПУСТО или ОТСУТСТВУЕТ`);
  } else {
    console.log(`    ✅ Специализация (${specs.length}): ${specs.join(', ')}`);
  }
  console.log('');
});

// 3. Проверяем доступных мастеров (которые должны получать заявки)
console.log('\n✅ 3. ДОСТУПНЫЕ МАСТЕРА (is_on_shift=1 AND status="available"):');
console.log('-'.repeat(80));

const availableMasters = query.all(`
  SELECT 
    m.id,
    m.user_id,
    m.specialization,
    m.is_on_shift,
    m.status,
    m.verification_status,
    u.name,
    u.email
  FROM masters m
  JOIN users u ON m.user_id = u.id
  WHERE m.is_on_shift = 1 AND m.status = 'available'
`);

console.log(`\nДоступных мастеров: ${availableMasters.length}\n`);

if (availableMasters.length === 0) {
  console.log('  ❌ НЕТ доступных мастеров! Мастера должны включить смену.\n');
} else {
  availableMasters.forEach(master => {
    console.log(`  ✅ Мастер #${master.id}: ${master.name}`);
    
    let specs = [];
    try {
      specs = JSON.parse(master.specialization || '[]');
    } catch (e) {
      console.log(`    ❌ Ошибка парсинга специализации`);
      specs = [];
    }
    
    if (specs.length === 0) {
      console.log(`    ❌ БЕЗ СПЕЦИАЛИЗАЦИИ - не будет получать заявки!`);
    } else {
      console.log(`    Специализация: ${specs.join(', ')}`);
    }
    console.log(`    Верификация: ${master.verification_status}`);
    console.log('');
  });
}

// 4. Проверяем соответствие типов устройств
console.log('\n🔗 4. СООТВЕТСТВИЕ ТИПОВ УСТРОЙСТВ:');
console.log('-'.repeat(80));

if (newOrders.length > 0 && availableMasters.length > 0) {
  // Собираем все типы устройств из заказов
  const orderDeviceTypes = [...new Set(newOrders.map(o => o.device_type).filter(Boolean))];
  
  console.log(`\nТипы устройств в заказах: ${orderDeviceTypes.join(', ')}\n`);
  
  // Для каждого мастера проверяем, подходит ли он хотя бы к одному заказу
  availableMasters.forEach(master => {
    let specs = [];
    try {
      specs = JSON.parse(master.specialization || '[]');
    } catch (e) {
      specs = [];
    }
    
    if (specs.length === 0) {
      console.log(`  Мастер #${master.id} (${master.name || 'без имени'}): ❌ БЕЗ СПЕЦИАЛИЗАЦИИ`);
      return;
    }
    
    const matchingOrders = orderDeviceTypes.filter(deviceType => specs.includes(deviceType));
    
    if (matchingOrders.length > 0) {
      console.log(`  Мастер #${master.id} (${master.name || 'без имени'}): ✅ Подходит к ${matchingOrders.length} типам заказов`);
      console.log(`    Соответствия: ${matchingOrders.join(', ')}`);
    } else {
      console.log(`  Мастер #${master.id} (${master.name || 'без имени'}): ❌ НЕ ПОДХОДИТ ни к одному заказу`);
      console.log(`    Специализация мастера: ${specs.join(', ')}`);
      console.log(`    Типы в заказах: ${orderDeviceTypes.join(', ')}`);
    }
  });
} else {
  console.log('\n  ⚠️ Нет данных для сравнения (нет новых заказов или доступных мастеров)\n');
}

// 5. Проверяем назначения
console.log('\n📨 5. ПРОВЕРКА НАЗНАЧЕНИЙ:');
console.log('-'.repeat(80));

const recentAssignments = query.all(`
  SELECT 
    oa.id,
    oa.order_id,
    oa.master_id,
    oa.status,
    oa.created_at,
    o.device_type,
    m.user_id
  FROM order_assignments oa
  JOIN orders o ON oa.order_id = o.id
  JOIN masters m ON oa.master_id = m.id
  ORDER BY oa.created_at DESC
  LIMIT 20
`);

console.log(`\nПоследних назначений: ${recentAssignments.length}\n`);

if (recentAssignments.length > 0) {
  recentAssignments.forEach(assignment => {
    console.log(`  Назначение #${assignment.id}:`);
    console.log(`    Заказ #${assignment.order_id} (${assignment.device_type}) -> Мастер #${assignment.master_id}`);
    console.log(`    Статус: ${assignment.status}`);
    console.log(`    Создано: ${assignment.created_at}`);
    console.log('');
  });
} else {
  console.log('  ⚠️ Нет последних назначений\n');
}

// 6. Рекомендации
console.log('\n💡 РЕКОМЕНДАЦИИ:');
console.log('='.repeat(80));

const issues = [];

if (newOrders.length === 0) {
  issues.push('❌ Нет новых заказов в системе');
}

if (availableMasters.length === 0) {
  issues.push('❌ Нет мастеров на смене (is_on_shift=1) или статус не "available"');
}

const mastersWithoutSpecs = availableMasters.filter(m => {
  try {
    const specs = JSON.parse(m.specialization || '[]');
    return specs.length === 0;
  } catch (e) {
    return true;
  }
});

if (mastersWithoutSpecs.length > 0) {
  issues.push(`❌ ${mastersWithoutSpecs.length} мастер(ов) без специализации`);
  mastersWithoutSpecs.forEach(m => {
    console.log(`   - Мастер #${m.id}: ${m.name || m.email}`);
  });
}

const unverifiedMasters = availableMasters.filter(m => m.verification_status !== 'verified');

if (unverifiedMasters.length > 0 && newOrders.length > 0) {
  issues.push(`⚠️ ${unverifiedMasters.length} мастер(ов) не верифицированы - не увидят новые заказы`);
  unverifiedMasters.forEach(m => {
    console.log(`   - Мастер #${m.id}: ${m.name || m.email} (статус: ${m.verification_status})`);
  });
}

if (issues.length === 0 && newOrders.length > 0 && availableMasters.length > 0) {
  console.log('\n✅ Все условия выполнены! Заявки должны назначаться.');
  console.log('\nЕсли заявки не появляются, проверьте:');
  console.log('  1. Логи сервера на наличие ошибок при вызове notifyMasters()');
  console.log('  2. WebSocket соединения с мастерами');
  console.log('  3. Правильность формата device_type в заказах');
} else {
  console.log('\n⚠️ Обнаружены проблемы:');
  issues.forEach(issue => console.log(`  ${issue}`));
}

console.log('\n' + '='.repeat(80) + '\n');

process.exit(0);
