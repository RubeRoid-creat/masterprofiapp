import nodemailer from 'nodemailer';
import { config } from '../config.js';

// Создаем транспортер для отправки email
// В продакшене используйте реальные настройки SMTP
const createTransporter = () => {
  // Для разработки используем консольный вывод
  if (process.env.NODE_ENV === 'development' || !config.email?.smtp) {
    return {
      sendMail: async (options) => {
        console.log('📧 [EMAIL SERVICE] Отправка email:');
        console.log('   To:', options.to);
        console.log('   Subject:', options.subject);
        console.log('   Text:', options.text);
        console.log('   HTML:', options.html);
        return { messageId: `dev-${Date.now()}` };
      }
    };
  }
  
  // Для продакшена используем реальный SMTP
  return nodemailer.createTransport({
    host: config.email.smtp.host,
    port: config.email.smtp.port,
    secure: config.email.smtp.secure, // true для 465, false для других портов
    auth: {
      user: config.email.smtp.user,
      pass: config.email.smtp.password
    }
  });
};

const transporter = createTransporter();

/**
 * Отправка кода подтверждения на email
 * @param {string} email - Email получателя
 * @param {string} code - 6-значный код подтверждения
 * @param {string} name - Имя пользователя (опционально)
 */
export async function sendVerificationEmail(email, code, name = 'Пользователь') {
  try {
    const subject = 'Код подтверждения BestApp';
    const text = `Здравствуйте, ${name}!\n\nВаш код подтверждения: ${code}\n\nКод действителен в течение 10 минут.\n\nЕсли вы не запрашивали этот код, проигнорируйте это сообщение.`;
    const html = `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <h2 style="color: #333;">Код подтверждения BestApp</h2>
        <p>Здравствуйте, ${name}!</p>
        <p>Ваш код подтверждения:</p>
        <div style="background-color: #f5f5f5; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; letter-spacing: 5px; margin: 20px 0;">
          ${code}
        </div>
        <p>Код действителен в течение <strong>10 минут</strong>.</p>
        <p style="color: #999; font-size: 12px; margin-top: 30px;">Если вы не запрашивали этот код, проигнорируйте это сообщение.</p>
      </div>
    `;
    
    const info = await transporter.sendMail({
      from: config.email?.from || 'noreply@bestapp.ru',
      to: email,
      subject: subject,
      text: text,
      html: html
    });
    
    console.log(`✅ Email отправлен на ${email}, messageId: ${info.messageId}`);
    return { success: true, messageId: info.messageId };
  } catch (error) {
    console.error('❌ Ошибка отправки email:', error);
    throw error;
  }
}

/**
 * Отправка уведомления об успешном подтверждении
 */
export async function sendConfirmationEmail(email, name = 'Пользователь', type = 'email') {
  try {
    const subject = type === 'email' 
      ? 'Email успешно подтвержден - BestApp'
      : 'Телефон успешно подтвержден - BestApp';
    
    const text = `Здравствуйте, ${name}!\n\nВаш ${type === 'email' ? 'email' : 'телефон'} успешно подтвержден.\n\nСпасибо за использование BestApp!`;
    const html = `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <h2 style="color: #4CAF50;">✅ Подтверждение успешно</h2>
        <p>Здравствуйте, ${name}!</p>
        <p>Ваш ${type === 'email' ? 'email' : 'телефон'} успешно подтвержден.</p>
        <p>Спасибо за использование BestApp!</p>
      </div>
    `;
    
    await transporter.sendMail({
      from: config.email?.from || 'noreply@bestapp.ru',
      to: email,
      subject: subject,
      text: text,
      html: html
    });
    
    console.log(`✅ Email подтверждения отправлен на ${email}`);
    return { success: true };
  } catch (error) {
    console.error('❌ Ошибка отправки email подтверждения:', error);
    // Не бросаем ошибку, так как это не критично
    return { success: false };
  }
}