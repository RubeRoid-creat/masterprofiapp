/**
 * Утилиты для оптимизации SQL запросов
 */

/**
 * Оптимизирует запрос с JOIN, добавляя только необходимые поля
 * @param {string} baseTable - Основная таблица
 * @param {Array} joins - Массив объектов {table, alias, condition, fields}
 * @param {Array} whereConditions - Условия WHERE
 * @param {string} orderBy - ORDER BY условие
 * @param {number} limit - LIMIT
 * @param {number} offset - OFFSET
 * @returns {Object} {sql, params}
 */
export function buildOptimizedQuery({
  baseTable,
  joins = [],
  whereConditions = [],
  orderBy = null,
  limit = null,
  offset = null,
  selectFields = ['*']
}) {
  let sql = `SELECT ${selectFields.join(', ')} FROM ${baseTable}`;
  const params = [];
  
  // Добавляем JOIN
  for (const join of joins) {
    sql += ` ${join.type || 'JOIN'} ${join.table} AS ${join.alias} ON ${join.condition}`;
  }
  
  // Добавляем WHERE условия
  if (whereConditions.length > 0) {
    sql += ' WHERE ' + whereConditions.map((cond, index) => {
      if (cond.operator === 'IN') {
        const placeholders = cond.values.map(() => '?').join(', ');
        params.push(...cond.values);
        return `${cond.field} ${cond.operator} (${placeholders})`;
      } else {
        params.push(cond.value);
        return `${cond.field} ${cond.operator} ?`;
      }
    }).join(' AND ');
  }
  
  // Добавляем ORDER BY
  if (orderBy) {
    sql += ` ORDER BY ${orderBy}`;
  }
  
  // Добавляем LIMIT и OFFSET
  if (limit) {
    sql += ' LIMIT ?';
    params.push(limit);
    if (offset) {
      sql += ' OFFSET ?';
      params.push(offset);
    }
  }
  
  return { sql, params };
}

/**
 * Оптимизирует запрос для получения заказов с фильтрами
 */
export function buildOrdersQuery(filters, pagination = {}) {
  const joins = [
    {
      type: 'JOIN',
      table: 'clients',
      alias: 'c',
      condition: 'o.client_id = c.id'
    },
    {
      type: 'JOIN',
      table: 'users',
      alias: 'u',
      condition: 'c.user_id = u.id'
    }
  ];
  
  const whereConditions = [];
  
  if (filters.clientId) {
    whereConditions.push({
      field: 'o.client_id',
      operator: '=',
      value: filters.clientId
    });
  }
  
  if (filters.masterId) {
    whereConditions.push({
      field: 'o.assigned_master_id',
      operator: '=',
      value: filters.masterId
    });
  }
  
  if (filters.status) {
    whereConditions.push({
      field: 'o.repair_status',
      operator: '=',
      value: filters.status
    });
  }
  
  if (filters.deviceType) {
    whereConditions.push({
      field: 'o.device_type',
      operator: '=',
      value: filters.deviceType
    });
  }
  
  if (filters.urgency) {
    whereConditions.push({
      field: 'o.urgency',
      operator: '=',
      value: filters.urgency
    });
  }
  
  const selectFields = [
    'o.*',
    'u.name as client_name',
    'u.phone as client_phone'
  ];
  
  let orderBy = 'o.created_at DESC';
  if (filters.sortBy === 'price') {
    orderBy = 'o.estimated_cost DESC, o.created_at DESC';
  } else if (filters.sortBy === 'urgency') {
    orderBy = `CASE 
      WHEN o.urgency = 'emergency' THEN 1
      WHEN o.urgency = 'urgent' THEN 2
      WHEN o.urgency = 'planned' THEN 3
      ELSE 4
    END, o.created_at DESC`;
  }
  
  return buildOptimizedQuery({
    baseTable: 'orders o',
    joins,
    whereConditions,
    orderBy,
    limit: pagination.limit,
    offset: pagination.offset,
    selectFields
  });
}

/**
 * Оптимизирует запрос для получения мастеров с фильтрами
 */
export function buildMastersQuery(filters, pagination = {}) {
  const joins = [
    {
      type: 'JOIN',
      table: 'users',
      alias: 'u',
      condition: 'm.user_id = u.id'
    }
  ];
  
  const whereConditions = [];
  
  if (filters.status) {
    whereConditions.push({
      field: 'm.status',
      operator: '=',
      value: filters.status
    });
  }
  
  if (filters.isOnShift !== undefined) {
    whereConditions.push({
      field: 'm.is_on_shift',
      operator: '=',
      value: filters.isOnShift ? 1 : 0
    });
  }
  
  if (filters.hasLocation) {
    whereConditions.push({
      field: 'm.latitude',
      operator: 'IS NOT',
      value: null
    });
    whereConditions.push({
      field: 'm.longitude',
      operator: 'IS NOT',
      value: null
    });
  }
  
  const selectFields = [
    'm.*',
    'u.name',
    'u.phone',
    'u.email'
  ];
  
  const orderBy = 'm.rating DESC';
  
  return buildOptimizedQuery({
    baseTable: 'masters m',
    joins,
    whereConditions,
    orderBy,
    limit: pagination.limit,
    offset: pagination.offset,
    selectFields
  });
}

/**
 * Проверяет, нужно ли использовать индекс для запроса
 */
export function shouldUseIndex(field, value) {
  // Используем индекс для часто фильтруемых полей
  const indexedFields = [
    'client_id',
    'assigned_master_id',
    'repair_status',
    'device_type',
    'urgency',
    'status',
    'user_id'
  ];
  
  return indexedFields.includes(field) && value !== null && value !== undefined;
}



