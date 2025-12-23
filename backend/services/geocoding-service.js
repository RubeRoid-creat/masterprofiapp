// Сервис геокодирования адресов
// Использует Yandex Geocoder API для преобразования адреса в координаты

const GEOCODER_API_URL = 'https://geocode-maps.yandex.ru/1.x/';

/**
 * Геокодирует адрес в координаты используя Yandex Geocoder API
 * @param {string} address - Адрес для геокодирования
 * @returns {Promise<{latitude: number, longitude: number} | null>}
 */
export async function geocodeAddress(address) {
  try {
    if (!address || address.trim() === '') {
      console.warn('⚠️ Пустой адрес для геокодирования');
      return null;
    }

    // Формируем URL для запроса к Yandex Geocoder API
    // Используем бесплатный API без ключа (с ограничениями)
    const encodedAddress = encodeURIComponent(address);
    const url = `${GEOCODER_API_URL}?format=json&geocode=${encodedAddress}&results=1`;

    console.log(`🔍 Геокодирование адреса: ${address}`);

    const response = await fetch(url);
    
    if (!response.ok) {
      console.error(`❌ Ошибка геокодирования: ${response.status} ${response.statusText}`);
      return null;
    }

    const data = await response.json();
    
    // Парсим ответ Yandex Geocoder API
    if (data.response && data.response.GeoObjectCollection) {
      const featureMembers = data.response.GeoObjectCollection.featureMember;
      
      if (featureMembers && featureMembers.length > 0) {
        const geoObject = featureMembers[0].GeoObject;
        const pos = geoObject.Point.pos;
        
        // Формат: "longitude latitude" (долгота широта)
        const [longitude, latitude] = pos.split(' ').map(Number);
        
        console.log(`✅ Координаты найдены: ${latitude}, ${longitude} для адреса: ${address}`);
        
        return { latitude, longitude };
      }
    }
    
    console.warn(`⚠️ Координаты не найдены для адреса: ${address}`);
    return null;
  } catch (error) {
    console.error('❌ Ошибка при геокодировании адреса:', error);
    return null;
  }
}

/**
 * Валидирует координаты
 * @param {number} latitude - Широта
 * @param {number} longitude - Долгота
 * @returns {boolean}
 */
export function validateCoordinates(latitude, longitude) {
  // Проверяем, что координаты в разумных пределах для России/Твери
  // Тверь примерно: 56.8-56.9, 35.8-36.0
  const isValidLat = latitude >= -90 && latitude <= 90;
  const isValidLon = longitude >= -180 && longitude <= 180;
  
  // Дополнительная проверка для Твери (можно расширить для других городов)
  const isInTverArea = latitude >= 56.7 && latitude <= 57.0 && 
                        longitude >= 35.7 && longitude <= 36.2;
  
  return isValidLat && isValidLon && isInTverArea;
}

/**
 * Получает координаты для адреса, используя геокодирование если нужно
 * @param {string} address - Адрес
 * @param {number|null} providedLatitude - Предоставленная широта
 * @param {number|null} providedLongitude - Предоставленная долгота
 * @returns {Promise<{latitude: number, longitude: number} | null>}
 */
export async function getCoordinatesForAddress(address, providedLatitude = null, providedLongitude = null) {
  // Если координаты предоставлены и валидны, используем их
  if (providedLatitude !== null && providedLongitude !== null) {
    if (validateCoordinates(providedLatitude, providedLongitude)) {
      console.log(`✅ Используются предоставленные координаты: ${providedLatitude}, ${providedLongitude}`);
      return { latitude: providedLatitude, longitude: providedLongitude };
    } else {
      console.warn(`⚠️ Предоставленные координаты невалидны, используем геокодирование`);
    }
  }
  
  // Если координаты не предоставлены или невалидны, используем геокодирование
  const geocoded = await geocodeAddress(address);
  
  if (geocoded) {
    return geocoded;
  }
  
  // Если геокодирование не удалось, возвращаем null
  console.error(`❌ Не удалось получить координаты для адреса: ${address}`);
  return null;
}






