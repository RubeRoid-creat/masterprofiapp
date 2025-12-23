# Настройка PostgreSQL на удаленном сервере 212.74.227.208

## Шаг 1: Подключение к серверу по SSH

### Windows (PowerShell):
```powershell
ssh root@212.74.227.208
# Или если есть конкретный пользователь:
ssh username@212.74.227.208
```

### Windows (PuTTY):
1. Скачайте PuTTY: https://www.putty.org/
2. Host Name: `212.74.227.208`
3. Port: `22`
4. Connection type: `SSH`
5. Click "Open"

---

## Шаг 2: Проверка операционной системы сервера

После подключения выполните:
```bash
cat /etc/os-release
```

Это покажет, какая ОС установлена (Ubuntu, Debian, CentOS и т.д.)

---

## Шаг 3: Установка PostgreSQL

### Для Ubuntu/Debian:
```bash
# Обновить список пакетов
sudo apt update

# Установить PostgreSQL
sudo apt install -y postgresql postgresql-contrib

# Проверить версию
psql --version

# Проверить статус службы
sudo systemctl status postgresql

# Если не запущен, запустить:
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### Для CentOS/RHEL/Rocky Linux:
```bash
# Установить репозиторий PostgreSQL
sudo dnf install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-8-x86_64/pgdg-redhat-repo-latest.noarch.rpm

# Отключить встроенный модуль PostgreSQL
sudo dnf -qy module disable postgresql

# Установить PostgreSQL 15
sudo dnf install -y postgresql15-server postgresql15-contrib

# Инициализировать базу данных
sudo /usr/pgsql-15/bin/postgresql-15-setup initdb

# Запустить службу
sudo systemctl start postgresql-15
sudo systemctl enable postgresql-15
```

---

## Шаг 4: Создание базы данных и пользователя

```bash
# Переключиться на пользователя postgres
sudo -u postgres psql

# В консоли PostgreSQL выполнить:
```

```sql
-- Создать базу данных для веб-сайта
CREATE DATABASE bestapp_website;

-- Создать базу данных для backend (если нужна отдельная)
CREATE DATABASE bestapp_backend;

-- Создать пользователя с паролем
CREATE USER masterprofi WITH PASSWORD 'Создайте_Сложный_Пароль_123!';

-- Дать права на базу данных
GRANT ALL PRIVILEGES ON DATABASE bestapp_website TO masterprofi;
GRANT ALL PRIVILEGES ON DATABASE bestapp_backend TO masterprofi;

-- Для PostgreSQL 15+ нужно также дать права на схему:
\c bestapp_website
GRANT ALL ON SCHEMA public TO masterprofi;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO masterprofi;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO masterprofi;

\c bestapp_backend
GRANT ALL ON SCHEMA public TO masterprofi;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO masterprofi;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO masterprofi;

-- Проверить созданные базы
\l

-- Выйти
\q
```

---

## Шаг 5: Настройка удаленного доступа

### 5.1. Настроить postgresql.conf

```bash
# Найти файл конфигурации (путь может отличаться):
# Ubuntu/Debian обычно: /etc/postgresql/15/main/postgresql.conf
# CentOS/RHEL обычно: /var/lib/pgsql/15/data/postgresql.conf

# Для Ubuntu/Debian:
sudo nano /etc/postgresql/15/main/postgresql.conf

# Для CentOS/RHEL:
sudo nano /var/lib/pgsql/15/data/postgresql.conf
```

Найдите строку:
```
#listen_addresses = 'localhost'
```

Измените на:
```
listen_addresses = '*'
```

Сохраните (Ctrl+O, Enter, Ctrl+X)

### 5.2. Настроить pg_hba.conf (правила доступа)

```bash
# Ubuntu/Debian:
sudo nano /etc/postgresql/15/main/pg_hba.conf

# CentOS/RHEL:
sudo nano /var/lib/pgsql/15/data/pg_hba.conf
```

Добавьте в конец файла:
```
# Разрешить подключения с любого IP с паролем MD5
host    all             all             0.0.0.0/0               md5
host    all             all             ::/0                    md5

# Или для большей безопасности - только с вашего IP
# host    all             all             ВАШ_IP/32              md5
```

Сохраните файл.

### 5.3. Перезапустить PostgreSQL

```bash
# Ubuntu/Debian:
sudo systemctl restart postgresql

# CentOS/RHEL:
sudo systemctl restart postgresql-15

# Проверить статус
sudo systemctl status postgresql
```

---

## Шаг 6: Настройка файрвола

### Для UFW (Ubuntu/Debian):
```bash
# Проверить статус
sudo ufw status

# Разрешить PostgreSQL порт
sudo ufw allow 5432/tcp

# Проверить правило
sudo ufw status numbered
```

### Для firewalld (CentOS/RHEL):
```bash
# Проверить статус
sudo firewall-cmd --state

# Разрешить PostgreSQL порт
sudo firewall-cmd --permanent --add-port=5432/tcp
sudo firewall-cmd --reload

# Проверить правило
sudo firewall-cmd --list-ports
```

### Для iptables:
```bash
# Разрешить входящие соединения на порт 5432
sudo iptables -A INPUT -p tcp --dport 5432 -j ACCEPT

# Сохранить правила
sudo netfilter-persistent save
# Или для CentOS:
sudo service iptables save
```

---

## Шаг 7: Проверка доступности с локальной машины

На вашем локальном компьютере (Windows):

```powershell
# Проверить порт
Test-NetConnection -ComputerName 212.74.227.208 -Port 5432

# Должно показать: TcpTestSucceeded : True
```

---

## Шаг 8: Настройка .env файла на локальной машине

Создайте файл `Z:\BestAPP\website\.env`:

```env
# Подключение к PostgreSQL на удаленном сервере
DATABASE_URL="postgresql://masterprofi:Создайте_Сложный_Пароль_123!@212.74.227.208:5432/bestapp_website"
```

Для backend создайте `Z:\BestAPP\backend\.env`:

```env
NODE_ENV=development
PORT=3000
DATABASE_TYPE=postgresql

# PostgreSQL настройки
POSTGRES_HOST=212.74.227.208
POSTGRES_PORT=5432
POSTGRES_DB=bestapp_backend
POSTGRES_USER=masterprofi
POSTGRES_PASSWORD=Создайте_Сложный_Пароль_123!

# JWT Secret (используйте надежный ключ)
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production

# Firebase (если используется)
FIREBASE_SERVICE_ACCOUNT=./firebase-service-account.json
```

---

## Шаг 9: Применение миграций

На локальной машине:

```powershell
cd Z:\BestAPP\website
node scripts/apply-migrations-direct.js
```

Должно вывести:
```
[INFO] Connecting to database...
[SUCCESS] Connected to database
[INFO] Reading migration file...
[INFO] Applying migration...
[SUCCESS] Migration applied successfully!
```

---

## Шаг 10: Проверка таблиц на сервере

На сервере:
```bash
sudo -u postgres psql -d bestapp_website

# В консоли PostgreSQL:
\dt  # Показать все таблицы

# Должны быть созданы:
# news, prices, forum_topics, forum_replies, contact_messages

\q  # Выйти
```

---

## Безопасность (ВАЖНО!)

### 1. Создайте надежный пароль:
```bash
# На сервере смените пароль пользователя
sudo -u postgres psql
ALTER USER masterprofi WITH PASSWORD 'Новый_Очень_Сложный_Пароль_2024!@#$';
\q
```

### 2. Ограничьте доступ по IP (рекомендуется):

Узнайте ваш внешний IP:
```powershell
# На локальной машине
Invoke-RestMethod -Uri 'https://api.ipify.org?format=json' | Select-Object -ExpandProperty ip
```

Затем на сервере в `pg_hba.conf` замените:
```
host    all             all             0.0.0.0/0               md5
```

На:
```
host    all             all             ВАШ_IP/32               md5
```

### 3. Настройте SSL (опционально, но рекомендуется):

```bash
# Включить SSL в postgresql.conf
ssl = on
ssl_cert_file = '/path/to/server.crt'
ssl_key_file = '/path/to/server.key'
```

Затем измените DATABASE_URL:
```env
DATABASE_URL="postgresql://masterprofi:пароль@212.74.227.208:5432/bestapp_website?sslmode=require"
```

---

## Резервное копирование

### Автоматическое резервное копирование (cron):

```bash
# Создать скрипт резервного копирования
sudo nano /usr/local/bin/postgres_backup.sh
```

```bash
#!/bin/bash
BACKUP_DIR="/var/backups/postgresql"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

# Бэкап базы website
pg_dump -U masterprofi -h localhost bestapp_website > $BACKUP_DIR/website_$DATE.sql

# Бэкап базы backend
pg_dump -U masterprofi -h localhost bestapp_backend > $BACKUP_DIR/backend_$DATE.sql

# Удалить бэкапы старше 30 дней
find $BACKUP_DIR -name "*.sql" -mtime +30 -delete

echo "Backup completed: $DATE"
```

```bash
# Сделать скрипт исполняемым
sudo chmod +x /usr/local/bin/postgres_backup.sh

# Добавить в cron (каждый день в 3:00)
sudo crontab -e

# Добавить строку:
0 3 * * * /usr/local/bin/postgres_backup.sh >> /var/log/postgres_backup.log 2>&1
```

---

## Мониторинг

### Проверить активные соединения:
```sql
SELECT * FROM pg_stat_activity WHERE datname = 'bestapp_website';
```

### Проверить размер баз данных:
```sql
SELECT pg_database.datname, 
       pg_size_pretty(pg_database_size(pg_database.datname)) AS size
FROM pg_database;
```

### Проверить логи:
```bash
# Ubuntu/Debian:
sudo tail -f /var/log/postgresql/postgresql-15-main.log

# CentOS/RHEL:
sudo tail -f /var/lib/pgsql/15/data/log/postgresql-*.log
```

---

## Решение проблем

### Ошибка: "connection refused"
- Проверьте, что PostgreSQL запущен: `sudo systemctl status postgresql`
- Проверьте файрвол: `sudo ufw status` или `sudo firewall-cmd --list-all`
- Проверьте `listen_addresses` в postgresql.conf

### Ошибка: "authentication failed"
- Проверьте пароль в .env файле
- Проверьте pg_hba.conf (должен быть метод `md5`)
- Попробуйте сбросить пароль: `ALTER USER masterprofi WITH PASSWORD 'новый_пароль';`

### Ошибка: "timeout"
- Проверьте сетевое соединение: `ping 212.74.227.208`
- Проверьте порт: `Test-NetConnection -ComputerName 212.74.227.208 -Port 5432`
- Возможно, провайдер блокирует порт 5432

### Порт 5432 заблокирован провайдером
Используйте SSH туннель:
```powershell
# На локальной машине создать туннель
ssh -L 5432:localhost:5432 root@212.74.227.208 -N

# Затем в .env используйте:
DATABASE_URL="postgresql://masterprofi:пароль@localhost:5432/bestapp_website"
```

---

## Команды для быстрой справки

```bash
# Войти в PostgreSQL
sudo -u postgres psql

# Подключиться к конкретной базе
psql -U masterprofi -d bestapp_website -h localhost

# Список баз данных
\l

# Список таблиц
\dt

# Описание таблицы
\d table_name

# Выйти
\q

# Перезапустить PostgreSQL
sudo systemctl restart postgresql

# Проверить логи
sudo journalctl -u postgresql -f
```

