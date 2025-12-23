'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'

const categories = [
  'Общие вопросы',
  'Ремонт холодильников',
  'Ремонт стиральных машин',
  'Ремонт микроволновок',
  'Другое',
]

export default function NewTopicPage() {
  const router = useRouter()
  const [formData, setFormData] = useState({
    title: '',
    content: '',
    author: '',
    category: '',
  })
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsSubmitting(true)

    try {
      const response = await fetch('/api/forum/topics', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      })

      if (response.ok) {
        const data = await response.json()
        router.push(`/forum/${data.topicId}`)
      } else {
        alert('Произошла ошибка при создании темы. Попробуйте позже.')
      }
    } catch (error) {
      alert('Произошла ошибка при создании темы. Попробуйте позже.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    })
  }

  return (
    <div className="container mx-auto px-4 py-12 max-w-2xl">
      <h1 className="text-4xl font-bold text-[#424242] mb-8">Создать новую тему</h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label htmlFor="title" className="block text-sm font-medium mb-2">
            Заголовок <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            id="title"
            name="title"
            required
            value={formData.title}
            onChange={handleChange}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#424242]"
            placeholder="Введите заголовок темы"
          />
        </div>

        <div>
          <label htmlFor="category" className="block text-sm font-medium mb-2">
            Категория <span className="text-red-500">*</span>
          </label>
          <select
            id="category"
            name="category"
            required
            value={formData.category}
            onChange={handleChange}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#424242]"
          >
            <option value="">Выберите категорию</option>
            {categories.map((category) => (
              <option key={category} value={category}>
                {category}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="author" className="block text-sm font-medium mb-2">
            Ваше имя <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            id="author"
            name="author"
            required
            value={formData.author}
            onChange={handleChange}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#424242]"
            placeholder="Введите ваше имя"
          />
        </div>

        <div>
          <label htmlFor="content" className="block text-sm font-medium mb-2">
            Содержание <span className="text-red-500">*</span>
          </label>
          <textarea
            id="content"
            name="content"
            required
            rows={10}
            value={formData.content}
            onChange={handleChange}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#424242]"
            placeholder="Опишите ваш вопрос или проблему"
          />
        </div>

        <div className="flex gap-4">
          <button
            type="submit"
            disabled={isSubmitting}
            className="flex-1 bg-[#424242] text-white py-3 rounded-lg hover:bg-[#212121] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSubmitting ? 'Создание...' : 'Создать тему'}
          </button>
          <button
            type="button"
            onClick={() => router.back()}
            className="px-6 py-3 border border-gray-300 rounded-lg hover:bg-gray-100 transition-colors"
          >
            Отмена
          </button>
        </div>
      </form>
    </div>
  )
}

