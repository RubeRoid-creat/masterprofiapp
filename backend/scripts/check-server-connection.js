/**
 * Скрипт для проверки доступности сервера
 * Запустите: node scripts/check-server-connection.js
 */

import http from 'http';
import net from 'net';

const SERVER_HOST = process.env.SERVER_HOST || '212.74.227.208';
const SERVER_PORT = parseInt(process.env.SERVER_PORT || '3000');

console.log('🔍 Проверка доступности сервера...');
console.log(`   Хост: ${SERVER_HOST}`);
console.log(`   Порт: ${SERVER_PORT}`);
console.log('');

// Проверка TCP подключения
function checkTCPConnection() {
  return new Promise((resolve, reject) => {
    console.log('1️⃣ Проверка TCP подключения...');
    const socket = new net.Socket();
    
    const timeout = setTimeout(() => {
      socket.destroy();
      reject(new Error('TCP подключение: TIMEOUT (порт закрыт или файрвол блокирует)'));
    }, 5000);
    
    socket.connect(SERVER_PORT, SERVER_HOST, () => {
      clearTimeout(timeout);
      socket.destroy();
      resolve(true);
    });
    
    socket.on('error', (err) => {
      clearTimeout(timeout);
      reject(new Error(`TCP подключение: FAILED - ${err.message}`));
    });
  });
}

// Проверка HTTP ответа
function checkHTTPResponse() {
  return new Promise((resolve, reject) => {
    console.log('2️⃣ Проверка HTTP ответа...');
    
    const options = {
      hostname: SERVER_HOST,
      port: SERVER_PORT,
      path: '/',
      method: 'GET',
      timeout: 5000
    };
    
    const req = http.request(options, (res) => {
      console.log(`   ✅ HTTP статус: ${res.statusCode}`);
      resolve(true);
    });
    
    req.on('error', (err) => {
      reject(new Error(`HTTP запрос: FAILED - ${err.message}`));
    });
    
    req.on('timeout', () => {
      req.destroy();
      reject(new Error('HTTP запрос: TIMEOUT'));
    });
    
    req.end();
  });
}

// Проверка API эндпоинта
function checkAPIEndpoint() {
  return new Promise((resolve, reject) => {
    console.log('3️⃣ Проверка API эндпоинта...');
    
    const options = {
      hostname: SERVER_HOST,
      port: SERVER_PORT,
      path: '/api/version/check',
      method: 'GET',
      timeout: 5000
    };
    
    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => {
        data += chunk;
      });
      res.on('end', () => {
        console.log(`   ✅ API статус: ${res.statusCode}`);
        if (data) {
          try {
            const json = JSON.parse(data);
            console.log(`   📄 Ответ: ${JSON.stringify(json, null, 2)}`);
          } catch (e) {
            console.log(`   📄 Ответ: ${data.substring(0, 100)}`);
          }
        }
        resolve(true);
      });
    });
    
    req.on('error', (err) => {
      reject(new Error(`API запрос: FAILED - ${err.message}`));
    });
    
    req.on('timeout', () => {
      req.destroy();
      reject(new Error('API запрос: TIMEOUT'));
    });
    
    req.end();
  });
}

// Основная функция проверки
async function main() {
  try {
    await checkTCPConnection();
    console.log('   ✅ TCP порт доступен\n');
    
    await checkHTTPResponse();
    console.log('   ✅ HTTP сервер отвечает\n');
    
    await checkAPIEndpoint();
    console.log('   ✅ API работает\n');
    
    console.log('🎉 Сервер полностью доступен и работает!');
    process.exit(0);
  } catch (error) {
    console.error('\n❌ Ошибка проверки:');
    console.error(`   ${error.message}\n`);
    
    console.log('💡 Возможные причины:');
    console.log('   1. Сервер не запущен - проверьте: pm2 status или ps aux | grep node');
    console.log('   2. Файрвол блокирует порт - проверьте: sudo ufw status');
    console.log('   3. Сервер слушает только на localhost - проверьте server.js');
    console.log('   4. Неправильный IP или порт - проверьте переменные окружения');
    console.log('   5. Проблемы с сетью - проверьте доступность сервера: ping 212.74.227.208');
    
    process.exit(1);
  }
}

main();
