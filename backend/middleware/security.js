/**
 * Security Middleware
 * Дополнительные меры безопасности для сервера
 */

/**
 * HTTPS Redirect Middleware
 * Перенаправляет HTTP на HTTPS в production
 */
export function httpsRedirect(req, res, next) {
  // Только в production
  if (process.env.NODE_ENV !== 'production') {
    return next();
  }
  
  // Проверяем протокол
  const protocol = req.headers['x-forwarded-proto'] || req.protocol;
  
  if (protocol === 'http') {
    const httpsUrl = `https://${req.headers.host}${req.url}`;
    console.log(`🔒 Редирект HTTP → HTTPS: ${req.url}`);
    return res.redirect(301, httpsUrl);
  }
  
  next();
}

/**
 * Security Headers Middleware
 * Добавляет заголовки безопасности
 */
export function securityHeaders(req, res, next) {
  // Предотвращение XSS атак
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-XSS-Protection', '1; mode=block');
  
  // Content Security Policy
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  
  // HTTPS Strict Transport Security (только для production)
  if (process.env.NODE_ENV === 'production') {
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
  }
  
  // Скрываем информацию о сервере
  res.removeHeader('X-Powered-By');
  
  next();
}

/**
 * Request Sanitization
 * Очистка входных данных от потенциально опасного содержимого
 */
export function sanitizeRequest(req, res, next) {
  // Проверка на SQL injection паттерны
  const dangerousPatterns = [
    /(\%27)|(\')|(\-\-)|(\%23)|(#)/i, // SQL инъекции
    /(<script[^>]*>.*?<\/script>)/gi, // XSS скрипты
    /(javascript:)/gi, // JavaScript протокол
    /(onclick|onerror|onload)/gi // Event handlers
  ];
  
  // Проверка строковых параметров
  function checkValue(value, path = '') {
    if (typeof value === 'string') {
      for (const pattern of dangerousPatterns) {
        if (pattern.test(value)) {
          console.error(`⚠️ Обнаружена попытка атаки в ${path}:`, value);
          throw new Error('Invalid input detected');
        }
      }
    } else if (typeof value === 'object' && value !== null) {
      for (const key in value) {
        checkValue(value[key], `${path}.${key}`);
      }
    }
  }
  
  try {
    // Проверка body
    if (req.body) {
      checkValue(req.body, 'body');
    }
    
    // Проверка query параметров
    if (req.query) {
      checkValue(req.query, 'query');
    }
    
    // Проверка params
    if (req.params) {
      checkValue(req.params, 'params');
    }
    
    next();
  } catch (error) {
    res.status(400).json({
      error: 'Bad Request',
      message: 'Обнаружены недопустимые символы в запросе'
    });
  }
}

/**
 * IP Whitelist/Blacklist
 */
const blacklistedIPs = new Set();
const whitelistedIPs = new Set();

export function ipFilter(req, res, next) {
  const ip = req.headers['x-forwarded-for']?.split(',')[0]?.trim() ||
             req.headers['x-real-ip'] ||
             req.connection?.remoteAddress ||
             req.ip;
  
  // Проверка blacklist
  if (blacklistedIPs.has(ip)) {
    console.error(`🚫 Заблокированный IP: ${ip}`);
    return res.status(403).json({
      error: 'Forbidden',
      message: 'Доступ запрещен'
    });
  }
  
  // Если есть whitelist, проверяем наличие IP в нем
  if (whitelistedIPs.size > 0 && !whitelistedIPs.has(ip)) {
    console.warn(`⚠️ IP не в whitelist: ${ip}`);
    return res.status(403).json({
      error: 'Forbidden',
      message: 'Доступ разрешен только для авторизованных IP'
    });
  }
  
  next();
}

/**
 * Функции управления IP списками
 */
export function blockIP(ip) {
  blacklistedIPs.add(ip);
  console.log(`🚫 IP добавлен в blacklist: ${ip}`);
}

export function unblockIP(ip) {
  blacklistedIPs.delete(ip);
  console.log(`✅ IP удален из blacklist: ${ip}`);
}

export function addToWhitelist(ip) {
  whitelistedIPs.add(ip);
  console.log(`✅ IP добавлен в whitelist: ${ip}`);
}

export function removeFromWhitelist(ip) {
  whitelistedIPs.delete(ip);
  console.log(`🚫 IP удален из whitelist: ${ip}`);
}

/**
 * Request Logger для аудита безопасности
 */
export function securityAuditLogger(req, res, next) {
  const start = Date.now();
  
  // Логируем подозрительные запросы
  const isSuspicious = req.url.includes('..') || // Path traversal
                       req.url.includes('<') ||  // XSS попытка
                       req.url.includes('script') ||
                       req.headers['user-agent']?.includes('bot');
  
  if (isSuspicious) {
    console.warn(`⚠️ Подозрительный запрос:`, {
      ip: req.ip,
      method: req.method,
      url: req.url,
      userAgent: req.headers['user-agent']
    });
  }
  
  // После завершения запроса
  res.on('finish', () => {
    const duration = Date.now() - start;
    
    // Логируем медленные запросы
    if (duration > 5000) {
      console.warn(`🐌 Медленный запрос (${duration}ms):`, {
        method: req.method,
        url: req.url,
        status: res.statusCode
      });
    }
    
    // Логируем ошибки аутентификации
    if (res.statusCode === 401 || res.statusCode === 403) {
      console.warn(`🔐 Неудачная попытка доступа:`, {
        ip: req.ip,
        url: req.url,
        status: res.statusCode
      });
    }
  });
  
  next();
}

export default {
  httpsRedirect,
  securityHeaders,
  sanitizeRequest,
  ipFilter,
  securityAuditLogger,
  blockIP,
  unblockIP,
  addToWhitelist,
  removeFromWhitelist
};
