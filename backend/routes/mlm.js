import express from 'express';
import { authenticate, authorize } from '../middleware/auth.js';
import { query } from '../database/db.js';
import {
  getUplineChain,
  getDownlineStructure,
  getMLMStatistics,
  inviteMaster,
  calculateMLMCommissions
} from '../services/mlm-service.js';

const router = express.Router();

/**
 * GET /api/mlm/structure
 * Получить структуру сети мастера (downline)
 */
router.get('/structure', authenticate, authorize('master'), (req, res) => {
  try {
    const master = query.get('SELECT user_id FROM masters WHERE user_id = ?', [req.user.id]);
    if (!master) {
      return res.status(404).json({ error: 'Профиль мастера не найден' });
    }

    const structure = getDownlineStructure(req.user.id, 3);
    
    res.json({
      success: true,
      structure
    });
  } catch (error) {
    console.error('Ошибка получения структуры сети:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

/**
 * GET /api/mlm/statistics
 * Получить MLM статистику мастера
 */
router.get('/statistics', authenticate, authorize('master'), (req, res) => {
  try {
    const statistics = getMLMStatistics(req.user.id);
    
    // Возвращаем пустую статистику, если данных нет (новый пользователь)
    const defaultStatistics = {
      master_id: null,
      user_id: req.user.id,
      rank: 'junior_master',
      join_date: new Date().toISOString(),
      downline: {
        level_1: 0,
        level_2: 0,
        level_3: 0,
        total: 0,
        active: 0
      },
      commissions: {
        last_30_days: {
          count: 0,
          amount: 0
        },
        total: {
          count: 0,
          amount: 0
        },
        by_level: {}
      }
    };

    res.json({
      success: true,
      statistics: statistics || defaultStatistics
    });
  } catch (error) {
    console.error('Ошибка получения MLM статистики:', error);
    res.status(500).json({ error: 'Ошибка сервера: ' + error.message });
  }
});

/**
 * GET /api/mlm/commissions
 * Получить историю комиссий мастера
 */
router.get('/commissions', authenticate, authorize('master'), (req, res) => {
  try {
    const { limit = 50, offset = 0, start_date, end_date } = req.query;

    let sql = `
      SELECT 
        mc.*,
        o.order_number,
        o.final_cost,
        u_from.name as from_master_name,
        u_to.name as to_master_name
      FROM mlm_commissions mc
      JOIN orders o ON o.id = mc.order_id
      JOIN users u_from ON u_from.id = mc.from_user_id
      JOIN users u_to ON u_to.id = mc.to_user_id
      WHERE mc.to_user_id = ?
    `;

    const params = [req.user.id];

    if (start_date) {
      sql += ' AND mc.created_at >= datetime(?)';
      params.push(start_date);
    }

    if (end_date) {
      sql += ' AND mc.created_at <= datetime(?)';
      params.push(end_date);
    }

    sql += ' ORDER BY mc.created_at DESC LIMIT ? OFFSET ?';
    params.push(parseInt(limit), parseInt(offset));

    const commissions = query.all(sql, params);

    // Получаем общее количество
    let countSql = 'SELECT COUNT(*) as total FROM mlm_commissions WHERE to_user_id = ?';
    const countParams = [req.user.id];

    if (start_date) {
      countSql += ' AND created_at >= datetime(?)';
      countParams.push(start_date);
    }

    if (end_date) {
      countSql += ' AND created_at <= datetime(?)';
      countParams.push(end_date);
    }

    const total = query.get(countSql, countParams);

    res.json({
      success: true,
      commissions,
      pagination: {
        total: total?.total || 0,
        limit: parseInt(limit),
        offset: parseInt(offset)
      }
    });
  } catch (error) {
    console.error('Ошибка получения истории комиссий:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

/**
 * POST /api/mlm/invite
 * Пригласить нового мастера (установить спонсора)
 */
router.post('/invite', authenticate, authorize('master'), (req, res) => {
  try {
    const { user_id, email } = req.body;

    if (!user_id && !email) {
      return res.status(400).json({ error: 'Укажите user_id или email пользователя' });
    }

    let newUserId = user_id;

    // Если передан email, находим user_id
    if (!newUserId && email) {
      const user = query.get('SELECT id FROM users WHERE email = ?', [email]);
      if (!user) {
        return res.status(404).json({ error: 'Пользователь с таким email не найден' });
      }
      newUserId = user.id;
    }

    // Проверяем, что пользователь является мастером
    const newMaster = query.get('SELECT id FROM masters WHERE user_id = ?', [newUserId]);
    if (!newMaster) {
      return res.status(400).json({ error: 'Пользователь не является мастером' });
    }

    inviteMaster(newUserId, req.user.id);

    res.json({
      success: true,
      message: 'Мастер успешно приглашен'
    });
  } catch (error) {
    console.error('Ошибка приглашения мастера:', error);
    res.status(400).json({ error: error.message || 'Ошибка приглашения мастера' });
  }
});

/**
 * GET /api/mlm/my-referral-code
 * Получить реферальный код мастера (для приглашений)
 */
router.get('/my-referral-code', authenticate, authorize('master'), (req, res) => {
  try {
    // Реферальный код - это просто user_id или можно сделать более сложный
    const referralCode = `REF-${req.user.id}`;
    const referralLink = `${process.env.APP_URL || 'https://masterprofi.ru'}/invite/${req.user.id}`;

    res.json({
      success: true,
      referral_code: referralCode,
      referral_link: referralLink,
      user_id: req.user.id
    });
  } catch (error) {
    console.error('Ошибка получения реферального кода:', error);
    res.status(500).json({ error: 'Ошибка сервера: ' + error.message });
  }
});

/**
 * GET /api/mlm/upline
 * Получить цепочку спонсоров (upline) мастера
 */
router.get('/upline', authenticate, authorize('master'), (req, res) => {
  try {
    const upline = getUplineChain(req.user.id, 3);

    // Получаем дополнительную информацию о спонсорах
    const uplineWithDetails = upline.map(item => {
      const sponsor = query.get(`
        SELECT u.id, u.name, u.email, m.id as master_id, m.rating, m.completed_orders, u.rank
        FROM users u
        LEFT JOIN masters m ON m.user_id = u.id
        WHERE u.id = ?
      `, [item.userId]);

      return {
        ...item,
        sponsor_info: sponsor
      };
    });

    res.json({
      success: true,
      upline: uplineWithDetails
    });
  } catch (error) {
    console.error('Ошибка получения upline:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

/**
 * GET /api/mlm/team-performance
 * Получить производительность команды
 */
router.get('/team-performance', authenticate, authorize('master'), (req, res) => {
  try {
    const { period = 30 } = req.query; // Период в днях

    const periodDate = new Date();
    periodDate.setDate(periodDate.getDate() - parseInt(period));

    // Получаем структуру сети
    const structure = getDownlineStructure(req.user.id, 3);

    // Получаем статистику заказов команды за период
    const allTeamMemberIds = [
      ...structure.level_1.map(m => m.master_id),
      ...structure.level_2.map(m => m.master_id),
      ...structure.level_3.map(m => m.master_id)
    ];

    let teamStats = {
      total_orders: 0,
      total_revenue: 0,
      active_members: 0,
      by_level: {
        level_1: { orders: 0, revenue: 0, active: 0 },
        level_2: { orders: 0, revenue: 0, active: 0 },
        level_3: { orders: 0, revenue: 0, active: 0 }
      }
    };

    if (allTeamMemberIds.length > 0) {
      const placeholders = allTeamMemberIds.map(() => '?').join(',');
      
      const orders = query.all(`
        SELECT 
          o.assigned_master_id,
          o.final_cost,
          COUNT(*) as order_count
        FROM orders o
        WHERE o.assigned_master_id IN (${placeholders})
          AND o.repair_status = 'completed'
          AND o.updated_at >= datetime(?)
        GROUP BY o.assigned_master_id
      `, [...allTeamMemberIds, periodDate.toISOString()]);

      // Подсчитываем статистику по уровням
      orders.forEach(order => {
        teamStats.total_orders += order.order_count || 0;
        teamStats.total_revenue += (order.final_cost || 0) * (order.order_count || 0);

        // Определяем уровень
        if (structure.level_1.some(m => m.master_id === order.assigned_master_id)) {
          teamStats.by_level.level_1.orders += order.order_count || 0;
          teamStats.by_level.level_1.revenue += (order.final_cost || 0) * (order.order_count || 0);
        } else if (structure.level_2.some(m => m.master_id === order.assigned_master_id)) {
          teamStats.by_level.level_2.orders += order.order_count || 0;
          teamStats.by_level.level_2.revenue += (order.final_cost || 0) * (order.order_count || 0);
        } else if (structure.level_3.some(m => m.master_id === order.assigned_master_id)) {
          teamStats.by_level.level_3.orders += order.order_count || 0;
          teamStats.by_level.level_3.revenue += (order.final_cost || 0) * (order.order_count || 0);
        }
      });

      // Подсчитываем активных участников
      teamStats.active_members = structure.active_members;
      teamStats.by_level.level_1.active = structure.level_1.filter(m => m.activity === 'active').length;
      teamStats.by_level.level_2.active = structure.level_2.filter(m => m.activity === 'active').length;
      teamStats.by_level.level_3.active = structure.level_3.filter(m => m.activity === 'active').length;
    }

    res.json({
      success: true,
      period_days: parseInt(period),
      team_performance: teamStats
    });
  } catch (error) {
    console.error('Ошибка получения производительности команды:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

export default router;

