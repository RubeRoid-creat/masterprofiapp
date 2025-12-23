/**
 * Сервис для расчета расстояний и времени прибытия
 */

/**
 * Вычисляет расстояние между двумя точками (формула гаверсинуса)
 * @param {number} lat1 - Широта первой точки
 * @param {number} lon1 - Долгота первой точки
 * @param {number} lat2 - Широта второй точки
 * @param {number} lon2 - Долгота второй точки
 * @returns {number} Расстояние в метрах
 */
export function calculateDistance(lat1, lon1, lat2, lon2) {
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
 * Преобразует градусы в радианы
 */
function toRad(degrees) {
  return degrees * (Math.PI / 180);
}

/**
 * Рассчитывает примерное время прибытия в минутах
 * @param {number} distanceInMeters - Расстояние в метрах
 * @param {string} transportMode - Режим транспорта: 'car', 'walk', 'bike', 'public'
 * @returns {number} Время в минутах
 */
export function calculateEstimatedTime(distanceInMeters, transportMode = 'car') {
  // Средние скорости в м/с
  const speeds = {
    car: 15,      // ~54 км/ч (городской трафик)
    walk: 1.4,    // ~5 км/ч
    bike: 4,      // ~14.4 км/ч
    public: 10    // ~36 км/ч (общественный транспорт)
  };
  
  const speed = speeds[transportMode] || speeds.car;
  const timeInSeconds = distanceInMeters / speed;
  const timeInMinutes = Math.ceil(timeInSeconds / 60);
  
  // Минимум 5 минут, максимум 120 минут
  return Math.max(5, Math.min(120, timeInMinutes));
}

/**
 * Находит ближайших мастеров к указанной точке
 * @param {Array} masters - Массив мастеров с координатами
 * @param {number} clientLat - Широта клиента
 * @param {number} clientLon - Долгота клиента
 * @param {number} maxDistance - Максимальное расстояние в метрах (по умолчанию 20км)
 * @param {number} limit - Максимальное количество мастеров (по умолчанию 10)
 * @returns {Array} Массив мастеров с расстоянием и временем прибытия
 */
export function findNearestMasters(masters, clientLat, clientLon, maxDistance = 20000, limit = 10) {
  if (!masters || masters.length === 0) {
    return [];
  }
  
  const mastersWithDistance = masters
    .filter(master => master.latitude && master.longitude)
    .map(master => {
      const distance = calculateDistance(
        clientLat,
        clientLon,
        parseFloat(master.latitude),
        parseFloat(master.longitude)
      );
      
      // Рассчитываем время прибытия (предполагаем, что мастер на машине)
      const estimatedTime = calculateEstimatedTime(distance, 'car');
      
      return {
        ...master,
        distance: Math.round(distance), // Расстояние в метрах
        distanceKm: (distance / 1000).toFixed(1), // Расстояние в километрах
        estimatedArrivalTime: estimatedTime // Время в минутах
      };
    })
    .filter(master => master.distance <= maxDistance)
    .sort((a, b) => a.distance - b.distance)
    .slice(0, limit);
  
  return mastersWithDistance;
}

/**
 * Форматирует расстояние для отображения
 */
export function formatDistance(distanceInMeters) {
  if (distanceInMeters < 1000) {
    return `${Math.round(distanceInMeters)} м`;
  }
  return `${(distanceInMeters / 1000).toFixed(1)} км`;
}

/**
 * Форматирует время прибытия для отображения
 */
export function formatArrivalTime(minutes) {
  if (minutes < 60) {
    return `${minutes} мин`;
  }
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  if (mins === 0) {
    return `${hours} ч`;
  }
  return `${hours} ч ${mins} мин`;
}







