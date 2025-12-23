# 🚀 Быстрый старт: Настройка PostgreSQL на сервере 212.74.227.208

## 📋 Что нужно сделать (Краткая версия)

### 1️⃣ Подключитесь к серверу
```bash
ssh root@212.74.227.208
```

### 2️⃣ Установите PostgreSQL (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install -y postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### 3️⃣ Создайте базу данных и пользователя
```bash
sudo -u postgres psql
```

В консоли PostgreSQL:
```sql
CREATE DATABASE bestapp_website;
CREATE USER masterprofi WITH PASSWORD 'Ваш_Сложный_Пароль_123!';
GRANT ALL PRIVILEGES ON DATABASE bestapp_website TO masterprofi;
\c bestapp_website
GRANT ALL ON SCHEMA public TO masterprofi;
\q
```

### 4️⃣ Разрешите удаленные подключения

**postgresql.conf:**
```bash
sudo nano /etc/postgresql/15/main/postgresql.conf
# Найти и изменить:
listen_addresses = '*'
```

**pg_hba.conf:**
```bash
sudo nano /etc/postgresql/15/main/pg_hba.conf
# Добавить в конец:
host    all    all    0.0.0.0/0    md5
```

**Перезапустить:**
```bash
sudo systemctl restart postgresql
```

### 5️⃣ Откройте порт в файрволе
```bash
sudo ufw allow 5432/tcp
sudo ufw reload
```

### 6️⃣ Создайте .env на локальной машине

Файл: `Z:\BestAPP\website\.env`
```env
DATABASE_URL="postgresql://masterprofi:Ваш_Сложный_Пароль_123!@212.74.227.208:5432/bestapp_website"
```

### 7️⃣ Примените миграции
```powershell
cd Z:\BestAPP\website
node scripts/apply-migrations-direct.js
```

## ✅ Проверка

Запустите тест подключения:
```powershell
cd Z:\BestAPP\website
.\test-connection.ps1
```

---

## 📚 Полная документация

- **Подробная инструкция:** `POSTGRESQL_SERVER_SETUP.md`
- **Руководство по миграциям:** `MIGRATIONS_GUIDE.md`

---

## ❓ Частые проблемы

| Проблема | Решение |
|----------|---------|
| Port 5432 закрыт | Проверьте файрвол: `sudo ufw status` |
| Authentication failed | Проверьте пароль в .env |
| Connection timeout | Проверьте `listen_addresses` в postgresql.conf |
| Prisma CDN недоступен | Используйте `apply-migrations-direct.js` |

---

## 🔐 Безопасность

⚠️ **ВАЖНО:** Замените `0.0.0.0/0` на ваш конкретный IP для безопасности!

Узнать ваш IP:
```powershell
Invoke-RestMethod https://api.ipify.org
```

В pg_hba.conf:
```
host    all    all    ВАШ_IP/32    md5
```

---

## 📞 Нужна помощь?

1. Запустите диагностику: `.\test-connection.ps1`
2. Проверьте логи на сервере: `sudo journalctl -u postgresql -f`
3. См. раздел "Решение проблем" в `POSTGRESQL_SERVER_SETUP.md`

