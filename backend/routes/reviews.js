import express from 'express';
import { query } from '../database/db.js';
import { authenticate, authorize } from '../middleware/auth.js';
import { awardPointsForReview } from '../services/loyalty-service.js';

const router = express.Router();

// Получить отзывы мастера
router.get('/master/:masterId', authenticate, (req, res) => {
  try {
    const { masterId } = req.params;
    const { limit = 20, offset = 0 } = req.query;
    
    const reviews = query.all(`
      SELECT 
        r.*,
        u.name as client_name,
        o.order_number,
        o.device_type
      FROM reviews r
      JOIN users u ON r.client_id = (SELECT id FROM clients WHERE user_id = u.id)
      JOIN orders o ON r.order_id = o.id
      WHERE r.master_id = ?
      ORDER BY r.created_at DESC
      LIMIT ? OFFSET ?
    `, [masterId, parseInt(limit), parseInt(offset)]);
    
    // Получаем общее количество отзывов
    const totalCount = query.get(
      'SELECT COUNT(*) as count FROM reviews WHERE master_id = ?',
      [masterId]
    );
    
    // Получаем статистику рейтингов
    const ratingStats = query.all(`
      SELECT rating, COUNT(*) as count
      FROM reviews
      WHERE master_id = ?
      GROUP BY rating
      ORDER BY rating DESC
    `, [masterId]);
    
    // Вычисляем средний рейтинг
    const avgRating = query.get(
      'SELECT AVG(rating) as avg FROM reviews WHERE master_id = ?',
      [masterId]
    );
    
    res.json({
      reviews: reviews,
      totalCount: totalCount?.count || 0,
      averageRating: avgRating?.avg || 0,
      ratingDistribution: ratingStats.reduce((acc, stat) => {
        acc[stat.rating] = stat.count;
        return acc;
      }, {})
    });
  } catch (error) {
    console.error('Ошибка получения отзывов:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить отзывы клиента
router.get('/client/me', authenticate, authorize('client'), (req, res) => {
  try {
    const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
    if (!client) {
      return res.status(404).json({ error: 'Клиент не найден' });
    }
    
    const reviews = query.all(`
      SELECT 
        r.*,
        m.id as master_id,
        u.name as master_name,
        o.order_number,
        o.device_type
      FROM reviews r
      JOIN masters m ON r.master_id = m.id
      JOIN users u ON m.user_id = u.id
      JOIN orders o ON r.order_id = o.id
      WHERE r.client_id = ?
      ORDER BY r.created_at DESC
    `, [client.id]);
    
    res.json(reviews);
  } catch (error) {
    console.error('Ошибка получения отзывов клиента:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить отзыв по ID заказа
router.get('/order/:orderId', authenticate, (req, res) => {
  try {
    const { orderId } = req.params;
    
    const review = query.get(`
      SELECT 
        r.*,
        u.name as client_name
      FROM reviews r
      JOIN clients c ON r.client_id = c.id
      JOIN users u ON c.user_id = u.id
      WHERE r.order_id = ?
    `, [orderId]);
    
    if (!review) {
      return res.status(404).json({ error: 'Отзыв не найден' });
    }
    
    res.json(review);
  } catch (error) {
    console.error('Ошибка получения отзыва:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Создать отзыв
router.post('/', authenticate, authorize('client'), async (req, res) => {
  try {
    const { order_id: orderId, rating, comment } = req.body;
    
    if (!orderId || !rating) {
      return res.status(400).json({ error: 'Необходимо указать order_id и rating' });
    }
    
    if (rating < 1 || rating > 5) {
      return res.status(400).json({ error: 'Рейтинг должен быть от 1 до 5' });
    }
    
    // Проверяем, что заказ существует и принадлежит клиенту
    const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
    if (!client) {
      return res.status(404).json({ error: 'Клиент не найден' });
    }
    
    const order = query.get('SELECT * FROM orders WHERE id = ? AND client_id = ?', [orderId, client.id]);
    if (!order) {
      return res.status(404).json({ error: 'Заказ не найден или не принадлежит вам' });
    }
    
    // Проверяем, что заказ завершен
    if (order.repair_status !== 'completed' && order.request_status !== 'completed') {
      return res.status(400).json({ error: 'Можно оставить отзыв только на завершенный заказ' });
    }
    
    // Проверяем, что отзыв еще не оставлен
    const existingReview = query.get('SELECT * FROM reviews WHERE order_id = ?', [orderId]);
    if (existingReview) {
      return res.status(400).json({ error: 'Отзыв на этот заказ уже оставлен' });
    }
    
    // Проверяем, что заказ назначен мастеру
    if (!order.assigned_master_id) {
      return res.status(400).json({ error: 'Заказ не назначен мастеру' });
    }
    
    // Создаем отзыв
    const result = query.run(`
      INSERT INTO reviews (order_id, master_id, client_id, rating, comment)
      VALUES (?, ?, ?, ?, ?)
    `, [orderId, order.assigned_master_id, client.id, rating, comment || null]);
    
    // Обновляем рейтинг мастера
    const masterReviews = query.all(
      'SELECT rating FROM reviews WHERE master_id = ?',
      [order.assigned_master_id]
    );
    const avgRating = masterReviews.reduce((sum, r) => sum + r.rating, 0) / masterReviews.length;
    
    query.run(
      'UPDATE masters SET rating = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      [avgRating, order.assigned_master_id]
    );
    
    // Начисляем баллы лояльности клиенту за отзыв
    try {
      awardPointsForReview(client.id, result.lastInsertRowid);
    } catch (error) {
      console.error('Ошибка начисления баллов лояльности за отзыв:', error);
      // Продолжаем выполнение, даже если начисление баллов не удалось
    }
    
    // Получаем созданный отзыв
    const newReview = query.get('SELECT * FROM reviews WHERE id = ?', [result.lastInsertRowid]);
    
    res.status(201).json({
      message: 'Отзыв успешно создан',
      review: newReview
    });
  } catch (error) {
    console.error('Ошибка создания отзыва:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Обновить отзыв
router.put('/:id', authenticate, authorize('client'), (req, res) => {
  try {
    const { id } = req.params;
    const { rating, comment } = req.body;
    
    const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
    if (!client) {
      return res.status(404).json({ error: 'Клиент не найден' });
    }
    
    const review = query.get('SELECT * FROM reviews WHERE id = ? AND client_id = ?', [id, client.id]);
    if (!review) {
      return res.status(404).json({ error: 'Отзыв не найден' });
    }
    
    const updateFields = [];
    const updateValues = [];
    
    if (rating !== undefined) {
      if (rating < 1 || rating > 5) {
        return res.status(400).json({ error: 'Рейтинг должен быть от 1 до 5' });
      }
      updateFields.push('rating = ?');
      updateValues.push(rating);
    }
    
    if (comment !== undefined) {
      updateFields.push('comment = ?');
      updateValues.push(comment);
    }
    
    if (updateFields.length === 0) {
      return res.status(400).json({ error: 'Нет полей для обновления' });
    }
    
    updateValues.push(id);
    
    query.run(
      `UPDATE reviews SET ${updateFields.join(', ')} WHERE id = ?`,
      updateValues
    );
    
    // Обновляем рейтинг мастера
    const masterReviews = query.all(
      'SELECT rating FROM reviews WHERE master_id = ?',
      [review.master_id]
    );
    const avgRating = masterReviews.reduce((sum, r) => sum + r.rating, 0) / masterReviews.length;
    
    query.run(
      'UPDATE masters SET rating = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
      [avgRating, review.master_id]
    );
    
    const updatedReview = query.get('SELECT * FROM reviews WHERE id = ?', [id]);
    
    res.json({
      message: 'Отзыв успешно обновлен',
      review: updatedReview
    });
  } catch (error) {
    console.error('Ошибка обновления отзыва:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Удалить отзыв
router.delete('/:id', authenticate, authorize('client'), (req, res) => {
  try {
    const { id } = req.params;
    
    const client = query.get('SELECT id FROM clients WHERE user_id = ?', [req.user.id]);
    if (!client) {
      return res.status(404).json({ error: 'Клиент не найден' });
    }
    
    const review = query.get('SELECT * FROM reviews WHERE id = ? AND client_id = ?', [id, client.id]);
    if (!review) {
      return res.status(404).json({ error: 'Отзыв не найден' });
    }
    
    const masterId = review.master_id;
    
    query.run('DELETE FROM reviews WHERE id = ?', [id]);
    
    // Обновляем рейтинг мастера
    const masterReviews = query.all(
      'SELECT rating FROM reviews WHERE master_id = ?',
      [masterId]
    );
    
    if (masterReviews.length > 0) {
      const avgRating = masterReviews.reduce((sum, r) => sum + r.rating, 0) / masterReviews.length;
      query.run(
        'UPDATE masters SET rating = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
        [avgRating, masterId]
      );
    } else {
      // Если отзывов не осталось, сбрасываем рейтинг
      query.run(
        'UPDATE masters SET rating = 0, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
        [masterId]
      );
    }
    
    res.json({ message: 'Отзыв успешно удален' });
  } catch (error) {
    console.error('Ошибка удаления отзыва:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

export default router;





