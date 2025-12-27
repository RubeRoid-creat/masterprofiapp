# Настройка доменного имени для сайта

## Шаг 1: Получение доменного имени

1. Купите домен у регистратора (например, reg.ru, nic.ru, namecheap.com)
2. Запомните доменное имя (например: `ispravleno.ru` или `masterprofi.ru`)

## Шаг 2: Настройка DNS записей

В панели управления доменом добавьте DNS записи:

### Вариант A: A-запись (прямое указание на IP)

```
Тип: A
Имя: @ (или оставить пустым)
Значение: 212.74.227.208
TTL: 3600

Тип: A
Имя: www
Значение: 212.74.227.208
TTL: 3600
```

### Вариант B: CNAME запись (если используете поддомен)

```
Тип: CNAME
Имя: www
Значение: ваш-основной-домен.ru
TTL: 3600
```

**Важно:** Изменения DNS могут распространяться от 5 минут до 48 часов.

## Шаг 3: Установка Nginx (если еще не установлен)

```bash
# На Ubuntu/Debian
sudo apt update
sudo apt install nginx -y

# Запустите и включите автозапуск
sudo systemctl start nginx
sudo systemctl enable nginx

# Проверьте статус
sudo systemctl status nginx
```

## Шаг 4: Настройка Nginx конфигурации

### 4.1. Создайте файл конфигурации

```bash
cd /var/www/ispravleno-website/website

# Скопируйте шаблон конфигурации
sudo cp nginx.conf /etc/nginx/sites-available/ispravleno-website
```

### 4.2. Отредактируйте конфигурацию

```bash
sudo nano /etc/nginx/sites-available/ispravleno-website
```

**Замените `ваш-домен.ru` на ваше доменное имя:**

```nginx
server_name ispravleno.ru www.ispravleno.ru;
# или
server_name masterprofi.ru www.masterprofi.ru;
```

### 4.3. Активируйте конфигурацию

```bash
# Создайте симлинк
sudo ln -s /etc/nginx/sites-available/ispravleno-website /etc/nginx/sites-enabled/

# Проверьте конфигурацию на ошибки
sudo nginx -t

# Если все ОК, перезагрузите Nginx
sudo systemctl reload nginx
```

## Шаг 5: Настройка SSL сертификата (HTTPS)

### 5.1. Установите Certbot

```bash
# Установка certbot
sudo apt install certbot python3-certbot-nginx -y
```

### 5.2. Получите SSL сертификат

```bash
# Замените на ваше доменное имя
sudo certbot --nginx -d ispravleno.ru -d www.ispravleno.ru

# Certbot автоматически:
# - Получит SSL сертификат от Let's Encrypt
# - Обновит конфигурацию Nginx
# - Настроит автопродление сертификата
```

### 5.3. Проверьте автопродление

```bash
# Проверьте, что автопродление настроено
sudo certbot renew --dry-run
```

## Шаг 6: Обновление конфигурации Next.js

### 6.1. Обновите .env файл

```bash
cd /var/www/ispravleno-website/website
nano .env
```

Измените:

```env
# Было:
NEXT_PUBLIC_SITE_URL="http://212.74.227.208:3003"

# Стало:
NEXT_PUBLIC_SITE_URL="https://ispravleno.ru"
```

### 6.2. Обновите next.config.js (если нужно)

В файле `next.config.js` уже есть настройка для домена `212.74.227.208`. После настройки домена можно добавить:

```javascript
const nextConfig = {
  images: {
    domains: ['localhost', '212.74.227.208', 'ispravleno.ru', 'www.ispravleno.ru'],
  },
  // ...
}
```

### 6.3. Пересоберите и перезапустите приложение

```bash
# Пересоберите проект
npm run build

# Перезапустите PM2
pm2 restart ispravleno-website
```

## Шаг 7: Настройка файрвола

Убедитесь, что порты открыты:

```bash
# Проверьте статус файрвола
sudo ufw status

# Если файрвол активен, разрешите HTTP и HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# При необходимости разрешите SSH (если еще не разрешен)
sudo ufw allow 22/tcp
```

## Шаг 8: Проверка работы

### 8.1. Проверьте DNS резолюцию

```bash
# На локальной машине или на сервере
nslookup ispravleno.ru
# Должен вернуть IP: 212.74.227.208

# Или через dig
dig ispravleno.ru +short
# Должен вернуть: 212.74.227.208
```

### 8.2. Проверьте доступность сайта

```bash
# На сервере
curl -I http://ispravleno.ru
curl -I https://ispravleno.ru

# Или откройте в браузере:
# http://ispravleno.ru (должен перенаправить на https://)
# https://ispravleno.ru
```

## Дополнительные настройки

### Редирект с HTTP на HTTPS

После установки SSL, Nginx автоматически настроит редирект. Если нужно вручную, добавьте в конфигурацию:

```nginx
server {
    listen 80;
    server_name ispravleno.ru www.ispravleno.ru;
    return 301 https://$server_name$request_uri;
}
```

### Редирект с www на без www (или наоборот)

Если хотите редиректить `www.ispravleno.ru` → `ispravleno.ru`:

```nginx
server {
    listen 443 ssl http2;
    server_name www.ispravleno.ru;
    # ... SSL конфигурация ...
    return 301 https://ispravleno.ru$request_uri;
}
```

## Устранение проблем

### Проблема: "502 Bad Gateway"

Проверьте, что приложение запущено:

```bash
pm2 list
pm2 logs ispravleno-website
```

### Проблема: DNS не резолвится

Проверьте DNS записи через онлайн-сервисы:
- https://mxtoolbox.com/DNSLookup.aspx
- https://www.whatsmydns.net/

### Проблема: SSL сертификат не устанавливается

Убедитесь, что:
1. DNS записи правильно настроены
2. Порты 80 и 443 открыты в файрволе
3. Домен указывает на IP сервера

### Проблема: Сайт открывается, но статические файлы не загружаются

Проверьте, что Next.js приложение слушает на `localhost:3003`:

```bash
netstat -tulpn | grep 3003
# или
ss -tulpn | grep 3003
```

## Быстрый чеклист

- [ ] Домен куплен
- [ ] DNS A-записи настроены (@ и www → 212.74.227.208)
- [ ] Nginx установлен и запущен
- [ ] Конфигурация Nginx создана и активирована
- [ ] SSL сертификат установлен (Certbot)
- [ ] .env файл обновлен с новым доменом
- [ ] Next.js приложение пересобрано и перезапущено
- [ ] Файрвол настроен (порты 80, 443 открыты)
- [ ] Сайт доступен по доменному имени
- [ ] HTTPS работает
