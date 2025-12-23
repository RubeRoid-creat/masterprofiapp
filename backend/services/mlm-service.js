import { query } from '../database/db.js';

/**
 * MLM Service - бизнес-логика для многоуровневой маркетинговой системы
 */

// Константы комиссий
const COMMISSION_RATES = {
  LEVEL_1: 0.03, // 3% с первого уровня
  LEVEL_2: 0.02, // 2% со второго уровня
  LEVEL_3: 0.01  // 1% с третьего уровня
};

// Минимальная активность для получения комиссии
const MIN_ACTIVITY = {
  LEVEL_1: 5, // минимум 5 заказов/месяц для 1 уровня
  LEVEL_2: 3  // минимум 3 заказа/месяц для 2 уровня
};

/**
 * Получить цепочку спонсоров (upline) для пользователя
 * @param {number} userId - ID пользователя
 * @param {number} maxLevel - Максимальный уровень глубины (по умолчанию 3)
 * @returns {Array} Массив объектов {userId, sponsorId, level}
 */
export function getUplineChain(userId, maxLevel = 3) {
  try {
    const upline = [];
    let currentUserId = userId;
    let level = 0;

    while (level < maxLevel) {
      const user = query.get('SELECT sponsor_id FROM users WHERE id = ?', [currentUserId]);
      
      if (!user || !user.sponsor_id) {
        break; // Достигли вершины структуры
      }

      // Проверяем, что спонсор является мастером
      const sponsor = query.get(`
        SELECT u.id, u.sponsor_id, m.id as master_id 
        FROM users u
        LEFT JOIN masters m ON m.user_id = u.id
        WHERE u.id = ?
      `, [user.sponsor_id]);

      if (!sponsor || !sponsor.master_id) {
        break; // Спонсор не является мастером
      }

      level++;
      upline.push({
        userId: sponsor.id,
        sponsorId: user.sponsor_id,
        level: level
      });

      currentUserId = user.sponsor_id;
    }

    return upline;
  } catch (error) {
    console.error('Ошибка получения upline цепочки:', error);
    return [];
  }
}

/**
 * Проверить активность мастера за последние 30 дней
 * @param {number} masterId - ID мастера
 * @param {number} minOrders - Минимальное количество заказов
 * @returns {boolean}
 */
export function checkMasterActivity(masterId, minOrders = 0) {
  try {
    if (minOrders === 0) return true; // Для уровня 3 активности не требуется

    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

    const result = query.get(`
      SELECT COUNT(*) as count
      FROM orders o
      JOIN masters m ON m.id = o.assigned_master_id
      WHERE m.id = ? 
        AND o.repair_status = 'completed'
        AND o.updated_at >= datetime(?)
    `, [masterId, thirtyDaysAgo.toISOString()]);

    return (result?.count || 0) >= minOrders;
  } catch (error) {
    console.error('Ошибка проверки активности мастера:', error);
    return false;
  }
}

/**
 * Рассчитать и начислить MLM комиссии при завершении заказа
 * @param {number} orderId - ID заказа
 * @param {number} masterId - ID мастера, выполнившего заказ
 * @param {number} orderAmount - Сумма заказа
 */
export function calculateMLMCommissions(orderId, masterId, orderAmount) {
  try {
    if (!orderId || !masterId || !orderAmount || orderAmount <= 0) {
      console.log('⚠️ Некорректные параметры для расчета MLM комиссий');
      return;
    }

    // Получаем user_id мастера, выполнившего заказ
    const master = query.get('SELECT user_id FROM masters WHERE id = ?', [masterId]);
    if (!master) {
      console.log(`⚠️ Мастер #${masterId} не найден`);
      return;
    }

    const executingMasterUserId = master.user_id;

    // Получаем цепочку спонсоров
    const upline = getUplineChain(executingMasterUserId, 3);

    if (upline.length === 0) {
      console.log(`ℹ️ Нет upline для мастера #${masterId}, комиссии не начисляются`);
      return;
    }

    console.log(`💰 Расчет MLM комиссий для заказа #${orderId}, сумма: ${orderAmount} ₽`);
    console.log(`📊 Найдено ${upline.length} уровней в upline`);

    // Начисляем комиссии для каждого уровня
    for (const sponsorInfo of upline) {
      const { userId: sponsorUserId, level } = sponsorInfo;

      // Получаем master_id спонсора
      const sponsorMaster = query.get('SELECT id FROM masters WHERE user_id = ?', [sponsorUserId]);
      if (!sponsorMaster) {
        console.log(`⚠️ Спонсор user_id=${sponsorUserId} не является мастером, пропускаем`);
        continue;
      }

      const sponsorMasterId = sponsorMaster.id;

      // Проверяем активность для уровня 1 и 2
      let isActive = true;
      if (level === 1) {
        isActive = checkMasterActivity(sponsorMasterId, MIN_ACTIVITY.LEVEL_1);
      } else if (level === 2) {
        isActive = checkMasterActivity(sponsorMasterId, MIN_ACTIVITY.LEVEL_2);
      }
      // Для уровня 3 активность не требуется

      if (!isActive) {
        console.log(`⚠️ Спонсор master_id=${sponsorMasterId} (уровень ${level}) неактивен, комиссия не начисляется`);
        continue;
      }

      // Определяем процент комиссии
      let commissionRate = 0;
      if (level === 1) {
        commissionRate = COMMISSION_RATES.LEVEL_1;
      } else if (level === 2) {
        commissionRate = COMMISSION_RATES.LEVEL_2;
      } else if (level === 3) {
        commissionRate = COMMISSION_RATES.LEVEL_3;
      }

      const commissionAmount = orderAmount * commissionRate;

      // Сохраняем комиссию в базу данных
      query.run(`
        INSERT INTO mlm_commissions 
        (order_id, from_user_id, to_user_id, amount, commission_rate, commission_amount, level, commission_type, status, description)
        VALUES (?, ?, ?, ?, ?, ?, ?, 'referral', 'pending', ?)
      `, [
        orderId,
        executingMasterUserId,
        sponsorUserId,
        orderAmount,
        commissionRate,
        commissionAmount,
        level,
        `Комиссия ${(commissionRate * 100).toFixed(0)}% за заказ #${orderId} (уровень ${level})`
      ]);

      // Начисляем комиссию на баланс мастера-спонсора
      query.run(`
        UPDATE masters 
        SET balance = COALESCE(balance, 0) + ?, updated_at = CURRENT_TIMESTAMP 
        WHERE id = ?
      `, [commissionAmount, sponsorMasterId]);

      // Создаем транзакцию для мастера
      query.run(`
        INSERT INTO master_transactions 
        (master_id, order_id, transaction_type, amount, status, description)
        VALUES (?, ?, 'commission', ?, 'completed', ?)
      `, [
        sponsorMasterId,
        orderId,
        commissionAmount,
        `MLM комиссия ${(commissionRate * 100).toFixed(0)}% за заказ #${orderId} (уровень ${level})`
      ]);

      // Обновляем статус комиссии на "completed"
      query.run(`
        UPDATE mlm_commissions 
        SET status = 'completed', completed_at = CURRENT_TIMESTAMP
        WHERE order_id = ? AND to_user_id = ? AND level = ?
      `, [orderId, sponsorUserId, level]);

      console.log(`✅ Начислена комиссия ${commissionAmount.toFixed(2)} ₽ (${(commissionRate * 100).toFixed(0)}%) мастеру #${sponsorMasterId} (уровень ${level})`);
    }

    console.log(`✅ MLM комиссии рассчитаны для заказа #${orderId}`);
  } catch (error) {
    console.error('❌ Ошибка расчета MLM комиссий:', error);
    // Не прерываем выполнение, просто логируем ошибку
  }
}

/**
 * Построить структуру сети (downline) для мастера
 * @param {number} masterUserId - user_id мастера
 * @param {number} maxLevel - Максимальная глубина
 * @returns {Object} Структура сети
 */
export function getDownlineStructure(masterUserId, maxLevel = 3) {
  try {
    const structure = {
      level_1: [],
      level_2: [],
      level_3: [],
      total_members: 0,
      active_members: 0
    };

    // Получаем прямых рефералов (уровень 1)
    const level1 = query.all(`
      SELECT u.id as user_id, u.name, u.email, m.id as master_id, m.rating, m.completed_orders,
             u.created_at, m.verification_status
      FROM users u
      JOIN masters m ON m.user_id = u.id
      WHERE u.sponsor_id = ?
    `, [masterUserId]);

    structure.level_1 = level1.map(member => ({
      ...member,
      activity: checkMasterActivity(member.master_id, 1) ? 'active' : 'inactive'
    }));

    // Получаем уровень 2 (рефералы рефералов)
    if (maxLevel >= 2 && level1.length > 0) {
      const level1UserIds = level1.map(m => m.user_id);
      const placeholders = level1UserIds.map(() => '?').join(',');
      
      const level2 = query.all(`
        SELECT u.id as user_id, u.name, u.email, m.id as master_id, m.rating, m.completed_orders,
               u.created_at, u.sponsor_id, m.verification_status
        FROM users u
        JOIN masters m ON m.user_id = u.id
        WHERE u.sponsor_id IN (${placeholders})
      `, level1UserIds);

      structure.level_2 = level2.map(member => ({
        ...member,
        activity: checkMasterActivity(member.master_id, 1) ? 'active' : 'inactive'
      }));
    }

    // Получаем уровень 3
    if (maxLevel >= 3 && structure.level_2.length > 0) {
      const level2UserIds = structure.level_2.map(m => m.user_id);
      const placeholders = level2UserIds.map(() => '?').join(',');
      
      const level3 = query.all(`
        SELECT u.id as user_id, u.name, u.email, m.id as master_id, m.rating, m.completed_orders,
               u.created_at, u.sponsor_id, m.verification_status
        FROM users u
        JOIN masters m ON m.user_id = u.id
        WHERE u.sponsor_id IN (${placeholders})
      `, level2UserIds);

      structure.level_3 = level3.map(member => ({
        ...member,
        activity: checkMasterActivity(member.master_id, 1) ? 'active' : 'inactive'
      }));
    }

    // Подсчитываем статистику
    structure.total_members = structure.level_1.length + structure.level_2.length + structure.level_3.length;
    structure.active_members = [
      ...structure.level_1,
      ...structure.level_2,
      ...structure.level_3
    ].filter(m => m.activity === 'active').length;

    return structure;
  } catch (error) {
    console.error('Ошибка получения структуры сети:', error);
    return {
      level_1: [],
      level_2: [],
      level_3: [],
      total_members: 0,
      active_members: 0
    };
  }
}

/**
 * Получить статистику MLM для мастера
 * @param {number} masterUserId - user_id мастера
 * @returns {Object} Статистика MLM
 */
export function getMLMStatistics(masterUserId) {
  try {
    // Проверяем, существует ли пользователь
    const user = query.get('SELECT id, rank, created_at FROM users WHERE id = ?', [masterUserId]);
    if (!user) {
      return null; // Пользователь не существует
    }

    // Проверяем, является ли пользователь мастером
    const master = query.get('SELECT id as master_id FROM masters WHERE user_id = ?', [masterUserId]);
    
    // Если мастера нет, создаем базовую статистику
    const masterId = master?.master_id || null;

    // Получаем структуру сети
    const downline = getDownlineStructure(masterUserId, 3);

    // Получаем статистику комиссий за последние 30 дней
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

    const commissions30Days = query.get(`
      SELECT 
        COUNT(*) as total_commissions,
        COALESCE(SUM(commission_amount), 0) as total_amount
      FROM mlm_commissions
      WHERE to_user_id = ? 
        AND status = 'completed'
        AND created_at >= datetime(?)
    `, [masterUserId, thirtyDaysAgo.toISOString()]);

    // Получаем общую статистику комиссий
    const totalCommissions = query.get(`
      SELECT 
        COUNT(*) as total_commissions,
        COALESCE(SUM(commission_amount), 0) as total_amount
      FROM mlm_commissions
      WHERE to_user_id = ? 
        AND status = 'completed'
    `, [masterUserId]);

    // Получаем комиссии по уровням
    const commissionsByLevel = query.all(`
      SELECT 
        level,
        COUNT(*) as count,
        COALESCE(SUM(commission_amount), 0) as amount
      FROM mlm_commissions
      WHERE to_user_id = ? 
        AND status = 'completed'
      GROUP BY level
    `, [masterUserId]);

    return {
      master_id: masterId,
      user_id: user.id,
      rank: user.rank || 'junior_master',
      join_date: user.created_at || new Date().toISOString(),
      downline: {
        level_1: downline.level_1.length,
        level_2: downline.level_2.length,
        level_3: downline.level_3.length,
        total: downline.total_members,
        active: downline.active_members
      },
      commissions: {
        last_30_days: {
          count: commissions30Days?.total_commissions || 0,
          amount: commissions30Days?.total_amount || 0
        },
        total: {
          count: totalCommissions?.total_commissions || 0,
          amount: totalCommissions?.total_amount || 0
        },
        by_level: commissionsByLevel.reduce((acc, item) => {
          acc[`level_${item.level}`] = {
            count: item.count,
            amount: item.amount
          };
          return acc;
        }, {})
      }
    };
  } catch (error) {
    console.error('Ошибка получения MLM статистики:', error);
    return null;
  }
}

/**
 * Пригласить нового мастера (установить спонсора)
 * @param {number} newUserId - user_id нового пользователя
 * @param {number} sponsorUserId - user_id спонсора
 * @returns {boolean} Успех операции
 */
export function inviteMaster(newUserId, sponsorUserId) {
  try {
    // Проверяем, что спонсор существует и является мастером
    const sponsor = query.get(`
      SELECT u.id, m.id as master_id
      FROM users u
      JOIN masters m ON m.user_id = u.id
      WHERE u.id = ?
    `, [sponsorUserId]);

    if (!sponsor || !sponsor.master_id) {
      throw new Error('Спонсор не найден или не является мастером');
    }

    // Проверяем, что новый пользователь еще не имеет спонсора
    const newUser = query.get('SELECT sponsor_id FROM users WHERE id = ?', [newUserId]);
    if (newUser?.sponsor_id) {
      throw new Error('У пользователя уже есть спонсор');
    }

    // Проверяем, что пользователь не приглашает сам себя
    if (newUserId === sponsorUserId) {
      throw new Error('Нельзя пригласить самого себя');
    }

    // Устанавливаем спонсора
    query.run('UPDATE users SET sponsor_id = ? WHERE id = ?', [sponsorUserId, newUserId]);

    // Строим структуру сети для сохранения в network_structure
    const upline = getUplineChain(newUserId, 3);
    
    // Сохраняем связи в network_structure
    for (const link of upline) {
      query.run(`
        INSERT OR IGNORE INTO network_structure (user_id, sponsor_id, level)
        VALUES (?, ?, ?)
      `, [newUserId, link.sponsorId, link.level]);
    }

    console.log(`✅ Мастер user_id=${newUserId} приглашен спонсором user_id=${sponsorUserId}`);
    return true;
  } catch (error) {
    console.error('Ошибка приглашения мастера:', error);
    throw error;
  }
}

