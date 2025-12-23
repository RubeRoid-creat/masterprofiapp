'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'

interface News {
  id: number
  title: string
  content: string
  imageUrl?: string
  createdAt: string
}

export default function NewsSection() {
  const [news, setNews] = useState<News[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchNews()
  }, [])

  const fetchNews = async () => {
    try {
      const response = await fetch('/api/news')
      if (response.ok) {
        const data = await response.json()
        setNews(data)
      }
    } catch (error) {
      console.error('Error fetching news:', error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <section>
      <h2 className="text-4xl md:text-5xl font-bold text-[#1a1a1a] mb-12">Новости и статьи</h2>
      {loading ? (
        <div className="text-center py-20">
          <div className="inline-block w-12 h-12 border-4 border-gray-200 border-t-black rounded-full animate-spin"></div>
          <p className="text-gray-600 mt-4">Загрузка новостей...</p>
        </div>
      ) : news.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {news.slice(0, 3).map((item) => (
            <article 
              key={item.id} 
              className="border-2 border-gray-100 rounded-2xl p-8 hover:border-black transition group"
            >
              {item.imageUrl && (
                <div className="mb-6 -mx-8 -mt-8">
                  <img 
                    src={item.imageUrl} 
                    alt={item.title}
                    className="w-full h-48 object-cover rounded-t-2xl"
                  />
                </div>
              )}
              <h3 className="text-2xl font-bold mb-4 text-[#1a1a1a] group-hover:underline">
                {item.title}
              </h3>
              <p className="text-gray-600 mb-6 leading-relaxed line-clamp-3">
                {item.content}
              </p>
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-500">
                  {new Date(item.createdAt).toLocaleDateString('ru-RU', {
                    day: 'numeric',
                    month: 'long',
                    year: 'numeric'
                  })}
                </span>
                <Link 
                  href="#" 
                  className="text-black font-medium group-hover:underline inline-flex items-center"
                >
                  Читать
                  <span className="ml-2">→</span>
                </Link>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <div className="text-center py-20 border-2 border-gray-100 rounded-2xl">
          <p className="text-gray-600 text-lg">Новостей пока нет</p>
          <p className="text-gray-500 mt-2">Скоро здесь появятся интересные статьи</p>
        </div>
      )}
    </section>
  )
}
