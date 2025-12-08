import { query } from '../database/db.js';
import { notifyMasters } from '../services/assignment-service.js';

console.log('🔧 СОЗДАНИЕ НЕДОСТАЮЩИХ ASSIGNMENTS\n');
console.log('=' .repeat(60));

// Находим все новые заказы без assignments
const ordersWithoutAssignments = query.all(`
  SELECT DISTINCT o.id, o.device_type, o.latitude, o.longitude, o.created_at
  FROM orders o
  WHERE o.repair_status = 'new'
    AND NOT EXISTS (
      SELECT 1 FROM order_assignments oa 
      WHERE oa.order_id = o.id 
        AND oa.status IN ('pending', 'accepted')
    )
  ORDER BY o.created_at DESC
  LIMIT 10
`);

console.log(`\nНайдено заказов без активных assignments: ${ordersWithoutAssignments.length}\n`);

if (ordersWithoutAssignments.length === 0) {
  console.log('✅ Все новые заказы уже имеют assignments!\n');
  process.exit(0);
}

ordersWithoutAssignments.forEach((order, index) => {
  console.log(`${index + 1}. Заказ #${order.id}: ${order.device_type}`);
  console.log(`   Создан: ${order.created_at}`);
  console.log(`   Координаты: ${order.latitude}, ${order.longitude}`);
  
  try {
    notifyMasters(order.id, order.device_type, order.latitude, order.longitude);
    console.log(`   ✅ Assignment создан\n`);
  } catch (error) {
    console.log(`   ❌ Ошибка: ${error.message}\n`);
  }
});

console.log('=' .repeat(60));
console.log('\n✅ Готово! Проверьте assignments командой:');
console.log('   node scripts/diagnose-assignments.js\n');

process.exit(0);
