import { createClient } from 'redis';
import { config } from '../config.js';

let redisClient = null;
let redisInitialized = false;
let redisConnectionFailed = false;

// Инициализация Redis клиента
export async function initRedis() {
  // Если уже пытались подключиться и не получилось, не пытаемся снова
  if (redisConnectionFailed) {
    return null;
  }
  
  // Если уже инициализирован, возвращаем клиент
  if (redisInitialized && redisClient) {
    return redisClient;
  }
  
  try {
    const redisUrl = process.env.REDIS_URL || 'redis://localhost:6379';
    redisClient = createClient({
      url: redisUrl,
      socket: {
        reconnectStrategy: false // Отключаем автоматическое переподключение
      }
    });

    // Обработчик ошибок - отключаем клиент при первой ошибке
    redisClient.on('error', async (err) => {
      // Выводим ошибку только один раз
      if (!redisConnectionFailed) {
        console.warn('⚠️ Redis не доступен, работаем без кэширования');
        redisConnectionFailed = true;
        
        // Закрываем клиент и удаляем все обработчики
        try {
          if (redisClient && redisClient.isOpen) {
            await redisClient.quit();
          }
        } catch (closeError) {
          // Игнорируем ошибки закрытия
        }
        
        // Удаляем все обработчики событий
        redisClient.removeAllListeners();
        redisClient = null;
      }
    });

    redisClient.on('connect', () => {
      console.log('✅ Redis подключен');
      redisInitialized = true;
      redisConnectionFailed = false;
    });

    // Пытаемся подключиться с таймаутом
    const connectPromise = redisClient.connect();
    const timeoutPromise = new Promise((_, reject) => 
      setTimeout(() => reject(new Error('Redis connection timeout')), 2000)
    );
    
    await Promise.race([connectPromise, timeoutPromise]);
    redisInitialized = true;
    redisConnectionFailed = false;
    return redisClient;
  } catch (error) {
    if (!redisConnectionFailed) {
      console.warn('⚠️ Redis не доступен, работаем без кэширования');
      redisConnectionFailed = true;
    }
    
    // Закрываем клиент если он был создан
    if (redisClient) {
      try {
        redisClient.removeAllListeners();
        if (redisClient.isOpen) {
          await redisClient.quit();
        }
      } catch (closeError) {
        // Игнорируем ошибки закрытия
      }
      redisClient = null;
    }
    
    return null;
  }
}

// Получить значение из кэша
export async function getCache(key) {
  if (!redisClient || redisConnectionFailed) return null;
  
  try {
    const value = await redisClient.get(key);
    return value ? JSON.parse(value) : null;
  } catch (error) {
    // Если ошибка подключения, помечаем как неудачное
    if (error.code === 'ECONNREFUSED' || error.message.includes('ECONNREFUSED')) {
      redisConnectionFailed = true;
      redisClient = null;
    }
    return null;
  }
}

// Установить значение в кэш
export async function setCache(key, value, ttl = 3600) {
  if (!redisClient || redisConnectionFailed) return false;
  
  try {
    await redisClient.setEx(key, ttl, JSON.stringify(value));
    return true;
  } catch (error) {
    // Если ошибка подключения, помечаем как неудачное
    if (error.code === 'ECONNREFUSED' || error.message.includes('ECONNREFUSED')) {
      redisConnectionFailed = true;
      redisClient = null;
    }
    return false;
  }
}

// Удалить значение из кэша
export async function deleteCache(key) {
  if (!redisClient || redisConnectionFailed) return false;
  
  try {
    await redisClient.del(key);
    return true;
  } catch (error) {
    // Если ошибка подключения, помечаем как неудачное
    if (error.code === 'ECONNREFUSED' || error.message.includes('ECONNREFUSED')) {
      redisConnectionFailed = true;
      redisClient = null;
    }
    return false;
  }
}

// Удалить все ключи по паттерну
export async function deleteCachePattern(pattern) {
  if (!redisClient || redisConnectionFailed) return false;
  
  try {
    const keys = await redisClient.keys(pattern);
    if (keys.length > 0) {
      await redisClient.del(keys);
    }
    return true;
  } catch (error) {
    // Если ошибка подключения, помечаем как неудачное
    if (error.code === 'ECONNREFUSED' || error.message.includes('ECONNREFUSED')) {
      redisConnectionFailed = true;
      redisClient = null;
    }
    return false;
  }
}

// Обертка для кэширования функции
export async function withCache(key, fn, ttl = 3600) {
  // Пытаемся получить из кэша
  const cached = await getCache(key);
  if (cached !== null) {
    return cached;
  }
  
  // Если нет в кэше, выполняем функцию
  const result = await fn();
  
  // Сохраняем в кэш
  await setCache(key, result, ttl);
  
  return result;
}

// Ключи кэша
export const CacheKeys = {
  masters: {
    all: 'masters:all',
    available: 'masters:available',
    byId: (id) => `masters:${id}`,
    byUserId: (userId) => `masters:user:${userId}`
  },
  services: {
    categories: 'services:categories',
    categoriesByParent: (parentId) => `services:categories:parent:${parentId}`,
    categoryById: (id) => `services:category:${id}`,
    templates: 'services:templates',
    templatesByCategory: (categoryId) => `services:templates:category:${categoryId}`,
    templatesByDevice: (deviceType) => `services:templates:device:${deviceType}`,
    popularTemplates: 'services:templates:popular'
  },
  orders: {
    byId: (id) => `orders:${id}`,
    byClient: (clientId) => `orders:client:${clientId}`,
    byMaster: (masterId) => `orders:master:${masterId}`,
    available: 'orders:available'
  }
};

// Инвалидация кэша при изменении данных
export async function invalidateMastersCache() {
  await deleteCachePattern('masters:*');
}

export async function invalidateServicesCache() {
  await deleteCachePattern('services:*');
}

export async function invalidateOrdersCache() {
  await deleteCachePattern('orders:*');
}

export async function invalidateOrderCache(orderId) {
  await deleteCache(CacheKeys.orders.byId(orderId));
  await deleteCachePattern('orders:client:*');
  await deleteCachePattern('orders:master:*');
  await deleteCache(CacheKeys.orders.available);
}

export default redisClient;

