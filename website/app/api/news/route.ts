import { NextResponse } from 'next/server'
import { prisma } from '@/lib/prisma'

export async function GET() {
  try {
    const news = await prisma.news.findMany({
      orderBy: {
        createdAt: 'desc',
      },
      take: 10,
    })

    return NextResponse.json(news)
  } catch (error) {
    console.error('Error fetching news:', error)
    return NextResponse.json(
      { error: 'Ошибка при получении новостей' },
      { status: 500 }
    )
  }
}

