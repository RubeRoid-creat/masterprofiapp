'use client'

import { useState, useEffect } from 'react'
import Link from 'next/link'

interface PriceItem {
  id: number
  category: string
  name: string
  price: number
  type: 'service' | 'part'
}

const categories = [
  'Все',
  'Холодильники',
  'Стиральные машины',
  'Микроволновые печи',
  'Посудомоечные машины',
  'Духовые шкафы',
  'Варочные панели',
  'Вытяжки',
  'Кондиционеры',
]

export default function PricePage() {
  const [prices, setPrices] = useState<PriceItem[]>([])
  const [selectedCategory, setSelectedCategory] = useState('Все')
  const [searchQuery, setSearchQuery] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchPrices()
  }, [])

  const fetchPrices = async () => {
    try {
      const response = await fetch('/api/prices')
      if (response.ok) {
        const data = await response.json()
        setPrices(data)
      }
    } catch (error) {
      console.error('Error fetching prices:', error)
    } finally {
      setLoading(false)
    }
  }

  const filteredPrices = prices.filter((item) => {
    const matchesCategory = selectedCategory === 'Все' || item.category === selectedCategory
    const matchesSearch = item.name.toLowerCase().includes(searchQuery.toLowerCase())
    return matchesCategory && matchesSearch
  })

  const services = filteredPrices.filter((item) => item.type === 'service')
  const parts = filteredPrices.filter((item) => item.type === 'part')

  return (
    <div className="min-h-screen bg-white">
      {/* Hero Section */}
      <section className="bg-white py-16 md:py-24 border-b border-gray-100">
        <div className="container mx-auto px-4 max-w-6xl">
          <h1 className="text-5xl md:text-6xl font-bold text-[#1a1a1a] mb-6 leading-tight">
            Прайс услуг и запчастей
          </h1>
          <p className="text-lg md:text-xl text-gray-600 leading-relaxed max-w-2xl">
            Честные и прозрачные цены. Никаких скрытых платежей.
          </p>
        </div>
      </section>

      <div className="container mx-auto px-4 py-12 max-w-6xl">
        {/* Search and Filters */}
        <div className="mb-12">
          <div className="mb-6">
            <input
              type="text"
              placeholder="Поиск по названию..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full max-w-2xl mx-auto block px-6 py-4 border-2 border-gray-200 rounded-lg focus:outline-none focus:border-black transition text-lg"
            />
          </div>

          <div className="flex flex-wrap gap-3 justify-center">
            {categories.map((category) => (
              <button
                key={category}
                onClick={() => setSelectedCategory(category)}
                className={`px-6 py-3 rounded-lg font-medium transition ${
                  selectedCategory === category
                    ? 'bg-black text-white'
                    : 'bg-gray-50 text-gray-700 hover:bg-gray-100 border-2 border-gray-100'
                }`}
              >
                {category}
              </button>
            ))}
          </div>
        </div>

        {loading ? (
          <div className="text-center py-20">
            <div className="inline-block w-12 h-12 border-4 border-gray-200 border-t-black rounded-full animate-spin"></div>
            <p className="text-gray-600 mt-4">Загрузка...</p>
          </div>
        ) : (
          <div className="space-y-16">
            {/* Услуги */}
            <section>
              <h2 className="text-4xl font-bold text-[#1a1a1a] mb-8">Услуги</h2>
              {services.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {services.map((item) => (
                    <div key={item.id} className="border-2 border-gray-100 rounded-2xl p-8 hover:border-black transition group">
                      <div className="mb-3">
                        <span className="text-sm text-gray-500 font-medium">{item.category}</span>
                      </div>
                      <h3 className="text-xl font-bold mb-4 text-[#1a1a1a]">{item.name}</h3>
                      <p className="text-3xl font-bold text-black">{item.price.toLocaleString()} ₽</p>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-gray-600 text-lg">Услуги не найдены</p>
              )}
            </section>

            {/* Запчасти */}
            <section>
              <h2 className="text-4xl font-bold text-[#1a1a1a] mb-8">Запчасти</h2>
              {parts.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {parts.map((item) => (
                    <div key={item.id} className="border-2 border-gray-100 rounded-2xl p-8 hover:border-black transition group">
                      <div className="mb-3">
                        <span className="text-sm text-gray-500 font-medium">{item.category}</span>
                      </div>
                      <h3 className="text-xl font-bold mb-4 text-[#1a1a1a]">{item.name}</h3>
                      <p className="text-3xl font-bold text-black">{item.price.toLocaleString()} ₽</p>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-gray-600 text-lg">Запчасти не найдены</p>
              )}
            </section>
          </div>
        )}

        {/* CTA */}
        <div className="mt-20 text-center bg-black text-white p-12 rounded-2xl">
          <h2 className="text-3xl md:text-4xl font-bold mb-4">Не нашли нужную услугу?</h2>
          <p className="text-xl mb-8 text-gray-300">
            Свяжитесь с нами, и мы подберем решение для вашей задачи
          </p>
          <Link
            href="/order"
            className="inline-block bg-white text-black px-8 py-4 rounded-lg font-medium hover:bg-gray-100 transition"
          >
            Вызвать мастера
          </Link>
        </div>
      </div>
    </div>
  )
}
