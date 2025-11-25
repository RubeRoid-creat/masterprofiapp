import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { createWriteStream } from 'fs';
import archiver from 'archiver';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const backendRoot = path.resolve(__dirname, '..');

// Файлы и папки для исключения
const excludePatterns = [
  'node_modules',
  'backups',
  'uploads',
  '*.sqlite',
  '*.sqlite3',
  '*.db',
  '*.log',
  'logs',
  '.env',
  'firebase-service-account.json',
  '*-firebase-adminsdk-*.json',
  '.git',
  '.vscode',
  '.idea',
  '*.swp',
  '*.swo',
  '.DS_Store',
  'Thumbs.db',
  'package-lock.json',
  'admin-panel/node_modules',
  'admin-panel/dist',
  'admin-panel/build'
];

// Файлы и папки для включения
const includePatterns = [
  'server.js',
  'config.js',
  'package.json',
  'websocket.js',
  'database/**',
  'routes/**',
  'services/**',
  'middleware/**',
  'scripts/**',
  'README.md',
  'DEPLOY.md',
  '.gitignore'
];

function shouldExclude(filePath) {
  const relativePath = path.relative(backendRoot, filePath);
  
  // Проверяем паттерны исключения
  for (const pattern of excludePatterns) {
    if (pattern.includes('*')) {
      const regex = new RegExp(pattern.replace(/\*/g, '.*'));
      if (regex.test(relativePath) || relativePath.includes(pattern.replace('*', ''))) {
        return true;
      }
    } else if (relativePath.includes(pattern) || relativePath.startsWith(pattern)) {
      return true;
    }
  }
  
  return false;
}

function shouldInclude(filePath) {
  const relativePath = path.relative(backendRoot, filePath);
  
  // Проверяем паттерны включения
  for (const pattern of includePatterns) {
    if (pattern.includes('**')) {
      const regex = new RegExp(pattern.replace(/\*\*/g, '.*'));
      if (regex.test(relativePath)) {
        return true;
      }
    } else if (relativePath.includes(pattern) || relativePath.startsWith(pattern)) {
      return true;
    }
  }
  
  // Включаем файлы в корне backend
  const fileName = path.basename(filePath);
  if (includePatterns.includes(fileName)) {
    return true;
  }
  
  return false;
}

function getAllFiles(dir, fileList = []) {
  const files = fs.readdirSync(dir);
  
  files.forEach(file => {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);
    
    if (stat.isDirectory()) {
      // Пропускаем исключенные директории
      if (!shouldExclude(filePath)) {
        getAllFiles(filePath, fileList);
      }
    } else {
      // Включаем файл, если он не исключен
      if (!shouldExclude(filePath) && (shouldInclude(filePath) || !filePath.includes('node_modules'))) {
        fileList.push(filePath);
      }
    }
  });
  
  return fileList;
}

async function createPackage() {
  console.log('📦 Создание архива для деплоя...\n');
  
  const outputPath = path.join(backendRoot, 'backend-deploy.zip');
  const output = createWriteStream(outputPath);
  const archive = archiver('zip', {
    zlib: { level: 9 }
  });
  
  return new Promise((resolve, reject) => {
    output.on('close', () => {
      const sizeMB = (archive.pointer() / 1024 / 1024).toFixed(2);
      console.log(`✅ Архив создан: ${outputPath}`);
      console.log(`   Размер: ${sizeMB} MB`);
      console.log(`   Всего файлов: ${archive.pointer()} байт\n`);
      resolve();
    });
    
    archive.on('error', (err) => {
      reject(err);
    });
    
    archive.pipe(output);
    
    // Получаем все файлы
    const allFiles = getAllFiles(backendRoot);
    
    console.log(`📁 Найдено файлов: ${allFiles.length}`);
    console.log('📝 Добавление файлов в архив...\n');
    
    let addedCount = 0;
    allFiles.forEach(file => {
      const relativePath = path.relative(backendRoot, file);
      archive.file(file, { name: relativePath });
      addedCount++;
      if (addedCount % 50 === 0) {
        process.stdout.write(`   Добавлено: ${addedCount} файлов\r`);
      }
    });
    
    console.log(`\n✅ Добавлено файлов: ${addedCount}`);
    console.log('📦 Завершение архивации...\n');
    
    archive.finalize();
  });
}


createPackage().then(() => {
  console.log('🎉 Готово! Архив готов к загрузке на сервер.');
  process.exit(0);
}).catch(error => {
  console.error('❌ Ошибка создания архива:', error);
  process.exit(1);
});

