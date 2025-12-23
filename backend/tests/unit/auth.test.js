import request from 'supertest';
import app from '../../server.js';
import { query } from '../../database/db.js';

describe('Auth Routes', () => {
  beforeEach(() => {
    // Очистка тестовых данных
    try {
      query.run('DELETE FROM users WHERE email LIKE ?', ['%@test.com']);
    } catch (e) {
      // Игнорируем ошибки очистки
    }
  });

  describe('POST /api/auth/register', () => {
    it('должен зарегистрировать нового пользователя', async () => {
      const userData = {
        email: 'newuser@test.com',
        password: 'password123',
        name: 'Test User',
        phone: '+79001234567',
        role: 'client'
      };

      const response = await request(app)
        .post('/api/auth/register')
        .send(userData)
        .expect(201);

      expect(response.body).toHaveProperty('token');
      expect(response.body).toHaveProperty('user');
      expect(response.body.user.email).toBe(userData.email);
      expect(response.body.user.name).toBe(userData.name);
    });

    it('должен вернуть ошибку при дублировании email', async () => {
      const userData = {
        email: 'duplicate@test.com',
        password: 'password123',
        name: 'Test User',
        phone: '+79001234567'
      };

      // Первая регистрация
      await request(app)
        .post('/api/auth/register')
        .send(userData);

      // Попытка повторной регистрации
      const response = await request(app)
        .post('/api/auth/register')
        .send(userData)
        .expect(400);

      expect(response.body).toHaveProperty('error');
    });

    it('должен вернуть ошибку при отсутствии обязательных полей', async () => {
      const response = await request(app)
        .post('/api/auth/register')
        .send({ email: 'test@test.com' })
        .expect(400);

      expect(response.body).toHaveProperty('error');
    });
  });

  describe('POST /api/auth/login', () => {
    beforeEach(async () => {
      // Создаем тестового пользователя
      await request(app)
        .post('/api/auth/register')
        .send({
          email: 'login@test.com',
          password: 'password123',
          name: 'Login Test',
          phone: '+79001234567'
        });
    });

    it('должен войти с правильными учетными данными', async () => {
      const response = await request(app)
        .post('/api/auth/login')
        .send({
          email: 'login@test.com',
          password: 'password123'
        })
        .expect(200);

      expect(response.body).toHaveProperty('token');
      expect(response.body).toHaveProperty('user');
      expect(response.body.user.email).toBe('login@test.com');
    });

    it('должен вернуть ошибку при неверном пароле', async () => {
      const response = await request(app)
        .post('/api/auth/login')
        .send({
          email: 'login@test.com',
          password: 'wrongpassword'
        })
        .expect(401);

      expect(response.body).toHaveProperty('error');
    });

    it('должен вернуть ошибку при несуществующем email', async () => {
      const response = await request(app)
        .post('/api/auth/login')
        .send({
          email: 'nonexistent@test.com',
          password: 'password123'
        })
        .expect(401);

      expect(response.body).toHaveProperty('error');
    });
  });

  describe('GET /api/auth/me', () => {
    let authToken;

    beforeEach(async () => {
      // Регистрация и получение токена
      const response = await request(app)
        .post('/api/auth/register')
        .send({
          email: 'me@test.com',
          password: 'password123',
          name: 'Me Test',
          phone: '+79001234567'
        });

      authToken = response.body.token;
    });

    it('должен вернуть информацию о текущем пользователе', async () => {
      const response = await request(app)
        .get('/api/auth/me')
        .set('Authorization', `Bearer ${authToken}`)
        .expect(200);

      expect(response.body).toHaveProperty('id');
      expect(response.body.email).toBe('me@test.com');
    });

    it('должен вернуть ошибку без токена', async () => {
      const response = await request(app)
        .get('/api/auth/me')
        .expect(401);

      expect(response.body).toHaveProperty('error');
    });

    it('должен вернуть ошибку с неверным токеном', async () => {
      const response = await request(app)
        .get('/api/auth/me')
        .set('Authorization', 'Bearer invalid-token')
        .expect(401);

      expect(response.body).toHaveProperty('error');
    });
  });
});
