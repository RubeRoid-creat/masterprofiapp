import { NextRequest, NextResponse } from 'next/server'
import { prisma } from '@/lib/prisma'

export async function GET(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const topicId = parseInt(params.id)

    const topic = await prisma.forumTopic.findUnique({
      where: { id: topicId },
      include: {
        replies: {
          orderBy: {
            createdAt: 'asc',
          },
        },
      },
    })

    if (!topic) {
      return NextResponse.json({ error: 'Тема не найдена' }, { status: 404 })
    }

    // Увеличиваем счетчик просмотров
    await prisma.forumTopic.update({
      where: { id: topicId },
      data: { views: { increment: 1 } },
    })

    return NextResponse.json({
      ...topic,
      createdAt: topic.createdAt.toISOString(),
      updatedAt: topic.updatedAt.toISOString(),
      replies: topic.replies.map((reply) => ({
        ...reply,
        createdAt: reply.createdAt.toISOString(),
        updatedAt: reply.updatedAt.toISOString(),
      })),
    })
  } catch (error) {
    console.error('Error fetching topic:', error)
    return NextResponse.json(
      { error: 'Ошибка при получении темы' },
      { status: 500 }
    )
  }
}

