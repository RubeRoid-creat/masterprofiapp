/**
 * @swagger
 * /api/auth/register:
 *   post:
 *     tags:
 *       - Аутентификация
 *     summary: Регистрация нового пользователя
 *     description: Создает нового пользователя (клиента или мастера) и возвращает JWT токен
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - email
 *               - password
 *               - name
 *               - phone
 *             properties:
 *               email:
 *                 type: string
 *                 format: email
 *                 example: ivan@example.com
 *               password:
 *                 type: string
 *                 format: password
 *                 minLength: 6
 *                 example: password123
 *               name:
 *                 type: string
 *                 example: Иван Иванов
 *               phone:
 *                 type: string
 *                 example: +79001234567
 *               role:
 *                 type: string
 *                 enum: [client, master]
 *                 default: client
 *                 example: master
 *     responses:
 *       201:
 *         description: Пользователь успешно зарегистрирован
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 message:
 *                   type: string
 *                   example: Пользователь успешно зарегистрирован
 *                 token:
 *                   type: string
 *                   example: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 *                 user:
 *                   $ref: '#/components/schemas/User'
 *       400:
 *         description: Ошибка валидации или пользователь уже существует
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/Error'
 *       500:
 *         description: Ошибка сервера
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/Error'
 */

/**
 * @swagger
 * /api/auth/login:
 *   post:
 *     tags:
 *       - Аутентификация
 *     summary: Вход в систему
 *     description: Аутентифицирует пользователя и возвращает JWT токен
 *     security: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - email
 *               - password
 *             properties:
 *               email:
 *                 type: string
 *                 format: email
 *                 example: ivan@example.com
 *               password:
 *                 type: string
 *                 format: password
 *                 example: password123
 *     responses:
 *       200:
 *         description: Успешная аутентификация
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 message:
 *                   type: string
 *                   example: Вход выполнен успешно
 *                 token:
 *                   type: string
 *                   example: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 *                 user:
 *                   $ref: '#/components/schemas/User'
 *       400:
 *         description: Отсутствуют обязательные поля
 *       401:
 *         description: Неверный email или пароль
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/auth/me:
 *   get:
 *     tags:
 *       - Аутентификация
 *     summary: Получить информацию о текущем пользователе
 *     description: Возвращает данные авторизованного пользователя на основе JWT токена
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Информация о пользователе
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/User'
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 *       404:
 *         description: Пользователь не найден
 *       500:
 *         description: Ошибка сервера
 */

/**
 * @swagger
 * /api/auth/refresh:
 *   post:
 *     tags:
 *       - Аутентификация
 *     summary: Обновить JWT токен
 *     description: Генерирует новый токен для авторизованного пользователя
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Токен успешно обновлен
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 message:
 *                   type: string
 *                   example: Токен обновлен
 *                 token:
 *                   type: string
 *                   example: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 *       401:
 *         $ref: '#/components/responses/UnauthorizedError'
 */
