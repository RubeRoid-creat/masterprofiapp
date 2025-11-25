import express from 'express';
import bcrypt from 'bcryptjs';
import { query } from '../database/db.js';
import { generateToken } from '../middleware/auth.js';

const router = express.Router();

// Регистрация
router.post('/register', async (req, res) => {
  try {
    const { email, password, name, phone, role = 'client' } = req.body;
    
    // Валидация
    if (!email || !password || !name || !phone) {
      return res.status(400).json({ error: 'Все поля обязательны' });
    }
    
    // Проверка, существует ли пользователь
    const existingUser = query.get('SELECT id FROM users WHERE email = ?', [email]);
    if (existingUser) {
      return res.status(400).json({ error: 'Пользователь с таким email уже существует' });
    }
    
    // Хешируем пароль
    const passwordHash = await bcrypt.hash(password, 10);
    
    // Создаем пользователя
    const result = query.run(
      'INSERT INTO users (email, password_hash, name, phone, role) VALUES (?, ?, ?, ?, ?)',
      [email, passwordHash, name, phone, role]
    );
    
    const userId = result.lastInsertRowid;
    
    // Если роль мастер, создаем запись в таблице мастеров
    if (role === 'master') {
      query.run(
        'INSERT INTO masters (user_id, specialization, status) VALUES (?, ?, ?)',
        [userId, JSON.stringify([]), 'offline']
      );
    } else if (role === 'client') {
      // Если роль клиент, создаем запись в таблице клиентов
      query.run(
        'INSERT INTO clients (user_id) VALUES (?)',
        [userId]
      );
    }
    
    // Генерируем токен
    const token = generateToken(userId, role);
    
    res.status(201).json({
      message: 'Пользователь успешно зарегистрирован',
      token,
      user: { id: userId, email, name, phone, role }
    });
  } catch (error) {
    console.error('Ошибка регистрации:', error);
    res.status(500).json({ error: 'Ошибка сервера при регистрации' });
  }
});

// Вход
router.post('/login', async (req, res) => {
  try {
    console.log('[POST /api/auth/login] Request received');
    const { email, password } = req.body;
    
    console.log(`[POST /api/auth/login] Email: ${email}, Password provided: ${!!password}`);
    
    // Валидация
    if (!email || !password) {
      console.log('[POST /api/auth/login] Missing email or password');
      return res.status(400).json({ error: 'Email и пароль обязательны' });
    }
    
    // Получаем пользователя
    const user = query.get('SELECT * FROM users WHERE email = ?', [email]);
    if (!user) {
      console.log(`[POST /api/auth/login] User not found: ${email}`);
      return res.status(401).json({ error: 'Неверный email или пароль' });
    }
    
    console.log(`[POST /api/auth/login] User found: id=${user.id}, role=${user.role}`);
    
    // Проверяем пароль
    const isValidPassword = await bcrypt.compare(password, user.password_hash);
    if (!isValidPassword) {
      console.log(`[POST /api/auth/login] Invalid password for user: ${email}`);
      return res.status(401).json({ error: 'Неверный email или пароль' });
    }
    
    console.log(`[POST /api/auth/login] Password verified for user: ${email}`);
    
    // Генерируем токен
    const token = generateToken(user.id, user.role);
    console.log(`[POST /api/auth/login] Token generated for user: ${user.id}`);
    
    // Получаем дополнительную информацию в зависимости от роли
    let additionalData = {};
    if (user.role === 'master') {
      const master = query.get('SELECT * FROM masters WHERE user_id = ?', [user.id]);
      additionalData = { masterId: master?.id, isOnShift: master?.is_on_shift === 1 };
      console.log(`[POST /api/auth/login] Master data: id=${master?.id}, isOnShift=${master?.is_on_shift === 1}`);
    } else if (user.role === 'client') {
      const client = query.get('SELECT * FROM clients WHERE user_id = ?', [user.id]);
      additionalData = { clientId: client?.id };
      console.log(`[POST /api/auth/login] Client data: id=${client?.id}`);
    }
    
    console.log(`[POST /api/auth/login] Login successful for user: ${user.email}, role: ${user.role}`);
    res.json({
      message: 'Успешный вход',
      token,
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        phone: user.phone,
        role: user.role,
        ...additionalData
      }
    });
  } catch (error) {
    console.error('Ошибка входа:', error);
    res.status(500).json({ error: 'Ошибка сервера при входе' });
  }
});

export default router;



