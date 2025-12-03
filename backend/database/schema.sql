-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    role TEXT NOT NULL CHECK(role IN ('client', 'master', 'admin')),
    sponsor_id INTEGER, -- ID спонсора (мастера, который пригласил)
    rank TEXT DEFAULT 'junior_master' CHECK(rank IN ('junior_master', 'senior_master', 'team_leader', 'regional_manager')),
    email_verified INTEGER DEFAULT 0, -- 0 = false, 1 = true
    phone_verified INTEGER DEFAULT 0, -- 0 = false, 1 = true
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sponsor_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Таблица мастеров
CREATE TABLE IF NOT EXISTS masters (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL UNIQUE,
    specialization TEXT NOT NULL, -- JSON массив специализаций
    rating REAL DEFAULT 0.0,
    completed_orders INTEGER DEFAULT 0,
    status TEXT NOT NULL CHECK(status IN ('available', 'busy', 'offline')) DEFAULT 'offline',
    latitude REAL,
    longitude REAL,
    is_on_shift INTEGER DEFAULT 0, -- 0 = false, 1 = true
    balance REAL DEFAULT 0.0, -- Баланс кошелька мастера
    verification_status TEXT DEFAULT 'not_verified' CHECK(verification_status IN ('not_verified', 'pending', 'verified', 'rejected')),
    inn TEXT, -- ИНН мастера для верификации
    photo_url TEXT, -- URL фото профиля мастера
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Таблица клиентов
CREATE TABLE IF NOT EXISTS clients (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL UNIQUE,
    address TEXT,
    latitude REAL,
    longitude REAL,
    total_loyalty_points INTEGER DEFAULT 0, -- Общее количество баллов лояльности
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Таблица заказов
CREATE TABLE IF NOT EXISTS orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    -- Уникальный номер заявки (автогенерация)
    order_number TEXT UNIQUE,
    client_id INTEGER NOT NULL,
    
    -- Статус и приоритет
    request_status TEXT NOT NULL CHECK(request_status IN ('new', 'accepted', 'in_progress', 'completed', 'cancelled')) DEFAULT 'new',
    priority TEXT NOT NULL CHECK(priority IN ('emergency', 'urgent', 'regular', 'planned')) DEFAULT 'regular',
    order_source TEXT CHECK(order_source IN ('app', 'website', 'phone')) DEFAULT 'app',
    
    -- Информация о технике
    device_type TEXT NOT NULL,
    device_category TEXT CHECK(device_category IN ('large', 'small', 'builtin')),
    device_brand TEXT,
    device_model TEXT,
    device_serial_number TEXT,
    device_year INTEGER, -- Год выпуска/покупки
    warranty_status TEXT CHECK(warranty_status IN ('warranty', 'post_warranty')),
    
    -- Описание проблемы
    problem_short_description TEXT, -- Краткое описание (1-2 предложения)
    problem_description TEXT NOT NULL, -- Подробное описание
    problem_when_started TEXT, -- Когда началась проблема
    problem_conditions TEXT, -- При каких условиях проявляется
    problem_error_codes TEXT, -- Коды ошибок на дисплее
    problem_attempted_fixes TEXT, -- Что уже пробовали сделать
    
    -- Адрес (детализированный)
    address TEXT NOT NULL,
    address_street TEXT,
    address_building TEXT,
    address_apartment TEXT,
    address_floor INTEGER,
    address_entrance_code TEXT, -- Код домофона
    address_landmark TEXT, -- Ориентир
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    
    -- Временные параметры
    arrival_time TEXT, -- Формат: "HH:MM - HH:MM" или "утро"/"день"/"вечер"
    desired_repair_date DATE, -- Желаемая дата ремонта
    urgency TEXT CHECK(urgency IN ('emergency', 'urgent', 'planned')) DEFAULT 'planned', -- Экстренный, срочный, плановый
    
    -- Финансовые параметры
    estimated_cost REAL,
    final_cost REAL,
    client_budget REAL, -- Предварительный бюджет клиента
    payment_type TEXT CHECK(payment_type IN ('cash', 'card', 'online', 'installment')), -- Тип оплаты
    visit_cost REAL, -- Стоимость выезда
    max_cost_without_approval REAL, -- Максимальная стоимость без согласования
    
    -- Дополнительная информация
    intercom_working INTEGER DEFAULT 1, -- Домофон работает (1) / не работает (0)
    needs_pass INTEGER DEFAULT 0, -- Нужен пропуск
    parking_available INTEGER DEFAULT 1, -- Парковка для мастера
    has_pets INTEGER DEFAULT 0, -- Домашние животные
    has_small_children INTEGER DEFAULT 0, -- Дети маленькие
    needs_shoe_covers INTEGER DEFAULT 0, -- Нужны бахилы/сменная обувь
    preferred_contact_method TEXT CHECK(preferred_contact_method IN ('call', 'sms', 'chat')) DEFAULT 'call',
    
    -- Предпочтения по мастеру
    master_gender_preference TEXT CHECK(master_gender_preference IN ('male', 'female', 'any')) DEFAULT 'any',
    master_min_experience INTEGER, -- Минимальный опыт в годах
    preferred_master_id INTEGER, -- Конкретный мастер из предыдущих визитов
    
    -- Служебная информация
    assigned_master_id INTEGER,
    assignment_date DATETIME, -- Дата и время назначения
    preliminary_diagnosis TEXT, -- Предварительный диагноз
    required_parts TEXT, -- JSON массив необходимых запчастей
    special_equipment TEXT, -- Специальное оборудование для ремонта
    repair_complexity TEXT CHECK(repair_complexity IN ('simple', 'medium', 'complex')),
    estimated_repair_time INTEGER, -- Расчетное время работы в минутах
    
    -- Теги и категории
    problem_tags TEXT, -- JSON массив тегов для поиска
    problem_category TEXT CHECK(problem_category IN ('electrical', 'mechanical', 'electronic', 'software')),
    problem_seasonality TEXT CHECK(problem_seasonality IN ('seasonal', 'permanent')) DEFAULT 'permanent',
    
    -- Связанные данные
    related_order_id INTEGER, -- Связь с гарантийным случаем или предыдущим ремонтом
    
    -- Старые поля (для совместимости)
    order_type TEXT CHECK(order_type IN ('regular', 'urgent')) DEFAULT 'regular',
    repair_status TEXT NOT NULL CHECK(repair_status IN ('new', 'assigned', 'in_progress', 'completed', 'cancelled')) DEFAULT 'new',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    -- Внешние ключи (должны быть в конце)
    FOREIGN KEY (client_id) REFERENCES clients(id),
    FOREIGN KEY (preferred_master_id) REFERENCES masters(id),
    FOREIGN KEY (assigned_master_id) REFERENCES masters(id),
    FOREIGN KEY (related_order_id) REFERENCES orders(id)
);

-- Таблица назначений заказов
CREATE TABLE IF NOT EXISTS order_assignments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    master_id INTEGER NOT NULL,
    status TEXT NOT NULL CHECK(status IN ('pending', 'accepted', 'rejected', 'expired')) DEFAULT 'pending',
    assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    responded_at DATETIME,
    rejection_reason TEXT,
    attempt_number INTEGER DEFAULT 1, -- Номер попытки назначения (для увеличения таймаута)
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (master_id) REFERENCES masters(id)
);

-- Таблица истории статусов заказов
CREATE TABLE IF NOT EXISTS order_status_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    old_status TEXT,
    new_status TEXT NOT NULL,
    changed_by INTEGER, -- user_id
    note TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(id)
);

-- Таблица медиафайлов заказов (фото, видео, документы)
CREATE TABLE IF NOT EXISTS order_media (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    media_type TEXT NOT NULL CHECK(media_type IN ('photo', 'video', 'document', 'audio')),
    file_path TEXT NOT NULL, -- Путь к файлу на сервере
    file_url TEXT, -- URL для доступа к файлу
    file_name TEXT, -- Оригинальное имя файла
    file_size INTEGER, -- Размер файла в байтах
    mime_type TEXT, -- MIME тип файла
    description TEXT, -- Описание (например, "код ошибки на дисплее", "модельная бирка")
    thumbnail_url TEXT, -- URL миниатюры для видео
    duration INTEGER, -- Длительность видео/аудио в секундах (для видео максимум 60 секунд)
    upload_order INTEGER DEFAULT 0, -- Порядок отображения
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- Таблица истории обращений клиента
CREATE TABLE IF NOT EXISTS client_order_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    client_id INTEGER NOT NULL,
    order_id INTEGER NOT NULL,
    related_device_type TEXT, -- Тип техники для связи по типу устройства
    related_device_model TEXT, -- Модель техники для связи
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- Таблица истории ремонтов техники (для связывания ремонтов одного устройства)
CREATE TABLE IF NOT EXISTS device_repair_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    device_type TEXT NOT NULL,
    device_brand TEXT,
    device_model TEXT,
    device_serial_number TEXT, -- Если известен серийный номер, можно связывать ремонты
    repair_date DATETIME NOT NULL,
    repair_description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- Таблица чек-листов для диагностики (автоподбор по типу техники)
CREATE TABLE IF NOT EXISTS diagnostic_checklists (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_type TEXT NOT NULL,
    device_brand TEXT, -- Опционально, может быть общим для всех брендов
    device_model TEXT, -- Опционально, может быть специфичным для модели
    checklist_item TEXT NOT NULL,
    item_order INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Таблица частых проблем для моделей техники
CREATE TABLE IF NOT EXISTS common_problems (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_type TEXT NOT NULL,
    device_brand TEXT,
    device_model TEXT,
    problem_description TEXT NOT NULL,
    problem_code TEXT, -- Код ошибки (например, "E2")
    solution_hint TEXT, -- Подсказка по решению
    frequency_rating INTEGER DEFAULT 0, -- Частота проблемы (1-10)
    average_repair_time INTEGER, -- Среднее время ремонта в минутах
    average_repair_cost REAL, -- Средняя стоимость ремонта
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Таблица отзывов
CREATE TABLE IF NOT EXISTS reviews (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    master_id INTEGER NOT NULL,
    client_id INTEGER NOT NULL,
    rating INTEGER NOT NULL CHECK(rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (master_id) REFERENCES masters(id),
    FOREIGN KEY (client_id) REFERENCES clients(id)
);

-- Таблица версий приложений (для мастера и клиента)
CREATE TABLE IF NOT EXISTS app_versions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    platform TEXT NOT NULL, -- 'android_master', 'android_client'
    current_version TEXT NOT NULL, -- например '1.0.0'
    min_required_version TEXT NOT NULL, -- минимальная поддерживаемая версия
    force_update INTEGER DEFAULT 0, -- 0 = нет, 1 = да
    release_notes TEXT,
    download_url TEXT,
    supported_os_versions TEXT, -- JSON массив версий ОС
    released_at DATETIME DEFAULT CURRENT_TIMESTAMP
);


-- Таблица категорий услуг
CREATE TABLE IF NOT EXISTS service_categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    parent_id INTEGER,
    icon TEXT,
    order_index INTEGER DEFAULT 0,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES service_categories(id) ON DELETE CASCADE
);

-- Таблица шаблонов услуг
CREATE TABLE IF NOT EXISTS service_templates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id INTEGER,
    name TEXT NOT NULL,
    description TEXT,
    fixed_price REAL,
    estimated_time INTEGER, -- В минутах
    device_type TEXT, -- Тип техники для быстрого поиска
    is_popular INTEGER DEFAULT 0, -- Популярный шаблон (1) или нет (0)
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES service_categories(id) ON DELETE SET NULL
);

-- Таблица FCM токенов для push-уведомлений
CREATE TABLE IF NOT EXISTS fcm_tokens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    token TEXT NOT NULL UNIQUE,
    device_type TEXT, -- 'android', 'ios'
    device_id TEXT, -- Уникальный ID устройства
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Таблица транзакций мастера (кошелек)
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
);

-- Индексы для оптимизации запросов
CREATE INDEX IF NOT EXISTS idx_orders_client_id ON orders(client_id);
CREATE INDEX IF NOT EXISTS idx_orders_master_id ON orders(assigned_master_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(repair_status);
CREATE INDEX IF NOT EXISTS idx_orders_request_status ON orders(request_status);
CREATE INDEX IF NOT EXISTS idx_orders_order_number ON orders(order_number);
CREATE INDEX IF NOT EXISTS idx_orders_device_type ON orders(device_type);
CREATE INDEX IF NOT EXISTS idx_orders_priority ON orders(priority);
CREATE INDEX IF NOT EXISTS idx_assignments_order_id ON order_assignments(order_id);
CREATE INDEX IF NOT EXISTS idx_assignments_master_id ON order_assignments(master_id);
CREATE INDEX IF NOT EXISTS idx_assignments_status ON order_assignments(status);
CREATE INDEX IF NOT EXISTS idx_masters_status ON masters(status);
CREATE INDEX IF NOT EXISTS idx_masters_user_id ON masters(user_id);
CREATE INDEX IF NOT EXISTS idx_clients_user_id ON clients(user_id);
CREATE INDEX IF NOT EXISTS idx_order_media_order_id ON order_media(order_id);
CREATE INDEX IF NOT EXISTS idx_order_media_type ON order_media(media_type);
CREATE INDEX IF NOT EXISTS idx_client_order_history_client_id ON client_order_history(client_id);
CREATE INDEX IF NOT EXISTS idx_device_repair_history_order_id ON device_repair_history(order_id);
CREATE INDEX IF NOT EXISTS idx_device_repair_history_serial ON device_repair_history(device_serial_number);
CREATE INDEX IF NOT EXISTS idx_common_problems_device ON common_problems(device_type, device_brand, device_model);
CREATE INDEX IF NOT EXISTS idx_service_categories_parent_id ON service_categories(parent_id);
CREATE INDEX IF NOT EXISTS idx_service_templates_category_id ON service_templates(category_id);
CREATE INDEX IF NOT EXISTS idx_service_templates_device_type ON service_templates(device_type);
CREATE INDEX IF NOT EXISTS idx_service_templates_popular ON service_templates(is_popular);
CREATE INDEX IF NOT EXISTS idx_fcm_tokens_user_id ON fcm_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_fcm_tokens_token ON fcm_tokens(token);
CREATE INDEX IF NOT EXISTS idx_master_transactions_master_id ON master_transactions(master_id);
CREATE INDEX IF NOT EXISTS idx_master_transactions_order_id ON master_transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_master_transactions_type ON master_transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_master_transactions_status ON master_transactions(status);
CREATE INDEX IF NOT EXISTS idx_master_transactions_created_at ON master_transactions(created_at);

-- Дополнительные составные индексы для оптимизации частых запросов
CREATE INDEX IF NOT EXISTS idx_orders_client_status ON orders(client_id, repair_status);
CREATE INDEX IF NOT EXISTS idx_orders_master_status ON orders(assigned_master_id, repair_status);
CREATE INDEX IF NOT EXISTS idx_orders_status_created ON orders(repair_status, created_at);
CREATE INDEX IF NOT EXISTS idx_orders_device_type_status ON orders(device_type, repair_status);
CREATE INDEX IF NOT EXISTS idx_orders_urgency_created ON orders(urgency, created_at);
CREATE INDEX IF NOT EXISTS idx_masters_status_shift ON masters(status, is_on_shift);
CREATE INDEX IF NOT EXISTS idx_masters_location_status ON masters(latitude, longitude, status);
CREATE INDEX IF NOT EXISTS idx_reviews_master_created ON reviews(master_id, created_at);
CREATE INDEX IF NOT EXISTS idx_reviews_order_master ON reviews(order_id, master_id);
CREATE INDEX IF NOT EXISTS idx_payments_client_status ON payments(client_id, payment_status);
CREATE INDEX IF NOT EXISTS idx_payments_order_status ON payments(order_id, payment_status);
CREATE INDEX IF NOT EXISTS idx_loyalty_points_client_used ON loyalty_points(client_id, used);
CREATE INDEX IF NOT EXISTS idx_loyalty_points_client_expires ON loyalty_points(client_id, expires_at, used);

-- Таблица расписания мастера
CREATE TABLE IF NOT EXISTS master_schedule (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    master_id INTEGER NOT NULL,
    date DATE NOT NULL, -- Дата расписания
    start_time TIME, -- Время начала работы (NULL = недоступен весь день)
    end_time TIME, -- Время окончания работы (NULL = недоступен весь день)
    is_available INTEGER DEFAULT 1, -- 1 = доступен, 0 = недоступен
    note TEXT, -- Примечание (например, "Выезд в другой город")
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (master_id) REFERENCES masters(id) ON DELETE CASCADE,
    UNIQUE(master_id, date)
);

CREATE INDEX IF NOT EXISTS idx_master_schedule_master_id ON master_schedule(master_id);
CREATE INDEX IF NOT EXISTS idx_master_schedule_date ON master_schedule(date);
CREATE INDEX IF NOT EXISTS idx_master_schedule_master_date ON master_schedule(master_id, date);

-- Таблица отчетов о выполненной работе
CREATE TABLE IF NOT EXISTS work_reports (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    master_id INTEGER NOT NULL,
    client_id INTEGER NOT NULL,
    report_type TEXT DEFAULT 'standard', -- 'standard', 'detailed', 'template'
    work_description TEXT NOT NULL, -- Описание выполненной работы
    parts_used TEXT, -- JSON массив использованных запчастей
    work_duration INTEGER, -- Длительность работы в минутах
    total_cost REAL NOT NULL, -- Общая стоимость работ
    parts_cost REAL DEFAULT 0, -- Стоимость запчастей
    labor_cost REAL DEFAULT 0, -- Стоимость работы
    before_photos TEXT, -- JSON массив URL фото "до"
    after_photos TEXT, -- JSON массив URL фото "после"
    client_signature TEXT, -- Base64 подпись клиента
    client_signed_at DATETIME, -- Дата подписания клиентом
    master_signed_at DATETIME DEFAULT CURRENT_TIMESTAMP, -- Дата подписания мастером
    status TEXT DEFAULT 'draft', -- 'draft', 'pending_signature', 'signed', 'completed'
    template_id INTEGER, -- ID шаблона (если использован)
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (master_id) REFERENCES masters(id) ON DELETE CASCADE,
    FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_work_reports_order_id ON work_reports(order_id);
CREATE INDEX IF NOT EXISTS idx_work_reports_master_id ON work_reports(master_id);
CREATE INDEX IF NOT EXISTS idx_work_reports_client_id ON work_reports(client_id);
CREATE INDEX IF NOT EXISTS idx_work_reports_status ON work_reports(status);

-- Таблица шаблонов отчетов
CREATE TABLE IF NOT EXISTS report_templates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    master_id INTEGER, -- NULL = общий шаблон
    name TEXT NOT NULL, -- Название шаблона
    description TEXT, -- Описание шаблона
    work_description_template TEXT, -- Шаблон описания работы
    default_parts TEXT, -- JSON массив стандартных запчастей
    default_labor_cost REAL, -- Стандартная стоимость работы
    is_active INTEGER DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (master_id) REFERENCES masters(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_report_templates_master_id ON report_templates(master_id);

-- Таблица документов верификации мастеров
CREATE TABLE IF NOT EXISTS master_verification_documents (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  master_id INTEGER NOT NULL,
  document_type TEXT NOT NULL CHECK(document_type IN ('passport', 'certificate', 'diploma', 'license', 'other')),
  document_name TEXT NOT NULL,
  file_url TEXT NOT NULL,
  file_name TEXT,
  file_size INTEGER,
  mime_type TEXT,
  status TEXT NOT NULL CHECK(status IN ('pending', 'approved', 'rejected')) DEFAULT 'pending',
  rejection_reason TEXT,
  reviewed_by INTEGER, -- user_id админа
  reviewed_at DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (master_id) REFERENCES masters(id) ON DELETE CASCADE,
  FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_verification_documents_master_id 
ON master_verification_documents(master_id);

CREATE INDEX IF NOT EXISTS idx_verification_documents_status 
ON master_verification_documents(status);

-- Таблица жалоб
CREATE TABLE IF NOT EXISTS complaints (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  order_id INTEGER,
  complainant_user_id INTEGER NOT NULL, -- ID пользователя, который подал жалобу
  complainant_role TEXT NOT NULL CHECK(complainant_role IN ('client', 'master')),
  accused_user_id INTEGER NOT NULL, -- ID пользователя, на которого подали жалобу
  accused_role TEXT NOT NULL CHECK(accused_role IN ('client', 'master')),
  complaint_type TEXT NOT NULL CHECK(complaint_type IN ('quality', 'behavior', 'payment', 'other')),
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  evidence_urls TEXT, -- JSON массив URL файлов (скриншоты, фото)
  status TEXT NOT NULL CHECK(status IN ('pending', 'reviewing', 'resolved', 'rejected', 'dismissed')) DEFAULT 'pending',
  resolution TEXT, -- Решение админа
  resolved_by INTEGER, -- user_id админа
  resolved_at DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL,
  FOREIGN KEY (complainant_user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (accused_user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (resolved_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_complaints_order_id ON complaints(order_id);
CREATE INDEX IF NOT EXISTS idx_complaints_complainant ON complaints(complainant_user_id);
CREATE INDEX IF NOT EXISTS idx_complaints_accused ON complaints(accused_user_id);
CREATE INDEX IF NOT EXISTS idx_complaints_status ON complaints(status);

-- Таблица платежей
CREATE TABLE IF NOT EXISTS payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    client_id INTEGER NOT NULL,
    amount REAL NOT NULL,
    currency TEXT DEFAULT 'RUB',
    payment_method TEXT NOT NULL CHECK(payment_method IN ('cash', 'card', 'online', 'yoomoney', 'qiwi', 'installment')),
    payment_provider TEXT DEFAULT 'manual' CHECK(payment_provider IN ('manual', 'yookassa', 'stripe', 'paypal')),
    payment_status TEXT NOT NULL CHECK(payment_status IN ('pending', 'processing', 'completed', 'failed', 'refunded', 'cancelled')) DEFAULT 'pending',
    provider_payment_id TEXT, -- ID платежа в платежной системе
    provider_response TEXT, -- JSON ответ от платежной системы
    receipt_url TEXT, -- URL чека
    receipt_data TEXT, -- JSON данные чека
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    paid_at DATETIME, -- Дата фактической оплаты
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

-- Таблица комиссий платформы
CREATE TABLE IF NOT EXISTS platform_commissions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    payment_id INTEGER NOT NULL,
    master_id INTEGER NOT NULL,
    order_amount REAL NOT NULL,
    commission_percentage REAL NOT NULL,
    commission_amount REAL NOT NULL,
    status TEXT NOT NULL CHECK(status IN ('pending', 'collected', 'refunded')) DEFAULT 'pending',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    collected_at DATETIME,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    FOREIGN KEY (master_id) REFERENCES masters(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_client_id ON payments(client_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(payment_status);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at);
CREATE INDEX IF NOT EXISTS idx_platform_commissions_order_id ON platform_commissions(order_id);
CREATE INDEX IF NOT EXISTS idx_platform_commissions_payment_id ON platform_commissions(payment_id);
CREATE INDEX IF NOT EXISTS idx_platform_commissions_master_id ON platform_commissions(master_id);
CREATE INDEX IF NOT EXISTS idx_platform_commissions_status ON platform_commissions(status);

-- Таблица баллов лояльности
CREATE TABLE IF NOT EXISTS loyalty_points (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    client_id INTEGER NOT NULL,
    points INTEGER NOT NULL,
    source_type TEXT NOT NULL CHECK(source_type IN ('order', 'review', 'referral', 'bonus')),
    source_id INTEGER, -- ID заказа, отзыва и т.д.
    description TEXT,
    expires_at DATETIME,
    used INTEGER DEFAULT 0, -- 0 = не использовано, 1 = использовано
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

-- Миграция: добавляем колонку used, если таблица уже существует
-- SQLite не поддерживает ALTER TABLE ADD COLUMN IF NOT EXISTS напрямую,
-- поэтому проверяем через PRAGMA table_info

-- Таблица транзакций использования баллов
CREATE TABLE IF NOT EXISTS loyalty_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    client_id INTEGER NOT NULL,
    points_used INTEGER NOT NULL,
    order_id INTEGER, -- ID заказа, в котором использованы баллы
    discount_amount REAL NOT NULL, -- Сумма скидки в рублях
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL
);

-- Добавляем поле total_loyalty_points в таблицу clients
-- Проверяем, существует ли поле
-- Если нет, добавим через миграцию

CREATE INDEX IF NOT EXISTS idx_loyalty_points_client_id ON loyalty_points(client_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_points_source_type ON loyalty_points(source_type);
CREATE INDEX IF NOT EXISTS idx_loyalty_points_expires_at ON loyalty_points(expires_at);
CREATE INDEX IF NOT EXISTS idx_loyalty_points_used ON loyalty_points(used);
CREATE INDEX IF NOT EXISTS idx_loyalty_transactions_client_id ON loyalty_transactions(client_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_transactions_order_id ON loyalty_transactions(order_id);

-- ==================== MLM СИСТЕМА ====================

-- Таблица структуры сети (иерархия мастеров)
CREATE TABLE IF NOT EXISTS network_structure (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL, -- ID пользователя (мастера)
    sponsor_id INTEGER NOT NULL, -- ID спонсора (мастера, который пригласил)
    level INTEGER NOT NULL CHECK(level >= 1 AND level <= 3), -- Уровень в структуре (1-3)
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (sponsor_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, sponsor_id, level)
);

CREATE INDEX IF NOT EXISTS idx_network_structure_user_id ON network_structure(user_id);
CREATE INDEX IF NOT EXISTS idx_network_structure_sponsor_id ON network_structure(sponsor_id);
CREATE INDEX IF NOT EXISTS idx_network_structure_level ON network_structure(level);

-- Таблица MLM комиссий
CREATE TABLE IF NOT EXISTS mlm_commissions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    from_user_id INTEGER NOT NULL, -- ID мастера, выполнившего заказ
    to_user_id INTEGER NOT NULL, -- ID мастера, которому начисляется комиссия
    amount REAL NOT NULL, -- Сумма заказа
    commission_rate REAL NOT NULL, -- Процент комиссии (0.03 = 3%)
    commission_amount REAL NOT NULL, -- Сумма комиссии
    level INTEGER NOT NULL CHECK(level >= 1 AND level <= 3), -- Уровень в структуре
    commission_type TEXT DEFAULT 'referral' CHECK(commission_type IN ('referral', 'volume_bonus', 'team_growth', 'leadership')),
    status TEXT DEFAULT 'pending' CHECK(status IN ('pending', 'completed', 'cancelled')),
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (from_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (to_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mlm_commissions_order_id ON mlm_commissions(order_id);
CREATE INDEX IF NOT EXISTS idx_mlm_commissions_from_user_id ON mlm_commissions(from_user_id);
CREATE INDEX IF NOT EXISTS idx_mlm_commissions_to_user_id ON mlm_commissions(to_user_id);
CREATE INDEX IF NOT EXISTS idx_mlm_commissions_level ON mlm_commissions(level);
CREATE INDEX IF NOT EXISTS idx_mlm_commissions_status ON mlm_commissions(status);
CREATE INDEX IF NOT EXISTS idx_mlm_commissions_created_at ON mlm_commissions(created_at);

-- Таблица истории изменения рангов
CREATE TABLE IF NOT EXISTS ranks_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    old_rank TEXT,
    new_rank TEXT NOT NULL CHECK(new_rank IN ('junior_master', 'senior_master', 'team_leader', 'regional_manager')),
    achieved_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    reason TEXT, -- Причина повышения (например, "50 заказов выполнено")
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ranks_history_user_id ON ranks_history(user_id);
CREATE INDEX IF NOT EXISTS idx_ranks_history_achieved_at ON ranks_history(achieved_at);

-- Индексы для оптимизации MLM запросов
-- Индексы idx_users_sponsor_id и idx_users_rank создаются в server.js после добавления колонок

-- Таблица кодов подтверждения (email и телефон)
CREATE TABLE IF NOT EXISTS verification_codes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    type TEXT NOT NULL CHECK(type IN ('email', 'phone')),
    code TEXT NOT NULL, -- 6-значный код
    expires_at DATETIME NOT NULL, -- Время истечения (обычно 10 минут)
    verified INTEGER DEFAULT 0, -- 0 = не использован, 1 = использован
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_verification_codes_user_id ON verification_codes(user_id);
CREATE INDEX IF NOT EXISTS idx_verification_codes_type ON verification_codes(type);
CREATE INDEX IF NOT EXISTS idx_verification_codes_code ON verification_codes(code);
CREATE INDEX IF NOT EXISTS idx_verification_codes_expires_at ON verification_codes(expires_at);


