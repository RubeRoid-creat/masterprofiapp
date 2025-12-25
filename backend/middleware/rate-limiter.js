/**
 * Rate Limiting Middleware
 * Защита от DDoS атак и злоупотреблений API
 */

const requestCounts = new Map();
const blockedIPs = new Map();

// Настройки по умолчанию
const defaultConfig = {
  windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS) || 15 * 60 * 1000, // 15 минут
  maxRequests: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS) || 100, // 100 запросов
  blockDuration: 60 * 60 * 1000, // 1 час блокировки при превышении
  enabled: process.env.RATE_LIMIT_ENABLED !== 'false' // Включено по умолчанию
};

/**
 * Получить IP адрес клиента
 */
function getClientIP(req) {
  return req.headers['x-forwarded-for']?.split(',')[0]?.trim() ||
         req.headers['x-real-ip'] ||
         req.connection?.remoteAddress ||
         req.socket?.remoteAddress ||
         req.ip ||
         'unknown';
}

/**
 * Очистка старых записей
 */
function cleanupOldRecords() {
  const now = Date.now();
  
  // Очистка счетчиков запросов
  for (const [ip, data] of requestCounts.entries()) {
    if (now - data.resetTime > defaultConfig.windowMs) {
      requestCounts.delete(ip);
    }
  }
  
  // Очистка заблокированных IP
  for (const [ip, blockTime] of blockedIPs.entries()) {
    if (now - blockTime > defaultConfig.blockDuration) {
      blockedIPs.delete(ip);
      console.log(`🔓 IP ${ip} разблокирован`);
    }
  }
}

// Периодическая очистка каждые 5 минут
setInterval(cleanupOldRecords, 5 * 60 * 1000);

/**
 * Rate Limiter Middleware
 */
export function rateLimiter(options = {}) {
  const config = { ...defaultConfig, ...options };
  
  return (req, res, next) => {
    // Если rate limiting отключен
    if (!config.enabled) {
      return next();
    }
    
    const ip = getClientIP(req);
    const now = Date.now();
    
    // Проверка блокировки IP
    if (blockedIPs.has(ip)) {
      const blockTime = blockedIPs.get(ip);
      const remainingTime = Math.ceil((config.blockDuration - (now - blockTime)) / 1000 / 60);
      
      console.warn(`🚫 Заблокированный IP пытается подключиться: ${ip}`);
      
      return res.status(429).json({
        error: 'Too Many Requests',
        message: `Ваш IP временно заблокирован. Попробуйте через ${remainingTime} минут.`,
        retryAfter: remainingTime * 60
      });
    }
    
    // Получение или создание записи для IP
    let record = requestCounts.get(ip);
    
    if (!record || now - record.resetTime > config.windowMs) {
      // Создание новой записи
      record = {
        count: 0,
        resetTime: now
      };
      requestCounts.set(ip, record);
    }
    
    // Увеличение счетчика
    record.count++;
    
    // Проверка лимита
    if (record.count > config.maxRequests) {
      // Блокировка IP
      blockedIPs.set(ip, now);
      requestCounts.delete(ip);
      
      console.error(`⛔ IP заблокирован за превышение лимита: ${ip} (${record.count} запросов)`);
      
      return res.status(429).json({
        error: 'Too Many Requests',
        message: 'Превышен лимит запросов. Ваш IP временно заблокирован.',
        retryAfter: config.blockDuration / 1000
      });
    }
    
    // Установка заголовков
    const remaining = config.maxRequests - record.count;
    const resetTime = Math.ceil((record.resetTime + config.windowMs) / 1000);
    
    res.setHeader('X-RateLimit-Limit', config.maxRequests);
    res.setHeader('X-RateLimit-Remaining', remaining);
    res.setHeader('X-RateLimit-Reset', resetTime);
    
    // Предупреждение при приближении к лимиту
    if (remaining <= 10) {
      console.warn(`⚠️ IP ${ip} приближается к лимиту: осталось ${remaining} запросов`);
    }
    
    next();
  };
}

/**
 * Строгий Rate Limiter для критичных эндпоинтов (авторизация, регистрация)
 */
export function strictRateLimiter(maxRequests = 5, windowMs = 15 * 60 * 1000) {
  return rateLimiter({
    maxRequests,
    windowMs,
    blockDuration: 30 * 60 * 1000 // 30 минут блокировки (уменьшено с 1 часа)
  });
}

/**
 * Rate Limiter для отправки кодов подтверждения
 */
export function verificationRateLimiter() {
  return rateLimiter({
    maxRequests: 3, // Только 3 попытки
    windowMs: 10 * 60 * 1000, // За 10 минут
    blockDuration: 30 * 60 * 1000 // Блокировка на 30 минут
  });
}

/**
 * Rate Limiter для статистики (более мягкий, так как запрашивается часто)
 */
export function statsRateLimiter() {
  return rateLimiter({
    maxRequests: 200, // 200 запросов
    windowMs: 15 * 60 * 1000, // За 15 минут
    blockDuration: 10 * 60 * 1000 // Блокировка на 10 минут (меньше, чем для других)
  });
}

/**
 * Получение статистики rate limiting
 */
export function getRateLimitStats() {
  return {
    activeIPs: requestCounts.size,
    blockedIPs: blockedIPs.size,
    records: Array.from(requestCounts.entries()).map(([ip, data]) => ({
      ip,
      requests: data.count,
      resetTime: new Date(data.resetTime + defaultConfig.windowMs).toISOString()
    })),
    blocked: Array.from(blockedIPs.entries()).map(([ip, blockTime]) => ({
      ip,
      blockedAt: new Date(blockTime).toISOString(),
      unblockAt: new Date(blockTime + defaultConfig.blockDuration).toISOString()
    }))
  };
}

/**
 * Разблокировка IP адреса
 */
export function unblockIP(ip) {
  if (blockedIPs.has(ip)) {
    blockedIPs.delete(ip);
    requestCounts.delete(ip);
    console.log(`🔓 IP ${ip} разблокирован вручную`);
    return true;
  }
  return false;
}

/**
 * Очистка счетчика запросов для IP
 */
export function resetIPCounter(ip) {
  requestCounts.delete(ip);
  console.log(`🔄 Счетчик запросов для IP ${ip} сброшен`);
}

export default rateLimiter;
