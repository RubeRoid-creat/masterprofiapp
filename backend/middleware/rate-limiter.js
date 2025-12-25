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
  
  // Очистка счетчиков статистики
  for (const [ip, data] of statsRequestCounts.entries()) {
    if (now - data.resetTime > 15 * 60 * 1000) {
      statsRequestCounts.delete(ip);
    }
  }
  
  // Очистка заблокированных IP для статистики
  for (const [ip, blockTime] of statsBlockedIPs.entries()) {
    if (now - blockTime > 5 * 60 * 1000) {
      statsBlockedIPs.delete(ip);
      console.log(`🔓 [STATS] IP ${ip} разблокирован для статистики`);
    }
  }
  
  // Очистка счетчиков верификации
  for (const [ip, data] of verificationRequestCounts.entries()) {
    if (now - data.resetTime > 15 * 60 * 1000) {
      verificationRequestCounts.delete(ip);
    }
  }
  
  // Очистка заблокированных IP для верификации
  for (const [ip, blockTime] of verificationBlockedIPs.entries()) {
    if (now - blockTime > 10 * 60 * 1000) {
      verificationBlockedIPs.delete(ip);
      console.log(`🔓 [VERIFICATION] IP ${ip} разблокирован для верификации`);
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
    
    // Исключаем эндпоинты статистики и верификации из общего rate limiting
    // Они используют свои rate limiters с более высокими лимитами
    const path = req.path || req.url;
    if (path.includes('/api/masters/stats/me') || 
        path.includes('/api/mlm/statistics') ||
        path.includes('/api/mlm/structure') ||
        path.includes('/api/verification')) {
      return next(); // Пропускаем общий rate limiter для статистики и верификации
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

// Отдельные счетчики для статистики (чтобы не конфликтовать с общим rate limiter)
const statsRequestCounts = new Map();
const statsBlockedIPs = new Map();

// Отдельные счетчики для верификации мастера
const verificationRequestCounts = new Map();
const verificationBlockedIPs = new Map();

/**
 * Rate Limiter для верификации мастера (более мягкий, так как процесс включает несколько запросов)
 * Использует отдельные счетчики, чтобы не конфликтовать с общим rate limiter
 */
export function verificationMasterRateLimiter() {
  return (req, res, next) => {
    const config = {
      maxRequests: 100, // 100 запросов за 15 минут (достаточно для загрузки документов)
      windowMs: 15 * 60 * 1000, // За 15 минут
      blockDuration: 10 * 60 * 1000, // Блокировка на 10 минут
      enabled: true
    };
    
    if (!config.enabled) {
      return next();
    }
    
    const ip = getClientIP(req);
    const now = Date.now();
    
    // Проверка блокировки IP в верификации
    if (verificationBlockedIPs.has(ip)) {
      const blockTime = verificationBlockedIPs.get(ip);
      const remainingTime = Math.ceil((config.blockDuration - (now - blockTime)) / 1000 / 60);
      
      console.warn(`🚫 [VERIFICATION] Заблокированный IP пытается верифицироваться: ${ip}`);
      
      return res.status(429).json({
        error: 'Too Many Requests',
        message: `Слишком много запросов верификации. Попробуйте через ${remainingTime} минут.`,
        retryAfter: remainingTime * 60
      });
    }
    
    // Получение или создание записи для IP
    let record = verificationRequestCounts.get(ip);
    
    if (!record || now - record.resetTime > config.windowMs) {
      record = {
        count: 0,
        resetTime: now
      };
      verificationRequestCounts.set(ip, record);
    }
    
    // Увеличение счетчика
    record.count++;
    
    // Проверка лимита
    if (record.count > config.maxRequests) {
      verificationBlockedIPs.set(ip, now);
      verificationRequestCounts.delete(ip);
      
      console.error(`⛔ [VERIFICATION] IP заблокирован за превышение лимита верификации: ${ip} (${record.count} запросов)`);
      
      return res.status(429).json({
        error: 'Too Many Requests',
        message: 'Превышен лимит запросов верификации. Ваш IP временно заблокирован.',
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
    if (remaining <= 20) {
      console.warn(`⚠️ [VERIFICATION] IP ${ip} приближается к лимиту верификации: осталось ${remaining} запросов`);
    }
    
    next();
  };
}

/**
 * Rate Limiter для статистики (более мягкий, так как запрашивается часто)
 * Использует отдельные счетчики, чтобы не конфликтовать с общим rate limiter
 */
export function statsRateLimiter() {
  return (req, res, next) => {
    const config = {
      maxRequests: 500, // 500 запросов (увеличено с 200)
      windowMs: 15 * 60 * 1000, // За 15 минут
      blockDuration: 5 * 60 * 1000, // Блокировка на 5 минут (уменьшено)
      enabled: true
    };
    
    if (!config.enabled) {
      return next();
    }
    
    const ip = getClientIP(req);
    const now = Date.now();
    
    // Проверка блокировки IP в статистике
    if (statsBlockedIPs.has(ip)) {
      const blockTime = statsBlockedIPs.get(ip);
      const remainingTime = Math.ceil((config.blockDuration - (now - blockTime)) / 1000 / 60);
      
      console.warn(`🚫 [STATS] Заблокированный IP пытается получить статистику: ${ip}`);
      
      return res.status(429).json({
        error: 'Too Many Requests',
        message: `Слишком много запросов статистики. Попробуйте через ${remainingTime} минут.`,
        retryAfter: remainingTime * 60
      });
    }
    
    // Получение или создание записи для IP
    let record = statsRequestCounts.get(ip);
    
    if (!record || now - record.resetTime > config.windowMs) {
      record = {
        count: 0,
        resetTime: now
      };
      statsRequestCounts.set(ip, record);
    }
    
    // Увеличение счетчика
    record.count++;
    
    // Проверка лимита
    if (record.count > config.maxRequests) {
      statsBlockedIPs.set(ip, now);
      statsRequestCounts.delete(ip);
      
      console.error(`⛔ [STATS] IP заблокирован за превышение лимита статистики: ${ip} (${record.count} запросов)`);
      
      return res.status(429).json({
        error: 'Too Many Requests',
        message: 'Превышен лимит запросов статистики. Ваш IP временно заблокирован.',
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
    if (remaining <= 50) {
      console.warn(`⚠️ [STATS] IP ${ip} приближается к лимиту статистики: осталось ${remaining} запросов`);
    }
    
    next();
  };
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
  let unblocked = false;
  if (blockedIPs.has(ip)) {
    blockedIPs.delete(ip);
    requestCounts.delete(ip);
    console.log(`🔓 IP ${ip} разблокирован вручную`);
    unblocked = true;
  }
  if (statsBlockedIPs.has(ip)) {
    statsBlockedIPs.delete(ip);
    statsRequestCounts.delete(ip);
    console.log(`🔓 [STATS] IP ${ip} разблокирован для статистики вручную`);
    unblocked = true;
  }
  if (verificationBlockedIPs.has(ip)) {
    verificationBlockedIPs.delete(ip);
    verificationRequestCounts.delete(ip);
    console.log(`🔓 [VERIFICATION] IP ${ip} разблокирован для верификации вручную`);
    unblocked = true;
  }
  return unblocked;
}

/**
 * Очистка счетчика запросов для IP
 */
export function resetIPCounter(ip) {
  requestCounts.delete(ip);
  statsRequestCounts.delete(ip);
  verificationRequestCounts.delete(ip);
  console.log(`🔄 Счетчики запросов для IP ${ip} сброшены`);
}

export default rateLimiter;
