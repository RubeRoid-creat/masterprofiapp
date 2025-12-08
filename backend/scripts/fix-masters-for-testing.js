import { query } from '../database/db.js';

console.log('🔧 ИСПРАВЛЕНИЕ НАСТРОЕК МАСТЕРОВ ДЛЯ ТЕСТИРОВАНИЯ\n');
console.log('='.repeat(80));

// Получаем всех мастеров
const masters = query.all(`
  SELECT 
    m.id,
    m.user_id,
    m.specialization,
    u.name,
    u.email
  FROM masters m
  JOIN users u ON m.user_id = u.id
`);

console.log(`\nНайдено мастеров: ${masters.length}\n`);

masters.forEach(master => {
  console.log(`\n👨‍🔧 Обработка мастера #${master.id}: ${master.name} (${master.email})`);
  
  // 1. Проверяем и устанавливаем специализацию
  let specs = [];
  try {
    specs = JSON.parse(master.specialization || '[]');
  } catch (e) {
    specs = [];
  }
  
  if (specs.length === 0) {
    console.log('  ⚠️ Нет специализации, устанавливаем по умолчанию...');
    const defaultSpecs = ['Холодильник', 'Стиральная машина', 'Посудомоечная машина'];
    const specsJson = JSON.stringify(defaultSpecs);
    query.run(
      'UPDATE masters SET specialization = ? WHERE id = ?',
      [specsJson, master.id]
    );
    console.log(`  ✅ Установлена специализация: ${defaultSpecs.join(', ')}`);
  } else {
    console.log(`  ✅ Специализация уже есть: ${specs.join(', ')}`);
  }
  
  // 2. Включаем смену
  query.run(
    'UPDATE masters SET is_on_shift = 1 WHERE id = ?',
    [master.id]
  );
  console.log('  ✅ Включена смена (is_on_shift = 1)');
  
  // 3. Устанавливаем статус available
  query.run(
    'UPDATE masters SET status = ? WHERE id = ?',
    ['available', master.id]
  );
  console.log('  ✅ Установлен статус "available"');
});

console.log('\n' + '='.repeat(80));
console.log('\n✅ Все мастера обновлены!\n');
console.log('Теперь мастера:');
console.log('  - На смене (is_on_shift = 1)');
console.log('  - Статус "available"');
console.log('  - Имеют специализацию');
console.log('\n💡 Для проверки создайте новый заказ или используйте скрипт создания тестового заказа.\n');

process.exit(0);
