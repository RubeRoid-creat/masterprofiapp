import { query } from '../database/db.js';

console.log('🔧 ОБНОВЛЕНИЕ СПЕЦИАЛИЗАЦИЙ МАСТЕРОВ\n');
console.log('='.repeat(80));

// Канонический список специализаций (соответствует приложению)
const canonicalSpecs = [
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
];

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

let updatedCount = 0;

masters.forEach(master => {
  console.log(`\n👨‍🔧 Мастер #${master.id}: ${master.name} (${master.email})`);
  
  let currentSpecs = [];
  try {
    const parsed = JSON.parse(master.specialization || '[]');
    if (Array.isArray(parsed)) {
      currentSpecs = parsed;
    }
  } catch (e) {
    console.log(`  ⚠️ Ошибка парсинга: ${e.message}`);
  }
  
  console.log(`  Текущие: ${currentSpecs.length > 0 ? currentSpecs.join(', ') : 'ПУСТО'}`);
  
  // Проверяем, нужно ли обновление
  let needsUpdate = false;
  let newSpecs = [];
  
  if (currentSpecs.length === 0) {
    // Если пусто - устанавливаем все популярные
    newSpecs = ['Холодильник', 'Стиральная машина', 'Посудомоечная машина', 'Духовой шкаф'];
    needsUpdate = true;
  } else {
    // Проверяем наличие "Духовой шкаф" и других популярных
    newSpecs = [...currentSpecs];
    
    // Добавляем недостающие популярные, если их нет
    const popularSpecs = ['Холодильник', 'Стиральная машина', 'Посудомоечная машина', 'Духовой шкаф'];
    popularSpecs.forEach(spec => {
      if (!newSpecs.includes(spec)) {
        newSpecs.push(spec);
        needsUpdate = true;
      }
    });
    
    // Нормализуем старые названия
    const normalizeMap = {
      'Микроволновка': 'Микроволновая печь',
      'Микроволновая': 'Микроволновая печь',
      'Морозильник': 'Морозильный ларь'
    };
    
    newSpecs = newSpecs.map(spec => {
      if (normalizeMap[spec]) {
        needsUpdate = true;
        return normalizeMap[spec];
      }
      return spec;
    }).filter((spec, index, self) => self.indexOf(spec) === index); // Удаляем дубликаты
  }
  
  if (needsUpdate) {
    const specsJson = JSON.stringify(newSpecs);
    query.run(
      'UPDATE masters SET specialization = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      [specsJson, master.id]
    );
    console.log(`  ✅ Обновлено: ${newSpecs.join(', ')}`);
    updatedCount++;
  } else {
    console.log(`  ✅ Специализация актуальна`);
  }
});

console.log('\n' + '='.repeat(80));
console.log(`\n✅ Обработано мастеров: ${masters.length}`);
console.log(`✅ Обновлено: ${updatedCount}\n`);

process.exit(0);


