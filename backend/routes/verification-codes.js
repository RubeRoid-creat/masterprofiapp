import express from 'express';
import { query } from '../database/db.js';
import { authenticate } from '../middleware/auth.js';
import { sendVerificationEmail, sendConfirmationEmail } from '../services/email-service.js';
import { sendVerificationSMS, generateVerificationCode } from '../services/sms-service.js';

const router = express.Router();

// Отправить код подтверждения на email
router.post('/send-email-code', authenticate, async (req, res) => {
  try {
    const userId = req.user.id;
    
    // Получаем email пользователя
    const user = query.get('SELECT email, name, email_verified FROM users WHERE id = ?', [userId]);
    if (!user) {
      return res.status(404).json({ error: 'Пользователь не найден' });
    }
    
    // Проверяем, не подтвержден ли уже email
    if (user.email_verified === 1) {
      return res.status(400).json({ error: 'Email уже подтвержден' });
    }
    
    // Генерируем код
    const code = generateVerificationCode();
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000); // 10 минут
    
    // Сохраняем код в БД
    query.run(`
      INSERT INTO verification_codes (user_id, type, code, expires_at)
      VALUES (?, 'email', ?, ?)
    `, [userId, code, expiresAt.toISOString()]);
    
    // Отправляем email
    try {
      await sendVerificationEmail(user.email, code, user.name);
      res.json({ 
        message: 'Код подтверждения отправлен на email',
        expiresIn: 600 // секунд
      });
    } catch (emailError) {
      console.error('Ошибка отправки email:', emailError);
      res.status(500).json({ error: 'Не удалось отправить код на email' });
    }
  } catch (error) {
    console.error('Ошибка отправки кода на email:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Отправить код подтверждения на телефон
router.post('/send-phone-code', authenticate, async (req, res) => {
  try {
    const userId = req.user.id;
    
    // Получаем телефон пользователя
    const user = query.get('SELECT phone, name, phone_verified FROM users WHERE id = ?', [userId]);
    if (!user) {
      return res.status(404).json({ error: 'Пользователь не найден' });
    }
    
    // Проверяем, не подтвержден ли уже телефон
    if (user.phone_verified === 1) {
      return res.status(400).json({ error: 'Телефон уже подтвержден' });
    }
    
    // Генерируем код
    const code = generateVerificationCode();
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000); // 10 минут
    
    // Сохраняем код в БД
    query.run(`
      INSERT INTO verification_codes (user_id, type, code, expires_at)
      VALUES (?, 'phone', ?, ?)
    `, [userId, code, expiresAt.toISOString()]);
    
    // Отправляем SMS
    try {
      await sendVerificationSMS(user.phone, code);
      res.json({ 
        message: 'Код подтверждения отправлен на телефон',
        expiresIn: 600 // секунд
      });
    } catch (smsError) {
      console.error('Ошибка отправки SMS:', smsError);
      res.status(500).json({ error: 'Не удалось отправить код на телефон' });
    }
  } catch (error) {
    console.error('Ошибка отправки кода на телефон:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Проверить код подтверждения email
router.post('/verify-email-code', authenticate, async (req, res) => {
  try {
    const { code } = req.body;
    const userId = req.user.id;
    
    if (!code) {
      return res.status(400).json({ error: 'Код обязателен' });
    }
    
    // Ищем актуальный код
    const verificationCode = query.get(`
      SELECT * FROM verification_codes
      WHERE user_id = ? AND type = 'email' AND code = ? AND verified = 0
      ORDER BY created_at DESC
      LIMIT 1
    `, [userId, code]);
    
    if (!verificationCode) {
      return res.status(400).json({ error: 'Неверный код' });
    }
    
    // Проверяем срок действия
    const expiresAt = new Date(verificationCode.expires_at);
    if (expiresAt < new Date()) {
      return res.status(400).json({ error: 'Код истек' });
    }
    
    // Помечаем код как использованный
    query.run('UPDATE verification_codes SET verified = 1 WHERE id = ?', [verificationCode.id]);
    
    // Обновляем статус подтверждения email
    const currentUser = query.get('SELECT email_verified FROM users WHERE id = ?', [userId]);
    if (currentUser && currentUser.email_verified !== 1) {
      query.run('UPDATE users SET email_verified = 1 WHERE id = ?', [userId]);
    }
    
    // Отправляем email подтверждения
    const user = query.get('SELECT email, name FROM users WHERE id = ?', [userId]);
    if (user) {
      sendConfirmationEmail(user.email, user.name, 'email').catch(err => {
        console.error('Ошибка отправки email подтверждения:', err);
      });
    }
    
    res.json({ message: 'Email успешно подтвержден' });
  } catch (error) {
    console.error('Ошибка проверки кода email:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Проверить код подтверждения телефона
router.post('/verify-phone-code', authenticate, async (req, res) => {
  try {
    const { code } = req.body;
    const userId = req.user.id;
    
    if (!code) {
      return res.status(400).json({ error: 'Код обязателен' });
    }
    
    // Ищем актуальный код
    const verificationCode = query.get(`
      SELECT * FROM verification_codes
      WHERE user_id = ? AND type = 'phone' AND code = ? AND verified = 0
      ORDER BY created_at DESC
      LIMIT 1
    `, [userId, code]);
    
    if (!verificationCode) {
      return res.status(400).json({ error: 'Неверный код' });
    }
    
    // Проверяем срок действия
    const expiresAt = new Date(verificationCode.expires_at);
    if (expiresAt < new Date()) {
      return res.status(400).json({ error: 'Код истек' });
    }
    
    // Помечаем код как использованный
    query.run('UPDATE verification_codes SET verified = 1 WHERE id = ?', [verificationCode.id]);
    
    // Обновляем статус подтверждения телефона
    const currentUser = query.get('SELECT phone_verified FROM users WHERE id = ?', [userId]);
    if (currentUser && currentUser.phone_verified !== 1) {
      query.run('UPDATE users SET phone_verified = 1 WHERE id = ?', [userId]);
    }
    
    // Отправляем email подтверждения (если email подтвержден)
    const user = query.get('SELECT email, name, email_verified FROM users WHERE id = ?', [userId]);
    if (user && user.email_verified === 1) {
      sendConfirmationEmail(user.email, user.name, 'phone').catch(err => {
        console.error('Ошибка отправки email подтверждения:', err);
      });
    }
    
    res.json({ message: 'Телефон успешно подтвержден' });
  } catch (error) {
    console.error('Ошибка проверки кода телефона:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить статус подтверждения
router.get('/status', authenticate, async (req, res) => {
  try {
    const userId = req.user.id;
    const user = query.get('SELECT email_verified, phone_verified FROM users WHERE id = ?', [userId]);
    
    if (!user) {
      return res.status(404).json({ error: 'Пользователь не найден' });
    }
    
    res.json({
      emailVerified: user.email_verified === 1,
      phoneVerified: user.phone_verified === 1
    });
  } catch (error) {
    console.error('Ошибка получения статуса подтверждения:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

export default router;


