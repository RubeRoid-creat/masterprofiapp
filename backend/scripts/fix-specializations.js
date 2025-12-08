import { query } from '../database/db.js';
import readline from 'readline';

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

console.log('🔧 ИСПРАВЛЕНИЕ СПЕЦИАЛИЗАЦИЙ МАСТЕРОВ\n');
console.log('=' .repeat(60));

// Получаем всех мастеров
const masters = query.all(`
  SELECT 
    m.id, m.user_id, m.specialization,
    u.name, u.email
  FROM masters m
  JOIN users u ON m.user_id = u.id
`);

console.log(`\nНайдено мастеров: ${masters.length}\n`);

// Проверяем каждого мастера
const problems = [];

masters.forEach(m => {
  let specs = [];
  let hasIssue = false;
  let issueType = '';
  
  try {
    // Пытаемся распарсить специализации
    if (!m.specialization || m.specialization === 'null' || m.specialization === '') {
      hasIssue = true;
      issueType = 'ПУСТО';
    } else {
      specs = JSON.parse(m.specialization);
      if (!Array.isArray(specs)) {
        hasIssue = true;
        issueType = 'НЕ МАССИВ';
      } else if (specs.length === 0) {
        hasIssue = true;
        issueType = 'ПУСТОЙ МАССИВ';
      }
    }
  } catch (e) {
    hasIssue = true;
    issueType = 'ОШИБКА ПАРСИНГА';
  }
  
  console.log(`Мастер #${m.id}: ${m.name} (${m.email})`);
  
  if (hasIssue) {
    console.log(`   ❌ Проблема: ${issueType}`);
    console.log(`   Текущее значение: ${m.specialization}`);
    problems.push(m);
  } else {
    console.log(`   ✅ Специализации: ${specs.join(', ')}`);
  }
  console.log('');
});

if (problems.length === 0) {
  console.log('\n✅ Все мастера имеют корректные специализации!\n');
  process.exit(0);
}

console.log('=' .repeat(60));
console.log(`\n❌ Найдено проблем: ${problems.length}\n`);

// Доступные специализации
const availableSpecs = [
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
  'Водонагреватель'
];

console.log('Доступные специализации:');
availableSpecs.forEach((spec, index) => {
  console.log(`  ${index + 1}. ${spec}`);
});

console.log('\n' + '='.repeat(60));
console.log('\nВарианты исправления:');
console.log('1. Установить все специализации для всех проблемных мастеров');
console.log('2. Установить выборочно (спросить для каждого мастера)');
console.log('3. Выход без изменений\n');

rl.question('Выберите вариант (1-3): ', (answer) => {
  const choice = parseInt(answer);
  
  if (choice === 3 || isNaN(choice)) {
    console.log('\n❌ Отменено. Специализации не изменены.\n');
    rl.close();
    process.exit(0);
  }
  
  if (choice === 1) {
    // Устанавливаем все специализации
    console.log('\n✅ Устанавливаем все специализации для всех проблемных мастеров...\n');
    
    problems.forEach(m => {
      const specsJson = JSON.stringify(availableSpecs);
      query.run(
        'UPDATE masters SET specialization = ? WHERE id = ?',
        [specsJson, m.id]
      );
      console.log(`✅ Мастер #${m.id} (${m.name}): установлены все специализации`);
    });
    
    console.log('\n✅ Готово! Все проблемные мастера обновлены.\n');
    rl.close();
    process.exit(0);
  }
  
  if (choice === 2) {
    console.log('\n🔧 Выборочная настройка (пока не реализовано, используйте вариант 1)\n');
    rl.close();
    process.exit(0);
  }
  
  console.log('\n❌ Неверный выбор.\n');
  rl.close();
  process.exit(1);
});
