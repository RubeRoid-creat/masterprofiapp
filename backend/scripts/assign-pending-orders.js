import { query } from '../database/db.js';
import { notifyMasters } from '../services/assignment-service.js';

console.log('🔧 НАЗНАЧЕНИЕ ЗАКАЗОВ БЕЗ НАЗНАЧЕНИЙ МАСТЕРАМ\n');
console.log('='.repeat(80));

// Получаем новые заказы без назначений
const ordersWithoutAssignments = query.all(`
  SELECT 
    o.id,
    o.order_number,
    o.device_type,
    o.repair_status,
    o.created_at,
    o.latitude,
    o.longitude
  FROM orders o
  LEFT JOIN order_assignments oa ON o.id = oa.order_id AND oa.status = 'pending'
  WHERE o.repair_status = 'new' AND oa.id IS NULL
  ORDER BY o.created_at DESC
`);

console.log(`\nНайдено новых заказов без назначений: ${ordersWithoutAssignments.length}\n`);

if (ordersWithoutAssignments.length === 0) {
  console.log('✅ Все новые заказы имеют назначения!\n');
  process.exit(0);
}

let assignedCount = 0;
let failedCount = 0;

ordersWithoutAssignments.forEach(order => {
  console.log(`\n📋 Заказ #${order.id} (${order.order_number || 'без номера'}):`);
  console.log(`   Тип: ${order.device_type}`);
  console.log(`   Координаты: ${order.latitude}, ${order.longitude}`);
  
  try {
    console.log(`   🔄 Отправка уведомлений мастерам...`);
    notifyMasters(order.id, order.device_type, order.latitude, order.longitude);
    assignedCount++;
    console.log(`   ✅ Назначение отправлено`);
  } catch (error) {
    console.error(`   ❌ Ошибка: ${error.message}`);
    failedCount++;
  }
});

console.log('\n' + '='.repeat(80));
console.log(`\n✅ Обработано заказов: ${ordersWithoutAssignments.length}`);
console.log(`✅ Назначено: ${assignedCount}`);
console.log(`❌ Ошибок: ${failedCount}\n`);

process.exit(0);
