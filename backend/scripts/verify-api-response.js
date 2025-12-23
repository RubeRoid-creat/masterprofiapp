import { query } from '../database/db.js';

console.log('🔍 ПРОВЕРКА ОТВЕТА API ДЛЯ МАСТЕРА\n');
console.log('='.repeat(80));

// Получаем мастера #2 (user_id=4)
const master = query.get('SELECT id, user_id FROM masters WHERE id = 2');

if (!master) {
  console.log('❌ Мастер #2 не найден');
  process.exit(1);
}

console.log(`\n👨‍🔧 Мастер #${master.id} (user_id: ${master.user_id})\n`);

// Имитируем SQL запрос из API endpoint /api/assignments/my
let sql = `
  SELECT 
    oa.*,
    o.device_type, o.device_brand, o.device_model,
    o.problem_description, o.address, o.latitude, o.longitude,
    o.arrival_time, o.order_type, o.estimated_cost,
    u.name as client_name, u.phone as client_phone
  FROM order_assignments oa
  JOIN orders o ON oa.order_id = o.id
  JOIN clients c ON o.client_id = c.id
  JOIN users u ON c.user_id = u.id
  WHERE oa.master_id = ?
  ORDER BY oa.created_at DESC
`;

const assignments = query.all(sql, [master.id]);

console.log(`📋 Всего назначений в БД: ${assignments.length}`);
console.log(`📅 Текущее время: ${new Date().toISOString()}\n`);

// Фильтруем истекшие pending назначения (как в API)
const now = new Date();
const filteredAssignments = assignments
  .filter(assignment => {
    if (assignment.status === 'pending' && assignment.expires_at) {
      const expiresAt = new Date(assignment.expires_at);
      if (expiresAt < now) {
        console.log(`⚠️ Фильтруем истекшее назначение #${assignment.id} (истекло: ${expiresAt.toISOString()})`);
        return false;
      }
    }
    return true;
  });

console.log(`✅ После фильтрации истекших: ${filteredAssignments.length} назначений\n`);

// Показываем активные pending назначения
const activePending = filteredAssignments.filter(a => a.status === 'pending');
if (activePending.length > 0) {
  console.log('✅ АКТИВНЫЕ PENDING НАЗНАЧЕНИЯ:\n');
  activePending.forEach((a, idx) => {
    const expiresAt = new Date(a.expires_at);
    const minutesLeft = Math.floor((expiresAt - now) / 1000 / 60);
    console.log(`   ${idx + 1}. Назначение #${a.id}:`);
    console.log(`      Заказ #${a.order_id} (${a.device_type})`);
    console.log(`      Осталось времени: ${minutesLeft} минут`);
    console.log(`      Истекает: ${a.expires_at}\n`);
  });
} else {
  console.log('⚠️ Нет активных pending назначений!\n');
  console.log('💡 Рекомендация: Создайте новый тестовый заказ:\n');
  console.log('   node scripts/create-test-order.js\n');
}

console.log('='.repeat(80));

process.exit(0);


