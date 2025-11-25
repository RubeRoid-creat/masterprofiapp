import bcrypt from 'bcryptjs';
import { query, initDatabase } from '../database/db.js';

async function checkAdmin() {
  try {
    console.log('🔄 Инициализация базы данных...');
    await initDatabase();
    
    const email = 'admin@test.com';
    const testPassword = 'admin123';
    
    console.log(`\n🔍 Проверка администратора:`);
    console.log(`   Email: ${email}`);
    
    const user = query.get('SELECT * FROM users WHERE email = ?', [email]);
    
    if (!user) {
      console.log(`\n❌ Пользователь ${email} не найден!`);
      console.log(`\n💡 Создайте администратора командой: npm run create-admin`);
      return;
    }
    
    console.log(`\n✅ Пользователь найден:`);
    console.log(`   ID: ${user.id}`);
    console.log(`   Имя: ${user.name}`);
    console.log(`   Роль: ${user.role}`);
    console.log(`   Email: ${user.email}`);
    console.log(`   Телефон: ${user.phone}`);
    console.log(`   Password hash: ${user.password_hash.substring(0, 20)}...`);
    
    // Проверяем пароль
    console.log(`\n🔐 Проверка пароля...`);
    const isValid = await bcrypt.compare(testPassword, user.password_hash);
    
    if (isValid) {
      console.log(`✅ Пароль "admin123" правильный!`);
    } else {
      console.log(`❌ Пароль "admin123" НЕ правильный!`);
      console.log(`\n💡 Обновите пароль командой: npm run create-admin`);
    }
    
    if (user.role !== 'admin') {
      console.log(`\n⚠️  ВНИМАНИЕ: Роль пользователя "${user.role}", а не "admin"!`);
      console.log(`\n💡 Обновите роль командой: npm run create-admin`);
    }
    
  } catch (error) {
    console.error('❌ Ошибка:', error);
    process.exit(1);
  }
}

checkAdmin();

