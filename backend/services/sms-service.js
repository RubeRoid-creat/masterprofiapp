import { config } from '../config.js';
import https from 'https';

/**
 * Форматирование номера телефона для SMS.ru
 * SMS.ru принимает номера в формате: 79XXXXXXXXX (без +)
 */
function formatPhoneForSMSRu(phone) {
  // Убираем все не-цифры
  let cleaned = phone.replace(/\D/g, '');
  
  // Если начинается с 8, заменяем на 7
  if (cleaned.startsWith('8')) {
    cleaned = '7' + cleaned.slice(1);
  }
  
  // Если не начинается с 7, добавляем
  if (!cleaned.startsWith('7')) {
    cleaned = '7' + cleaned;
  }
  
  return cleaned;
}

/**
 * Валидация номера телефона
 */
function validatePhone(phone) {
  const cleaned = phone.replace(/\D/g, '');
  // Российский номер: 11 цифр, начинается с 7 или 8
  return cleaned.length === 11 && (cleaned.startsWith('7') || cleaned.startsWith('8'));
}

/**
 * Отправка SMS через SMS.ru API
 */
async function sendViaSMSRu(phone, message) {
  return new Promise((resolve, reject) => {
    const formattedPhone = formatPhoneForSMSRu(phone);
    const apiId = config.sms.smsru.api_id;
    
    if (!apiId) {
      return reject(new Error('SMS.ru API ID не настроен'));
    }
    
    // Параметры запроса
    const params = new URLSearchParams({
      api_id: apiId,
      to: formattedPhone,
      msg: message,
      json: '1' // Получить ответ в JSON
    });
    
    const options = {
      hostname: 'sms.ru',
      port: 443,
      path: '/sms/send?' + params.toString(),
      method: 'GET',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    };
    
    console.log(`📱 [SMS.RU] Отправка SMS на ${formattedPhone}...`);
    
    const req = https.request(options, (res) => {
      let data = '';
      
      res.on('data', (chunk) => {
        data += chunk;
      });
      
      res.on('end', () => {
        try {
          const response = JSON.parse(data);
          
          console.log(`📱 [SMS.RU] Ответ:`, response);
          
          // Проверка статуса
          if (response.status === 'OK' && response.sms) {
            const smsId = Object.keys(response.sms)[0];
            const smsData = response.sms[smsId];
            
            if (smsData.status === 'OK') {
              console.log(`✅ SMS успешно отправлена на ${formattedPhone}, ID: ${smsData.sms_id}`);
              resolve({
                success: true,
                messageId: smsData.sms_id,
                cost: smsData.cost,
                balance: response.balance
              });
            } else {
              reject(new Error(`SMS.ru ошибка: ${smsData.status_text || smsData.status}`));
            }
          } else {
            reject(new Error(`SMS.ru ошибка: ${response.status_text || response.status}`));
          }
        } catch (error) {
          reject(new Error(`Ошибка парсинга ответа SMS.ru: ${error.message}`));
        }
      });
    });
    
    req.on('error', (error) => {
      console.error('❌ [SMS.RU] Ошибка подключения:', error.message);
      reject(error);
    });
    
    req.end();
  });
}

/**
 * Отправка SMS через Twilio API (fallback)
 */
async function sendViaTwilio(phone, message) {
  // Динамический импорт twilio (если установлен)
  try {
    const twilio = await import('twilio');
    const client = twilio.default(
      config.sms.twilio.accountSid,
      config.sms.twilio.authToken
    );
    
    console.log(`📱 [TWILIO] Отправка SMS на ${phone}...`);
    
    const result = await client.messages.create({
      body: message,
      from: config.sms.twilio.phoneNumber,
      to: phone
    });
    
    console.log(`✅ SMS успешно отправлена через Twilio, ID: ${result.sid}`);
    
    return {
      success: true,
      messageId: result.sid,
      status: result.status
    };
  } catch (error) {
    console.error('❌ [TWILIO] Ошибка:', error.message);
    throw error;
  }
}

/**
 * Отправка SMS кода подтверждения
 * @param {string} phone - Номер телефона в формате +7XXXXXXXXXX
 * @param {string} code - 6-значный код подтверждения
 * @returns {Promise<{success: boolean, messageId?: string}>}
 */
export async function sendVerificationSMS(phone, code) {
  try {
    // Валидация номера
    if (!validatePhone(phone)) {
      throw new Error('Неверный формат номера телефона');
    }
    
    const message = `Ваш код подтверждения МастерПрофи: ${code}. Код действителен 10 минут.`;
    
    // Режим разработки
    if (process.env.NODE_ENV === 'development' || !config.sms?.provider) {
      console.log('📱 [SMS SERVICE] Отправка SMS (DEV MODE):');
      console.log('   Phone:', phone);
      console.log('   Code:', code);
      console.log('   Message:', message);
      return { success: true, messageId: `dev-sms-${Date.now()}` };
    }
    
    // Выбор провайдера
    const provider = config.sms.provider;
    
    try {
      // Попытка отправки через основной провайдер
      if (provider === 'smsru') {
        return await sendViaSMSRu(phone, message);
      } else if (provider === 'twilio') {
        return await sendViaTwilio(phone, message);
      } else {
        throw new Error('SMS провайдер не настроен');
      }
    } catch (primaryError) {
      console.warn(`⚠️ Ошибка отправки через ${provider}:`, primaryError.message);
      
      // Fallback на альтернативный провайдер
      if (provider === 'smsru' && config.sms.twilio.accountSid) {
        console.log('🔄 Попытка отправки через Twilio (fallback)...');
        try {
          return await sendViaTwilio(phone, message);
        } catch (fallbackError) {
          console.error('❌ Fallback также не удался:', fallbackError.message);
        }
      }
      
      // Если все провайдеры не сработали, бросаем ошибку
      throw primaryError;
    }
    
  } catch (error) {
    console.error('❌ Ошибка отправки SMS:', error);
    throw error;
  }
}

/**
 * Генерация 6-значного кода
 */
export function generateVerificationCode() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

/**
 * Проверка баланса SMS.ru
 */
export async function checkSMSRuBalance() {
  return new Promise((resolve, reject) => {
    const apiId = config.sms.smsru.api_id;
    
    if (!apiId) {
      return reject(new Error('SMS.ru API ID не настроен'));
    }
    
    const options = {
      hostname: 'sms.ru',
      port: 443,
      path: `/my/balance?api_id=${apiId}&json=1`,
      method: 'GET'
    };
    
    const req = https.request(options, (res) => {
      let data = '';
      
      res.on('data', (chunk) => {
        data += chunk;
      });
      
      res.on('end', () => {
        try {
          const response = JSON.parse(data);
          
          if (response.status === 'OK') {
            resolve({
              success: true,
              balance: response.balance,
              currency: 'RUB'
            });
          } else {
            reject(new Error(`SMS.ru ошибка: ${response.status_text}`));
          }
        } catch (error) {
          reject(new Error(`Ошибка парсинга ответа: ${error.message}`));
        }
      });
    });
    
    req.on('error', (error) => {
      reject(error);
    });
    
    req.end();
  });
}

/**
 * Проверка статуса SMS по ID (SMS.ru)
 */
export async function checkSMSStatus(smsId) {
  return new Promise((resolve, reject) => {
    const apiId = config.sms.smsru.api_id;
    
    if (!apiId) {
      return reject(new Error('SMS.ru API ID не настроен'));
    }
    
    const options = {
      hostname: 'sms.ru',
      port: 443,
      path: `/sms/status?api_id=${apiId}&sms_id=${smsId}&json=1`,
      method: 'GET'
    };
    
    const req = https.request(options, (res) => {
      let data = '';
      
      res.on('data', (chunk) => {
        data += chunk;
      });
      
      res.on('end', () => {
        try {
          const response = JSON.parse(data);
          
          if (response.status === 'OK' && response.sms) {
            const status = response.sms[smsId];
            resolve({
              success: true,
              status: status.status,
              statusText: status.status_text
            });
          } else {
            reject(new Error(`SMS.ru ошибка: ${response.status_text}`));
          }
        } catch (error) {
          reject(new Error(`Ошибка парсинга ответа: ${error.message}`));
        }
      });
    });
    
    req.on('error', (error) => {
      reject(error);
    });
    
    req.end();
  });
}

/**
 * Проверка работоспособности SMS сервиса
 */
export async function verifySMSService() {
  try {
    if (!config.sms?.provider) {
      return {
        success: true,
        message: 'SMS сервис в режиме разработки',
        provider: 'dev'
      };
    }
    
    if (config.sms.provider === 'smsru') {
      const balanceInfo = await checkSMSRuBalance();
      return {
        success: true,
        message: 'SMS.ru сервис работает',
        provider: 'smsru',
        balance: balanceInfo.balance
      };
    } else if (config.sms.provider === 'twilio') {
      // Для Twilio можно добавить проверку аккаунта
      return {
        success: true,
        message: 'Twilio сервис настроен',
        provider: 'twilio'
      };
    }
    
    return {
      success: false,
      message: 'Неизвестный SMS провайдер'
    };
  } catch (error) {
    console.error('❌ Ошибка проверки SMS сервиса:', error);
    return {
      success: false,
      message: error.message,
      provider: config.sms?.provider
    };
  }
}


