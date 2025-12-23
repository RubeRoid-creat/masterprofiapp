import { query } from '../database/db.js';
import jwt from 'jsonwebtoken';
import { config } from '../config.js';
import http from 'http';

console.log('🧪 ТЕСТИРОВАНИЕ API НАЗНАЧЕНИЙ С АВТОРИЗАЦИЕЙ\n');
console.log('='.repeat(80));

// Получаем мастера #2 (user_id=4)
const masterUser = query.get(`
  SELECT u.id, u.email, m.id as master_id
  FROM users u
  JOIN masters m ON m.user_id = u.id
  WHERE m.id = 2
`);

if (!masterUser) {
  console.log('❌ Мастер #2 не найден');
  process.exit(1);
}

console.log(`\n👨‍🔧 Мастер: ${masterUser.email} (user_id: ${masterUser.id}, master_id: ${masterUser.master_id})\n`);

// Создаем токен
const token = jwt.sign(
  { id: masterUser.id, role: 'master' },
  config.jwtSecret,
  { expiresIn: '24h' }
);

console.log('📡 Тестируем API запрос...\n');

// Тестируем API запрос
const options = {
  hostname: 'localhost',
  port: 3000,
  path: '/api/assignments/my',
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
};

const req = http.request(options, (res) => {
  let data = '';
  
  res.on('data', (chunk) => {
    data += chunk;
  });
  
  res.on('end', () => {
    console.log(`📥 Ответ API:`);
    console.log(`   Код: ${res.statusCode}`);
    console.log(`   Статус: ${res.statusMessage}\n`);
    
    if (res.statusCode === 200) {
      try {
        const assignments = JSON.parse(data);
        console.log(`✅ Получено назначений: ${assignments.length}\n`);
        
        if (assignments.length > 0) {
          console.log('📋 НАЗНАЧЕНИЯ:\n');
          assignments.forEach((a, idx) => {
            console.log(`   ${idx + 1}. Назначение #${a.id}:`);
            console.log(`      Заказ #${a.order_id}`);
            console.log(`      Статус: ${a.status}`);
            if (a.status === 'pending' && a.expires_at) {
              const expiresAt = new Date(a.expires_at);
              const now = new Date();
              const minutesLeft = Math.floor((expiresAt - now) / 1000 / 60);
              console.log(`      Осталось времени: ${minutesLeft} минут`);
              console.log(`      Истекает: ${a.expires_at}`);
            }
            console.log('');
          });
        } else {
          console.log('⚠️ Назначений нет\n');
        }
      } catch (e) {
        console.error('❌ Ошибка парсинга JSON:', e.message);
        console.log('Ответ:', data);
      }
    } else {
      console.error(`❌ Ошибка API: ${res.statusCode}`);
      console.log('Ответ:', data);
    }
    
    console.log('='.repeat(80));
    process.exit(0);
  });
});

req.on('error', (error) => {
  console.error(`❌ Ошибка запроса: ${error.message}\n`);
  console.log('💡 Убедитесь, что сервер запущен на порту 3000\n');
  process.exit(1);
});

req.end();


