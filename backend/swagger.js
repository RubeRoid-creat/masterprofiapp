import swaggerJsdoc from 'swagger-jsdoc';
import { config } from './config.js';

const options = {
  definition: {
    openapi: '3.0.0',
    info: {
      title: 'МастерПрофи API',
      version: '1.0.0',
      description: `
API для сервиса вызова мастеров по ремонту бытовой техники

## Основные возможности:
- 👤 Аутентификация пользователей (JWT)
- 🔧 Управление заказами на ремонт
- 👨‍🔧 Профили мастеров с верификацией
- 📍 Геолокация и маршрутизация
- 💰 Интеграция с платежными системами
- 💬 Чат между клиентом и мастером
- 📊 Аналитика и отчеты
- 🎁 MLM-система бонусов
- 📱 Push-уведомления (FCM)
- 🔄 WebSocket для real-time обновлений

## Аутентификация:
Большинство эндпоинтов требуют JWT токен в заголовке Authorization:
\`\`\`
Authorization: Bearer <your-jwt-token>
\`\`\`

Получить токен можно через \`POST /api/auth/login\`
      `,
      contact: {
        name: 'МастерПрофи Support',
        url: 'https://github.com/RubeRoid-creat/masterprofiapp',
        email: 'support@masterprofi.ru'
      },
      license: {
        name: 'MIT',
        url: 'https://opensource.org/licenses/MIT'
      }
    },
    servers: [
      {
        url: `http://212.74.227.208:${config.port}`,
        description: 'Production сервер'
      },
      {
        url: `http://localhost:${config.port}`,
        description: 'Development сервер'
      }
    ],
    tags: [
      {
        name: 'Аутентификация',
        description: 'Регистрация, вход, восстановление пароля'
      },
      {
        name: 'Заказы',
        description: 'Создание, просмотр, управление заказами'
      },
      {
        name: 'Мастера',
        description: 'Профили мастеров, статистика, управление сменами'
      },
      {
        name: 'Назначения',
        description: 'Система назначения заказов мастерам'
      },
      {
        name: 'Верификация',
        description: 'Верификация документов мастеров'
      },
      {
        name: 'Чат',
        description: 'Сообщения между клиентом и мастером'
      },
      {
        name: 'Платежи',
        description: 'Интеграция с платежными системами'
      },
      {
        name: 'Отзывы',
        description: 'Отзывы клиентов о работе мастеров'
      },
      {
        name: 'FCM',
        description: 'Push-уведомления через Firebase'
      },
      {
        name: 'MLM',
        description: 'Многоуровневая система мотивации'
      },
      {
        name: 'Админ',
        description: 'Административные функции (требуется роль admin)'
      },
      {
        name: 'Версионирование',
        description: 'Проверка версий приложения'
      },
      {
        name: 'WebSocket',
        description: 'Real-time обновления через WebSocket'
      }
    ],
    components: {
      securitySchemes: {
        bearerAuth: {
          type: 'http',
          scheme: 'bearer',
          bearerFormat: 'JWT',
          description: 'JWT токен, полученный при входе'
        }
      },
      schemas: {
        Error: {
          type: 'object',
          properties: {
            error: {
              type: 'string',
              example: 'Описание ошибки'
            },
            details: {
              type: 'string',
              example: 'Подробности ошибки'
            }
          }
        },
        User: {
          type: 'object',
          properties: {
            id: { type: 'integer', example: 1 },
            name: { type: 'string', example: 'Иван Иванов' },
            email: { type: 'string', format: 'email', example: 'ivan@example.com' },
            phone: { type: 'string', example: '+79001234567' },
            role: { type: 'string', enum: ['client', 'master', 'admin'], example: 'master' },
            created_at: { type: 'string', format: 'date-time' }
          }
        },
        Order: {
          type: 'object',
          properties: {
            id: { type: 'integer', example: 1 },
            order_number: { type: 'string', example: 'ORD-20250101-0001' },
            client_id: { type: 'integer', example: 5 },
            device_type: { type: 'string', example: 'холодильник' },
            device_brand: { type: 'string', example: 'Samsung' },
            device_model: { type: 'string', example: 'RB37J5000SA' },
            problem_description: { type: 'string', example: 'Не морозит' },
            address: { type: 'string', example: 'ул. Ленина, д. 10, кв. 5' },
            latitude: { type: 'number', format: 'double', example: 55.751244 },
            longitude: { type: 'number', format: 'double', example: 37.618423 },
            estimated_cost: { type: 'number', format: 'double', example: 2500.00 },
            repair_status: { 
              type: 'string', 
              enum: ['new', 'assigned', 'in_progress', 'diagnostics', 'waiting_parts', 'completed', 'cancelled'],
              example: 'new'
            },
            urgency: { 
              type: 'string', 
              enum: ['emergency', 'urgent', 'planned'],
              example: 'urgent'
            },
            created_at: { type: 'string', format: 'date-time' }
          }
        },
        Master: {
          type: 'object',
          properties: {
            id: { type: 'integer', example: 1 },
            user_id: { type: 'integer', example: 3 },
            specialization: { 
              type: 'array',
              items: { type: 'string' },
              example: ['холодильник', 'стиральная_машина']
            },
            rating: { type: 'number', format: 'double', example: 4.8 },
            completed_orders: { type: 'integer', example: 156 },
            verification_status: {
              type: 'string',
              enum: ['pending', 'verified', 'rejected'],
              example: 'verified'
            },
            is_on_shift: { type: 'boolean', example: true },
            latitude: { type: 'number', format: 'double', example: 55.751244 },
            longitude: { type: 'number', format: 'double', example: 37.618423 }
          }
        }
      },
      responses: {
        UnauthorizedError: {
          description: 'Токен отсутствует или недействителен',
          content: {
            'application/json': {
              schema: {
                $ref: '#/components/schemas/Error'
              },
              example: {
                error: 'Требуется аутентификация'
              }
            }
          }
        },
        ForbiddenError: {
          description: 'Недостаточно прав доступа',
          content: {
            'application/json': {
              schema: {
                $ref: '#/components/schemas/Error'
              },
              example: {
                error: 'Доступ запрещен'
              }
            }
          }
        },
        NotFoundError: {
          description: 'Ресурс не найден',
          content: {
            'application/json': {
              schema: {
                $ref: '#/components/schemas/Error'
              },
              example: {
                error: 'Ресурс не найден'
              }
            }
          }
        }
      }
    },
    security: [
      {
        bearerAuth: []
      }
    ]
  },
  apis: [
    './routes/*.swagger.js',
    './routes/*.js',
    './server.js'
  ]
};

const swaggerSpec = swaggerJsdoc(options);

export default swaggerSpec;
