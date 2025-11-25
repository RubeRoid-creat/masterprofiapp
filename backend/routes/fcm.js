import express from 'express';
import { query } from '../database/db.js';
import { authenticate } from '../middleware/auth.js';

const router = express.Router();

/**
 * POST /api/fcm/register
 * Регистрация FCM токена пользователя
 */
router.post('/register', authenticate, (req, res) => {
  try {
    const { token, device_type: deviceType, device_id: deviceId } = req.body;
    
    console.log(`📥 Запрос на регистрацию FCM токена от пользователя #${req.user.id}`);
    console.log(`   Токен: ${token ? token.substring(0, 30) + '...' : 'НЕТ'}`);
    console.log(`   Device Type: ${deviceType || 'android'}`);
    console.log(`   Device ID: ${deviceId || 'не указан'}`);
    
    if (!token) {
      console.log('❌ Ошибка: токен не предоставлен');
      return res.status(400).json({ error: 'Токен обязателен' });
    }
    
    // Проверяем, есть ли уже такой токен
    const existing = query.get('SELECT id, user_id FROM fcm_tokens WHERE token = ?', [token]);
    
    if (existing) {
      // Если токен принадлежит другому пользователю, обновляем
      if (existing.user_id !== req.user.id) {
        query.run(
          'UPDATE fcm_tokens SET user_id = ?, device_type = ?, device_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
          [req.user.id, deviceType || 'android', deviceId || null, existing.id]
        );
        console.log(`🔄 FCM токен обновлен для пользователя #${req.user.id} (был #${existing.user_id})`);
      } else {
        // Токен уже зарегистрирован для этого пользователя, просто обновляем метаданные
        query.run(
          'UPDATE fcm_tokens SET device_type = ?, device_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
          [deviceType || 'android', deviceId || null, existing.id]
        );
        console.log(`✅ FCM токен обновлен для пользователя #${req.user.id}`);
      }
    } else {
      // Создаем новый токен
      query.run(
        'INSERT INTO fcm_tokens (user_id, token, device_type, device_id) VALUES (?, ?, ?, ?)',
        [req.user.id, token, deviceType || 'android', deviceId || null]
      );
      console.log(`✅ FCM токен зарегистрирован для пользователя #${req.user.id}`);
    }
    
    // Проверяем, что токен действительно сохранился
    const saved = query.get('SELECT id FROM fcm_tokens WHERE user_id = ? AND token = ?', [req.user.id, token]);
    if (saved) {
      console.log(`✅ Токен подтвержден в БД (ID: ${saved.id})`);
    } else {
      console.log(`⚠️ ВНИМАНИЕ: Токен не найден в БД после сохранения!`);
    }
    
    res.json({ message: 'Токен успешно зарегистрирован' });
  } catch (error) {
    console.error('❌ Ошибка регистрации FCM токена:', error);
    console.error('   Stack:', error.stack);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

/**
 * DELETE /api/fcm/unregister
 * Удаление FCM токена
 */
router.delete('/unregister', authenticate, (req, res) => {
  try {
    const { token } = req.body;
    
    if (!token) {
      return res.status(400).json({ error: 'Токен обязателен' });
    }
    
    // Удаляем токен только если он принадлежит текущему пользователю
    const result = query.run(
      'DELETE FROM fcm_tokens WHERE token = ? AND user_id = ?',
      [token, req.user.id]
    );
    
    if (result.changes > 0) {
      console.log(`🗑️ FCM токен удален для пользователя #${req.user.id}`);
      res.json({ message: 'Токен успешно удален' });
    } else {
      res.status(404).json({ error: 'Токен не найден' });
    }
  } catch (error) {
    console.error('Ошибка удаления FCM токена:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

/**
 * GET /api/fcm/tokens
 * Получить все токены текущего пользователя
 */
router.get('/tokens', authenticate, (req, res) => {
  try {
    const tokens = query.all(
      'SELECT id, token, device_type, device_id, created_at, updated_at FROM fcm_tokens WHERE user_id = ?',
      [req.user.id]
    );
    
    res.json(tokens);
  } catch (error) {
    console.error('Ошибка получения FCM токенов:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

export default router;

