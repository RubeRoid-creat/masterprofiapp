import express from 'express';
import { query } from '../database/db.js';
import { authenticate } from '../middleware/auth.js';
import { withCache, CacheKeys, invalidateServicesCache } from '../services/cache-service.js';

const router = express.Router();

// Получить все категории услуг (с иерархией)
router.get('/categories', authenticate, async (req, res) => {
  try {
    const { parent_id: parentId } = req.query;
    
    // Создаем ключ кэша
    const cacheKey = parentId !== undefined 
      ? CacheKeys.services.categoriesByParent(parentId)
      : CacheKeys.services.categories;
    
    // Получаем из кэша или выполняем запрос
    const categories = await withCache(cacheKey, async () => {
      let sql = 'SELECT * FROM service_categories WHERE 1=1';
      const params = [];
      
      if (parentId !== undefined) {
        if (parentId === null || parentId === 'null') {
          sql += ' AND parent_id IS NULL';
        } else {
          sql += ' AND parent_id = ?';
          params.push(parseInt(parentId));
        }
      }
      
      sql += ' ORDER BY order_index ASC, name ASC';
      
      return query.all(sql, params);
    }, 3600); // Кэш на 1 час
    
    // Для каждой категории получаем количество подкатегорий
    const categoriesWithCounts = categories.map(cat => {
      const subCount = query.get(
        'SELECT COUNT(*) as count FROM service_categories WHERE parent_id = ?',
        [cat.id]
      );
      return {
        ...cat,
        subcategories_count: subCount ? subCount.count : 0
      };
    });
    
    res.json(categoriesWithCounts);
  } catch (error) {
    console.error('Ошибка получения категорий:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить категорию по ID
router.get('/categories/:id', authenticate, async (req, res) => {
  try {
    const { id } = req.params;
    
    // Получаем из кэша
    const category = await withCache(CacheKeys.services.categoryById(id), async () => {
      return query.get('SELECT * FROM service_categories WHERE id = ?', [id]);
    }, 3600);
    
    if (!category) {
      return res.status(404).json({ error: 'Категория не найдена' });
    }
    
    // Получаем подкатегории
    const subcategories = query.all(
      'SELECT * FROM service_categories WHERE parent_id = ? ORDER BY order_index ASC, name ASC',
      [id]
    );
    
    res.json({
      ...category,
      subcategories
    });
  } catch (error) {
    console.error('Ошибка получения категории:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить шаблоны услуг
router.get('/templates', authenticate, async (req, res) => {
  try {
    const { category_id: categoryId, device_type: deviceType, popular, limit, offset } = req.query;
    
    // Создаем ключ кэша
    const cacheKey = categoryId 
      ? CacheKeys.services.templatesByCategory(categoryId)
      : deviceType
      ? CacheKeys.services.templatesByDevice(deviceType)
      : popular === 'true'
      ? CacheKeys.services.popularTemplates
      : CacheKeys.services.templates;
    
    // Получаем из кэша или выполняем запрос
    const templates = await withCache(cacheKey, async () => {
      let sql = 'SELECT * FROM service_templates WHERE 1=1';
      const params = [];
      
      if (categoryId) {
        sql += ' AND category_id = ?';
        params.push(parseInt(categoryId));
      }
      
      if (deviceType) {
        sql += ' AND device_type = ?';
        params.push(deviceType);
      }
      
      if (popular === 'true') {
        sql += ' AND is_popular = 1';
      }
      
      sql += ' ORDER BY is_popular DESC, name ASC';
      
      // Пагинация
      if (limit) {
        sql += ' LIMIT ?';
        params.push(parseInt(limit));
        if (offset) {
          sql += ' OFFSET ?';
          params.push(parseInt(offset));
        }
      }
      
      return query.all(sql, params);
    }, 1800); // Кэш на 30 минут
    
    res.json(templates);
  } catch (error) {
    console.error('Ошибка получения шаблонов:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

// Получить шаблон по ID
router.get('/templates/:id', authenticate, (req, res) => {
  try {
    const { id } = req.params;
    
    const template = query.get('SELECT * FROM service_templates WHERE id = ?', [id]);
    
    if (!template) {
      return res.status(404).json({ error: 'Шаблон не найден' });
    }
    
    res.json(template);
  } catch (error) {
    console.error('Ошибка получения шаблона:', error);
    res.status(500).json({ error: 'Ошибка сервера' });
  }
});

export default router;





