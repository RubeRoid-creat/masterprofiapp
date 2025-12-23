'use client'

import { useState, useEffect } from 'react'
import Link from 'next/link'

interface ForumTopic {
  id: number
  title: string
  content: string
  author: string
  createdAt: string
  repliesCount: number
  category: string
  views: number
}

const categories = [
  'Все',
  'Общие вопросы',
  'Ремонт холодильников',
  'Ремонт стиральных машин',
  'Ремонт микроволновок',
  'Другое',
]

export default function ForumPage() {
  const [topics, setTopics] = useState<ForumTopic[]>([])
  const [selectedCategory, setSelectedCategory] = useState('Все')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchTopics()
  }, [selectedCategory])

  const fetchTopics = async () => {
    try {
      const response = await fetch(`/api/forum/topics?category=${selectedCategory}`)
      if (response.ok) {
        const data = await response.json()
        setTopics(data)
      }
    } catch (error) {
      console.error('Error fetching topics:', error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="container mx-auto px-4 py-12">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-4xl font-bold text-[#424242]">Форум</h1>
        <Link
          href="/forum/new"
          className="bg-[#424242] text-white px-6 py-2 rounded-lg hover:bg-[#212121] transition-colors"
        >
          Создать тему
        </Link>
      </div>

      <div className="flex flex-wrap gap-2 mb-8">
        {categories.map((category) => (
          <button
            key={category}
            onClick={() => setSelectedCategory(category)}
            className={`px-4 py-2 rounded-lg transition-colors ${
              selectedCategory === category
                ? 'bg-[#424242] text-white'
                : 'bg-gray-100 text-[#424242] hover:bg-gray-200'
            }`}
          >
            {category}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="text-center py-12">
          <p className="text-gray-600">Загрузка...</p>
        </div>
      ) : (
        <div className="space-y-4">
          {topics.length > 0 ? (
            topics.map((topic) => (
              <Link
                key={topic.id}
                href={`/forum/${topic.id}`}
                className="block border border-gray-200 rounded-lg p-6 hover:shadow-lg transition-shadow"
              >
                <div className="flex justify-between items-start mb-2">
                  <h3 className="text-xl font-semibold text-[#424242]">{topic.title}</h3>
                  <span className="text-sm text-gray-500 bg-gray-100 px-2 py-1 rounded">
                    {topic.category}
                  </span>
                </div>
                <p className="text-gray-600 mb-4 line-clamp-2">{topic.content}</p>
                <div className="flex justify-between items-center text-sm text-gray-500">
                  <div className="flex gap-4">
                    <span>Автор: {topic.author}</span>
                    <span>{new Date(topic.createdAt).toLocaleDateString('ru-RU')}</span>
                  </div>
                  <div className="flex gap-4">
                    <span>Ответов: {topic.repliesCount}</span>
                    <span>Просмотров: {topic.views}</span>
                  </div>
                </div>
              </Link>
            ))
          ) : (
            <div className="text-center py-12">
              <p className="text-gray-600">Темы не найдены</p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

