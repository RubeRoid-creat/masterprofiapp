import axios from 'axios';

/**
 * Сервис для работы с ЮMoney API (выплаты мастерам)
 * Документация: https://yoomoney.ru/docs/payment-buttons
 * 
 * ВАЖНО: Для работы нужен OAuth токен ЮMoney
 * Получить можно через: https://yoomoney.ru/oauth/authorize
 */
class YooMoneyService {
  constructor() {
    const accessToken = process.env.YOOMONEY_ACCESS_TOKEN;
    const clientId = process.env.YOOMONEY_CLIENT_ID;
    const clientSecret = process.env.YOOMONEY_CLIENT_SECRET;
    
    if (!accessToken && (!clientId || !clientSecret)) {
      console.warn('⚠️ ЮMoney не настроен: отсутствует YOOMONEY_ACCESS_TOKEN или YOOMONEY_CLIENT_ID/CLIENT_SECRET');
      this.enabled = false;
      return;
    }
    
    this.accessToken = accessToken;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.baseUrl = 'https://yoomoney.ru/api';
    this.enabled = !!accessToken;
    
    if (this.enabled) {
      console.log('✅ ЮMoney сервис инициализирован');
    }
  }
  
  /**
   * Проверка баланса кошелька ЮMoney
   * @returns {Promise<number>} Баланс в рублях
   */
  async getBalance() {
    if (!this.enabled) {
      throw new Error('ЮMoney не настроен. Проверьте переменную окружения YOOMONEY_ACCESS_TOKEN');
    }
    
    try {
      const response = await axios.post(
        `${this.baseUrl}/account-info`,
        null,
        {
          headers: {
            'Authorization': `Bearer ${this.accessToken}`,
            'Content-Type': 'application/x-www-form-urlencoded'
          }
        }
      );
      
      return parseFloat(response.data.balance || 0);
    } catch (error) {
      console.error('❌ Ошибка получения баланса ЮMoney:', error.response?.data || error.message);
      throw error;
    }
  }
  
  /**
   * Выплата на кошелек ЮMoney
   * @param {Object} payoutData - Данные выплаты
   * @param {string} payoutData.to - Номер кошелька ЮMoney получателя (формат: 410011234567890)
   * @param {number} payoutData.amount - Сумма в рублях
   * @param {string} payoutData.label - Комментарий к переводу
   * @param {string} payoutData.masterId - ID мастера (для метаданных)
   * @returns {Promise<Object>} Результат выплаты
   */
  async payoutToWallet(payoutData) {
    if (!this.enabled) {
      throw new Error('ЮMoney не настроен');
    }
    
    try {
      const { to, amount, label, masterId } = payoutData;
      
      // Проверяем формат номера кошелька
      if (!/^\d{13,15}$/.test(to)) {
        throw new Error('Неверный формат номера кошелька ЮMoney. Должен быть 13-15 цифр');
      }
      
      // Проверяем минимальную сумму (обычно 1 рубль для ЮMoney)
      if (amount < 1) {
        throw new Error('Минимальная сумма выплаты: 1 ₽');
      }
      
      const response = await axios.post(
        `${this.baseUrl}/request-payment`,
        {
          pattern_id: 'p2p',
          to: to,
          amount: amount.toFixed(2),
          label: label || `Выплата мастеру #${masterId}`,
          message: `Выплата за выполненную работу`,
          comment: masterId ? `Мастер ID: ${masterId}` : undefined
        },
        {
          headers: {
            'Authorization': `Bearer ${this.accessToken}`,
            'Content-Type': 'application/x-www-form-urlencoded'
          }
        }
      );
      
      const requestId = response.data.request_id;
      
      // Подтверждаем платеж
      const confirmResponse = await axios.post(
        `${this.baseUrl}/process-payment`,
        {
          request_id: requestId
        },
        {
          headers: {
            'Authorization': `Bearer ${this.accessToken}`,
            'Content-Type': 'application/x-www-form-urlencoded'
          }
        }
      );
      
      console.log(`✅ Выплата ЮMoney выполнена: ${requestId}, получатель: ${to}, сумма: ${amount} ₽`);
      
      return {
        success: true,
        requestId: requestId,
        status: confirmResponse.data.status || 'success',
        amount: amount,
        recipient: to
      };
    } catch (error) {
      console.error('❌ Ошибка выплаты через ЮMoney:', error.response?.data || error.message);
      throw error;
    }
  }
  
  /**
   * Выплата на банковскую карту через СБП (Систему быстрых платежей)
   * Использует API ЮMoney для переводов на карту
   * @param {Object} payoutData - Данные выплаты
   * @param {string} payoutData.cardNumber - Номер карты (16 цифр)
   * @param {number} payoutData.amount - Сумма в рублях
   * @param {string} payoutData.masterId - ID мастера
   * @returns {Promise<Object>} Результат выплаты
   */
  async payoutToCard(payoutData) {
    if (!this.enabled) {
      throw new Error('ЮMoney не настроен');
    }
    
    try {
      const { cardNumber, amount, masterId } = payoutData;
      
      // Проверяем формат номера карты (16 цифр)
      const cleanCardNumber = cardNumber.replace(/\s/g, '');
      if (!/^\d{16}$/.test(cleanCardNumber)) {
        throw new Error('Неверный формат номера карты. Должно быть 16 цифр');
      }
      
      if (amount < 1) {
        throw new Error('Минимальная сумма выплаты: 1 ₽');
      }
      
      // TODO: Реализовать выплату на карту через ЮMoney API
      // В зависимости от типа интеграции может потребоваться другой API endpoint
      console.warn('⚠️ Выплата на карту через ЮMoney еще не реализована. Используйте payoutToWallet или СБП напрямую.');
      
      throw new Error('Выплата на карту через ЮMoney временно недоступна. Используйте кошелек ЮMoney или СБП.');
    } catch (error) {
      console.error('❌ Ошибка выплаты на карту:', error);
      throw error;
    }
  }
  
  /**
   * Проверка статуса выплаты
   * @param {string} requestId - ID запроса выплаты
   * @returns {Promise<Object>} Статус выплаты
   */
  async getPayoutStatus(requestId) {
    if (!this.enabled) {
      throw new Error('ЮMoney не настроен');
    }
    
    try {
      const response = await axios.post(
        `${this.baseUrl}/operation-history`,
        {
          type: 'deposition',
          request_id: requestId
        },
        {
          headers: {
            'Authorization': `Bearer ${this.accessToken}`,
            'Content-Type': 'application/x-www-form-urlencoded'
          }
        }
      );
      
      return response.data;
    } catch (error) {
      console.error(`❌ Ошибка проверки статуса выплаты ${requestId}:`, error.response?.data || error.message);
      throw error;
    }
  }
}

// Экспортируем singleton
export const yooMoneyService = new YooMoneyService();

export default yooMoneyService;






