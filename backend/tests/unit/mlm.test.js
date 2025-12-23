import request from 'supertest';
import app from '../../server.js';

describe('MLM Routes', () => {
  let masterToken;
  let sponsorToken;
  let masterId;
  let sponsorId;

  beforeAll(async () => {
    // Создать спонсора
    const sponsorRes = await request(app)
      .post('/api/auth/register')
      .send({
        email: 'sponsor.mlm@test.com',
        password: 'password123',
        name: 'Sponsor Master',
        phone: '+79001234569',
        role: 'master'
      });
    sponsorToken = sponsorRes.body.token;
    sponsorId = sponsorRes.body.user.id;

    // Создать мастера с реферальным кодом
    const masterRes = await request(app)
      .post('/api/auth/register')
      .send({
        email: 'master.mlm@test.com',
        password: 'password123',
        name: 'MLM Master',
        phone: '+79001234570',
        role: 'master',
        sponsor_id: sponsorId
      });
    masterToken = masterRes.body.token;
    masterId = masterRes.body.user.id;
  });

  describe('GET /api/mlm/structure', () => {
    it('должен получить MLM структуру', async () => {
      const response = await request(app)
        .get('/api/mlm/structure')
        .set('Authorization', `Bearer ${sponsorToken}`)
        .expect(200);

      expect(response.body).toHaveProperty('user_id');
      expect(response.body).toHaveProperty('team_size');
    });

    it('должен вернуть ошибку без авторизации', async () => {
      const response = await request(app)
        .get('/api/mlm/structure')
        .expect(401);

      expect(response.body).toHaveProperty('error');
    });
  });

  describe('POST /api/mlm/invite', () => {
    it('должен создать приглашение', async () => {
      const inviteData = {
        name: 'Новый мастер',
        phone: '+79001234571',
        email: 'newmaster@test.com'
      };

      const response = await request(app)
        .post('/api/mlm/invite')
        .set('Authorization', `Bearer ${sponsorToken}`)
        .send(inviteData)
        .expect(200);

      expect(response.body).toHaveProperty('referral_code');
      expect(response.body).toHaveProperty('referral_link');
    });
  });

  describe('GET /api/mlm/commissions', () => {
    it('должен получить историю комиссий', async () => {
      const response = await request(app)
        .get('/api/mlm/commissions')
        .set('Authorization', `Bearer ${sponsorToken}`)
        .query({ period: 'month' })
        .expect(200);

      expect(response.body).toHaveProperty('total_earned');
      expect(response.body).toHaveProperty('commissions');
      expect(Array.isArray(response.body.commissions)).toBe(true);
    });

    it('должен поддерживать пагинацию', async () => {
      const response = await request(app)
        .get('/api/mlm/commissions')
        .set('Authorization', `Bearer ${sponsorToken}`)
        .query({ limit: 10, offset: 0 })
        .expect(200);

      expect(response.body).toHaveProperty('commissions');
    });
  });

  describe('GET /api/mlm/statistics', () => {
    it('должен получить MLM статистику', async () => {
      const response = await request(app)
        .get('/api/mlm/statistics')
        .set('Authorization', `Bearer ${sponsorToken}`)
        .expect(200);

      expect(response.body).toHaveProperty('rank');
      expect(response.body).toHaveProperty('team_size');
      expect(response.body).toHaveProperty('active_members');
      expect(response.body).toHaveProperty('total_commissions');
    });
  });

  describe('GET /api/mlm/team-performance', () => {
    it('должен получить производительность команды', async () => {
      const response = await request(app)
        .get('/api/mlm/team-performance')
        .set('Authorization', `Bearer ${sponsorToken}`)
        .expect(200);

      expect(Array.isArray(response.body)).toBe(true);
    });

    it('должен фильтровать по уровню', async () => {
      const response = await request(app)
        .get('/api/mlm/team-performance')
        .set('Authorization', `Bearer ${sponsorToken}`)
        .query({ level: 1 })
        .expect(200);

      expect(Array.isArray(response.body)).toBe(true);
    });
  });

  describe('GET /api/mlm/ranks', () => {
    it('должен получить информацию о рангах', async () => {
      const response = await request(app)
        .get('/api/mlm/ranks')
        .expect(200);

      expect(Array.isArray(response.body)).toBe(true);
      if (response.body.length > 0) {
        expect(response.body[0]).toHaveProperty('rank_name');
        expect(response.body[0]).toHaveProperty('requirements');
        expect(response.body[0]).toHaveProperty('benefits');
      }
    });
  });
});
