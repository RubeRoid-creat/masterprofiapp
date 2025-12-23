import request from 'supertest';
import app from '../../server.js';

describe('Orders Routes', () => {
  let clientToken;
  let masterToken;
  let testOrderId;

  beforeAll(async () => {
    // Создать тестового клиента
    const clientRes = await request(app)
      .post('/api/auth/register')
      .send({
        email: 'client.orders@test.com',
        password: 'password123',
        name: 'Test Client',
        phone: '+79001234567',
        role: 'client'
      });
    clientToken = clientRes.body.token;

    // Создать тестового мастера
    const masterRes = await request(app)
      .post('/api/auth/register')
      .send({
        email: 'master.orders@test.com',
        password: 'password123',
        name: 'Test Master',
        phone: '+79001234568',
        role: 'master'
      });
    masterToken = masterRes.body.token;
  });

  describe('POST /api/orders', () => {
    it('клиент должен создать заказ', async () => {
      const orderData = {
        device_type: 'холодильник',
        device_brand: 'Samsung',
        device_model: 'RB37J5000SA',
        problem_description: 'Не морозит',
        address: 'г. Москва, ул. Ленина, д. 10, кв. 5',
        latitude: 55.751244,
        longitude: 37.618423,
        urgency: 'urgent'
      };

      const response = await request(app)
        .post('/api/orders')
        .set('Authorization', `Bearer ${clientToken}`)
        .send(orderData)
        .expect(201);

      expect(response.body).toHaveProperty('order');
      expect(response.body.order.device_type).toBe(orderData.device_type);
      expect(response.body.order.repair_status).toBe('new');
      
      testOrderId = response.body.order.id;
    });

    it('должен вернуть ошибку без обязательных полей', async () => {
      const response = await request(app)
        .post('/api/orders')
        .set('Authorization', `Bearer ${clientToken}`)
        .send({
          device_type: 'холодильник'
        })
        .expect(400);

      expect(response.body).toHaveProperty('error');
    });

    it('должен вернуть ошибку без авторизации', async () => {
      const response = await request(app)
        .post('/api/orders')
        .send({
          device_type: 'холодильник',
          problem_description: 'Тест',
          address: 'Тест'
        })
        .expect(401);

      expect(response.body).toHaveProperty('error');
    });
  });

  describe('GET /api/orders', () => {
    it('клиент должен получить свои заказы', async () => {
      const response = await request(app)
        .get('/api/orders')
        .set('Authorization', `Bearer ${clientToken}`)
        .expect(200);

      expect(Array.isArray(response.body)).toBe(true);
      if (response.body.length > 0) {
        expect(response.body[0]).toHaveProperty('id');
        expect(response.body[0]).toHaveProperty('device_type');
      }
    });

    it('мастер должен получить доступные заказы', async () => {
      const response = await request(app)
        .get('/api/orders')
        .set('Authorization', `Bearer ${masterToken}`)
        .expect(200);

      expect(Array.isArray(response.body)).toBe(true);
    });
  });

  describe('GET /api/orders/:id', () => {
    it('клиент должен получить детали своего заказа', async () => {
      if (!testOrderId) {
        // Создать заказ если не существует
        const orderRes = await request(app)
          .post('/api/orders')
          .set('Authorization', `Bearer ${clientToken}`)
          .send({
            device_type: 'холодильник',
            problem_description: 'Тест',
            address: 'Тестовый адрес'
          });
        testOrderId = orderRes.body.order.id;
      }

      const response = await request(app)
        .get(`/api/orders/${testOrderId}`)
        .set('Authorization', `Bearer ${clientToken}`)
        .expect(200);

      expect(response.body).toHaveProperty('id', testOrderId);
      expect(response.body).toHaveProperty('device_type');
    });

    it('должен вернуть 404 для несуществующего заказа', async () => {
      const response = await request(app)
        .get('/api/orders/999999')
        .set('Authorization', `Bearer ${clientToken}`)
        .expect(404);

      expect(response.body).toHaveProperty('error');
    });
  });

  describe('PUT /api/orders/:id', () => {
    it('клиент должен обновить свой заказ', async () => {
      if (!testOrderId) {
        const orderRes = await request(app)
          .post('/api/orders')
          .set('Authorization', `Bearer ${clientToken}`)
          .send({
            device_type: 'холодильник',
            problem_description: 'Тест',
            address: 'Тестовый адрес'
          });
        testOrderId = orderRes.body.order.id;
      }

      const response = await request(app)
        .put(`/api/orders/${testOrderId}`)
        .set('Authorization', `Bearer ${clientToken}`)
        .send({
          problem_description: 'Обновленное описание проблемы'
        })
        .expect(200);

      expect(response.body).toHaveProperty('order');
      expect(response.body.order.problem_description).toContain('Обновленное');
    });
  });

  describe('POST /api/orders/:id/cancel', () => {
    it('клиент должен отменить свой заказ', async () => {
      // Создать новый заказ для отмены
      const orderRes = await request(app)
        .post('/api/orders')
        .set('Authorization', `Bearer ${clientToken}`)
        .send({
          device_type: 'холодильник',
          problem_description: 'Заказ для отмены',
          address: 'Тестовый адрес'
        });

      const cancelOrderId = orderRes.body.order.id;

      const response = await request(app)
        .post(`/api/orders/${cancelOrderId}/cancel`)
        .set('Authorization', `Bearer ${clientToken}`)
        .send({
          cancellation_reason: 'Передумал'
        })
        .expect(200);

      expect(response.body).toHaveProperty('message');
    });
  });
});
