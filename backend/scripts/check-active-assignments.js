import { query } from '../database/db.js';

console.log('🔍 ПРОВЕРКА АКТИВНЫХ НАЗНАЧЕНИЙ ДЛЯ МАСТЕРА #2\n');
console.log('='.repeat(80));

// Получаем мастера #2
const master = query.get('SELECT id, user_id FROM masters WHERE id = 2');
if (!master) {
  console.log('❌ Мастер #2 не найден');
  process.exit(1);
}

console.log(`👨‍🔧 Мастер #${master.id} (user_id: ${master.user_id})\n`);

// Получаем все pending назначения
const sql = `
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
  WHERE oa.master_id = ? AND oa.status = 'pending'
  ORDER BY oa.created_at DESC
`;

const assignments = query.all(sql, [master.id]);
console.log(`📋 Всего pending назначений в БД: ${assignments.length}\n`);

if (assignments.length === 0) {
  console.log('⚠️ Нет pending назначений!');
  process.exit(0);
}

const now = new Date();
console.log(`📅 Текущее время (сервер): ${now.toISOString()}\n`);

console.log('📋 ДЕТАЛИ НАЗНАЧЕНИЙ:\n');
assignments.forEach((a, idx) => {
  console.log(`${idx + 1}. Назначение #${a.id}:`);
  console.log(`   Заказ #${a.order_id}`);
  console.log(`   Устройство: ${a.device_type}`);
  console.log(`   expires_at (raw): ${a.expires_at}`);
  
  if (a.expires_at) {
    const expiresAt = new Date(a.expires_at);
    const isActive = expiresAt > now;
    const diffMs = expiresAt - now;
    const diffMinutes = Math.floor(diffMs / 1000 / 60);
    
    console.log(`   expires_at (parsed): ${expiresAt.toISOString()}`);
    console.log(`   Активно: ${isActive ? '✅ ДА' : '❌ НЕТ'}`);
    console.log(`   Разница: ${diffMinutes} минут (${diffMs} мс)`);
  } else {
    console.log(`   ⚠️ expires_at отсутствует - должно быть активным`);
  }
  console.log('');
});

// Фильтруем активные
const activeAssignments = assignments.filter(a => {
  if (!a.expires_at) return true; // Без expires_at считаем активным
  const expiresAt = new Date(a.expires_at);
  return expiresAt > now;
});

console.log(`\n✅ Активных назначений: ${activeAssignments.length} из ${assignments.length}`);
console.log('='.repeat(80));

process.exit(0);
