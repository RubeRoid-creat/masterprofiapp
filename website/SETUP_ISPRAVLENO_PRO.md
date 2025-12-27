# Настройка домена ispravleno.pro

## Быстрая инструкция

### Важно: Запустите скрипт из директории website проекта

```bash
# Перейдите в директорию website (где находится package.json)
cd ~/Ispravleno/ispravleno-website/website
# или
cd /var/www/ispravleno-website/website
# или откуда у вас находится проект

# Убедитесь, что вы в правильной директории
ls package.json  # Должен показать файл package.json
```

### Шаг 1: Обновите код из репозитория

```bash
git pull origin main
```

### Шаг 2: Запустите скрипт настройки

```bash
# Сделайте скрипт исполняемым
chmod +x setup-ispravleno-pro.sh

# Запустите скрипт (из директории website!)
./setup-ispravleno-pro.sh
```

Скрипт автоматически:
- ✅ Установит Nginx (если не установлен)
- ✅ Проверит DNS записи
- ✅ Настроит конфигурацию Nginx для ispravleno.pro
- ✅ Предложит установить SSL сертификат
- ✅ Обновит .env файл
- ✅ Обновит next.config.js

### Шаг 3: Пересоберите и перезапустите

```bash
# Убедитесь, что вы в директории проекта
cd ~/Ispravleno/ispravleno-website/website
# или где у вас находится проект

# Пересоберите проект
npm run build

# Перезапустите PM2
pm2 restart ispravleno-website
```

### Шаг 4: Проверьте сайт

Откройте в браузере:
- http://ispravleno.pro
- https://ispravleno.pro (после установки SSL)

---

## Если нужно настроить SSL вручную

Если скрипт не смог автоматически установить SSL:

```bash
# Установите Certbot (если не установлен)
sudo apt install certbot python3-certbot-nginx -y

# Получите SSL сертификат
sudo certbot --nginx -d ispravleno.pro -d www.ispravleno.pro

# После установки SSL обновите .env
cd ~/Ispravleno/ispravleno-website/website  # или ваш путь
nano .env
# Измените:
# NEXT_PUBLIC_SITE_URL="https://ispravleno.pro"

# Пересоберите и перезапустите
npm run build
pm2 restart ispravleno-website
```

---

## Проверка работы

### Проверить статус Nginx
```bash
sudo systemctl status nginx
```

### Проверить логи Nginx
```bash
sudo tail -f /var/log/nginx/ispravleno-website-error.log
```

### Проверить статус PM2
```bash
pm2 logs ispravleno-website
```

### Проверить доступность приложения
```bash
curl http://localhost:3003
```

---

## Решение проблем

### Ошибка: "No such file or directory"

Если скрипт выдает ошибку о том, что директория не найдена:

1. Убедитесь, что вы запускаете скрипт из директории `website` проекта:
   ```bash
   pwd  # Проверьте текущую директорию
   ls package.json  # Должен показать package.json
   ```

2. Если проект находится в другой директории, обновите скрипт или запустите из правильной директории.

### Сайт не открывается

1. Проверьте DNS: `dig ispravleno.pro`
2. Проверьте статус Nginx: `sudo systemctl status nginx`
3. Проверьте логи: `sudo tail -f /var/log/nginx/ispravleno-website-error.log`

### 502 Bad Gateway

1. Проверьте, что приложение запущено: `pm2 list`
2. Проверьте, что приложение слушает порт 3003: `netstat -tulpn | grep 3003`
3. Проверьте логи PM2: `pm2 logs ispravleno-website`

### SSL не работает

1. Убедитесь, что порты 80 и 443 открыты: `sudo ufw status`
2. Проверьте сертификат: `sudo certbot certificates`
3. Попробуйте обновить: `sudo certbot renew`
