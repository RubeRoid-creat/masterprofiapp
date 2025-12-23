/**
 * @swagger
 * /api/orders:
 *   get:
 *     tags:
 *       - Заказы
 *     summary: Получить список заказов
 *     description: |
 *       Для клиентов - возвращает их собственные заказы.
 *       Для мастеров - возвращает доступные заказы по специализации.
 *       Для администраторов - возвращает все заказы с фильтрами.
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: status
 *         schema:
 *           type: string
 *           enum: [new, assigned, in_progress, diagnostics, waiting_parts, completed, cancelled]
 *         description: Фильтр по статусу заказа
 *       - in: query
 *         name: master_id
 *         schema:
 *           type: integer
 *         description: Фильтр по ID мастера (только для админов)
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 50
 *         description: Количество заказов на странице
 *       - in: query
 *         name: offset
 *         schema:
 *           type: integer
 *           default: 0
 *         description: Смещение для пагинации
 *     responses:
 *       200:
 *         description: Список заказов
 *         content:
 *           application/json:
 *             schema:
 *               type: array
 *               items:
 *                 $ref: '#/components/schemas/Order'
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/orders:
 *   post:
 *     tags:
 *       - Заказы
 *     summary: Создать новый заказ
 *     description: Создает новый заказ на ремонт бытовой техники
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - device_type
 *               - problem_description
 *               - address
 *             properties:
 *               device_type:
 *                 type: string
 *                 example: холодильник
 *                 description: Тип техники
 *               device_brand:
 *                 type: string
 *                 example: Samsung
 *                 description: Бренд техники
 *               device_model:
 *                 type: string
 *                 example: RB37J5000SA
 *                 description: Модель техники
 *               device_serial_number:
 *                 type: string
 *                 example: 123456789
 *                 description: Серийный номер
 *               problem_description:
 *                 type: string
 *                 example: Не морозит, течет вода
 *                 description: Описание проблемы
 *               address:
 *                 type: string
 *                 example: г. Москва, ул. Ленина, д. 10, кв. 5
 *                 description: Адрес для ремонта
 *               latitude:
 *                 type: number
 *                 format: double
 *                 example: 55.751244
 *                 description: Широта адреса
 *               longitude:
 *                 type: number
 *                 format: double
 *                 example: 37.618423
 *                 description: Долгота адреса
 *               urgency:
 *                 type: string
 *                 enum: [emergency, urgent, planned]
 *                 default: urgent
 *                 example: urgent
 *                 description: Срочность заказа
 *               preferred_date:
 *                 type: string
 *                 format: date-time
 *                 example: "2025-01-15T14:00:00Z"
 *                 description: Желаемая дата визита
 *               photos:
 *                 type: array
 *                 items:
 *                   type: string
 *                 example: ["/uploads/photo1.jpg", "/uploads/photo2.jpg"]
 *                 description: Массив URL фотографий
 *     responses:
 *       201:
 *         description: Заказ успешно создан
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 message:
 *                   type: string
 *                   example: Заказ успешно создан
 *                 order:
 *                   $ref: '#/components/schemas/Order'
 *       400:
 *         description: Ошибка валидации
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/orders/{id}:
 *   get:
 *     tags:
 *       - Заказы
 *     summary: Получить детали заказа
 *     description: Возвращает подробную информацию о конкретном заказе
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID заказа
 *     responses:
 *       200:
 *         description: Детали заказа
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/Order'
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       404:
 *         $ref: '#/components/responses/NotFoundError'
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/orders/{id}:
 *   put:
 *     tags:
 *       - Заказы
 *     summary: Обновить заказ
 *     description: Обновляет информацию о заказе (только владелец или мастер)
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID заказа
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               repair_status:
 *                 type: string
 *                 enum: [new, assigned, in_progress, diagnostics, waiting_parts, completed, cancelled]
 *               master_notes:
 *                 type: string
 *                 example: Требуется замена компрессора
 *               estimated_cost:
 *                 type: number
 *                 example: 5000.00
 *               completion_date:
 *                 type: string
 *                 format: date-time
 *     responses:
 *       200:
 *         description: Заказ успешно обновлен
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 message:
 *                   type: string
 *                   example: Заказ успешно обновлен
 *                 order:
 *                   $ref: '#/components/schemas/Order'
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       403:
 *         $ref: '#/components/responses/ForbiddenError'
 *       404:
 *         $ref: '#/components/responses/NotFoundError'
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/orders/{id}/cancel:
 *   post:
 *     tags:
 *       - Заказы
 *     summary: Отменить заказ
 *     description: Отменяет заказ (клиент или админ)
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID заказа
 *     requestBody:
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               cancellation_reason:
 *                 type: string
 *                 example: Передумал
 *     responses:
 *       200:
 *         description: Заказ успешно отменен
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       404:
 *         $ref: '#/components/responses/NotFoundError'
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/orders/{id}/photos:
 *   post:
 *     tags:
 *       - Заказы
 *     summary: Загрузить фото к заказу
 *     description: Загружает фотографии к существующему заказу
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: ID заказа
 *     requestBody:
 *       required: true
 *       content:
 *         multipart/form-data:
 *           schema:
 *             type: object
 *             properties:
 *               photos:
 *                 type: array
 *                 items:
 *                   type: string
 *                   format: binary
 *                 description: Массив изображений (до 5 файлов, макс. 10MB каждый)
 *     responses:
 *       200:
 *         description: Фото успешно загружены
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 message:
 *                   type: string
 *                   example: Фото успешно загружены
 *                 photos:
 *                   type: array
 *                   items:
 *                     type: string
 *                   example: ["/uploads/photo1.jpg", "/uploads/photo2.jpg"]
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       404:
 *         $ref: '#/components/responses/NotFoundError'
 *       500:
 *         description: Ошибка сервера
 */
