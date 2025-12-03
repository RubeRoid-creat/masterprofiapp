import express from 'express';
import cors from 'cors';
import { createServer } from 'http';
import { config } from './config.js';
import { initDatabase, query } from './database/db.js';
import { initWebSocket } from './websocket.js';

// Импорт маршрутов
import authRoutes from './routes/auth.js';
import ordersRoutes from './routes/orders.js';
import mastersRoutes from './routes/masters.js';
import assignmentsRoutes from './routes/assignments.js';
import servicesRoutes from './routes/services.js';
import reviewsRoutes from './routes/reviews.js';
import fcmRoutes from './routes/fcm.js';
import chatRoutes from './routes/chat.js';
import reportsRoutes from './routes/reports.js';
import versionRoutes from './routes/version.js';
import verificationRoutes from './routes/verification.js';
import complaintsRoutes from './routes/complaints.js';
import adminRoutes from './routes/admin.js';
import paymentsRoutes from './routes/payments.js';
import subscriptionsRoutes from './routes/subscriptions.js';
import promotionsRoutes from './routes/promotions.js';
import loyaltyRoutes from './routes/loyalty.js';
import routeOptimizationRoutes from './routes/route-optimization.js';
import mlmRoutes from './routes/mlm.js';
// Импортируем push-notification-service для инициализации Firebase Admin SDK
import './services/push-notification-service.js';
// Инициализируем Redis для кэширования
import { initRedis } from './services/cache-service.js';

// Инициализация Express
const app = express();
const server = createServer(app);

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Статические файлы (для доступа к загруженным медиа)
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
app.use('/uploads', express.static(join(__dirname, 'uploads')));
// Статичный сайт обновлений приложений
app.use('/updates', express.static(join(__dirname, 'public', 'updates')));

// Админ-панель (SPA на React/Vite), собирается в backend/admin-panel/dist
app.use('/admin', express.static(join(__dirname, 'admin-panel', 'dist')));
app.get('/admin/*', (req, res) => {
  res.sendFile(join(__dirname, 'admin-panel', 'dist', 'index.html'));
});

// Логирование запросов
app.use((req, res, next) => {
  console.log(`${new Date().toISOString()} ${req.method} ${req.path}`);
  if (req.path.includes('wallet') || req.path.includes('subscriptions') || req.path.includes('promotions') || req.path.includes('reorder')) {
    console.log(`[API DEBUG] Request: ${req.method} ${req.path}`);
    console.log(`[API DEBUG] Headers:`, JSON.stringify(req.headers, null, 2));
  }
  next();
});

// Инициализация базы данных и Redis
(async () => {
  try {
    await initDatabase();
    
    // Проверяем и добавляем поле inn, если его нет
    try {
      const tableInfo = query.all("PRAGMA table_info(masters)");
      const hasInn = tableInfo && Array.isArray(tableInfo) && tableInfo.some(col => col && col.name === 'inn');
      
      if (!hasInn) {
        console.log('📝 Добавление поля inn в таблицу masters...');
        try {
          query.run('ALTER TABLE masters ADD COLUMN inn TEXT');
          console.log('✅ Поле inn успешно добавлено в таблицу masters');
        } catch (e) {
          if (e.message.includes('duplicate column') || e.message.includes('already exists')) {
            console.log('ℹ️ Поле inn уже существует');
          } else {
            console.error('⚠️ Ошибка добавления поля inn:', e.message);
          }
        }
      } else {
        console.log('✅ Поле inn присутствует в таблице masters');
      }
    } catch (e) {
      console.error('⚠️ Ошибка проверки поля inn:', e.message);
      // При ошибке проверки все равно пытаемся добавить поле
      try {
        query.run('ALTER TABLE masters ADD COLUMN inn TEXT');
        console.log('✅ Поле inn добавлено после ошибки проверки');
      } catch (e2) {
        if (!e2.message.includes('duplicate column') && !e2.message.includes('already exists')) {
          console.error('⚠️ Критическая ошибка добавления поля inn:', e2.message);
        }
      }
    }
    
    // Проверяем и добавляем поле sponsor_id в таблицу users, если его нет
    try {
      const usersTableInfo = query.all("PRAGMA table_info(users)");
      const hasSponsorId = usersTableInfo && Array.isArray(usersTableInfo) && usersTableInfo.some(col => col && col.name === 'sponsor_id');
      
      if (!hasSponsorId) {
        console.log('📝 Добавление поля sponsor_id в таблицу users...');
        try {
          query.run('ALTER TABLE users ADD COLUMN sponsor_id INTEGER');
          console.log('✅ Поле sponsor_id успешно добавлено в таблицу users');
          
          // Создаем индекс после добавления колонки
          try {
            query.run('CREATE INDEX IF NOT EXISTS idx_users_sponsor_id ON users(sponsor_id)');
            console.log('✅ Индекс idx_users_sponsor_id создан');
          } catch (indexError) {
            if (!indexError.message.includes('already exists')) {
              console.warn('⚠️ Ошибка создания индекса idx_users_sponsor_id:', indexError.message);
            }
          }
        } catch (e) {
          if (e.message.includes('duplicate column') || e.message.includes('already exists')) {
            console.log('ℹ️ Поле sponsor_id уже существует');
          } else {
            console.error('⚠️ Ошибка добавления поля sponsor_id:', e.message);
          }
        }
      } else {
        console.log('✅ Поле sponsor_id присутствует в таблице users');
        
        // Проверяем наличие индекса
        try {
          query.run('CREATE INDEX IF NOT EXISTS idx_users_sponsor_id ON users(sponsor_id)');
          console.log('✅ Индекс idx_users_sponsor_id проверен');
        } catch (indexError) {
          if (!indexError.message.includes('already exists')) {
            console.warn('⚠️ Ошибка создания индекса idx_users_sponsor_id:', indexError.message);
          }
        }
      }
    } catch (e) {
      console.error('⚠️ Ошибка проверки поля sponsor_id:', e.message);
      // При ошибке проверки все равно пытаемся добавить поле
      try {
        query.run('ALTER TABLE users ADD COLUMN sponsor_id INTEGER');
        console.log('✅ Поле sponsor_id добавлено после ошибки проверки');
        
        // Создаем индекс
        try {
          query.run('CREATE INDEX IF NOT EXISTS idx_users_sponsor_id ON users(sponsor_id)');
        } catch (indexError) {
          // Игнорируем ошибки индекса
        }
      } catch (e2) {
        if (!e2.message.includes('duplicate column') && !e2.message.includes('already exists')) {
          console.error('⚠️ Критическая ошибка добавления поля sponsor_id:', e2.message);
        }
      }
    }
    
    // Инициализация Redis (опционально, если доступен)
    await initRedis();
    
    // Проверяем наличие тестового мастера
    const testMaster = query.get('SELECT id, email, name, role FROM users WHERE email = ?', ['master@test.com']);
    if (testMaster) {
      console.log(`✅ Test master found: id=${testMaster.id}, email=${testMaster.email}`);
    } else {
      console.log('⚠️ Test master not found. Run: node scripts/create-test-master.js');
    }
    
    // Инициализация автоматического резервного копирования
    if (config.backupEnabled) {
      const { createBackup } = await import('./services/backup-service.js');
      
      // Создаем первый бэкап при запуске
      try {
        createBackup();
      } catch (error) {
        console.error('⚠️ Ошибка создания начального бэкапа:', error.message);
      }
      
      // Настраиваем периодическое создание бэкапов
      setInterval(() => {
        try {
          createBackup();
        } catch (error) {
          console.error('⚠️ Ошибка автоматического бэкапа:', error.message);
        }
      }, config.backupInterval);
      
      const intervalHours = config.backupInterval / (60 * 60 * 1000);
      console.log(`💾 Автоматическое резервное копирование включено (каждые ${intervalHours} часов)`);
    }
  } catch (error) {
    console.error('❌ Ошибка инициализации БД:', error);
    process.exit(1);
  }
})();

// Инициализация WebSocket
initWebSocket(server);

// Базовый маршрут
app.get('/', (req, res) => {
  res.json({
    message: 'BestApp API Server',
    version: '1.0.0',
    status: 'running',
    endpoints: {
      auth: '/api/auth',
      orders: '/api/orders',
      masters: '/api/masters',
      assignments: '/api/assignments',
      services: '/api/services',
      chat: '/api/chat',
      verification: '/api/verification',
      complaints: '/api/complaints',
      admin: '/api/admin',
      payments: '/api/payments',
      subscriptions: '/api/subscriptions',
      promotions: '/api/promotions',
      loyalty: '/api/loyalty',
      mlm: '/api/mlm',
      version: '/api/version',
      websocket: '/ws'
    }
  });
});

// Маршруты API
app.use('/api/auth', authRoutes);
app.use('/api/orders', ordersRoutes);
app.use('/api/masters', mastersRoutes);
app.use('/api/assignments', assignmentsRoutes);
app.use('/api/services', servicesRoutes);
app.use('/api/reviews', reviewsRoutes);
app.use('/api/fcm', fcmRoutes);
app.use('/api/chat', chatRoutes);
app.use('/api/reports', reportsRoutes);
app.use('/api/verification', verificationRoutes);
app.use('/api/complaints', complaintsRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/payments', paymentsRoutes);
app.use('/api/subscriptions', subscriptionsRoutes);
app.use('/api/promotions', promotionsRoutes);
app.use('/api/loyalty', loyaltyRoutes);
app.use('/api/orders', routeOptimizationRoutes);
app.use('/api/mlm', mlmRoutes);
app.use('/api/version', versionRoutes);

// Health check
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    uptime: process.uptime()
  });
});

// Обработка 404
app.use((req, res) => {
  res.status(404).json({
    error: 'Маршрут не найден',
    path: req.path
  });
});

// Обработка ошибок
app.use((error, req, res, next) => {
  console.error('❌ Ошибка сервера:', error);
  res.status(500).json({
    error: 'Внутренняя ошибка сервера',
    message: config.nodeEnv === 'development' ? error.message : undefined
  });
});

// Запуск сервера
server.listen(config.port, '0.0.0.0', () => {
  console.log('');
  console.log('🚀 =====================================================');
  console.log(`   BestApp Backend Server запущен!`);
  console.log('   =====================================================');
  console.log(`   🌐 HTTP Server:  http://localhost:${config.port}`);
  console.log(`   🔌 WebSocket:    ws://localhost:${config.port}/ws`);
  console.log(`   📊 Окружение:    ${config.nodeEnv}`);
  console.log(`   💾 База данных:  ${config.databasePath}`);
  console.log('   =====================================================');
  console.log('');
  console.log('   📝 Доступные эндпоинты:');
  console.log('      POST   /api/auth/register       - Регистрация');
  console.log('      POST   /api/auth/login          - Вход');
  console.log('      GET    /api/orders              - Список заказов');
  console.log('      POST   /api/orders              - Создать заказ');
  console.log('      GET    /api/masters             - Список мастеров');
  console.log('      POST   /api/masters/shift/start - Начать смену');
  console.log('      POST   /api/masters/shift/end   - Завершить смену');
  console.log('      GET    /api/assignments/my      - Мои назначения');
  console.log('      POST   /api/assignments/:id/accept - Принять заказ');
  console.log('      POST   /api/assignments/:id/reject - Отклонить заказ');
  console.log('   =====================================================');
  console.log('');
});

// Обработка сигналов завершения
process.on('SIGINT', () => {
  console.log('\n👋 Завершение работы сервера...');
  server.close(() => {
    console.log('✅ Сервер остановлен');
    process.exit(0);
  });
});

process.on('SIGTERM', () => {
  console.log('\n👋 Завершение работы сервера...');
  server.close(() => {
    console.log('✅ Сервер остановлен');
    process.exit(0);
  });
});

export default app;

