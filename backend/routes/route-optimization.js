import express from 'express';
import { query } from '../database/db.js';

const router = express.Router();

/**
 * POST /api/orders/optimize-route
 * Оптимизирует маршрут для нескольких заказов
 * 
 * Body: {
 *   orderIds: [1, 2, 3],
 *   startLatitude: 56.859611, // опционально
 *   startLongitude: 35.911896 // опционально
 * }
 */
router.post('/optimize-route', async (req, res) => {
    try {
        const { orderIds, startLatitude, startLongitude } = req.body;
        
        if (!orderIds || !Array.isArray(orderIds) || orderIds.length < 2) {
            return res.status(400).json({
                error: 'Необходимо указать минимум 2 заказа для оптимизации маршрута'
            });
        }
        
        // Получаем заказы из базы данных
        const placeholders = orderIds.map(() => '?').join(',');
        const orders = query.all(
            `SELECT * FROM orders WHERE id IN (${placeholders})`,
            orderIds
        );
        
        if (orders.length !== orderIds.length) {
            return res.status(404).json({
                error: 'Некоторые заказы не найдены'
            });
        }
        
        // Фильтруем заказы с координатами
        const ordersWithCoords = orders.filter(o => o.latitude && o.longitude);
        
        if (ordersWithCoords.length < 2) {
            return res.status(400).json({
                error: 'Недостаточно заказов с координатами для построения маршрута'
            });
        }
        
        // Оптимизируем маршрут (алгоритм Nearest Neighbor)
        const optimizedRoute = optimizeRoute(
            ordersWithCoords,
            startLatitude,
            startLongitude
        );
        
        res.json(optimizedRoute);
    } catch (error) {
        console.error('Ошибка оптимизации маршрута:', error);
        res.status(500).json({ error: 'Ошибка сервера при оптимизации маршрута' });
    }
});

/**
 * Алгоритм оптимизации маршрута (Nearest Neighbor)
 */
function optimizeRoute(orders, startLat, startLon) {
    if (orders.length === 0) {
        return { orders: [], totalDistance: 0, totalTime: 0 };
    }
    
    const routeOrders = [];
    const unvisited = [...orders];
    let currentLat = startLat;
    let currentLon = startLon;
    let totalDistance = 0;
    let totalTime = 0;
    
    // Если начальная точка не указана, используем первый заказ
    if (currentLat == null || currentLon == null) {
        const firstOrder = unvisited[0];
        currentLat = firstOrder.latitude;
        currentLon = firstOrder.longitude;
        unvisited.splice(0, 1);
    }
    
    // Находим ближайший заказ на каждой итерации
    while (unvisited.length > 0) {
        let nearestIndex = 0;
        let nearestDistance = calculateDistance(
            currentLat, currentLon,
            unvisited[0].latitude, unvisited[0].longitude
        );
        
        for (let i = 1; i < unvisited.length; i++) {
            const distance = calculateDistance(
                currentLat, currentLon,
                unvisited[i].latitude, unvisited[i].longitude
            );
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = i;
            }
        }
        
        const nearestOrder = unvisited[nearestIndex];
        const time = calculateEstimatedTime(nearestDistance);
        
        routeOrders.push({
            order: nearestOrder,
            distanceFromPrevious: nearestDistance,
            timeFromPrevious: time,
            cumulativeDistance: totalDistance + nearestDistance,
            cumulativeTime: totalTime + time
        });
        
        totalDistance += nearestDistance;
        totalTime += time;
        
        currentLat = nearestOrder.latitude;
        currentLon = nearestOrder.longitude;
        unvisited.splice(nearestIndex, 1);
    }
    
    return {
        orders: routeOrders,
        totalDistance: totalDistance,
        totalTime: totalTime,
        startLocation: startLat && startLon ? [startLat, startLon] : null
    };
}

/**
 * Рассчитывает расстояние между двумя точками (формула Haversine)
 */
function calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371000; // Радиус Земли в метрах
    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);
    
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
              Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
              Math.sin(dLon / 2) * Math.sin(dLon / 2);
    
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

/**
 * Рассчитывает примерное время в пути (в минутах)
 */
function calculateEstimatedTime(distanceMeters) {
    // Средняя скорость в городе ~40 км/ч = 11.1 м/с
    // Добавляем 30% на светофоры и пробки
    const averageSpeed = 11.1 * 0.7; // ~7.8 м/с
    const timeSeconds = distanceMeters / averageSpeed;
    return Math.ceil(timeSeconds / 60) + 1; // +1 минута на парковку
}

function toRad(degrees) {
    return degrees * (Math.PI / 180);
}

export default router;

