import { query } from '../database/db.js';
import bcrypt from 'bcryptjs';

async function createTestMaster() {
  try {
    const email = 'master@test.com';
    const password = '123456';
    const name = 'Тестовый Мастер';
    const phone = '+79991234567';
    
    // Проверяем, существует ли уже пользователь
    const existingUser = query.get('SELECT id FROM users WHERE email = ?', [email]);
    if (existingUser) {
      console.log(`User ${email} already exists with id=${existingUser.id}`);
      return;
    }
    
    // Хешируем пароль
    const passwordHash = await bcrypt.hash(password, 10);
    
    // Создаем пользователя
    const result = query.run(
      'INSERT INTO users (email, password_hash, name, phone, role) VALUES (?, ?, ?, ?, ?)',
      [email, passwordHash, name, phone, 'master']
    );
    
    const userId = result.lastInsertRowid;
    console.log(`✅ User created: id=${userId}, email=${email}`);
    
    // Создаем мастера
    query.run(
      'INSERT INTO masters (user_id, specialization, status) VALUES (?, ?, ?)',
      [userId, JSON.stringify(['Стиральная машина', 'Холодильник', 'Посудомоечная машина']), 'offline']
    );
    
    console.log(`✅ Master created: id=${userId}`);
    console.log(`\n📝 Test credentials:`);
    console.log(`   Email: ${email}`);
    console.log(`   Password: ${password}`);
  } catch (error) {
    console.error('Error creating test master:', error);
    throw error;
  }
}

createTestMaster().then(() => {
  process.exit(0);
}).catch(error => {
  console.error(error);
  process.exit(1);
});





