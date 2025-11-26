-- PostgreSQL схема для BestApp
-- Миграция с SQLite на PostgreSQL

-- Расширения
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK(role IN ('client', 'master', 'admin')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица мастеров
CREATE TABLE IF NOT EXISTS masters (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    specialization TEXT NOT NULL, -- JSON массив специализаций
    rating REAL DEFAULT 0.0,
    completed_orders INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL CHECK(status IN ('available', 'busy', 'offline')) DEFAULT 'offline',
    latitude REAL,
    longitude REAL,
    is_on_shift BOOLEAN DEFAULT FALSE,
    balance REAL DEFAULT 0.0,
    verification_status VARCHAR(20) DEFAULT 'not_verified' CHECK(verification_status IN ('not_verified', 'pending', 'verified', 'rejected')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица клиентов
CREATE TABLE IF NOT EXISTS clients (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    address TEXT,
    latitude REAL,
    longitude REAL,
    total_loyalty_points INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица заказов (основные поля)
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    client_id INTEGER NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    request_status VARCHAR(20) DEFAULT 'new' CHECK(request_status IN ('new', 'assigned', 'in_progress', 'completed', 'cancelled')),
    priority VARCHAR(20) DEFAULT 'regular' CHECK(priority IN ('low', 'regular', 'high', 'urgent')),
    order_source VARCHAR(20) DEFAULT 'app' CHECK(order_source IN ('app', 'website', 'phone', 'admin')),
    
    -- Информация об устройстве
    device_type VARCHAR(100),
    device_category VARCHAR(100),
    device_brand VARCHAR(100),
    device_model VARCHAR(100),
    device_serial_number VARCHAR(100),
    device_year INTEGER,
    warranty_status VARCHAR(20) CHECK(warranty_status IN ('no_warranty', 'warranty', 'expired')),
    
    -- Описание проблемы
    problem_short_description TEXT,
    problem_description TEXT,
    problem_when_started TEXT,
    problem_conditions TEXT,
    problem_error_codes TEXT,
    problem_attempted_fixes TEXT,
    
    -- Адрес
    address TEXT,
    address_street VARCHAR(255),
    address_building VARCHAR(50),
    address_apartment VARCHAR(50),
    address_floor INTEGER,
    address_entrance_code VARCHAR(50),
    address_landmark TEXT,
    latitude REAL,
    longitude REAL,
    
    -- Время
    arrival_time TIMESTAMP,
    desired_repair_date DATE,
    urgency VARCHAR(20) DEFAULT 'planned' CHECK(urgency IN ('emergency', 'urgent', 'planned')),
    
    -- Финансы
    estimated_cost REAL,
    final_cost REAL,
    client_budget REAL,
    payment_type VARCHAR(20) CHECK(payment_type IN ('cash', 'card', 'online', 'yoomoney', 'qiwi', 'installment')),
    visit_cost REAL,
    max_cost_without_approval REAL,
    
    -- Дополнительная информация
    intercom_working BOOLEAN DEFAULT TRUE,
    needs_pass BOOLEAN DEFAULT FALSE,
    parking_available BOOLEAN DEFAULT TRUE,
    has_pets BOOLEAN DEFAULT FALSE,
    has_small_children BOOLEAN DEFAULT FALSE,
    needs_shoe_covers BOOLEAN DEFAULT FALSE,
    preferred_contact_method VARCHAR(20) DEFAULT 'call' CHECK(preferred_contact_method IN ('call', 'sms', 'whatsapp', 'telegram')),
    
    -- Мастер
    master_gender_preference VARCHAR(10) DEFAULT 'any' CHECK(master_gender_preference IN ('male', 'female', 'any')),
    master_min_experience INTEGER,
    preferred_master_id INTEGER REFERENCES masters(id) ON DELETE SET NULL,
    assigned_master_id INTEGER REFERENCES masters(id) ON DELETE SET NULL,
    assignment_date TIMESTAMP,
    
    -- Диагностика и ремонт
    preliminary_diagnosis TEXT,
    required_parts TEXT,
    special_equipment TEXT,
    repair_complexity VARCHAR(20) CHECK(repair_complexity IN ('simple', 'medium', 'complex', 'very_complex')),
    estimated_repair_time INTEGER, -- В минутах
    
    -- Теги и категории
    problem_tags TEXT,
    problem_category VARCHAR(100),
    problem_seasonality VARCHAR(20) DEFAULT 'permanent' CHECK(problem_seasonality IN ('seasonal', 'permanent')),
    
    -- Связи
    related_order_id INTEGER REFERENCES orders(id) ON DELETE SET NULL,
    order_type VARCHAR(20) DEFAULT 'regular' CHECK(order_type IN ('regular', 'warranty', 'reorder')),
    repair_status VARCHAR(20) DEFAULT 'new' CHECK(repair_status IN ('new', 'assigned', 'in_progress', 'completed', 'cancelled')),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Индексы для оптимизации
CREATE INDEX IF NOT EXISTS idx_orders_client_id ON orders(client_id);
CREATE INDEX IF NOT EXISTS idx_orders_master_id ON orders(assigned_master_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(repair_status);
CREATE INDEX IF NOT EXISTS idx_orders_request_status ON orders(request_status);
CREATE INDEX IF NOT EXISTS idx_orders_order_number ON orders(order_number);
CREATE INDEX IF NOT EXISTS idx_orders_device_type ON orders(device_type);
CREATE INDEX IF NOT EXISTS idx_orders_priority ON orders(priority);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_orders_location ON orders USING GIST (point(longitude, latitude));

-- Таблица назначений заказов
CREATE TABLE IF NOT EXISTS order_assignments (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    master_id INTEGER NOT NULL REFERENCES masters(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL CHECK(status IN ('pending', 'accepted', 'rejected', 'expired')),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP,
    expires_at TIMESTAMP,
    UNIQUE(order_id, master_id)
);

CREATE INDEX IF NOT EXISTS idx_assignments_order_id ON order_assignments(order_id);
CREATE INDEX IF NOT EXISTS idx_assignments_master_id ON order_assignments(master_id);
CREATE INDEX IF NOT EXISTS idx_assignments_status ON order_assignments(status);

-- Таблица медиа заказов
CREATE TABLE IF NOT EXISTS order_media (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    media_type VARCHAR(20) NOT NULL CHECK(media_type IN ('image', 'video', 'document')),
    file_path TEXT NOT NULL,
    upload_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_order_media_order_id ON order_media(order_id);
CREATE INDEX IF NOT EXISTS idx_order_media_type ON order_media(media_type);

-- Таблица истории заказов клиента
CREATE TABLE IF NOT EXISTS client_order_history (
    id SERIAL PRIMARY KEY,
    client_id INTEGER NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    related_device_type VARCHAR(100),
    related_device_model VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_client_order_history_client_id ON client_order_history(client_id);

-- Таблица истории ремонтов техники
CREATE TABLE IF NOT EXISTS device_repair_history (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES orders(id) ON DELETE SET NULL,
    device_type VARCHAR(100),
    device_brand VARCHAR(100),
    device_model VARCHAR(100),
    device_serial_number VARCHAR(100),
    repair_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_device_repair_history_order_id ON device_repair_history(order_id);
CREATE INDEX IF NOT EXISTS idx_device_repair_history_serial ON device_repair_history(device_serial_number);

-- Таблица общих проблем
CREATE TABLE IF NOT EXISTS common_problems (
    id SERIAL PRIMARY KEY,
    device_type VARCHAR(100),
    device_brand VARCHAR(100),
    device_model VARCHAR(100),
    problem_description TEXT NOT NULL,
    frequency_rating INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_common_problems_device ON common_problems(device_type, device_brand, device_model);

-- Таблица отзывов
CREATE TABLE IF NOT EXISTS reviews (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    master_id INTEGER NOT NULL REFERENCES masters(id) ON DELETE CASCADE,
    client_id INTEGER NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK(rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Таблица версий приложений (для Android мастера и клиента)
CREATE TABLE IF NOT EXISTS app_versions (
    id SERIAL PRIMARY KEY,
    platform VARCHAR(50) NOT NULL, -- 'android_master', 'android_client'
    current_version VARCHAR(20) NOT NULL,
    min_required_version VARCHAR(20) NOT NULL,
    force_update BOOLEAN DEFAULT FALSE,
    release_notes TEXT,
    download_url TEXT,
    supported_os_versions TEXT,
    released_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- Таблица категорий услуг
CREATE TABLE IF NOT EXISTS service_categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_id INTEGER REFERENCES service_categories(id) ON DELETE CASCADE,
    icon VARCHAR(100),
    order_index INTEGER DEFAULT 0,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_service_categories_parent_id ON service_categories(parent_id);

-- Таблица шаблонов услуг
CREATE TABLE IF NOT EXISTS service_templates (
    id SERIAL PRIMARY KEY,
    category_id INTEGER REFERENCES service_categories(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    fixed_price REAL,
    estimated_time INTEGER,
    device_type VARCHAR(100),
    is_popular BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_service_templates_category_id ON service_templates(category_id);
CREATE INDEX IF NOT EXISTS idx_service_templates_device_type ON service_templates(device_type);
CREATE INDEX IF NOT EXISTS idx_service_templates_popular ON service_templates(is_popular);

-- Таблица FCM токенов
CREATE TABLE IF NOT EXISTS fcm_tokens (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token TEXT NOT NULL UNIQUE,
    device_type VARCHAR(20),
    device_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fcm_tokens_user_id ON fcm_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_fcm_tokens_token ON fcm_tokens(token);

-- Таблица транзакций мастера
CREATE TABLE IF NOT EXISTS master_transactions (
    id SERIAL PRIMARY KEY,
    master_id INTEGER NOT NULL REFERENCES masters(id) ON DELETE CASCADE,
    order_id INTEGER REFERENCES orders(id) ON DELETE SET NULL,
    transaction_type VARCHAR(20) NOT NULL CHECK(transaction_type IN ('income', 'payout', 'refund', 'commission')),
    amount REAL NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL CHECK(status IN ('pending', 'completed', 'failed', 'cancelled')) DEFAULT 'pending',
    commission_percentage REAL,
    commission_amount REAL,
    payout_method VARCHAR(20),
    payout_details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_master_transactions_master_id ON master_transactions(master_id);
CREATE INDEX IF NOT EXISTS idx_master_transactions_order_id ON master_transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_master_transactions_type ON master_transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_master_transactions_status ON master_transactions(status);
CREATE INDEX IF NOT EXISTS idx_master_transactions_created_at ON master_transactions(created_at);

-- Таблица платежей
CREATE TABLE IF NOT EXISTS payments (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    client_id INTEGER NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    amount REAL NOT NULL,
    payment_type VARCHAR(20) NOT NULL CHECK(payment_type IN ('cash', 'card', 'online', 'yoomoney', 'qiwi', 'installment')),
    payment_status VARCHAR(20) NOT NULL CHECK(payment_status IN ('pending', 'completed', 'failed', 'refunded')) DEFAULT 'pending',
    payment_method_details TEXT,
    transaction_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_client_id ON payments(client_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(payment_status);

-- Таблица комиссий платформы
CREATE TABLE IF NOT EXISTS platform_commissions (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    commission_amount REAL NOT NULL,
    commission_percentage REAL NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_platform_commissions_order_id ON platform_commissions(order_id);

-- Таблица баллов лояльности
CREATE TABLE IF NOT EXISTS loyalty_points (
    id SERIAL PRIMARY KEY,
    client_id INTEGER NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    points INTEGER NOT NULL,
    source_type VARCHAR(20) NOT NULL CHECK(source_type IN ('order', 'review', 'referral', 'bonus')),
    source_id INTEGER,
    description TEXT,
    expires_at TIMESTAMP,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_loyalty_points_client_id ON loyalty_points(client_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_points_source ON loyalty_points(source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_points_expires_at ON loyalty_points(expires_at);

-- Таблица транзакций лояльности
CREATE TABLE IF NOT EXISTS loyalty_transactions (
    id SERIAL PRIMARY KEY,
    client_id INTEGER NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    points INTEGER NOT NULL,
    transaction_type VARCHAR(20) NOT NULL CHECK(transaction_type IN ('earned', 'spent', 'expired', 'bonus')),
    description TEXT,
    related_order_id INTEGER REFERENCES orders(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_loyalty_transactions_client_id ON loyalty_transactions(client_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_transactions_type ON loyalty_transactions(transaction_type);

-- Таблица мастеров (индексы)
CREATE INDEX IF NOT EXISTS idx_masters_status ON masters(status);
CREATE INDEX IF NOT EXISTS idx_masters_user_id ON masters(user_id);
CREATE INDEX IF NOT EXISTS idx_masters_location ON masters USING GIST (point(longitude, latitude));

-- Функция для автоматического обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Триггеры для автоматического обновления updated_at
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_masters_updated_at BEFORE UPDATE ON masters
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_clients_updated_at BEFORE UPDATE ON clients
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_fcm_tokens_updated_at BEFORE UPDATE ON fcm_tokens
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


