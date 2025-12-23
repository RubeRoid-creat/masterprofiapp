import bcrypt from 'bcryptjs';
import { query, initDatabase } from '../database/db.js';

async function createAdmin() {
  try {
    console.log('🔄 Инициализация базы данных...');
    await initDatabase();
    
    const email = process.env.ADMIN_EMAIL || 'admin@test.com';
    const password = process.env.ADMIN_PASSWORD || 'admin123';
    const name = process.env.ADMIN_NAME || 'Администратор';
    const phone = process.env.ADMIN_PHONE || '+79991234567';
    
    console.log(`\n📝 Создание администратора:`);
    console.log(`   Email: ${email}`);
    console.log(`   Имя: ${name}`);
    console.log(`   Телефон: ${phone}`);
    
    // Проверяем, существует ли уже пользователь с таким email
    const existingUser = query.get('SELECT id, role FROM users WHERE email = ?', [email]);
    
    if (existingUser) {
      if (existingUser.role === 'admin') {
        console.log(`\n⚠️  Пользователь с email ${email} уже существует и является администратором.`);
        console.log('   Хотите изменить пароль? (y/n)');
        // Для автоматического режима - обновляем пароль
        const passwordHash = await bcrypt.hash(password, 10);
        query.run(
          'UPDATE users SET password_hash = ?, name = ?, phone = ? WHERE email = ?',
          [passwordHash, name, phone, email]
        );
        console.log(`\n✅ Пароль администратора обновлен!`);
      } else {
        // Обновляем роль на admin
        const passwordHash = await bcrypt.hash(password, 10);
        query.run(
          'UPDATE users SET password_hash = ?, name = ?, phone = ?, role = ? WHERE email = ?',
          [passwordHash, name, phone, 'admin', email]
        );
        console.log(`\n✅ Пользователь ${email} теперь администратор!`);
      }
    } else {
      // Создаем нового администратора
      const passwordHash = await bcrypt.hash(password, 10);
      
      const result = query.run(
        'INSERT INTO users (email, password_hash, name, phone, role) VALUES (?, ?, ?, ?, ?)',
        [email, passwordHash, name, phone, 'admin']
      );
      
      console.log(`\n✅ Администратор успешно создан!`);
      console.log(`   ID: ${result.lastInsertRowid}`);
    }
    
    console.log(`\n🔑 Данные для входа:`);
    console.log(`   Email: ${email}`);
    console.log(`   Пароль: ${password}`);
    console.log(`\n⚠️  ВАЖНО: Измените пароль после первого входа!`);
    console.log('');
    
  } catch (error) {
    console.error('❌ Ошибка создания администратора:', error);
    process.exit(1);
  }
}

createAdmin();

