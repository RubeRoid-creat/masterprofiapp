import { NextRequest, NextResponse } from 'next/server'

/**
 * ВРЕМЕННЫЙ MOCK ENDPOINT для тестирования создания заказов
 * Используется когда backend недоступен
 * 
 * ДЛЯ ПРОДАКШЕНА: Удалите этот файл и используйте /api/orders с backend интеграцией
 */

export async function POST(request: NextRequest) {
  try {
    const body = await request.json()
    
    const { address, equipmentType, problemType, brand, date, time, description, name, phone, email } = body

    // Валидация обязательных полей
    if (!address || !equipmentType || !problemType || !brand || !date || !time || !name || !phone || !email) {
      return NextResponse.json(
        { error: 'Не все обязательные поля заполнены' },
        { status: 400 }
      )
    }

    console.log('🧪 [MOCK] Creating order:', {
      name,
      phone,
      email,
      address,
      equipmentType,
      problemType,
      brand,
      date,
      time,
      description
    })

    // Симулируем успешное создание заказа
    const mockOrderId = Date.now()
    const mockOrderNumber = `ORD-${mockOrderId}`

    // Задержка для реалистичности
    await new Promise(resolve => setTimeout(resolve, 500))

    return NextResponse.json({ 
      success: true, 
      orderId: mockOrderId,
      orderNumber: mockOrderNumber,
      message: '✅ Заказ успешно создан (MOCK MODE - для тестирования)',
      warning: 'Это тестовый заказ. Backend API недоступен. Подключите backend для реального создания заказов.'
    }, { status: 201 })
  } catch (error) {
    console.error('[MOCK] Error creating order:', error)
    return NextResponse.json(
      { error: 'Ошибка при создании заказа' },
      { status: 500 }
    )
  }
}

