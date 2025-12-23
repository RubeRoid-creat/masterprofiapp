import { query } from '../database/db.js';
import fetch from 'node-fetch';

// Тестируем API endpoint с реальным токеном мастера
console.log('🔍 ТЕСТИРОВАНИЕ API /api/assignments/my\n');
console.log('='.repeat(80));

// Получаем мастера #2 и его токен
const master = query.get('SELECT id, user_id FROM masters WHERE id = 2');
if (!master) {
  console.log('❌ Мастер #2 не найден');
  process.exit(1);
}

console.log(`\n👨‍🔧 Мастер #${master.id} (user_id: ${master.user_id})\n`);

// Получаем последний токен для этого пользователя
const tokenData = query.get('SELECT token FROM user_tokens WHERE user_id = ? ORDER BY created_at DESC LIMIT 1', [master.user_id]);

if (!tokenData) {
  console.log('❌ Токен не найден для мастера. Нужно войти в приложение.\n');
  process.exit(1);
}

const token = tokenData.token;
console.log(`🔑 Токен найден: ${token.substring(0, 30)}...\n`);

// Тестируем API endpoint
const apiUrl = 'http://localhost:3000/api/assignments/my';
console.log(`📡 Запрос к API: ${apiUrl}\n`);

fetch(apiUrl, {
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
})
  .then(async (response) => {
    console.log(`📥 Статус ответа: ${response.status} ${response.statusText}\n`);
    
    const data = await response.json();
    
    if (!response.ok) {
      console.log('❌ Ошибка API:');
      console.log(JSON.stringify(data, null, 2));
      return;
    }
    
    console.log(`✅ Успешный ответ: ${Array.isArray(data) ? data.length : 'не массив'} назначений\n`);
    
    if (Array.isArray(data) && data.length > 0) {
      console.log('📋 Назначения:\n');
      data.forEach((assignment, idx) => {
        console.log(`   ${idx + 1}. Назначение #${assignment.id}:`);
        console.log(`      Статус: ${assignment.status}`);
        console.log(`      Заказ #${assignment.order_id || assignment.orderId}`);
        console.log(`      Устройство: ${assignment.device_type}`);
        console.log(`      expires_at: ${assignment.expires_at || assignment.expiresAt || 'не указано'}`);
        console.log('');
      });
      
      const pending = data.filter(a => a.status === 'pending');
      console.log(`✅ Активных pending назначений: ${pending.length}`);
    } else {
      console.log('⚠️ Список назначений пуст!\n');
      console.log('💡 Возможные причины:');
      console.log('   1. Нет назначений для этого мастера');
      console.log('   2. Все назначения истекли');
      console.log('   3. Проблема с фильтрацией на сервере\n');
    }
  })
  .catch((error) => {
    console.error('❌ Ошибка запроса:', error.message);
  })
  .finally(() => {
    console.log('='.repeat(80));
    process.exit(0);
  });


