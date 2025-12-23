import { query } from '../database/db.js';
import { checkAndProcessExpiredAssignments } from '../services/assignment-service.js';

console.log('🔄 ОБРАБОТКА ВСЕХ ИСТЕКШИХ НАЗНАЧЕНИЙ\n');
console.log('='.repeat(80));

// Проверяем истекшие назначения
const processedCount = checkAndProcessExpiredAssignments();

console.log('\n' + '='.repeat(80));
console.log(`\n✅ Обработка завершена. Обработано заказов: ${processedCount}\n`);

// Проверяем новые назначения после обработки
const newAssignments = query.all(`
  SELECT 
    oa.id,
    oa.order_id,
    oa.master_id,
    oa.attempt_number,
    oa.status,
    oa.created_at,
    oa.expires_at,
    o.device_type,
    u.name as master_name
  FROM order_assignments oa
  JOIN orders o ON oa.order_id = o.id
  JOIN masters m ON oa.master_id = m.id
  JOIN users u ON m.user_id = u.id
  WHERE oa.created_at > datetime('now', '-2 minutes')
    AND oa.status = 'pending'
  ORDER BY oa.created_at DESC
`);

if (newAssignments.length > 0) {
  console.log('✅ НОВЫЕ НАЗНАЧЕНИЯ (созданы при перераспределении):\n');
  newAssignments.forEach((a, idx) => {
    const expiresAt = new Date(a.expires_at);
    const now = new Date();
    const minutesLeft = Math.floor((expiresAt - now) / 1000 / 60);
    console.log(`   ${idx + 1}. Назначение #${a.id}:`);
    console.log(`      Заказ #${a.order_id} (${a.device_type})`);
    console.log(`      Мастер: ${a.master_name} (ID: ${a.master_id})`);
    console.log(`      Попытка: #${a.attempt_number}`);
    console.log(`      Осталось времени: ${minutesLeft} минут`);
    console.log(`      Истекает: ${a.expires_at}\n`);
  });
} else {
  console.log('ℹ️ Новых назначений не создано\n');
}

process.exit(0);


