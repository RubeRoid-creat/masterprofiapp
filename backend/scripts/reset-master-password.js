import { query } from '../database/db.js';
import bcrypt from 'bcryptjs';

async function resetPassword() {
  try {
    const email = 'master@test.com';
    const newPassword = '123456';
    
    // Получаем пользователя
    const user = query.get('SELECT id, email FROM users WHERE email = ?', [email]);
    if (!user) {
      console.log(`❌ User not found: ${email}`);
      return;
    }
    
    console.log(`Found user: id=${user.id}, email=${user.email}`);
    
    // Хешируем новый пароль
    const passwordHash = await bcrypt.hash(newPassword, 10);
    
    // Обновляем пароль
    query.run('UPDATE users SET password_hash = ? WHERE id = ?', [passwordHash, user.id]);
    
    console.log(`✅ Password reset for ${email}`);
    console.log(`   New password: ${newPassword}`);
    
    // Проверяем пароль
    const updatedUser = query.get('SELECT * FROM users WHERE id = ?', [user.id]);
    const isValid = await bcrypt.compare(newPassword, updatedUser.password_hash);
    console.log(`✅ Password verification: ${isValid ? 'PASSED' : 'FAILED'}`);
    
  } catch (error) {
    console.error('Error resetting password:', error);
    throw error;
  }
}

resetPassword().then(() => {
  process.exit(0);
}).catch(error => {
  console.error(error);
  process.exit(1);
});





