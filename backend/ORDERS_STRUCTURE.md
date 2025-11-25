# Структура заявок на ремонт

## Реализованная структура

Была реализована полная структура заявок на ремонт согласно спецификации.

## База данных

### Таблица `orders` - Расширена

Добавлены все поля из спецификации:

#### Основная информация
- `order_number` - Уникальный номер заявки (автогенерация в формате #XXXX-КЛ)
- `request_status` - Статус заявки (new, accepted, in_progress, completed, cancelled)
- `priority` - Приоритет (emergency, urgent, regular, planned)
- `order_source` - Источник заявки (app, website, phone)

#### Информация о технике
- `device_category` - Категория (large, small, builtin)
- `device_serial_number` - Серийный номер
- `device_year` - Год выпуска/покупки
- `warranty_status` - Гарантийный статус (warranty, post_warranty)

#### Описание проблемы
- `problem_short_description` - Краткое описание (1-2 предложения)
- `problem_when_started` - Когда началась проблема
- `problem_conditions` - При каких условиях проявляется
- `problem_error_codes` - Коды ошибок на дисплее
- `problem_attempted_fixes` - Что уже пробовали сделать

#### Адрес (детализированный)
- `address_street` - Улица
- `address_building` - Дом
- `address_apartment` - Квартира
- `address_floor` - Этаж
- `address_entrance_code` - Код домофона
- `address_landmark` - Ориентир

#### Временные параметры
- `desired_repair_date` - Желаемая дата ремонта
- `urgency` - Срочность (emergency, urgent, planned)

#### Финансовые параметры
- `client_budget` - Предварительный бюджет клиента
- `payment_type` - Тип оплаты (cash, card, online, installment)
- `visit_cost` - Стоимость выезда
- `max_cost_without_approval` - Максимальная стоимость без согласования

#### Дополнительная информация
- `intercom_working` - Домофон работает (1) / не работает (0)
- `needs_pass` - Нужен пропуск
- `parking_available` - Парковка для мастера
- `has_pets` - Домашние животные
- `has_small_children` - Дети маленькие
- `needs_shoe_covers` - Нужны бахилы/сменная обувь
- `preferred_contact_method` - Предпочтительный способ связи (call, sms, chat)

#### Предпочтения по мастеру
- `master_gender_preference` - Предпочтение по полу (male, female, any)
- `master_min_experience` - Минимальный опыт в годах
- `preferred_master_id` - Конкретный мастер из предыдущих визитов

#### Служебная информация
- `assignment_date` - Дата и время назначения
- `preliminary_diagnosis` - Предварительный диагноз
- `required_parts` - JSON массив необходимых запчастей
- `special_equipment` - Специальное оборудование для ремонта
- `repair_complexity` - Сложность ремонта (simple, medium, complex)
- `estimated_repair_time` - Расчетное время работы в минутах

#### Теги и категории
- `problem_tags` - JSON массив тегов для поиска
- `problem_category` - Категория проблемы (electrical, mechanical, electronic, software)
- `problem_seasonality` - Сезонность проблемы (seasonal, permanent)

#### Связанные данные
- `related_order_id` - Связь с гарантийным случаем или предыдущим ремонтом

### Новые таблицы

#### `order_media` - Медиафайлы заказов
- Фотографии (до 5 штук)
- Видео (до 1 минуты, опционально)
- Документы (гарантийные талоны, чеки, инструкции)
- Аудиосообщения

#### `client_order_history` - История обращений клиента
- Связь между заказами одного клиента
- Тип и модель техники для связи

#### `device_repair_history` - История ремонтов техники
- История ремонтов одного устройства (по серийному номеру)
- Описание ремонта и дата

#### `diagnostic_checklists` - Чек-листы для диагностики
- Автоподбор по типу техники
- Пункты чек-листа для мастера

#### `common_problems` - Частые проблемы для моделей техники
- Список частых проблем для данной модели
- Коды ошибок, подсказки по решению
- Среднее время и стоимость ремонта

## API

### Создание заказа (POST /api/orders)

Теперь поддерживает все новые поля. Пример запроса:

```json
{
  "device_type": "Стиральная машина",
  "device_category": "large",
  "device_brand": "Samsung",
  "device_model": "WW70J52E0HW",
  "device_serial_number": "SN123456",
  "device_year": 2022,
  "warranty_status": "post_warranty",
  "problem_short_description": "Не отжимает, показывает ошибку E2",
  "problem_description": "Машина нормально стирает, но когда доходит до отжима останавливается и показывает ошибку E2",
  "problem_when_started": "Вчера вечером",
  "problem_error_codes": "E2",
  "address": "ул. Ленина, 15, кв. 42",
  "address_street": "ул. Ленина",
  "address_building": "15",
  "address_apartment": "42",
  "address_floor": 3,
  "address_entrance_code": "148",
  "latitude": 56.859611,
  "longitude": 35.911896,
  "arrival_time": "10:00-13:00",
  "urgency": "urgent",
  "priority": "urgent",
  "client_budget": 5000,
  "payment_type": "card",
  "intercom_working": true,
  "parking_available": true,
  "has_small_children": true,
  "preferred_contact_method": "call",
  "problem_tags": ["не отжимает", "ошибка E2"],
  "problem_category": "electronic"
}
```

### Получение заказа (GET /api/orders/:id)

Возвращает полную информацию о заказе, включая:
- Все поля заказа
- Медиафайлы (фото, видео, документы)
- Историю обращений клиента
- Историю ремонтов техники (если есть серийный номер)
- Частые проблемы для данной модели

### Медиафайлы

#### GET /api/orders/:id/media
Получить все медиафайлы заказа

#### POST /api/orders/:id/media
Добавить медиафайл к заказу

```json
{
  "media_type": "photo",
  "file_url": "https://example.com/image.jpg",
  "file_name": "error_display.jpg",
  "description": "Код ошибки на дисплее",
  "mime_type": "image/jpeg",
  "file_size": 102400
}
```

#### DELETE /api/orders/:id/media/:mediaId
Удалить медиафайл

### История обращений

#### GET /api/orders/client/:clientId/history
Получить историю обращений клиента

### Частые проблемы

#### GET /api/orders/common-problems/:deviceType?deviceBrand=XXX&deviceModel=YYY
Получить частые проблемы для типа техники

## Миграция

Для обновления существующей базы данных:

```bash
npm run migrate
```

Скрипт миграции:
- Добавляет все новые поля в таблицу `orders`
- Создает новые таблицы
- Генерирует номера заявок для существующих заказов
- Создает необходимые индексы

## Генерация номера заявки

Номер заявки генерируется автоматически при создании заказа в формате:
- `#XXXX-КЛ` где XXXX - порядковый номер заявки с ведущими нулями

Пример: `#0001-КЛ`, `#2847-КЛ`






