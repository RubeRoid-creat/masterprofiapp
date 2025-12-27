# Настройка домена ispravleno.pro

## Быстрая инструкция

### Шаг 1: Подключитесь к серверу

```bash
ssh user@212.74.227.208
```

### Шаг 2: Перейдите в директорию проекта

```bash
cd /var/www/ispravleno-website/website
```

### Шаг 3: Обновите код из репозитория

```bash
git pull origin main
```

### Шаг 4: Запустите скрипт настройки

```bash
# Сделайте скрипт исполняемым
chmod +x setup-ispravleno-pro.sh

# Запустите скрипт
./setup-ispravleno-pro.sh
```

Скрипт автоматически:
- ✅ Установит Nginx (если не установлен)
- ✅ Проверит DNS записи
- ✅ Настроит конфигурацию Nginx для ispravleno.pro
- ✅ Предложит установить SSL сертификат
- ✅ Обновит .env файл
- ✅ Обновит next.config.js

### Шаг 5: Пересоберите и перезапустите

```bash
# Пересоберите проект
npm run build

# Перезапустите PM2
pm2 restart ispravleno-website
```

### Шаг 6: Проверьте сайт

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

## Если что-то не работает

1. **Сайт не открывается**
   - Проверьте DNS: `dig ispravleno.pro`
   - Проверьте статус Nginx: `sudo systemctl status nginx`
   - Проверьте логи: `sudo tail -f /var/log/nginx/ispravleno-website-error.log`

2. **502 Bad Gateway**
   - Проверьте, что приложение запущено: `pm2 list`
   - Проверьте, что приложение слушает порт 3003: `netstat -tulpn | grep 3003`
   - Проверьте логи PM2: `pm2 logs ispravleno-website`

3. **SSL не работает**
   - Убедитесь, что порты 80 и 443 открыты: `sudo ufw status`
   - Проверьте сертификат: `sudo certbot certificates`
   - Попробуйте обновить: `sudo certbot renew`
