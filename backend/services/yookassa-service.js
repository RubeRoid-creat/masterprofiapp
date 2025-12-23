import { YooCheckout } from '@a2seven/yoo-checkout';
import { config } from '../config.js';

/**
 * Сервис для работы с ЮKassa (прием платежей от клиентов)
 * Документация: https://yookassa.ru/developers/api
 */
class YooKassaService {
  constructor() {
    const shopId = process.env.YOOKASSA_SHOP_ID;
    const secretKey = process.env.YOOKASSA_SECRET_KEY;
    
    if (!shopId || !secretKey) {
      console.warn('⚠️ ЮKassa не настроен: отсутствуют YOOKASSA_SHOP_ID или YOOKASSA_SECRET_KEY');
      this.checkout = null;
      this.enabled = false;
      return;
    }
    
    this.checkout = new YooCheckout({
      shopId,
      secretKey
    });
    this.enabled = true;
    
    console.log('✅ ЮKassa сервис инициализирован');
  }
  
  /**
   * Создание платежа
   * @param {Object} paymentData - Данные платежа
   * @param {number} paymentData.amount - Сумма в рублях
   * @param {string} paymentData.description - Описание платежа
   * @param {string} paymentData.orderId - ID заказа
   * @param {string} paymentData.returnUrl - URL для возврата после оплаты
   * @param {Object} paymentData.metadata - Дополнительные метаданные
   * @returns {Promise<Object>} Объект платежа с confirmation_url
   */
  async createPayment(paymentData) {
    if (!this.enabled) {
      throw new Error('ЮKassa не настроен. Проверьте переменные окружения YOOKASSA_SHOP_ID и YOOKASSA_SECRET_KEY');
    }
    
    try {
      const { amount, description, orderId, returnUrl, metadata = {} } = paymentData;
      
      // Формируем объект платежа
      const paymentData = {
        amount: {
          value: amount.toFixed(2),
          currency: 'RUB'
        },
        confirmation: {
          type: 'redirect',
          return_url: returnUrl || `${process.env.APP_URL || 'http://localhost:3000'}/payment/success`
        },
        description: description || `Оплата заказа #${orderId}`,
        metadata: {
          order_id: orderId.toString(),
          ...metadata
        }
      };
      
      // Добавляем чек только если настроены данные компании (ИНН и т.д.)
      // TODO: После настройки онлайн-кассы добавить receipt
      // if (process.env.OFD_INN && process.env.OFD_PAYMENT_ADDRESS) {
      //   paymentData.receipt = { ... };
      // }
      
      const idempotenceKey = 'payment_' + orderId + '_' + Date.now();
      const payment = await this.checkout.createPayment(paymentData, idempotenceKey);
      
      console.log(`✅ Создан платеж ЮKassa: ${payment.id} для заказа #${orderId}`);
      
      return {
        id: payment.id,
        status: payment.status,
        confirmationUrl: payment.confirmation?.confirmation_url,
        amount: payment.amount.value,
        currency: payment.amount.currency,
        metadata: payment.metadata
      };
    } catch (error) {
      console.error('❌ Ошибка создания платежа ЮKassa:', error);
      throw error;
    }
  }
  
  /**
   * Получение информации о платеже
   * @param {string} paymentId - ID платежа в ЮKassa
   * @returns {Promise<Object>} Данные платежа
   */
  async getPayment(paymentId) {
    if (!this.enabled) {
      throw new Error('ЮKassa не настроен');
    }
    
    try {
      const payment = await this.checkout.getPayment(paymentId);
      return payment;
    } catch (error) {
      console.error(`❌ Ошибка получения платежа ${paymentId}:`, error);
      throw error;
    }
  }
  
  /**
   * Проверка и обработка webhook от ЮKassa
   * @param {Object} webhookData - Данные webhook
   * @returns {Object} Результат обработки
   */
  async handleWebhook(webhookData) {
    if (!this.enabled) {
      throw new Error('ЮKassa не настроен');
    }
    
    try {
      // Проверяем тип события
      const event = webhookData.event;
      const payment = webhookData.object;
      
      console.log(`📥 Webhook от ЮKassa: ${event}, платеж: ${payment.id}, статус: ${payment.status}`);
      
      if (event === 'payment.succeeded') {
        return {
          success: true,
          event: 'payment.succeeded',
          paymentId: payment.id,
          orderId: payment.metadata?.order_id,
          amount: parseFloat(payment.amount.value),
          status: payment.status
        };
      } else if (event === 'payment.canceled') {
        return {
          success: true,
          event: 'payment.canceled',
          paymentId: payment.id,
          orderId: payment.metadata?.order_id,
          status: payment.status
        };
      }
      
      return {
        success: true,
        event: event,
        paymentId: payment.id,
        status: payment.status
      };
    } catch (error) {
      console.error('❌ Ошибка обработки webhook ЮKassa:', error);
      throw error;
    }
  }
  
  /**
   * Возврат средств (refund)
   * @param {string} paymentId - ID платежа
   * @param {number} amount - Сумма возврата
   * @param {string} description - Причина возврата
   * @returns {Promise<Object>} Данные возврата
   */
  async createRefund(paymentId, amount, description = 'Возврат средств') {
    if (!this.enabled) {
      throw new Error('ЮKassa не настроен');
    }
    
    try {
      const refund = await this.checkout.createRefund({
        payment_id: paymentId,
        amount: {
          value: amount.toFixed(2),
          currency: 'RUB'
        },
        description: description
      }, 'refund_' + paymentId + '_' + Date.now()); // idempotence_key
      
      console.log(`✅ Создан возврат ЮKassa: ${refund.id} для платежа ${paymentId}`);
      
      return refund;
    } catch (error) {
      console.error('❌ Ошибка создания возврата:', error);
      throw error;
    }
  }
}

// Экспортируем singleton
export const yooKassaService = new YooKassaService();

export default yooKassaService;

