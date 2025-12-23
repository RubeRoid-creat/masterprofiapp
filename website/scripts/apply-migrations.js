/**
 * Скрипт для применения миграций базы данных
 * Использует Prisma для подключения к PostgreSQL
 */

import { PrismaClient } from '@prisma/client';
import { readFileSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

let prisma;
try {
  prisma = new PrismaClient();
} catch (error) {
  console.error('❌ Ошибка создания Prisma клиента:', error.message);
  console.error('\n💡 Решение:');
  console.error('1. Установите зависимости: npm install');
  console.error('2. Сгенерируйте Prisma клиент: npx prisma generate');
  process.exit(1);
}

async function applyMigrations() {
  try {
    console.log('📦 Применение миграций для таблиц веб-сайта...\n');

    // Читаем SQL файл миграции
    const migrationPath = join(__dirname, '../prisma/migrations/website_tables.sql');
    const sql = readFileSync(migrationPath, 'utf-8');

    // Разбиваем на отдельные команды
    // Учитываем многострочные команды и функции
    const commands = [];
    let currentCommand = '';
    let inFunction = false;
    
    const lines = sql.split('\n');
    for (const line of lines) {
      const trimmed = line.trim();
      
      // Пропускаем комментарии
      if (trimmed.startsWith('--') || trimmed.length === 0) {
        continue;
      }
      
      currentCommand += line + '\n';
      
      // Проверяем начало функции
      if (trimmed.includes('CREATE OR REPLACE FUNCTION') || trimmed.includes('CREATE FUNCTION')) {
        inFunction = true;
      }
      
      // Проверяем конец функции ($$ language)
      if (inFunction && trimmed.includes('$$ language')) {
        inFunction = false;
        commands.push(currentCommand.trim());
        currentCommand = '';
      }
      
      // Обычные команды заканчиваются точкой с запятой
      if (!inFunction && trimmed.endsWith(';')) {
        commands.push(currentCommand.trim());
        currentCommand = '';
      }
    }
    
    // Добавляем последнюю команду, если есть
    if (currentCommand.trim().length > 0) {
      commands.push(currentCommand.trim());
    }
    
    // Фильтруем пустые команды
    const validCommands = commands.filter(cmd => cmd.length > 0 && !cmd.startsWith('--'));

    console.log(`Найдено ${commands.length} SQL команд для выполнения\n`);

    // Выполняем каждую команду
    for (let i = 0; i < commands.length; i++) {
      const command = commands[i];
      if (command.trim().length === 0) continue;

      try {
        console.log(`[${i + 1}/${commands.length}] Выполнение команды...`);
        // Используем $executeRawUnsafe для выполнения произвольного SQL
        await prisma.$executeRawUnsafe(command);
        console.log('✅ Команда выполнена успешно\n');
      } catch (error) {
        // Игнорируем ошибки "уже существует" для CREATE TABLE IF NOT EXISTS
        if (error.message.includes('already exists') || 
            error.message.includes('duplicate') ||
            error.message.includes('relation') && error.message.includes('already exists')) {
          console.log('⚠️  Таблица/объект уже существует, пропускаем\n');
        } else {
          console.error(`❌ Ошибка выполнения команды:`, error.message);
          console.error(`Команда: ${command.substring(0, 100)}...\n`);
          // Не прерываем выполнение, продолжаем с следующей команды
        }
      }
    }

    console.log('✅ Миграции применены успешно!');
    
    // Проверяем, что таблицы созданы
    console.log('\n📊 Проверка созданных таблиц:');
    const tables = ['news', 'prices', 'forum_topics', 'forum_replies', 'contact_messages'];
    
    for (const table of tables) {
      try {
        const result = await prisma.$queryRawUnsafe(
          `SELECT COUNT(*) as count FROM ${table}`
        );
        console.log(`✅ Таблица "${table}" существует`);
      } catch (error) {
        console.error(`❌ Таблица "${table}" не найдена:`, error.message);
      }
    }

  } catch (error) {
    console.error('❌ Критическая ошибка при применении миграций:', error);
    process.exit(1);
  } finally {
    await prisma.$disconnect();
  }
}

// Запускаем миграции
applyMigrations();
