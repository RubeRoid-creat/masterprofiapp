#!/usr/bin/env node

/**
 * Скрипт для тестирования SMS сервиса
 * 
 * Использование:
 * node scripts/test-sms.js +79991234567
 */

import { sendVerificationSMS, verifySMSService, checkSMSRuBalance } from '../services/sms-service.js';
import { config } from '../config.js';

const phone = process.argv[2];

async function testSMS() {
  console.log('========================================');
  console.log('  ТЕСТИРОВАНИЕ SMS СЕРВИСА');
  console.log('========================================\n');
  
  // 1. Проверка конфигурации
  console.log('1️⃣ Проверка конфигурации...\n');
  console.log('NODE_ENV:', process.env.NODE_ENV || 'development');
  console.log('SMS Provider:', config.sms?.provider || 'не настроен');
  
  if (config.sms?.provider === 'smsru') {
    console.log('SMS.ru API ID:', config.sms.smsru.api_id ? '✅ Настроен' : '❌ Отсутствует');
  } else if (config.sms?.provider === 'twilio') {
    console.log('Twilio Account SID:', config.sms.twilio.accountSid ? '✅ Настроен' : '❌ Отсутствует');
  }
  console.log('');
  
  // 2. Проверка работоспособности сервиса
  console.log('2️⃣ Проверка работоспособности сервиса...\n');
  try {
    const status = await verifySMSService();
    console.log('Статус:', status.success ? '✅ Работает' : '❌ Не работает');
    console.log('Сообщение:', status.message);
    console.log('Провайдер:', status.provider);
    if (status.balance !== undefined) {
      console.log('Баланс:', status.balance, 'руб');
    }
  } catch (error) {
    console.error('❌ Ошибка:', error.message);
  }
  console.log('');
  
  // 3. Проверка баланса (только для SMS.ru)
  if (config.sms?.provider === 'smsru' && config.sms.smsru.api_id) {
    console.log('3️⃣ Проверка баланса SMS.ru...\n');
    try {
      const balance = await checkSMSRuBalance();
      console.log('Баланс:', balance.balance, balance.currency);
      
      if (balance.balance < 10) {
        console.warn('⚠️ ВНИМАНИЕ: Низкий баланс! Рекомендуется пополнить счет.');
      }
    } catch (error) {
      console.error('❌ Ошибка проверки баланса:', error.message);
    }
    console.log('');
  }
  
  // 4. Тестовая отправка SMS (если указан номер)
  if (phone) {
    console.log(`4️⃣ Отправка тестовой SMS на ${phone}...\n`);
    
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    console.log('Код подтверждения:', code);
    console.log('');
    
    try {
      const result = await sendVerificationSMS(phone, code);
      
      console.log('✅ SMS успешно отправлена!');
      console.log('Message ID:', result.messageId);
      if (result.cost) {
        console.log('Стоимость:', result.cost, 'руб');
      }
      if (result.balance) {
        console.log('Остаток баланса:', result.balance, 'руб');
      }
    } catch (error) {
      console.error('❌ Ошибка отправки SMS:', error.message);
      
      if (error.message.includes('API ID')) {
        console.log('\n💡 Совет: Настройте SMS_PROVIDER и SMSRU_API_ID в .env файле');
      } else if (error.message.includes('Insufficient funds')) {
        console.log('\n💡 Совет: Пополните баланс на https://sms.ru/panel');
      } else if (error.message.includes('Invalid phone')) {
        console.log('\n💡 Совет: Используйте формат +79991234567');
      }
    }
  } else {
    console.log('4️⃣ Отправка SMS пропущена (не указан номер телефона)\n');
    console.log('💡 Для тестовой отправки используйте:');
    console.log('   node scripts/test-sms.js +79991234567');
  }
  
  console.log('');
  console.log('========================================');
  console.log('  ТЕСТИРОВАНИЕ ЗАВЕРШЕНО');
  console.log('========================================');
}

// Запуск теста
testSMS().catch(error => {
  console.error('\n❌ Критическая ошибка:', error);
  process.exit(1);
});
