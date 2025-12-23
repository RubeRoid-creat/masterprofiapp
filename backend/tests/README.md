# 🧪 Backend Tests Documentation

## Обзор

Проект использует **Jest** и **Supertest** для тестирования API и сервисов.

## 📁 Структура тестов

```
tests/
├── setup.js                    # Настройка окружения для тестов
├── unit/                       # Unit тесты
│   ├── auth.test.js           # Тесты аутентификации
│   ├── orders.test.js         # Тесты заказов
│   ├── mlm.test.js            # Тесты MLM системы
│   └── services/              # Тесты сервисов
│       └── assignment-service.test.js
└── integration/               # Интеграционные тесты (будут добавлены)
```

## 🚀 Запуск тестов

### Все тесты

```bash
npm test
```

### С покрытием кода

```bash
npm run test:coverage
```

### В режиме наблюдения

```bash
npm run test:watch
```

### Только unit тесты

```bash
npm run test:unit
```

### Только integration тесты

```bash
npm run test:integration
```

## 📊 Покрытие кода

Минимальные требования покрытия (jest.config.js):
- **Branches**: 60%
- **Functions**: 60%
- **Lines**: 60%
- **Statements**: 60%

Отчет покрытия создается в папке `coverage/`:

```bash
npm run test:coverage
open coverage/lcov-report/index.html
```

## 🧪 Написание тестов

### Базовый тест API эндпоинта

```javascript
import request from 'supertest';
import app from '../../server.js';

describe('My API Endpoint', () => {
  it('should return 200', async () => {
    const response = await request(app)
      .get('/api/my-endpoint')
      .expect(200);

    expect(response.body).toHaveProperty('data');
  });
});
```

### Тест с авторизацией

```javascript
let authToken;

beforeAll(async () => {
  const response = await request(app)
    .post('/api/auth/login')
    .send({
      email: 'test@test.com',
      password: 'password123'
    });
  
  authToken = response.body.token;
});

it('should access protected route', async () => {
  const response = await request(app)
    .get('/api/protected')
    .set('Authorization', `Bearer ${authToken}`)
    .expect(200);
});
```

### Тест сервиса

```javascript
import { myService } from '../../../services/my-service.js';

describe('My Service', () => {
  it('should calculate correctly', () => {
    const result = myService.calculate(10, 20);
    expect(result).toBe(30);
  });
});
```

## 🔧 Конфигурация

### jest.config.js

Основные настройки Jest:
- `testEnvironment`: 'node' - окружение для Node.js
- `collectCoverageFrom`: массив файлов для анализа покрытия
- `coverageThreshold`: минимальное покрытие кода
- `testTimeout`: таймаут для тестов (10 секунд)

### setup.js

Настройка окружения перед запуском тестов:
- Установка NODE_ENV=test
- Mock для Firebase Admin
- Mock для WebSocket
- Глобальные утилиты

## 📝 Best Practices

1. **Изоляция тестов**
   - Каждый тест должен быть независимым
   - Используйте `beforeEach` для подготовки данных

2. **Очистка данных**
   - Удаляйте тестовые данные после тестов
   - Используйте уникальные email для каждого теста

3. **Описательные названия**
   - Используйте понятные названия для describe и it
   - Формат: "should [ожидаемое поведение]"

4. **Тестируйте edge cases**
   - Проверяйте граничные случаи
   - Тестируйте ошибки и исключения

5. **Mock внешние зависимости**
   - Firebase, WebSocket, и т.д.
   - Используйте jest.mock()

## 🐛 Отладка тестов

### Запуск одного теста

```bash
npm test -- --testNamePattern="should return 200"
```

### Режим отладки

```bash
node --inspect-brk node_modules/.bin/jest --runInBand
```

Затем откройте Chrome DevTools: `chrome://inspect`

### Вывод подробной информации

```bash
npm test -- --verbose
```

## 📈 Метрики

### Текущее покрытие (цель)

- ✅ Auth Routes: 80%+
- ✅ Orders Routes: 70%+
- ✅ MLM Routes: 65%+
- ✅ Assignment Service: 75%+
- 🚧 Masters Routes: 60%+ (в разработке)
- 🚧 Payments Routes: 60%+ (в разработке)

## 🔄 Continuous Integration

Тесты автоматически запускаются в GitHub Actions при каждом push/PR:
- `.github/workflows/backend-ci.yml`

## 📚 Полезные ресурсы

- [Jest Documentation](https://jestjs.io/docs/getting-started)
- [Supertest Documentation](https://github.com/visionmedia/supertest)
- [Testing Best Practices](https://github.com/goldbergyoni/javascript-testing-best-practices)

---

**Последнее обновление**: 23.12.2025
