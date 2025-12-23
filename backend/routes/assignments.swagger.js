/**
 * @swagger
 * /api/assignments/my:
 *   get:
 *     tags:
 *       - Назначения
 *     summary: Получить мои назначения
 *     description: Возвращает список заказов, назначенных текущему мастеру
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: status
 *         schema:
 *           type: string
 *           enum: [pending, accepted, rejected, expired]
 *         description: Фильтр по статусу назначения
 *     responses:
 *       200:
 *         description: Список назначений
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 type: object
 *                 properties:
 *                   id:
 *                     type: integer
 *                   order_id:
 *                     type: integer
 *                   order:
 *                     $ref: '#/components/schemas/Order'
 *                   status:
 *                     type: string
 *                     enum: [pending, accepted, rejected, expired]
 *                   assigned_at:
 *                     type: string
 *                     format: date-time
 *                   expires_at:
 *                     type: string
 *                     format: date-time
 *                   time_remaining:
 *                     type: integer
 *                     description: Оставшееся время в секундах
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/assignments/{id}/accept:
 *   post:
 *     tags:
 *       - Назначения
 *     summary: Принять заказ
 *     description: |
 *       Мастер принимает назначенный заказ. 
 *       Важно: У мастера есть 3-5 минут на принятие, иначе заказ переназначается.
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID назначения
 *     responses:
 *       200:
 *         description: Заказ успешно принят
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 message:
 *                   type: string
 *                   example: Заказ успешно принят
 *                 assignment:
 *                   type: object
 *                   properties:
 *                     id:
 *                       type: integer
 *                     order_id:
 *                       type: integer
 *                     status:
 *                       type: string
 *                       example: accepted
 *                 order:
 *                   $ref: '#/components/schemas/Order'
 *       400:
 *         description: Заказ уже принят или время истекло
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       404:
 *         $ref: '#/components/responses/NotFoundError'
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/assignments/{id}/reject:
 *   post:
 *     tags:
 *       - Назначения
 *     summary: Отклонить заказ
 *     description: |
 *       Мастер отклоняет назначенный заказ.
 *       Заказ будет автоматически переназначен другому доступному мастеру.
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID назначения
 *     requestBody:
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               reason:
 *                 type: string
 *                 example: Не могу выполнить в указанное время
 *                 description: Причина отклонения (опционально)
 *     responses:
 *       200:
 *         description: Заказ успешно отклонен
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 message:
 *                   type: string
 *                   example: Заказ отклонен
 *       400:
 *         description: Заказ уже обработан
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       404:
 *         $ref: '#/components/responses/NotFoundError'
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/assignments/statistics:
 *   get:
 *     tags:
 *       - Назначения
 *     summary: Статистика по назначениям
 *     description: Возвращает статистику принятия/отклонения заказов мастером
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: period
 *         schema:
 *           type: string
 *           enum: [day, week, month, year, all]
 *           default: month
 *         description: Период для статистики
 *     responses:
 *       200:
 *         description: Статистика назначений
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 total_assignments:
 *                   type: integer
 *                   example: 50
 *                 accepted:
 *                   type: integer
 *                   example: 45
 *                 rejected:
 *                   type: integer
 *                   example: 3
 *                 expired:
 *                   type: integer
 *                   example: 2
 *                 acceptance_rate:
 *                   type: number
 *                   example: 0.90
 *                   description: Процент принятых заказов
 *                 average_response_time:
 *                   type: integer
 *                   example: 120
 *                   description: Среднее время ответа в секундах
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       500:
 *         description: Ошибка сервера
 */
