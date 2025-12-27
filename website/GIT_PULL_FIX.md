# Решение конфликта при git pull

## Проблема

При выполнении `git pull origin main` возникает ошибка:
```
error: Your local changes to the following files would be overwritten by merge:
website/setup-ispravleno-pro.sh
```

## Решение

### Вариант 1: Сохранить локальные изменения (если они важны)

```bash
# Сохранить локальные изменения
git stash

# Обновить код из репозитория
git pull origin main

# Применить сохраненные изменения обратно
git stash pop

# Если есть конфликты, разрешите их вручную
```

### Вариант 2: Отменить локальные изменения (рекомендуется, если изменения уже в репозитории)

```bash
# Отменить локальные изменения в файле
git checkout -- website/setup-ispravleno-pro.sh

# Обновить код из репозитория
git pull origin main
```

### Вариант 3: Посмотреть, какие изменения есть локально

```bash
# Посмотреть изменения
git diff website/setup-ispravleno-pro.sh

# Если изменения не нужны, отмените их:
git checkout -- website/setup-ispravleno-pro.sh

# Затем обновите код
git pull origin main
```

## После успешного pull

```bash
# Убедитесь, что вы в правильной директории
cd ~/Ispravleno/ispravleno-website/website

# Сделайте скрипт исполняемым
chmod +x setup-ispravleno-pro.sh

# Запустите скрипт
./setup-ispravleno-pro.sh
```
