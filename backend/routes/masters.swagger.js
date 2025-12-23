/**
 * @swagger
 * /api/masters:
 *   get:
 *     tags:
 *       - Мастера
 *     summary: Получить список мастеров
 *     description: Возвращает список всех верифицированных мастеров с возможностью фильтрации
 *     parameters:
 *       - in: query
 *         name: specialization
 *         schema:
 *           type: string
 *         description: Фильтр по специализации
 *         example: холодильник
 *       - in: query
 *         name: rating_min
 *         schema:
 *           type: number
 *           format: double
 *         description: Минимальный рейтинг
 *         example: 4.5
 *       - in: query
 *         name: is_on_shift
 *         schema:
 *           type: boolean
 *         description: Фильтр по статусу смены
 *       - in: query
 *         name: latitude
 *         schema:
 *           type: number
 *           format: double
 *         description: Широта для поиска ближайших мастеров
 *       - in: query
 *         name: longitude
 *         schema:
 *           type: number
 *           format: double
 *         description: Долгота для поиска ближайших мастеров
 *       - in: query
 *         name: radius
 *         schema:
 *           type: number
 *         description: Радиус поиска в метрах
 *         default: 10000
 *     responses:
 *       200:
 *         description: Список мастеров
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 $ref: '#/components/schemas/Master'
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/masters/me:
 *   get:
 *     tags:
 *       - Мастера
 *     summary: Получить профиль текущего мастера
 *     description: Возвращает полный профиль авторизованного мастера
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Профиль мастера
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/Master'
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       404:
 *         description: Профиль мастера не найден
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/masters/profile:
 *   put:
 *     tags:
 *       - Мастера
 *     summary: Обновить профиль мастера
 *     description: Обновляет информацию в профиле мастера
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               specialization:
 *                 type: array
 *                 items:
 *                   type: string
 *                 example: ["холодильник", "стиральная_машина", "посудомоечная_машина"]
 *               experience_years:
 *                 type: integer
 *                 example: 5
 *               bio:
 *                 type: string
 *                 example: Опытный мастер с 5-летним стажем
 *               certificates:
 *                 type: array
 *                 items:
 *                   type: string
 *                 example: ["/uploads/cert1.pdf", "/uploads/cert2.pdf"]
 *               inn:
 *                 type: string
 *                 example: "123456789012"
 *               passport_data:
 *                 type: string
 *                 example: "4507 123456"
 *     responses:
 *       200:
 *         description: Профиль успешно обновлен
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 message:
 *                   type: string
 *                   example: Профиль успешно обновлен
 *                 master:
 *                   $ref: '#/components/schemas/Master'
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       404:
 *         description: Профиль мастера не найден
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/masters/shift/start:
 *   post:
 *     tags:
 *       - Мастера
 *     summary: Начать смену
 *     description: Мастер начинает смену и становится доступным для заказов
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - latitude
 *               - longitude
 *             properties:
 *               latitude:
 *                 type: number
 *                 format: double
 *                 example: 55.751244
 *               longitude:
 *                 type: number
 *                 format: double
 *                 example: 37.618423
 *     responses:
 *       200:
 *         description: Смена успешно начата
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 message:
 *                   type: string
 *                   example: Смена начата
 *                 shift_id:
 *                   type: integer
 *                   example: 123
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       404:
 *         description: Профиль мастера не найден
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/masters/shift/end:
 *   post:
 *     tags:
 *       - Мастера
 *     summary: Завершить смену
 *     description: Мастер завершает смену и становится недоступным для новых заказов
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Смена успешно завершена
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 message:
 *                   type: string
 *                   example: Смена завершена
 *                 shift_summary:
 *                   type: object
 *                   properties:
 *                     completed_orders:
 *                       type: integer
 *                       example: 5
 *                     total_earnings:
 *                       type: number
 *                       example: 15000.00
 *                     shift_duration:
 *                       type: string
 *                       example: "8 часов 30 минут"
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       404:
 *         description: Активная смена не найдена
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/masters/location:
 *   put:
 *     tags:
 *       - Мастера
 *     summary: Обновить геолокацию мастера
 *     description: Обновляет текущие координаты мастера во время смены
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - latitude
 *               - longitude
 *             properties:
 *               latitude:
 *                 type: number
 *                 format: double
 *                 example: 55.751244
 *               longitude:
 *                 type: number
 *                 format: double
 *                 example: 37.618423
 *     responses:
 *       200:
 *         description: Геолокация успешно обновлена
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       404:
 *         description: Профиль мастера не найден
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/masters/statistics:
 *   get:
 *     tags:
 *       - Мастера
 *     summary: Получить статистику мастера
 *     description: Возвращает подробную статистику работы мастера
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
 *         description: Статистика мастера
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 completed_orders:
 *                   type: integer
 *                   example: 156
 *                 total_earnings:
 *                   type: number
 *                   example: 450000.00
 *                 average_rating:
 *                   type: number
 *                   example: 4.8
 *                 acceptance_rate:
 *                   type: number
 *                   example: 0.95
 *                 response_time_avg:
 *                   type: number
 *                   example: 180
 *                   description: Среднее время ответа в секундах
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/masters/{id}/reviews:
 *   get:
 *     tags:
 *       - Мастера
 *     summary: Получить отзывы о мастере
 *     description: Возвращает список отзывов о конкретном мастере
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID мастера
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 20
 *         description: Количество отзывов на странице
 *       - in: query
 *         name: offset
 *         schema:
 *           type: integer
 *           default: 0
 *         description: Смещение для пагинации
 *     responses:
 *       200:
 *         description: Список отзывов
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
 *                   rating:
 *                     type: integer
 *                   comment:
 *                     type: string
 *                   client_name:
 *                     type: string
 *                   created_at:
 *                     type: string
 *                     format: date-time
 *       404:
 *         $ref: '#/components/responses/NotFoundError'
 *       500:
 *         description: Ошибка сервера
 */
