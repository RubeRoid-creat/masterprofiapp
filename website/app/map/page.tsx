'use client'

import { useEffect, useRef } from 'react'

export default function MapPage() {
  const mapRef = useRef<HTMLDivElement>(null)
  const mapInstanceRef = useRef<any>(null)
  const scriptLoadedRef = useRef(false)

  useEffect(() => {
    if (typeof window === 'undefined' || !mapRef.current || mapInstanceRef.current) {
      return
    }

    // Проверяем, не загружен ли уже скрипт
    const existingScript = document.querySelector('script[src*="api-maps.yandex.ru"]')
    
    if (existingScript && window.ymaps) {
      // Скрипт уже загружен, создаем карту сразу
      window.ymaps.ready(() => {
        if (!mapInstanceRef.current && mapRef.current) {
          mapInstanceRef.current = new window.ymaps.Map(mapRef.current, {
            center: [55.751244, 37.618423], // Москва (замените на нужные координаты)
            zoom: 10,
          })

          // Пример точек на карте
          const points = [
            { coords: [55.751244, 37.618423], title: 'Сервисный центр 1', address: 'Адрес 1' },
            { coords: [55.755244, 37.628423], title: 'Сервисный центр 2', address: 'Адрес 2' },
          ]

          points.forEach((point) => {
            const placemark = new window.ymaps.Placemark(
              point.coords,
              {
                balloonContent: `<strong>${point.title}</strong><br>${point.address}`,
              },
              {
                preset: 'islands#darkIcon',
              }
            )
            mapInstanceRef.current.geoObjects.add(placemark)
          })
        }
      })
      return
    }

    // Загружаем скрипт только если его еще нет
    if (!scriptLoadedRef.current) {
      scriptLoadedRef.current = true
      const script = document.createElement('script')
      script.src = 'https://api-maps.yandex.ru/2.1/?apikey=YOUR_API_KEY&lang=ru_RU'
      script.async = true
      script.onload = () => {
        if (window.ymaps && mapRef.current && !mapInstanceRef.current) {
          window.ymaps.ready(() => {
            if (!mapInstanceRef.current && mapRef.current) {
              mapInstanceRef.current = new window.ymaps.Map(mapRef.current, {
                center: [55.751244, 37.618423], // Москва (замените на нужные координаты)
                zoom: 10,
              })

              // Пример точек на карте
              const points = [
                { coords: [55.751244, 37.618423], title: 'Сервисный центр 1', address: 'Адрес 1' },
                { coords: [55.755244, 37.628423], title: 'Сервисный центр 2', address: 'Адрес 2' },
              ]

              points.forEach((point) => {
                const placemark = new window.ymaps.Placemark(
                  point.coords,
                  {
                    balloonContent: `<strong>${point.title}</strong><br>${point.address}`,
                  },
                  {
                    preset: 'islands#darkIcon',
                  }
                )
                mapInstanceRef.current.geoObjects.add(placemark)
              })
            }
          })
        }
      }
      document.head.appendChild(script)
    }

    return () => {
      // Очистка карты при размонтировании
      if (mapInstanceRef.current) {
        mapInstanceRef.current.destroy()
        mapInstanceRef.current = null
      }
    }
  }, [])

  return (
    <div className="min-h-screen bg-white">
      {/* Hero Section */}
      <section className="bg-white py-16 md:py-24 border-b border-gray-100">
        <div className="container mx-auto px-4 max-w-6xl">
          <h1 className="text-5xl md:text-6xl font-bold text-[#1a1a1a] mb-6 leading-tight">
            Карта оказания услуг
          </h1>
          <p className="text-lg md:text-xl text-gray-600 leading-relaxed max-w-2xl">
            Найдите ближайший сервисный центр или магазин на карте. Нажмите на метку для получения
            подробной информации.
          </p>
        </div>
      </section>

      <div className="container mx-auto px-4 py-16 max-w-6xl">
        <div ref={mapRef} className="w-full h-[600px] rounded-2xl border-2 border-gray-100 mb-16" />

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div className="border-2 border-gray-100 rounded-2xl p-8 hover:border-black transition">
            <h3 className="text-2xl font-bold text-[#1a1a1a] mb-4">Сервисный центр 1</h3>
            <div className="space-y-3 text-gray-600">
              <p><span className="font-medium text-black">Адрес:</span> ул. Примерная, д. 1</p>
              <p><span className="font-medium text-black">Телефон:</span> +7 (XXX) XXX-XX-XX</p>
              <p><span className="font-medium text-black">Часы работы:</span> Пн-Пт 9:00-18:00</p>
            </div>
          </div>
          <div className="border-2 border-gray-100 rounded-2xl p-8 hover:border-black transition">
            <h3 className="text-2xl font-bold text-[#1a1a1a] mb-4">Сервисный центр 2</h3>
            <div className="space-y-3 text-gray-600">
              <p><span className="font-medium text-black">Адрес:</span> ул. Примерная, д. 2</p>
              <p><span className="font-medium text-black">Телефон:</span> +7 (XXX) XXX-XX-XX</p>
              <p><span className="font-medium text-black">Часы работы:</span> Пн-Пт 9:00-18:00</p>
            </div>
          </div>
          <div className="border-2 border-gray-100 rounded-2xl p-8 hover:border-black transition">
            <h3 className="text-2xl font-bold text-[#1a1a1a] mb-4">Магазин запчастей</h3>
            <div className="space-y-3 text-gray-600">
              <p><span className="font-medium text-black">Адрес:</span> ул. Примерная, д. 3</p>
              <p><span className="font-medium text-black">Телефон:</span> +7 (XXX) XXX-XX-XX</p>
              <p><span className="font-medium text-black">Часы работы:</span> Пн-Сб 10:00-20:00</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

// Расширение типов для Яндекс.Карт
declare global {
  interface Window {
    ymaps: any
  }
}

