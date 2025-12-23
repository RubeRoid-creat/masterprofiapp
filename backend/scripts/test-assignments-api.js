import { query } from '../database/db.js';

console.log('🧪 ТЕСТИРОВАНИЕ API НАЗНАЧЕНИЙ\n');
console.log('='.repeat(80));

// Получаем мастера и его токен (если есть)
const master = query.get(`
  SELECT 
    m.id as master_id,
    m.user_id,
    u.email,
    u.password_hash
  FROM masters m
  JOIN users u ON m.user_id = u.id
  WHERE m.id = 2
`);

if (!master) {
  console.log('❌ Мастер #2 не найден');
  process.exit(1);
}

console.log(`\n👨‍🔧 Тестируем для мастера:`);
console.log(`   ID: ${master.master_id}`);
console.log(`   user_id: ${master.user_id}`);
console.log(`   Email: ${master.email}\n`);

// Имитируем SQL запрос из API
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
  WHERE oa.master_id = ?
  ORDER BY oa.created_at DESC
`;

const assignments = query.all(sql, [master.master_id]);

console.log(`📋 Найдено назначений в БД: ${assignments.length}\n`);

// Группируем по статусу
const byStatus = {
  pending: [],
  accepted: [],
  rejected: [],
  expired: []
};

assignments.forEach(a => {
  const status = a.status || 'unknown';
  if (byStatus[status]) {
    byStatus[status].push(a);
  }
});

console.log('📊 Распределение по статусам:');
Object.keys(byStatus).forEach(status => {
  if (byStatus[status].length > 0) {
    console.log(`   ${status}: ${byStatus[status].length}`);
  }
});

// Показываем pending назначения
if (byStatus.pending.length > 0) {
  console.log(`\n⏳ PENDING НАЗНАЧЕНИЯ (${byStatus.pending.length}):\n`);
  byStatus.pending.forEach((a, idx) => {
    const expiresAt = new Date(a.expires_at);
    const now = new Date();
    const isExpired = expiresAt < now;
    
    console.log(`   ${idx + 1}. Назначение #${a.id}:`);
    console.log(`      Заказ #${a.order_id} (${a.device_type})`);
    console.log(`      Создано: ${a.created_at}`);
    console.log(`      Истекает: ${a.expires_at}`);
    console.log(`      Статус: ${isExpired ? '⚠️ ИСТЕКЛО' : '✅ Активно'}`);
    if (isExpired) {
      const minutesAgo = Math.floor((now - expiresAt) / 1000 / 60);
      console.log(`      Прошло: ${minutesAgo} минут`);
    }
    console.log('');
  });
}

// Показываем, что будет возвращено API (без фильтрации по expires_at)
console.log('\n' + '='.repeat(80));
console.log('\n💡 ВАЖНО: API возвращает ВСЕ pending назначения, включая истекшие!');
console.log('   Приложение должно фильтровать истекшие назначения самостоятельно.\n');

process.exit(0);


