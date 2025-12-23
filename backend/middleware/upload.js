import multer from 'multer';
import { existsSync, mkdirSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// Создаем папку для загрузок, если её нет
const uploadsDir = join(__dirname, '..', 'uploads');
if (!existsSync(uploadsDir)) {
  try {
    mkdirSync(uploadsDir, { recursive: true });
    console.log(`✅ Папка uploads создана: ${uploadsDir}`);
  } catch (error) {
    console.error(`❌ Ошибка создания папки uploads: ${error.message}`);
    throw error;
  }
} else {
  console.log(`✅ Папка uploads существует: ${uploadsDir}`);
}

// Настройка хранилища
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    try {
      // Проверяем существование папки перед сохранением
      if (!existsSync(uploadsDir)) {
        mkdirSync(uploadsDir, { recursive: true });
        console.log(`✅ Папка uploads создана при сохранении: ${uploadsDir}`);
      }
      cb(null, uploadsDir);
    } catch (error) {
      console.error(`❌ Ошибка при сохранении файла в ${uploadsDir}:`, error);
      cb(error, null);
    }
  },
  filename: (req, file, cb) => {
    try {
      // Генерируем уникальное имя файла: timestamp-random-originalname
      const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
      const ext = file.originalname.split('.').pop() || 'jpg';
      const filename = `${uniqueSuffix}.${ext}`;
      console.log(`📁 Генерируем имя файла: ${filename}`);
      cb(null, filename);
    } catch (error) {
      console.error(`❌ Ошибка генерации имени файла:`, error);
      cb(error, null);
    }
  }
});

// Фильтр файлов
const fileFilter = (req, file, cb) => {
  console.log(`🔍 Проверка файла: fieldname=${file.fieldname}, originalname=${file.originalname}, mimetype=${file.mimetype}`);
  const allowedMimes = {
    'image/jpeg': true,
    'image/jpg': true,
    'image/png': true,
    'image/webp': true,
    'video/mp4': true,
    'video/quicktime': true, // MOV
    'video/x-msvideo': true  // AVI
  };
  
  if (allowedMimes[file.mimetype]) {
    console.log(`✅ Файл разрешен: ${file.mimetype}`);
    cb(null, true);
  } else {
    console.error(`❌ Неподдерживаемый тип файла: ${file.mimetype}`);
    cb(new Error(`Неподдерживаемый тип файла: ${file.mimetype}`), false);
  }
};

// Настройка multer
export const upload = multer({
  storage: storage,
  fileFilter: fileFilter,
  limits: {
    fileSize: 50 * 1024 * 1024, // 50MB максимум
    files: 5 // Максимум 5 файлов за раз
  }
});

// Middleware для обработки ошибок загрузки
export const handleUploadError = (err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    if (err.code === 'LIMIT_FILE_SIZE') {
      return res.status(400).json({ error: 'Файл слишком большой. Максимум 50MB' });
    }
    if (err.code === 'LIMIT_FILE_COUNT') {
      return res.status(400).json({ error: 'Слишком много файлов. Максимум 5' });
    }
    return res.status(400).json({ error: `Ошибка загрузки: ${err.message}` });
  }
  if (err) {
    return res.status(400).json({ error: err.message });
  }
  next();
};





