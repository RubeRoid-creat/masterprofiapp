import jwt from 'jsonwebtoken';
import { config } from '../config.js';
import { query } from '../database/db.js';

// Middleware для проверки JWT токена
export const authenticate = (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      console.log('[AUTH] No token provided');
      return res.status(401).json({ error: 'Токен не предоставлен' });
    }
    
    const token = authHeader.substring(7);
    const decoded = jwt.verify(token, config.jwtSecret);
    
    // Получаем пользователя из БД
    const user = query.get('SELECT id, email, name, phone, role FROM users WHERE id = ?', [decoded.userId]);
    
    if (!user) {
      console.log(`[AUTH] User not found: ${decoded.userId}`);
      return res.status(401).json({ error: 'Пользователь не найден' });
    }
    
    console.log(`[AUTH] User authenticated: id=${user.id}, role=${user.role}`);
    
    // Добавляем информацию о пользователе в запрос
    req.user = user;
    next();
  } catch (error) {
    if (error.name === 'JsonWebTokenError') {
      return res.status(401).json({ error: 'Неверный токен' });
    }
    if (error.name === 'TokenExpiredError') {
      return res.status(401).json({ error: 'Токен истек' });
    }
    return res.status(500).json({ error: 'Ошибка аутентификации' });
  }
};

// Middleware для проверки роли пользователя
export const authorize = (...roles) => {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({ error: 'Требуется аутентификация' });
    }
    
    if (!roles.includes(req.user.role)) {
      return res.status(403).json({ error: 'Недостаточно прав доступа' });
    }
    
    next();
  };
};

// Генерация JWT токена
export const generateToken = (userId, role) => {
  return jwt.sign(
    { userId, role },
    config.jwtSecret,
    { expiresIn: '7d' }
  );
};



