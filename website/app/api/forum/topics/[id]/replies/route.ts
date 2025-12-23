import { NextRequest, NextResponse } from 'next/server'
import { prisma } from '@/lib/prisma'

export async function POST(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const topicId = parseInt(params.id)
    const body = await request.json()
    const { content, author } = body

    if (!content || !author) {
      return NextResponse.json(
        { error: 'Не все обязательные поля заполнены' },
        { status: 400 }
      )
    }

    // Проверяем существование темы
    const topic = await prisma.forumTopic.findUnique({
      where: { id: topicId },
    })

    if (!topic) {
      return NextResponse.json({ error: 'Тема не найдена' }, { status: 404 })
    }

    const reply = await prisma.forumReply.create({
      data: {
        topicId,
        content,
        author,
      },
    })

    return NextResponse.json({ success: true, replyId: reply.id }, { status: 201 })
  } catch (error) {
    console.error('Error creating reply:', error)
    return NextResponse.json(
      { error: 'Ошибка при создании ответа' },
      { status: 500 }
    )
  }
}

