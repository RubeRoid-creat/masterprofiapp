import { initDatabase, query } from '../database/db.js';

/**
 * Миграция: добавление кошелька мастера (баланс и транзакции)
 */
async function migrateMasterWallet() {
  try {
    console.log('🔄 Начало миграции: добавление кошелька мастера...');
    
    await initDatabase();
    
    // Добавляем поле balance в таблицу masters
    console.log('📝 Добавление поля balance в таблицу masters...');
    try {
      query.run(`
        ALTER TABLE masters 
        ADD COLUMN balance REAL DEFAULT 0.0
      `);
      console.log('  ✅ Добавлено поле balance');
    } catch (e) {
      if (!e.message.includes('duplicate column')) {
        console.log('  ⚠️ Поле balance уже существует или ошибка:', e.message);
      } else {
        console.log('  ✅ Поле balance уже существует');
      }
    }
    
    // Проверяем, существует ли уже таблица master_transactions
    const tableExists = query.get(`
      SELECT name FROM sqlite_master 
      WHERE type='table' AND name='master_transactions'
    `);
    
    if (tableExists) {
      console.log('✅ Таблица master_transactions уже существует');
      return;
    }
    
    // Создаем таблицу транзакций
    console.log('📝 Создание таблицы master_transactions...');
    query.run(`
      CREATE TABLE IF NOT EXISTS master_transactions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        master_id INTEGER NOT NULL,
        order_id INTEGER,
        transaction_type TEXT NOT NULL CHECK(transaction_type IN ('income', 'payout', 'refund', 'commission')),
        amount REAL NOT NULL,
        description TEXT,
        status TEXT NOT NULL CHECK(status IN ('pending', 'completed', 'failed', 'cancelled')) DEFAULT 'pending',
        commission_percentage REAL,
        commission_amount REAL,
        payout_method TEXT, -- 'bank', 'card', 'yoomoney', 'qiwi'
        payout_details TEXT, -- JSON с деталями выплаты
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        completed_at DATETIME,
        FOREIGN KEY (master_id) REFERENCES masters(id) ON DELETE CASCADE,
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL
      )
    `);
    console.log('  ✅ Таблица master_transactions создана');
    
    // Создаем индексы
    query.run('CREATE INDEX IF NOT EXISTS idx_master_transactions_master_id ON master_transactions(master_id)');
    query.run('CREATE INDEX IF NOT EXISTS idx_master_transactions_order_id ON master_transactions(order_id)');
    query.run('CREATE INDEX IF NOT EXISTS idx_master_transactions_type ON master_transactions(transaction_type)');
    query.run('CREATE INDEX IF NOT EXISTS idx_master_transactions_status ON master_transactions(status)');
    query.run('CREATE INDEX IF NOT EXISTS idx_master_transactions_created_at ON master_transactions(created_at)');
    
    console.log('✅ Миграция завершена: кошелек мастера настроен');
  } catch (error) {
    console.error('❌ Ошибка миграции:', error);
    process.exit(1);
  }
}

migrateMasterWallet();



