import { query } from '../database/db.js';
import { checkAndProcessExpiredAssignments } from '../services/assignment-service.js';

console.log('🧪 ТЕСТИРОВАНИЕ ПЕРЕРАСПРЕДЕЛЕНИЯ ИСТЕКШИХ НАЗНАЧЕНИЙ\n');
console.log('='.repeat(80));

// Получаем pending назначения
const pendingAssignments = query.all(`
  SELECT 
    oa.id,
    oa.order_id,
    oa.master_id,
    oa.status,
    oa.expires_at,
    o.repair_status,
    o.device_type,
    m.user_id,
    u.name as master_name
  FROM order_assignments oa
  JOIN orders o ON oa.order_id = o.id
  JOIN masters m ON oa.master_id = m.id
  JOIN users u ON m.user_id = u.id
  WHERE oa.status = 'pending'
  ORDER BY oa.expires_at ASC
  LIMIT 10
`);

console.log(`\n📋 Найдено pending назначений: ${pendingAssignments.length}\n`);

if (pendingAssignments.length === 0) {
  console.log('✅ Нет pending назначений для тестирования\n');
  process.exit(0);
}

// Показываем pending назначения
console.log('⏳ PENDING НАЗНАЧЕНИЯ:\n');
pendingAssignments.forEach((a, idx) => {
  const expiresAt = new Date(a.expires_at);
  const now = new Date();
  const isExpired = expiresAt < now;
  const minutesLeft = isExpired 
    ? Math.floor((now - expiresAt) / 1000 / 60)
    : Math.floor((expiresAt - now) / 1000 / 60);
  
  console.log(`   ${idx + 1}. Назначение #${a.id}:`);
  console.log(`      Заказ #${a.order_id} (${a.device_type})`);
  console.log(`      Мастер: ${a.master_name} (ID: ${a.master_id})`);
  console.log(`      Истекает: ${a.expires_at}`);
  console.log(`      Статус: ${isExpired ? `⚠️ ИСТЕКЛО (${minutesLeft} мин назад)` : `✅ Активно (осталось ${minutesLeft} мин)`}`);
  console.log('');
});

// Проверяем истекшие
const expiredCount = pendingAssignments.filter(a => new Date(a.expires_at) < new Date()).length;
console.log(`\n⚠️ Истекших назначений: ${expiredCount}\n`);

if (expiredCount === 0) {
  console.log('✅ Нет истекших назначений для перераспределения\n');
  process.exit(0);
}

console.log('🔄 Запускаем проверку и обработку истекших назначений...\n');
const processedCount = checkAndProcessExpiredAssignments();

console.log('\n' + '='.repeat(80));
console.log(`\n✅ Обработано заказов: ${processedCount}\n`);

// Проверяем результат - ищем новые назначения
const newAssignments = query.all(`
  SELECT 
    oa.id,
    oa.order_id,
    oa.master_id,
    oa.attempt_number,
    oa.created_at,
    oa.status,
    u.name as master_name
  FROM order_assignments oa
  JOIN masters m ON oa.master_id = m.id
  JOIN users u ON m.user_id = u.id
  WHERE oa.created_at > datetime('now', '-1 minute')
    AND oa.status = 'pending'
  ORDER BY oa.created_at DESC
`);

if (newAssignments.length > 0) {
  console.log('✅ НОВЫЕ НАЗНАЧЕНИЯ (созданы при перераспределении):\n');
  newAssignments.forEach((a, idx) => {
    console.log(`   ${idx + 1}. Назначение #${a.id}:`);
    console.log(`      Заказ #${a.order_id}`);
    console.log(`      Мастер: ${a.master_name} (ID: ${a.master_id})`);
    console.log(`      Попытка: #${a.attempt_number}`);
    console.log(`      Создано: ${a.created_at}\n`);
  });
} else {
  console.log('ℹ️ Новых назначений не создано (возможно, нет доступных мастеров)\n');
}

process.exit(0);
