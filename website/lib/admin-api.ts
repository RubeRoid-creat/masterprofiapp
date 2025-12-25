/**
 * Интеграция с админ-панелью
 * API для синхронизации данных между сайтом и админ-панелью
 */

const ADMIN_API_URL = process.env.NEXT_PUBLIC_ADMIN_API_URL || 'http://212.74.227.208:3000/api';
const ADMIN_API_KEY = process.env.ADMIN_API_KEY || '';

interface OrderData {
  address: string;
  equipmentType: string;
  problemType: string;
  brand: string;
  date: string;
  time: string;
  description?: string;
  name: string;
  phone: string;
  email?: string;
}

/**
 * Отправка заказа в админ-панель (backend API)
 * 
 * ВАЖНО: Backend требует авторизацию с токеном пользователя с ролью 'client' или 'admin'
 * Пользователь должен иметь запись в таблице clients
 */
export async function sendOrderToAdmin(orderData: OrderData) {
  try {
    // Получаем координаты адреса (по умолчанию Москва)
    // TODO: Добавить геокодинг адреса для получения реальных координат
    let latitude = 55.751244; // Москва по умолчанию
    let longitude = 37.618423;

    console.log('Sending order to admin panel:', {
      url: `${ADMIN_API_URL}/orders/from-website`,
      orderData: {
        device_type: orderData.equipmentType,
        address: orderData.address,
        name: orderData.name,
        email: orderData.email,
      }
    });

    // Используем специальный endpoint для заказов с сайта (не требует авторизации)
    const response = await fetch(`${ADMIN_API_URL}/orders/from-website`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        // Данные клиента
        name: orderData.name,
        phone: orderData.phone,
        email: orderData.email || `${orderData.phone.replace(/\D/g, '')}@website.local`, // Если email не указан, используем телефон
        
        // Данные заказа (формат snake_case для backend)
        device_type: orderData.equipmentType,
        device_brand: orderData.brand,
        device_model: '', // Не указано на сайте
        problem_description: orderData.problemType + (orderData.description ? `: ${orderData.description}` : ''),
        problem_short_description: orderData.problemType,
        address: orderData.address,
        latitude: latitude,
        longitude: longitude,
        desired_repair_date: orderData.date,
        arrival_time: orderData.time,
        urgency: 'planned', // 'emergency', 'urgent', 'planned'
        priority: 'regular', // 'emergency', 'urgent', 'regular', 'planned'
        
        // Дополнительные поля (опциональные)
        device_category: null,
        intercom_working: true,
        parking_available: true,
        preferred_contact_method: 'call',
      }),
    });

    const responseText = await response.text();
    console.log('Admin API Response Status:', response.status);
    console.log('Admin API Response:', responseText);

    if (!response.ok) {
      let errorData;
      try {
        errorData = JSON.parse(responseText);
      } catch {
        errorData = { error: responseText || 'Unknown error' };
      }
      console.error('Admin API Error Details:', {
        status: response.status,
        statusText: response.statusText,
        error: errorData,
        url: `${ADMIN_API_URL}/orders`,
      });
      throw new Error(errorData.error || `HTTP ${response.status}: ${response.statusText}`);
    }

    const result = JSON.parse(responseText);
    console.log('✅ Order sent to admin panel successfully:', {
      orderId: result.order?.id,
      orderNumber: result.order?.order_number,
      message: result.message,
    });
    return result;
  } catch (error: any) {
    console.error('❌ Error sending order to admin panel:', {
      message: error.message,
      stack: error.stack,
      name: error.name,
    });
    throw error;
  }
}

/**
 * Получение заказов из админ-панели
 */
export async function getOrdersFromAdmin(params?: { status?: string; limit?: number }) {
  try {
    const queryParams = new URLSearchParams();
    if (params?.status) queryParams.append('status', params.status);
    if (params?.limit) queryParams.append('limit', params.limit.toString());

    const response = await fetch(`${ADMIN_API_URL}/orders?${queryParams.toString()}`, {
      headers: {
        'Content-Type': 'application/json',
        ...(ADMIN_API_KEY && { 'X-API-Key': ADMIN_API_KEY }),
      },
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching orders from admin panel:', error);
    throw error;
  }
}

/**
 * Синхронизация новостей с админ-панелью
 */
export async function syncNewsWithAdmin() {
  // TODO: Реализовать синхронизацию новостей
  // Это будет зависеть от API админ-панели для управления новостями
  return [];
}

/**
 * Интерфейс позиции прайса
 */
export interface PriceItem {
  id?: number;
  category: string;
  name: string;
  price: number;
  type: 'service' | 'part';
  description?: string;
  unit?: string;
  created_at?: string;
  updated_at?: string;
}

/**
 * Получение прайса из админ-панели
 */
export async function getPricesFromAdmin(params?: { category?: string; type?: 'service' | 'part' }) {
  try {
    const queryParams = new URLSearchParams();
    if (params?.category) queryParams.append('category', params.category);
    if (params?.type) queryParams.append('type', params.type);

    const response = await fetch(`${ADMIN_API_URL}/prices?${queryParams.toString()}`, {
      headers: {
        'Content-Type': 'application/json',
        ...(ADMIN_API_KEY && { 'X-API-Key': ADMIN_API_KEY }),
      },
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching prices from admin panel:', error);
    throw error;
  }
}

/**
 * Создание позиции прайса (требует авторизацию админа)
 */
export async function createPriceItem(token: string, priceItem: Omit<PriceItem, 'id' | 'created_at' | 'updated_at'>) {
  try {
    const response = await fetch(`${ADMIN_API_URL}/prices`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        ...(ADMIN_API_KEY && { 'X-API-Key': ADMIN_API_KEY }),
      },
      body: JSON.stringify(priceItem),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`HTTP ${response.status}: ${errorText}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error creating price item:', error);
    throw error;
  }
}

/**
 * Обновление позиции прайса (требует авторизацию админа)
 */
export async function updatePriceItem(token: string, id: number, priceItem: Partial<Omit<PriceItem, 'id' | 'created_at' | 'updated_at'>>) {
  try {
    const response = await fetch(`${ADMIN_API_URL}/prices/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        ...(ADMIN_API_KEY && { 'X-API-Key': ADMIN_API_KEY }),
      },
      body: JSON.stringify(priceItem),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`HTTP ${response.status}: ${errorText}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error updating price item:', error);
    throw error;
  }
}

/**
 * Удаление позиции прайса (требует авторизацию админа)
 */
export async function deletePriceItem(token: string, id: number) {
  try {
    const response = await fetch(`${ADMIN_API_URL}/prices/${id}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
        ...(ADMIN_API_KEY && { 'X-API-Key': ADMIN_API_KEY }),
      },
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`HTTP ${response.status}: ${errorText}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error deleting price item:', error);
    throw error;
  }
}

/**
 * Синхронизация прайса с админ-панелью
 */
export async function syncPricesWithAdmin() {
  return await getPricesFromAdmin();
}

