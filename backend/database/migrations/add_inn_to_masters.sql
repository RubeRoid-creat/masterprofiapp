-- Миграция: добавление поля ИНН в таблицу masters
-- Выполнить на сервере: sqlite3 database.db < migrations/add_inn_to_masters.sql

-- Проверяем, существует ли уже поле inn
-- Если нет, добавляем его
ALTER TABLE masters ADD COLUMN inn TEXT;






