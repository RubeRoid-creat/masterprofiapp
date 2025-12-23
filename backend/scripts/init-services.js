import { initDatabase, query, saveDatabase } from '../database/db.js';
import { config } from '../config.js';
import { existsSync } from 'fs';

console.log('🔄 Инициализация категорий и шаблонов услуг...');

// Проверяем наличие базы данных
if (!existsSync(config.databasePath)) {
  console.log('❌ База данных не найдена. Запустите init-db.js для создания новой БД.');
  process.exit(1);
}

await initDatabase();

try {
  // Проверяем, есть ли уже категории
  const existingCategories = query.get('SELECT COUNT(*) as count FROM service_categories');
  if (existingCategories && existingCategories.count > 0) {
    console.log('⚠️  Категории уже существуют. Пропускаем инициализацию.');
    process.exit(0);
  }
  
  console.log('📋 Создание категорий услуг...');
  
  // Создаем основные категории
  const mainCategories = [
    { name: 'Сантехника', icon: 'plumbing', order_index: 1 },
    { name: 'Электрика', icon: 'electrical', order_index: 2 },
    { name: 'Бытовая техника', icon: 'appliance', order_index: 3 },
    { name: 'Отопление', icon: 'heating', order_index: 4 },
    { name: 'Кондиционирование', icon: 'ac', order_index: 5 },
    { name: 'Компьютеры и IT', icon: 'computer', order_index: 6 }
  ];
  
  const categoryIds = {};
  
  for (const cat of mainCategories) {
    const result = query.run(
      'INSERT INTO service_categories (name, icon, order_index) VALUES (?, ?, ?)',
      [cat.name, cat.icon, cat.order_index]
    );
    categoryIds[cat.name] = result.lastInsertRowid;
    console.log(`  ✅ Создана категория: ${cat.name}`);
  }
  
  // Создаем подкатегории для Сантехники
  console.log('\n📋 Создание подкатегорий для Сантехники...');
  const plumbingSubcategories = [
    { name: 'Протекает кран', parent: 'Сантехника', order_index: 1 },
    { name: 'Засор в трубах', parent: 'Сантехника', order_index: 2 },
    { name: 'Установка смесителя', parent: 'Сантехника', order_index: 3 },
    { name: 'Ремонт унитаза', parent: 'Сантехника', order_index: 4 },
    { name: 'Установка раковины', parent: 'Сантехника', order_index: 5 }
  ];
  
  for (const subcat of plumbingSubcategories) {
    query.run(
      'INSERT INTO service_categories (name, parent_id, order_index) VALUES (?, ?, ?)',
      [subcat.name, categoryIds[subcat.parent], subcat.order_index]
    );
    console.log(`  ✅ Создана подкатегория: ${subcat.name}`);
  }
  
  // Создаем подкатегории для Бытовая техника
  console.log('\n📋 Создание подкатегорий для Бытовая техника...');
  const applianceSubcategories = [
    { name: 'Стиральная машина', parent: 'Бытовая техника', order_index: 1 },
    { name: 'Холодильник', parent: 'Бытовая техника', order_index: 2 },
    { name: 'Посудомоечная машина', parent: 'Бытовая техника', order_index: 3 },
    { name: 'Микроволновка', parent: 'Бытовая техника', order_index: 4 },
    { name: 'Духовой шкаф', parent: 'Бытовая техника', order_index: 5 }
  ];
  
  for (const subcat of applianceSubcategories) {
    query.run(
      'INSERT INTO service_categories (name, parent_id, order_index) VALUES (?, ?, ?)',
      [subcat.name, categoryIds[subcat.parent], subcat.order_index]
    );
    console.log(`  ✅ Создана подкатегория: ${subcat.name}`);
  }
  
  // Создаем шаблоны услуг
  console.log('\n📋 Создание шаблонов услуг...');
  
  const templates = [
    {
      category_id: categoryIds['Сантехника'],
      name: 'Установка смесителя',
      description: 'Установка нового смесителя на кухне или в ванной',
      fixed_price: 2000,
      estimated_time: 60,
      device_type: 'Смеситель',
      is_popular: 1
    },
    {
      category_id: categoryIds['Сантехника'],
      name: 'Прочистка засора',
      description: 'Прочистка засора в раковине, ванне или унитазе',
      fixed_price: 1500,
      estimated_time: 45,
      device_type: 'Трубы',
      is_popular: 1
    },
    {
      category_id: categoryIds['Бытовая техника'],
      name: 'Ремонт стиральной машины',
      description: 'Диагностика и ремонт стиральной машины',
      fixed_price: 3000,
      estimated_time: 120,
      device_type: 'Стиральная машина',
      is_popular: 1
    },
    {
      category_id: categoryIds['Бытовая техника'],
      name: 'Ремонт холодильника',
      description: 'Диагностика и ремонт холодильника',
      fixed_price: 3500,
      estimated_time: 120,
      device_type: 'Холодильник',
      is_popular: 1
    },
    {
      category_id: categoryIds['Электрика'],
      name: 'Установка розетки',
      description: 'Установка новой розетки',
      fixed_price: 1500,
      estimated_time: 60,
      device_type: 'Розетка',
      is_popular: 1
    },
    {
      category_id: categoryIds['Электрика'],
      name: 'Ремонт электропроводки',
      description: 'Диагностика и ремонт электропроводки',
      fixed_price: 4000,
      estimated_time: 180,
      device_type: 'Электропроводка',
      is_popular: 0
    }
  ];
  
  for (const template of templates) {
    query.run(
      `INSERT INTO service_templates 
       (category_id, name, description, fixed_price, estimated_time, device_type, is_popular) 
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      [
        template.category_id,
        template.name,
        template.description,
        template.fixed_price,
        template.estimated_time,
        template.device_type,
        template.is_popular
      ]
    );
    console.log(`  ✅ Создан шаблон: ${template.name}`);
  }
  
  saveDatabase();
  console.log('\n✅ Инициализация категорий и шаблонов завершена!');
  process.exit(0);
} catch (error) {
  console.error('❌ Ошибка инициализации:', error);
  process.exit(1);
}





