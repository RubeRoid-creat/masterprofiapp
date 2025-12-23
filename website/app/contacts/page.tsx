'use client'

import { useState } from 'react'

export default function ContactsPage() {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    message: '',
  })
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsSubmitting(true)

    try {
      const response = await fetch('/api/contacts', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      })

      if (response.ok) {
        alert('Сообщение отправлено! Мы свяжемся с вами в ближайшее время.')
        setFormData({ name: '', email: '', phone: '', message: '' })
      } else {
        alert('Произошла ошибка при отправке сообщения. Попробуйте позже.')
      }
    } catch (error) {
      alert('Произошла ошибка при отправке сообщения. Попробуйте позже.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    })
  }

  return (
    <div className="min-h-screen bg-white">
      {/* Hero Section */}
      <section className="bg-white py-16 md:py-24 border-b border-gray-100">
        <div className="container mx-auto px-4 max-w-6xl">
          <h1 className="text-5xl md:text-6xl font-bold text-[#1a1a1a] mb-6 leading-tight">
            Свяжитесь с нами
          </h1>
          <p className="text-lg md:text-xl text-gray-600 leading-relaxed max-w-2xl">
            Мы всегда на связи и готовы ответить на все ваши вопросы
          </p>
        </div>
      </section>

      <div className="container mx-auto px-4 py-16 max-w-6xl">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-16">
          {/* Контактная информация */}
          <div>
            <h2 className="text-3xl font-bold text-[#1a1a1a] mb-8">Контактная информация</h2>
            
            <div className="space-y-8">
              <div className="border-2 border-gray-100 rounded-2xl p-6 hover:border-black transition">
                <h3 className="font-bold text-xl mb-3 text-[#1a1a1a]">Телефон</h3>
                <a href="tel:+7XXXXXXXXXX" className="text-gray-600 hover:text-black transition text-lg">
                  +7 (XXX) XXX-XX-XX
                </a>
                <p className="text-sm text-gray-500 mt-2">Звоните в любое время</p>
              </div>
              
              <div className="border-2 border-gray-100 rounded-2xl p-6 hover:border-black transition">
                <h3 className="font-bold text-xl mb-3 text-[#1a1a1a]">Email</h3>
                <a href="mailto:info@ispravleno.ru" className="text-gray-600 hover:text-black transition text-lg">
                  info@ispravleno.ru
                </a>
                <p className="text-sm text-gray-500 mt-2">Ответим в течение 24 часов</p>
              </div>

              <div className="border-2 border-gray-100 rounded-2xl p-6 hover:border-black transition">
                <h3 className="font-bold text-xl mb-3 text-[#1a1a1a]">Мессенджеры</h3>
                <div className="flex flex-wrap gap-3">
                  <a
                    href="https://t.me/ispravleno"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="bg-[#0088cc] text-white px-6 py-3 rounded-lg hover:bg-[#006699] transition font-medium"
                  >
                    Telegram
                  </a>
                  <a
                    href="https://wa.me/7XXXXXXXXXX"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="bg-[#25D366] text-white px-6 py-3 rounded-lg hover:bg-[#20BA5A] transition font-medium"
                  >
                    WhatsApp
                  </a>
                </div>
              </div>

              <div className="border-2 border-gray-100 rounded-2xl p-6">
                <h3 className="font-bold text-xl mb-3 text-[#1a1a1a]">Адрес</h3>
                <p className="text-gray-600 text-lg">г. Москва, ул. Примерная, д. 1</p>
              </div>

              <div className="border-2 border-gray-100 rounded-2xl p-6">
                <h3 className="font-bold text-xl mb-3 text-[#1a1a1a]">Часы работы</h3>
                <div className="space-y-2 text-gray-600">
                  <p>Понедельник - Пятница: <span className="font-medium text-black">9:00 - 18:00</span></p>
                  <p>Суббота: <span className="font-medium text-black">10:00 - 16:00</span></p>
                  <p>Воскресенье: <span className="font-medium text-black">Выходной</span></p>
                </div>
              </div>
            </div>
          </div>

          {/* Форма обратной связи */}
          <div>
            <h2 className="text-3xl font-bold text-[#1a1a1a] mb-8">Форма обратной связи</h2>
            
            <form onSubmit={handleSubmit} className="space-y-6 bg-white p-8 rounded-2xl border-2 border-gray-100">
              <div>
                <label htmlFor="name" className="block text-sm font-medium mb-2 text-[#1a1a1a]">
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

              <div>
                <label htmlFor="email" className="block text-sm font-medium mb-2 text-[#1a1a1a]">
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

              <div>
                <label htmlFor="phone" className="block text-sm font-medium mb-2 text-[#1a1a1a]">
                  Телефон
                </label>
                <input
                  type="tel"
                  id="phone"
                  name="phone"
                  value={formData.phone}
                  onChange={handleChange}
                  className="w-full px-4 py-3 border-2 border-gray-200 rounded-lg focus:outline-none focus:border-black transition"
                  placeholder="+7 (XXX) XXX-XX-XX"
                />
              </div>

              <div>
                <label htmlFor="message" className="block text-sm font-medium mb-2 text-[#1a1a1a]">
                  Сообщение <span className="text-red-500">*</span>
                </label>
                <textarea
                  id="message"
                  name="message"
                  required
                  rows={6}
                  value={formData.message}
                  onChange={handleChange}
                  className="w-full px-4 py-3 border-2 border-gray-200 rounded-lg focus:outline-none focus:border-black transition resize-none"
                  placeholder="Введите ваше сообщение"
                />
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full bg-black text-white py-4 rounded-lg font-medium hover:bg-gray-800 transition disabled:opacity-50 disabled:cursor-not-allowed text-lg"
              >
                {isSubmitting ? 'Отправка...' : 'Отправить сообщение'}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  )
}
