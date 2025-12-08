import { query } from '../database/db.js';
import { getConnectedClientsCount } from '../websocket.js';

console.log('🔍 ПРОВЕРКА НАЗНАЧЕНИЙ И WEB SOCKET СОЕДИНЕНИЙ\n');
console.log('='.repeat(80));

// 1. Проверяем последние назначения
console.log('\n📨 ПОСЛЕДНИЕ НАЗНАЧЕНИЯ:');
console.log('-'.repeat(80));

const recentAssignments = query.all(`
  SELECT 
    oa.id,
    oa.order_id,
    oa.master_id,
    oa.status,
    oa.created_at,
    oa.expires_at,
    o.device_type,
    o.repair_status,
    m.user_id,
    u.name as master_name,
    u.email as master_email
  FROM order_assignments oa
  JOIN orders o ON oa.order_id = o.id
  JOIN masters m ON oa.master_id = m.id
  JOIN users u ON m.user_id = u.id
  ORDER BY oa.created_at DESC
  LIMIT 10
`);

console.log(`\nНайдено назначений: ${recentAssignments.length}\n`);

if (recentAssignments.length > 0) {
  recentAssignments.forEach(assignment => {
    console.log(`  Назначение #${assignment.id}:`);
    console.log(`    Заказ #${assignment.order_id} (${assignment.device_type})`);
    console.log(`    Статус заказа: ${assignment.repair_status}`);
    console.log(`    Мастер: #${assignment.master_id} (user_id: ${assignment.user_id})`);
    console.log(`    Имя мастера: ${assignment.master_name} (${assignment.master_email})`);
    console.log(`    Статус назначения: ${assignment.status}`);
    console.log(`    Создано: ${assignment.created_at}`);
    console.log(`    Истекает: ${assignment.expires_at}`);
    
    // Проверяем, не истекло ли время
    const expiresAt = new Date(assignment.expires_at);
    const now = new Date();
    if (assignment.status === 'pending' && expiresAt < now) {
      console.log(`    ⚠️ ВНИМАНИЕ: Назначение истекло!`);
    }
    console.log('');
  });
} else {
  console.log('  ⚠️ Нет назначений\n');
}

// 2. Проверяем новые заказы без назначений
console.log('\n📋 ЗАКАЗЫ БЕЗ НАЗНАЧЕНИЙ:');
console.log('-'.repeat(80));

const ordersWithoutAssignments = query.all(`
  SELECT 
    o.id,
    o.order_number,
    o.device_type,
    o.repair_status,
    o.created_at
  FROM orders o
  LEFT JOIN order_assignments oa ON o.id = oa.order_id
  WHERE o.repair_status = 'new' AND oa.id IS NULL
  ORDER BY o.created_at DESC
  LIMIT 10
`);

console.log(`\nНайдено новых заказов без назначений: ${ordersWithoutAssignments.length}\n`);

if (ordersWithoutAssignments.length > 0) {
  ordersWithoutAssignments.forEach(order => {
    console.log(`  Заказ #${order.id} (${order.order_number || 'без номера'}):`);
    console.log(`    Тип: ${order.device_type}`);
    console.log(`    Создан: ${order.created_at}`);
    console.log(`    ⚠️ Нет назначения - мастерам не отправлено уведомление!`);
    console.log('');
  });
} else {
  console.log('  ✅ Все новые заказы имеют назначения\n');
}

// 3. Проверяем подключенных к WebSocket
console.log('\n🔌 WEB SOCKET ПОДКЛЮЧЕНИЯ:');
console.log('-'.repeat(80));

try {
  const connectedCount = getConnectedClientsCount();
  console.log(`\n  Подключено пользователей: ${connectedCount}\n`);
  
  if (connectedCount === 0) {
    console.log('  ⚠️ ВНИМАНИЕ: Нет подключенных пользователей к WebSocket!');
    console.log('    Мастера не получат уведомления в реальном времени.');
    console.log('    Уведомления появятся только при следующем запросе заказов через API.\n');
  }
} catch (e) {
  console.log(`  ⚠️ Не удалось проверить WebSocket: ${e.message}\n`);
}

// 4. Проверяем pending назначения
console.log('\n⏳ PENDING НАЗНАЧЕНИЯ (ожидающие ответа):');
console.log('-'.repeat(80));

const pendingAssignments = query.all(`
  SELECT 
    oa.id,
    oa.order_id,
    oa.master_id,
    oa.created_at,
    oa.expires_at,
    o.device_type,
    m.user_id,
    u.name as master_name,
    m.is_on_shift,
    m.status as master_status
  FROM order_assignments oa
  JOIN orders o ON oa.order_id = o.id
  JOIN masters m ON oa.master_id = m.id
  JOIN users u ON m.user_id = u.id
  WHERE oa.status = 'pending'
  ORDER BY oa.created_at DESC
`);

console.log(`\nНайдено pending назначений: ${pendingAssignments.length}\n`);

if (pendingAssignments.length > 0) {
  pendingAssignments.forEach(assignment => {
    const expiresAt = new Date(assignment.expires_at);
    const now = new Date();
    const timeLeft = Math.round((expiresAt - now) / 1000 / 60); // минут
    
    console.log(`  Назначение #${assignment.id}:`);
    console.log(`    Заказ #${assignment.order_id} (${assignment.device_type})`);
    console.log(`    Мастер: ${assignment.master_name} (user_id: ${assignment.user_id})`);
    console.log(`    На смене: ${assignment.is_on_shift === 1 ? 'ДА' : 'НЕТ'}`);
    console.log(`    Статус: ${assignment.master_status}`);
    console.log(`    Осталось времени: ${timeLeft > 0 ? `${timeLeft} мин` : 'ИСТЕКЛО'}`);
    if (timeLeft <= 0) {
      console.log(`    ⚠️ ВНИМАНИЕ: Назначение истекло, но статус все еще 'pending'!`);
    }
    console.log('');
  });
} else {
  console.log('  ✅ Нет pending назначений\n');
}

console.log('='.repeat(80));
console.log('\n💡 РЕКОМЕНДАЦИИ:\n');

if (ordersWithoutAssignments.length > 0) {
  console.log('  ❌ Есть заказы без назначений!');
  console.log('     Запустите скрипт создания заказов или проверьте логи сервера.\n');
}

if (pendingAssignments.length > 0) {
  const expired = pendingAssignments.filter(a => new Date(a.expires_at) < new Date());
  if (expired.length > 0) {
    console.log(`  ⚠️ Найдено ${expired.length} истекших назначений со статусом 'pending'!`);
    console.log('     Система должна была обработать их автоматически.\n');
  }
}

console.log('  📱 Для получения заявок мастер должен:');
console.log('     1. Быть подключен к интернету');
console.log('     2. Иметь активное WebSocket соединение (открыто приложение)');
console.log('     3. Быть на смене (is_on_shift = 1)');
console.log('     4. Иметь соответствующую специализацию');
console.log('\n');

process.exit(0);
