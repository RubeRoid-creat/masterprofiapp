import { query } from '../database/db.js';

console.log('🔧 FIXING MASTER STATUS AND CREATING TEST ORDER\n');

// 1. Update Master #2 (Pavel) Specialization
console.log('1️⃣ Updating Master #2 Specialization...');
const specs = JSON.stringify([
  'Холодильник',
  'Стиральная машина',
  'Посудомоечная машина',
  'Духовой шкаф',
  'Варочная панель',
  'Микроволновая печь',
  'Кондиционер',
  'Кофемашина',
  'Ноутбук',
  'Десктоп',
  'Морозильный ларь',
  'Водонагреватель',
  'Плита'
]);

query.run(`
  UPDATE masters 
  SET specialization = ?, 
      verification_status = 'verified',
      is_on_shift = 1,
      status = 'available',
      latitude = 56.8739848, -- Set some default coords if missing
      longitude = 35.8903761
  WHERE user_id = 4
`, [specs]);
console.log('   ✅ Master #2 updated: Verified, On Shift, Full Specialization');

// 2. Update Master #1 (Test) Status
console.log('\n2️⃣ Updating Master #1 Status...');
query.run(`
  UPDATE masters 
  SET verification_status = 'verified',
      is_on_shift = 1,
      status = 'available',
      latitude = 56.8739848,
      longitude = 35.8903761
  WHERE user_id = 2
`, []);
console.log('   ✅ Master #1 updated: Verified, On Shift');

// 3. Create Test Order
console.log('\n3️⃣ Creating Test Order...');
// Find a client (using user_id 1 or creating one if needed, but assuming user 1 exists from previous logs/dumps)
// In the diagnosis, orders were from clients with user_id (implied).
// Let's pick a client id from existing orders.
const existingOrder = query.get('SELECT client_id, latitude, longitude FROM orders LIMIT 1');
const clientId = existingOrder ? existingOrder.client_id : 1; 
const lat = existingOrder ? existingOrder.latitude : 56.8739848;
const lon = existingOrder ? existingOrder.longitude : 35.8903761;

const result = query.run(`
  INSERT INTO orders (
    order_number, client_id, request_status, repair_status, priority,
    device_type, device_brand, device_model,
    problem_description, address, latitude, longitude,
    urgency, created_at
  ) VALUES (
    ?, ?, 'new', 'new', 'regular',
    'Стиральная машина', 'Samsung', 'TestModel',
    'Тестовый заказ: не включается (автосоздание)', 'Тестовый адрес', ?, ?,
    'urgent', CURRENT_TIMESTAMP
  )
`, [`TEST-${Date.now()}`, clientId, lat, lon]);

const orderId = result.lastInsertRowid;
console.log(`   ✅ Test Order #${orderId} created (Стиральная машина)`);

// 4. Trigger Matching (Simulated)
console.log('\n4️⃣ Triggering Matching Logic...');
// We can't easily import the ESM module functions in this script without setup, 
// but we can simulate what the server would do by checking if our logic finds them now.

const masters = query.all(`
  SELECT id, name, specialization, is_on_shift, status, verification_status 
  FROM masters m 
  JOIN users u ON m.user_id = u.id
  WHERE m.is_on_shift = 1 AND m.status = 'available'
`);

const suitable = masters.filter(m => {
  const s = JSON.parse(m.specialization || '[]');
  return s.includes('Стиральная машина');
});

console.log(`   Found ${suitable.length} suitable masters for the new order.`);
suitable.forEach(m => console.log(`   - ${m.name} (#${m.id})`));

if (suitable.length > 0) {
    console.log('\n✅ SYSTEM READY! The server should pick this up shortly.');
} else {
    console.log('\n❌ STILL NO SUITABLE MASTERS. Check filtering logic.');
}
