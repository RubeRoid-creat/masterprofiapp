import { NextRequest, NextResponse } from 'next/server'
import { prisma } from '@/lib/prisma'

export async function GET(request: NextRequest) {
  try {
    const searchParams = request.nextUrl.searchParams
    const category = searchParams.get('category')

    const where = category && category !== 'Все' ? { category } : {}

    const topics = await prisma.forumTopic.findMany({
      where,
      include: {
        replies: {
          select: {
            id: true,
          },
        },
      },
      orderBy: {
        createdAt: 'desc',
      },
    })

    const topicsWithRepliesCount = topics.map((topic) => ({
      id: topic.id,
      title: topic.title,
      content: topic.content,
      author: topic.author,
      category: topic.category,
      views: topic.views,
      createdAt: topic.createdAt.toISOString(),
      repliesCount: topic.replies.length,
    }))

    return NextResponse.json(topicsWithRepliesCount)
  } catch (error) {
    console.error('Error fetching topics:', error)
    return NextResponse.json(
      { error: 'Ошибка при получении тем' },
      { status: 500 }
    )
  }
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json()
    const { title, content, author, category } = body

    if (!title || !content || !author || !category) {
      return NextResponse.json(
        { error: 'Не все обязательные поля заполнены' },
        { status: 400 }
      )
    }

    const topic = await prisma.forumTopic.create({
      data: {
        title,
        content,
        author,
        category,
      },
    })

    return NextResponse.json({ success: true, topicId: topic.id }, { status: 201 })
  } catch (error) {
    console.error('Error creating topic:', error)
    return NextResponse.json(
      { error: 'Ошибка при создании темы' },
      { status: 500 }
    )
  }
}

