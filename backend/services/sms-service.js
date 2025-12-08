import { config } from '../config.js';

/**
 * Отправка SMS кода подтверждения
 * @param {string} phone - Номер телефона в формате +7XXXXXXXXXX
 * @param {string} code - 6-значный код подтверждения
 * @returns {Promise<{success: boolean, messageId?: string}>}
 */
export async function sendVerificationSMS(phone, code) {
  try {
    // В продакшене здесь будет интеграция с SMS-провайдером (SMS.ru, Twilio, и т.д.)
    // Для разработки просто логируем
    
    if (process.env.NODE_ENV === 'development' || !config.sms?.provider) {
      console.log('📱 [SMS SERVICE] Отправка SMS:');
      console.log('   Phone:', phone);
      console.log('   Code:', code);
      console.log('   Message: Ваш код подтверждения BestApp: ' + code);
      return { success: true, messageId: `dev-sms-${Date.now()}` };
    }
    
    // Пример интеграции с SMS.ru (раскомментируйте и настройте при необходимости)
    /*
    const smsru = require('sms_ru');
    const sms = new smsru(config.sms.smsru.api_id);
    
    const result = await sms.sms.send({
      to: phone,
      msg: `Ваш код подтверждения BestApp: ${code}. Код действителен 10 минут.`
    });
    
    if (result.status === 'OK') {
      console.log(`✅ SMS отправлена на ${phone}, messageId: ${result.sms[phone].sms_id}`);
      return { success: true, messageId: result.sms[phone].sms_id };
    } else {
      throw new Error(`SMS отправка не удалась: ${result.status_text}`);
    }
    */
    
    // Пример интеграции с Twilio (раскомментируйте и настройте при необходимости)
    /*
    const twilio = require('twilio');
    const client = twilio(config.sms.twilio.accountSid, config.sms.twilio.authToken);
    
    const message = await client.messages.create({
      body: `Ваш код подтверждения BestApp: ${code}. Код действителен 10 минут.`,
      from: config.sms.twilio.phoneNumber,
      to: phone
    });
    
    console.log(`✅ SMS отправлена на ${phone}, messageId: ${message.sid}`);
    return { success: true, messageId: message.sid };
    */
    
    // Заглушка для продакшена без настроенного провайдера
    console.log('⚠️ SMS провайдер не настроен, используем режим разработки');
    console.log('📱 [SMS SERVICE] Отправка SMS:');
    console.log('   Phone:', phone);
    console.log('   Code:', code);
    return { success: true, messageId: `dev-sms-${Date.now()}` };
    
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
