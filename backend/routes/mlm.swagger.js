/**
 * @swagger
 * /api/mlm/structure:
 *   get:
 *     tags:
 *       - MLM
 *     summary: Получить MLM структуру
 *     description: Возвращает иерархию рефералов и структуру команды
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: depth
 *         schema:
 *           type: integer
 *           default: 3
 *         description: Глубина иерархии (максимум 3 уровня)
 *     responses:
 *       200:
 *         description: MLM структура
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 user_id:
 *                   type: integer
 *                   example: 1
 *                 referral_code:
 *                   type: string
 *                   example: "REF123ABC"
 *                 rank:
 *                   type: string
 *                   example: "Старший мастер"
 *                 team_size:
 *                   type: integer
 *                   example: 15
 *                 direct_referrals:
 *                   type: array
 *                   items:
 *                     type: object
 *                     properties:
 *                       user_id:
 *                         type: integer
 *                       name:
 *                         type: string
 *                       level:
 *                         type: integer
 *                       completed_orders:
 *                         type: integer
 *                       monthly_volume:
 *                         type: number
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/mlm/invite:
 *   post:
 *     tags:
 *       - MLM
 *     summary: Пригласить нового мастера
 *     description: Генерирует реферальную ссылку для приглашения нового мастера
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               name:
 *                 type: string
 *                 example: Иван Петров
 *               phone:
 *                 type: string
 *                 example: +79001234567
 *               email:
 *                 type: string
 *                 format: email
 *                 example: ivan@example.com
 *     responses:
 *       200:
 *         description: Приглашение создано
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 message:
 *                   type: string
 *                   example: Приглашение отправлено
 *                 referral_link:
 *                   type: string
 *                   example: https://masterprofi.ru/register?ref=REF123ABC
 *                 referral_code:
 *                   type: string
 *                   example: REF123ABC
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/mlm/commissions:
 *   get:
 *     tags:
 *       - MLM
 *     summary: Получить историю комиссий
 *     description: Возвращает историю начисленных комиссий с рефералов
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: period
 *         schema:
 *           type: string
 *           enum: [day, week, month, year, all]
 *           default: month
 *         description: Период для отчета
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 50
 *         description: Количество записей на странице
 *       - in: query
 *         name: offset
 *         schema:
 *           type: integer
 *           default: 0
 *         description: Смещение для пагинации
 *     responses:
 *       200:
 *         description: История комиссий
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 total_earned:
 *                   type: number
 *                   example: 25000.00
 *                 commissions:
 *                   type: array
 *                   items:
 *                     type: object
 *                     properties:
 *                       id:
 *                         type: integer
 *                       order_id:
 *                         type: integer
 *                       from_user_id:
 *                         type: integer
 *                       from_user_name:
 *                         type: string
 *                       amount:
 *                         type: number
 *                       level:
 *                         type: integer
 *                         description: Уровень в иерархии (1-3)
 *                       commission_rate:
 *                         type: number
 *                         example: 0.03
 *                       created_at:
 *                         type: string
 *                         format: date-time
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/mlm/statistics:
 *   get:
 *     tags:
 *       - MLM
 *     summary: Получить MLM статистику
 *     description: Возвращает статистику по MLM команде и доходам
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: MLM статистика
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 rank:
 *                   type: string
 *                   example: "Старший мастер"
 *                 team_size:
 *                   type: integer
 *                   example: 15
 *                 active_members:
 *                   type: integer
 *                   example: 12
 *                 team_volume_month:
 *                   type: number
 *                   example: 300000.00
 *                 total_commissions:
 *                   type: number
 *                   example: 45000.00
 *                 next_rank:
 *                   type: string
 *                   example: "Лидер команды"
 *                 next_rank_requirements:
 *                   type: object
 *                   properties:
 *                     completed_orders:
 *                       type: object
 *                       properties:
 *                         current:
 *                           type: integer
 *                         required:
 *                           type: integer
 *                     team_size:
 *                       type: object
 *                       properties:
 *                         current:
 *                           type: integer
 *                         required:
 *                           type: integer
 *                     team_volume:
 *                       type: object
 *                       properties:
 *                         current:
 *                           type: number
 *                         required:
 *                           type: number
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/mlm/team-performance:
 *   get:
 *     tags:
 *       - MLM
 *     summary: Получить производительность команды
 *     description: Возвращает детальную статистику по членам команды
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: level
 *         schema:
 *           type: integer
 *           enum: [1, 2, 3]
 *         description: Фильтр по уровню в иерархии
 *       - in: query
 *         name: sort_by
 *         schema:
 *           type: string
 *           enum: [volume, orders, rating, join_date]
 *           default: volume
 *         description: Сортировка
 *     responses:
 *       200:
 *         description: Производительность команды
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 type: object
 *                 properties:
 *                   user_id:
 *                     type: integer
 *                   name:
 *                     type: string
 *                   level:
 *                     type: integer
 *                   completed_orders:
 *                     type: integer
 *                   monthly_volume:
 *                     type: number
 *                   rating:
 *                     type: number
 *                   is_active:
 *                     type: boolean
 *                   join_date:
 *                     type: string
 *                     format: date-time
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/mlm/ranks:
 *   get:
 *     tags:
 *       - MLM
 *     summary: Получить информацию о рангах
 *     description: Возвращает список всех доступных рангов и их требования
 *     responses:
 *       200:
 *         description: Список рангов
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 type: object
 *                 properties:
 *                   rank_name:
 *                     type: string
 *                     example: "Младший мастер"
 *                   requirements:
 *                     type: object
 *                     properties:
 *                       completed_orders:
 *                         type: integer
 *                       rating:
 *                         type: number
 *                       team_size:
 *                         type: integer
 *                   benefits:
 *                     type: array
 *                     items:
 *                       type: string
 *                     example: ["Базовые комиссии", "Доступ к заказам"]
 *       500:
 *         description: Ошибка сервера
 */
