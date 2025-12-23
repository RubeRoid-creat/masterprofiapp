'use client'

import { useState, useEffect } from 'react'
import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'

interface ForumReply {
  id: number
  content: string
  author: string
  createdAt: string
}

interface ForumTopic {
  id: number
  title: string
  content: string
  author: string
  category: string
  views: number
  createdAt: string
  replies: ForumReply[]
}

export default function TopicPage() {
  const params = useParams()
  const router = useRouter()
  const [topic, setTopic] = useState<ForumTopic | null>(null)
  const [replyContent, setReplyContent] = useState('')
  const [replyAuthor, setReplyAuthor] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchTopic()
  }, [params.id])

  const fetchTopic = async () => {
    try {
      const response = await fetch(`/api/forum/topics/${params.id}`)
      if (response.ok) {
        const data = await response.json()
        setTopic(data)
      }
    } catch (error) {
      console.error('Error fetching topic:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleReplySubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!replyContent || !replyAuthor) return

    setIsSubmitting(true)

    try {
      const response = await fetch(`/api/forum/topics/${params.id}/replies`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          content: replyContent,
          author: replyAuthor,
        }),
      })

      if (response.ok) {
        setReplyContent('')
        setReplyAuthor('')
        fetchTopic()
      } else {
        alert('Произошла ошибка при отправке ответа. Попробуйте позже.')
      }
    } catch (error) {
      alert('Произошла ошибка при отправке ответа. Попробуйте позже.')
    } finally {
      setIsSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-12">
        <p className="text-center text-gray-600">Загрузка...</p>
      </div>
    )
  }

  if (!topic) {
    return (
      <div className="container mx-auto px-4 py-12">
        <p className="text-center text-gray-600">Тема не найдена</p>
        <Link href="/forum" className="text-[#424242] hover:underline mt-4 block text-center">
          Вернуться к форуму
        </Link>
      </div>
    )
  }

  return (
    <div className="container mx-auto px-4 py-12 max-w-4xl">
      <Link href="/forum" className="text-[#424242] hover:underline mb-4 inline-block">
        ← Вернуться к форуму
      </Link>

      <article className="border border-gray-200 rounded-lg p-6 mb-8">
        <div className="flex justify-between items-start mb-4">
          <h1 className="text-3xl font-bold text-[#424242]">{topic.title}</h1>
          <span className="text-sm text-gray-500 bg-gray-100 px-2 py-1 rounded">
            {topic.category}
          </span>
        </div>

        <div className="flex justify-between items-center mb-6 text-sm text-gray-500">
          <div>
            <span>Автор: {topic.author}</span>
            <span className="ml-4">{new Date(topic.createdAt).toLocaleDateString('ru-RU')}</span>
          </div>
          <div>
            <span>Просмотров: {topic.views}</span>
          </div>
        </div>

        <div className="prose max-w-none mb-6">
          <p className="text-gray-700 whitespace-pre-wrap">{topic.content}</p>
        </div>
      </article>

      <section className="mb-8">
        <h2 className="text-2xl font-bold text-[#424242] mb-6">
          Ответы ({topic.replies.length})
        </h2>

        {topic.replies.length > 0 ? (
          <div className="space-y-4">
            {topic.replies.map((reply) => (
              <div key={reply.id} className="border border-gray-200 rounded-lg p-6">
                <div className="flex justify-between items-start mb-2">
                  <span className="font-semibold text-[#424242]">{reply.author}</span>
                  <span className="text-sm text-gray-500">
                    {new Date(reply.createdAt).toLocaleDateString('ru-RU')}
                  </span>
                </div>
                <p className="text-gray-700 whitespace-pre-wrap">{reply.content}</p>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-gray-600">Пока нет ответов. Будьте первым!</p>
        )}
      </section>

      <section className="border border-gray-200 rounded-lg p-6">
        <h3 className="text-xl font-semibold text-[#424242] mb-4">Добавить ответ</h3>
        <form onSubmit={handleReplySubmit} className="space-y-4">
          <div>
            <label htmlFor="replyAuthor" className="block text-sm font-medium mb-2">
              Ваше имя <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              id="replyAuthor"
              required
              value={replyAuthor}
              onChange={(e) => setReplyAuthor(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#424242]"
              placeholder="Введите ваше имя"
            />
          </div>
          <div>
            <label htmlFor="replyContent" className="block text-sm font-medium mb-2">
              Ваш ответ <span className="text-red-500">*</span>
            </label>
            <textarea
              id="replyContent"
              required
              rows={6}
              value={replyContent}
              onChange={(e) => setReplyContent(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#424242]"
              placeholder="Введите ваш ответ"
            />
          </div>
          <button
            type="submit"
            disabled={isSubmitting}
            className="bg-[#424242] text-white px-6 py-2 rounded-lg hover:bg-[#212121] transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSubmitting ? 'Отправка...' : 'Отправить ответ'}
          </button>
        </form>
      </section>
    </div>
  )
}

