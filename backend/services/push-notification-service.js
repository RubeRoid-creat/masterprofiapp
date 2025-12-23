import { query } from '../database/db.js';

/**
 * Сервис для отправки push-уведомлений через FCM
 * 
 * Для полной интеграции необходимо:
 * 1. Установить firebase-admin: npm install firebase-admin
 * 2. Добавить сервисный ключ Firebase в config.js
 * 3. Инициализировать Firebase Admin SDK
 */

// Инициализация Firebase Admin SDK
import admin from 'firebase-admin';
import { config } from '../config.js';
import { readFileSync, existsSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// Флаг для отслеживания инициализации Firebase
let isFirebaseInitialized = false;

// Инициализация Firebase Admin SDK
if (!admin.apps.length) {
  try {
    // Загружаем сервисный ключ
    const serviceAccountPath = config.firebaseServiceAccount.startsWith('./') 
      ? join(__dirname, '..', config.firebaseServiceAccount.replace('./', ''))
      : config.firebaseServiceAccount;
    
    // Проверяем существование файла
    if (!existsSync(serviceAccountPath)) {
      console.log('⚠️ Firebase service account file not found:', serviceAccountPath);
      console.log('⚠️ Push-уведомления будут работать в режиме заглушки');
      console.log('💡 Для включения push-уведомлений загрузите firebase-service-account.json на сервер');
      isFirebaseInitialized = false;
    } else {
      const serviceAccount = JSON.parse(readFileSync(serviceAccountPath, 'utf8'));
      
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
      });
      
      isFirebaseInitialized = true;
      console.log('✅ Firebase Admin SDK инициализирован');
    }
  } catch (error) {
    console.error('❌ Ошибка инициализации Firebase Admin SDK:', error.message);
    console.log('⚠️ Push-уведомления будут работать в режиме заглушки');
    console.log('💡 Для включения push-уведомлений проверьте наличие firebase-service-account.json');
    isFirebaseInitialized = false;
  }
} else {
  isFirebaseInitialized = true;
}

/**
 * Получить FCM токены пользователя
 */
export function getUserFcmTokens(userId) {
  try {
    const tokens = query.all(
      'SELECT token FROM fcm_tokens WHERE user_id = ?',
      [userId]
    );
    console.log(`🔍 Найдено FCM токенов для пользователя #${userId}: ${tokens.length}`);
    if (tokens.length > 0) {
      tokens.forEach((t, idx) => {
        console.log(`   Токен ${idx + 1}: ${t.token.substring(0, 30)}...`);
      });
    } else {
      console.log(`⚠️ Нет FCM токенов для пользователя #${userId}`);
    }
    return tokens.map(t => t.token);
  } catch (error) {
    console.error('Ошибка получения FCM токенов:', error);
    return [];
  }
}

/**
 * Отправить push-уведомление пользователю
 * 
 * @param {number} userId - ID пользователя
 * @param {object} notification - Объект уведомления { title, body, data }
 */
export async function sendPushNotification(userId, notification) {
  try {
    const tokens = getUserFcmTokens(userId);
    
    if (tokens.length === 0) {
      console.log(`⚠️ Нет FCM токенов для пользователя #${userId}`);
      return { success: false, reason: 'no_tokens' };
    }
    
    // Отправка через Firebase Admin SDK
    if (!isFirebaseInitialized) {
      console.log(`⚠️ Firebase не инициализирован, push-уведомление не отправлено для пользователя #${userId}`);
      return { success: false, reason: 'firebase_not_initialized' };
    }
    
    try {
      const message = {
        notification: {
          title: notification.title,
          body: notification.body
        },
        data: notification.data || {},
        tokens: tokens
      };
      
      const response = await admin.messaging().sendEachForMulticast(message);
      
      console.log(`📱 Push-уведомление отправлено пользователю #${userId}: ${response.successCount} успешно, ${response.failureCount} ошибок`);
      
      // Удаляем невалидные токены
      if (response.failureCount > 0) {
        const failedTokens = [];
        response.responses.forEach((resp, idx) => {
          if (!resp.success) {
            failedTokens.push(tokens[idx]);
            console.log(`❌ Ошибка отправки токену ${tokens[idx]}: ${resp.error?.message}`);
          }
        });
        
        // Удаляем невалидные токены из БД
        if (failedTokens.length > 0) {
          failedTokens.forEach(token => {
            query.run('DELETE FROM fcm_tokens WHERE token = ?', [token]);
          });
        }
      }
      
      return {
        success: response.successCount > 0,
        successCount: response.successCount,
        failureCount: response.failureCount
      };
    } catch (error) {
      console.error('❌ Ошибка отправки push-уведомления через Firebase:', error);
      return { success: false, reason: 'error', error: error.message };
    }
  } catch (error) {
    console.error('Ошибка отправки push-уведомления:', error);
    return { success: false, reason: 'error', error: error.message };
  }
}

/**
 * Отправить уведомление о изменении статуса заказа
 */
export async function notifyOrderStatusChange(userId, orderId, status, message, masterName = null) {
  const statusMessages = {
    'new': 'Создан новый заказ',
    'accepted': 'Заказ принят мастером',
    'in_progress': 'Мастер начал работу',
    'completed': 'Заказ завершен',
    'cancelled': 'Заказ отменен'
  };
  
  const title = statusMessages[status] || 'Статус заказа изменен';
  const body = masterName ? `${message} (${masterName})` : message;
  
  return await sendPushNotification(userId, {
    title,
    body,
    data: {
      type: 'order_status_changed',
      orderId: orderId.toString(),
      status,
      masterName: masterName || ''
    }
  });
}

/**
 * Отправить уведомление о назначении мастера
 */
export async function notifyMasterAssigned(userId, orderId, masterName) {
  return await sendPushNotification(userId, {
    title: 'Мастер назначен',
    body: `Мастер ${masterName} принял ваш заказ`,
    data: {
      type: 'master_assigned',
      orderId: orderId.toString(),
      masterName
    }
  });
}

export default {
  getUserFcmTokens,
  sendPushNotification,
  notifyOrderStatusChange,
  notifyMasterAssigned
};

