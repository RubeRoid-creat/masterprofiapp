// Миграция: создание таблиц для системы оплаты и монетизации
import { query } from '../database/db.js';

try {
  console.log('🔄 Миграция: создание таблиц для системы оплаты...');
  
  // Таблица платежей
  query.run(`
    CREATE TABLE IF NOT EXISTS payments (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      order_id INTEGER NOT NULL,
      client_id INTEGER NOT NULL,
      amount REAL NOT NULL,
      currency TEXT DEFAULT 'RUB',
      payment_method TEXT NOT NULL CHECK(payment_method IN ('card', 'cash', 'yoomoney', 'qiwi', 'bank_transfer', 'installment')),
      payment_provider TEXT, -- 'yookassa', 'stripe', 'paypal', 'manual'
      payment_status TEXT NOT NULL CHECK(payment_status IN ('pending', 'processing', 'completed', 'failed', 'refunded', 'cancelled')) DEFAULT 'pending',
      provider_payment_id TEXT, -- ID платежа в платежной системе
      provider_response TEXT, -- JSON ответ от платежной системы
      receipt_url TEXT, -- URL чека
      receipt_data TEXT, -- JSON данные чека
      paid_at DATETIME,
      refunded_at DATETIME,
      refund_amount REAL,
      refund_reason TEXT,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
      FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
    )
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id)
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_payments_client_id ON payments(client_id)
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(payment_status)
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_payments_provider_id ON payments(provider_payment_id)
  `);
  
  // Таблица комиссий платформы
  query.run(`
    CREATE TABLE IF NOT EXISTS platform_commissions (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      order_id INTEGER NOT NULL,
      payment_id INTEGER,
      master_id INTEGER NOT NULL,
      order_amount REAL NOT NULL,
      commission_percentage REAL NOT NULL DEFAULT 15.0,
      commission_amount REAL NOT NULL,
      status TEXT NOT NULL CHECK(status IN ('pending', 'collected', 'refunded')) DEFAULT 'pending',
      collected_at DATETIME,
      refunded_at DATETIME,
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
      FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL,
      FOREIGN KEY (master_id) REFERENCES masters(id) ON DELETE CASCADE
    )
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_platform_commissions_order_id ON platform_commissions(order_id)
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_platform_commissions_master_id ON platform_commissions(master_id)
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_platform_commissions_status ON platform_commissions(status)
  `);
  
  // Таблица подписок мастеров
  query.run(`
    CREATE TABLE IF NOT EXISTS master_subscriptions (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      master_id INTEGER NOT NULL,
      subscription_type TEXT NOT NULL CHECK(subscription_type IN ('basic', 'premium')) DEFAULT 'basic',
      status TEXT NOT NULL CHECK(status IN ('active', 'expired', 'cancelled')) DEFAULT 'active',
      started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      expires_at DATETIME,
      auto_renew INTEGER DEFAULT 0, -- 0 = false, 1 = true
      payment_id INTEGER, -- ID платежа за подписку
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (master_id) REFERENCES masters(id) ON DELETE CASCADE,
      FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL
    )
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_master_subscriptions_master_id ON master_subscriptions(master_id)
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_master_subscriptions_status ON master_subscriptions(status)
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_master_subscriptions_type ON master_subscriptions(subscription_type)
  `);
  
  // Таблица платных продвижений
  query.run(`
    CREATE TABLE IF NOT EXISTS master_promotions (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      master_id INTEGER NOT NULL,
      promotion_type TEXT NOT NULL CHECK(promotion_type IN ('top_listing', 'highlighted_profile', 'featured')),
      status TEXT NOT NULL CHECK(status IN ('active', 'expired', 'cancelled')) DEFAULT 'active',
      started_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      expires_at DATETIME NOT NULL,
      payment_id INTEGER, -- ID платежа за продвижение
      created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (master_id) REFERENCES masters(id) ON DELETE CASCADE,
      FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL
    )
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_master_promotions_master_id ON master_promotions(master_id)
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_master_promotions_status ON master_promotions(status)
  `);
  
  query.run(`
    CREATE INDEX IF NOT EXISTS idx_master_promotions_type ON master_promotions(promotion_type)
  `);
  
  // Добавляем поле subscription_type в masters, если его нет
  const tableInfo = query.all("PRAGMA table_info(masters)");
  const hasSubscriptionType = tableInfo.some(col => col.name === 'subscription_type');
  
  if (!hasSubscriptionType) {
    query.run(`
      ALTER TABLE masters 
      ADD COLUMN subscription_type TEXT DEFAULT 'basic' 
      CHECK(subscription_type IN ('basic', 'premium'))
    `);
    console.log('✅ Поле subscription_type добавлено в таблицу masters');
  }
  
  console.log('✅ Миграция завершена');
} catch (error) {
  console.error('❌ Ошибка миграции:', error);
  throw error;
}

