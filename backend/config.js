// Конфигурация приложения
export const config = {
  port: process.env.PORT || 3000,
  jwtSecret: process.env.JWT_SECRET || 'dev-secret-key-change-in-production',
  databasePath: process.env.DATABASE_PATH || './database.sqlite',
  nodeEnv: process.env.NODE_ENV || 'development',
  
  // Настройки для назначения заказов
  assignmentTimeout: 5 * 60 * 1000, // 5 минут в миллисекундах
  
  // Тестовые координаты (центр Твери)
  defaultLocation: {
    latitude: 56.859611,
    longitude: 35.911896
  },
  
  // Firebase Admin SDK (для push-уведомлений)
  // Путь к сервисному ключу Firebase (JSON файл)
  // Получите его в Firebase Console → Настройки проекта → Сервисные аккаунты
  // Или через Google Cloud Console → IAM → Сервисные аккаунты
  firebaseServiceAccount: process.env.FIREBASE_SERVICE_ACCOUNT || './firebase-service-account.json',
  
  // Настройки резервного копирования
  backupEnabled: process.env.BACKUP_ENABLED !== 'false', // По умолчанию включено
  backupInterval: parseInt(process.env.BACKUP_INTERVAL || '86400000'), // 24 часа в миллисекундах
  maxBackups: parseInt(process.env.MAX_BACKUPS || '30'), // Хранить последние 30 бэкапов
  
  // Настройки базы данных
  databaseType: process.env.DATABASE_TYPE || 'sqlite', // 'sqlite' или 'postgresql'
  
  // PostgreSQL настройки (если используется)
  postgres: {
    host: process.env.POSTGRES_HOST || 'localhost',
    port: parseInt(process.env.POSTGRES_PORT || '5432'),
    database: process.env.POSTGRES_DB || 'bestapp',
    user: process.env.POSTGRES_USER || 'bestapp_user',
    password: process.env.POSTGRES_PASSWORD || ''
  },
  
  // Настройки email (для отправки кодов подтверждения)
  email: {
    from: process.env.EMAIL_FROM || 'noreply@bestapp.ru',
    smtp: process.env.EMAIL_SMTP_ENABLED === 'true' ? {
      host: process.env.EMAIL_SMTP_HOST || 'smtp.gmail.com',
      port: parseInt(process.env.EMAIL_SMTP_PORT || '587'),
      secure: process.env.EMAIL_SMTP_SECURE === 'true', // true для 465, false для других портов
      user: process.env.EMAIL_SMTP_USER || '',
      password: process.env.EMAIL_SMTP_PASSWORD || ''
    } : null
  },
  
  // Настройки SMS (для отправки кодов подтверждения)
  sms: {
    provider: process.env.SMS_PROVIDER || null, // 'smsru', 'twilio', или null для режима разработки
    smsru: {
      api_id: process.env.SMSRU_API_ID || ''
    },
    twilio: {
      accountSid: process.env.TWILIO_ACCOUNT_SID || '',
      authToken: process.env.TWILIO_AUTH_TOKEN || '',
      phoneNumber: process.env.TWILIO_PHONE_NUMBER || ''
    }
  }
};




