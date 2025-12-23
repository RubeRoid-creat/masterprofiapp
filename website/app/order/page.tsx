'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'

const equipmentTypes = [
  'Холодильник',
  'Стиральная машина',
  'Микроволновая печь',
  'Посудомоечная машина',
  'Духовой шкаф',
  'Варочная панель',
  'Вытяжка',
  'Кондиционер',
  'Телевизор',
  'Другое',
]

const problemTypes = [
  'Не включается',
  'Не работает',
  'Течет вода',
  'Не греет/не охлаждает',
  'Шумит',
  'Ошибка на дисплее',
  'Другое',
]

const brands = [
  'Samsung',
  'LG',
  'Bosch',
  'Indesit',
  'Beko',
  'Ariston',
  'Electrolux',
  'Whirlpool',
  'Atlant',
  'Другое',
]

export default function OrderPage() {
  const router = useRouter()
  const [formData, setFormData] = useState({
    address: '',
    equipmentType: '',
    problemType: '',
    brand: '',
    date: '',
    time: '',
    description: '',
    name: '',
    phone: '',
    email: '',
  })
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsSubmitting(true)

    try {
      const response = await fetch('/api/orders', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      })

      if (response.ok) {
        const result = await response.json()
        if (result.warning) {
          alert(`${result.message}\n\n⚠️ ${result.warning}`)
        } else {
          alert('Заказ успешно создан! Мы свяжемся с вами в ближайшее время.')
        }
        router.push('/')
      } else {
        const error = await response.json()
        alert(`Произошла ошибка: ${error.error || 'Попробуйте позже'}`)
      }
    } catch (error) {
      alert('Произошла ошибка при создании заказа. Попробуйте позже.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    })
  }

  return (
    <div className="container mx-auto px-4 py-16 md:py-24 max-w-3xl">
      <h1 className="text-5xl md:text-6xl font-bold text-[#1a1a1a] mb-6 leading-tight">Создание заказа</h1>
      <p className="text-lg text-gray-600 mb-12">
        Заполните форму, и мы свяжемся с вами в течение 5 минут
      </p>
      
      <form onSubmit={handleSubmit} className="space-y-6 bg-white p-8 rounded-2xl border-2 border-gray-100">
        <div>
          <label htmlFor="address" className="block text-sm font-medium mb-2">
            Адрес <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            id="address"
            name="address"
            required
            value={formData.address}
            onChange={handleChange}
            className="w-full px-4 py-3 border-2 border-gray-200 rounded-lg focus:outline-none focus:border-black transition"
            placeholder="Введите адрес"
          />
        </div>

        <div>
          <label htmlFor="equipmentType" className="block text-sm font-medium mb-2">
            Тип техники <span className="text-red-500">*</span>
          </label>
          <select
            id="equipmentType"
            name="equipmentType"
            required
            value={formData.equipmentType}
            onChange={handleChange}
            className="w-full px-4 py-3 border-2 border-gray-200 rounded-lg focus:outline-none focus:border-black transition"
          >
            <option value="">Выберите тип техники</option>
            {equipmentTypes.map((type) => (
              <option key={type} value={type}>
                {type}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="brand" className="block text-sm font-medium mb-2">
            Бренд техники <span className="text-red-500">*</span>
          </label>
          <select
            id="brand"
            name="brand"
            required
            value={formData.brand}
            onChange={handleChange}
            className="w-full px-4 py-3 border-2 border-gray-200 rounded-lg focus:outline-none focus:border-black transition"
          >
            <option value="">Выберите бренд</option>
            {brands.map((brand) => (
              <option key={brand} value={brand}>
                {brand}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="problemType" className="block text-sm font-medium mb-2">
            Проблема <span className="text-red-500">*</span>
          </label>
          <select
            id="problemType"
            name="problemType"
            required
            value={formData.problemType}
            onChange={handleChange}
            className="w-full px-4 py-3 border-2 border-gray-200 rounded-lg focus:outline-none focus:border-black transition"
          >
            <option value="">Выберите проблему</option>
            {problemTypes.map((problem) => (
              <option key={problem} value={problem}>
                {problem}
              </option>
            ))}
          </select>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="date" className="block text-sm font-medium mb-2">
              Дата <span className="text-red-500">*</span>
            </label>
            <input
              type="date"
              id="date"
              name="date"
              required
              value={formData.date}
              onChange={handleChange}
              min={new Date().toISOString().split('T')[0]}
              className="w-full px-4 py-3 border-2 border-gray-200 rounded-lg focus:outline-none focus:border-black transition"
            />
          </div>
          <div>
            <label htmlFor="time" className="block text-sm font-medium mb-2">
              Время <span className="text-red-500">*</span>
            </label>
            <input
              type="time"
              id="time"
              name="time"
              required
              value={formData.time}
              onChange={handleChange}
              className="w-full px-4 py-3 border-2 border-gray-200 rounded-lg focus:outline-none focus:border-black transition"
            />
          </div>
        </div>

        <div>
          <label htmlFor="description" className="block text-sm font-medium mb-2">
            Описание проблемы
          </label>
          <textarea
            id="description"
            name="description"
            rows={4}
            value={formData.description}
            onChange={handleChange}
            className="w-full px-4 py-3 border-2 border-gray-200 rounded-lg focus:outline-none focus:border-black transition"
            placeholder="Опишите проблему подробнее (необязательно)"
          />
        </div>

        <div className="border-t-2 border-gray-100 pt-8 mt-2">
          <h3 className="text-2xl font-bold text-[#1a1a1a] mb-6">Контактная информация</h3>
          
          <div className="mb-4">
            <label htmlFor="name" className="block text-sm font-medium mb-2">
              Имя <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              id="name"
              name="name"
              required
              value={formData.name}
              onChange={handleChange}
              className="w-full px-4 py-3 border-2 border-gray-200 rounded-lg focus:outline-none focus:border-black transition"
              placeholder="Введите ваше имя"
            />
          </div>

          <div className="mb-4">
            <label htmlFor="phone" className="block text-sm font-medium mb-2">
              Телефон <span className="text-red-500">*</span>
            </label>
            <input
              type="tel"
              id="phone"
              name="phone"
              required
              value={formData.phone}
              onChange={handleChange}
              className="w-full px-4 py-3 border-2 border-gray-200 rounded-lg focus:outline-none focus:border-black transition"
              placeholder="+7 (XXX) XXX-XX-XX"
            />
          </div>

          <div>
            <label htmlFor="email" className="block text-sm font-medium mb-2">
              Email <span className="text-red-500">*</span>
            </label>
            <input
              type="email"
              id="email"
              name="email"
              required
              value={formData.email}
              onChange={handleChange}
              className="w-full px-4 py-3 border-2 border-gray-200 rounded-lg focus:outline-none focus:border-black transition"
              placeholder="email@example.com"
            />
          </div>
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full bg-black text-white py-4 rounded-lg font-medium hover:bg-gray-800 transition disabled:opacity-50 disabled:cursor-not-allowed text-lg"
        >
          {isSubmitting ? 'Отправка...' : 'Создать заказ'}
        </button>
      </form>
    </div>
  )
}

