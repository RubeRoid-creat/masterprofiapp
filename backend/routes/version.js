import express from 'express';
import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const router = express.Router();

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const configPath = join(__dirname, '..', 'version-config.json');

function isVersionLess(v1, v2) {
  try {
    const a = (v1 || '0.0.0').split('.').map(n => parseInt(n, 10) || 0);
    const b = (v2 || '0.0.0').split('.').map(n => parseInt(n, 10) || 0);
    const len = Math.max(a.length, b.length);
    for (let i = 0; i < len; i++) {
      const x = a[i] || 0;
      const y = b[i] || 0;
      if (x < y) return true;
      if (x > y) return false;
    }
    return false;
  } catch (e) {
    console.error('Ошибка сравнения версий:', e.message);
    return false;
  }
}

function loadVersionConfig() {
  try {
    const raw = readFileSync(configPath, 'utf-8');
    return JSON.parse(raw);
  } catch (e) {
    console.error('Ошибка чтения version-config.json:', e.message);
    return {};
  }
}

// Проверка версии приложения (публичный эндпоинт)
router.post('/check', (req, res) => {
  try {
    const { platform = 'android_master', app_version } = req.body || {};
    
    console.log('[POST /api/version/check] body =', req.body);
    
    if (!app_version) {
      return res.status(400).json({ error: 'app_version is required' });
    }
    
    const config = loadVersionConfig();
    const versionConfig = config[platform] || config['android_master'];
    
    if (!versionConfig) {
      // Нет конфига — считаем, что обновление не требуется
      return res.json({
        update_required: false,
        force_update: false,
        current_version: app_version,
        release_notes: '',
        download_url: null,
        supported: true
      });
    }
    
    const currentVersion = versionConfig.current_version || app_version;
    const minRequiredVersion = versionConfig.min_required_version || currentVersion;
    const forceUpdate = !!versionConfig.force_update;
    
    // Обновление требуется, если текущая версия приложения меньше версии на сервере
    const updateRequired = isVersionLess(app_version, currentVersion);
    
    const response = {
      update_required: updateRequired,
      force_update: forceUpdate,
      current_version: currentVersion,
      release_notes: versionConfig.release_notes || '',
      download_url: versionConfig.download_url || null,
      supported: true
    };
    
    console.log('[POST /api/version/check] response =', response);
    res.json(response);
  } catch (error) {
    console.error('Ошибка /api/version/check:', error);
    res.status(500).json({ error: 'Ошибка проверки версии' });
  }
});

export default router;


