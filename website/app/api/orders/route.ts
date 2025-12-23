import { NextRequest, NextResponse } from 'next/server'

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

    // ВРЕМЕННО: Используем mock endpoint пока backend не исправлен
    // TODO: После исправления backend заменить на реальное API
    console.log('⚠️ Using MOCK endpoint (backend returns 404)')
    console.log('📦 Creating order (MOCK):', {
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
    const mockOrderNumber = `MOCK-${mockOrderId}`

    // Задержка для реалистичности
    await new Promise(resolve => setTimeout(resolve, 500))

    return NextResponse.json({ 
      success: true, 
      orderId: mockOrderId,
      orderNumber: mockOrderNumber,
      message: '✅ Заказ создан (тестовый режим)',
      warning: 'Backend API недоступен. Заказ не сохранен в базу. Требуется перезапуск backend на сервере.'
    }, { status: 201 })
    
    /* ОТКЛЮЧЕНО до исправления backend
    try {
      const { sendOrderToAdmin } = await import('@/lib/admin-api')
      console.log('Attempting to send order to backend API')
      const adminResponse = await sendOrderToAdmin({
        address,
        equipmentType,
        problemType,
        brand,
        date,
        time,
        description: description || '',
        name,
        phone,
        email: email || '',
      })
      console.log('✅ Order successfully created in backend:', adminResponse)
      
      return NextResponse.json({ 
        success: true, 
        orderId: adminResponse.order?.id,
        orderNumber: adminResponse.order?.order_number,
        message: adminResponse.message || 'Заказ успешно создан'
      }, { status: 201 })
    } catch (error: any) {
      console.error('❌ Failed to create order in backend:', {
        error: error.message,
        stack: error.stack,
      })
      
      return NextResponse.json(
        { error: error.message || 'Ошибка при создании заказа в системе' },
        { status: 500 }
      )
    }
    */
  } catch (error) {
    console.error('Error creating order:', error)
    return NextResponse.json(
      { error: 'Ошибка при создании заказа' },
      { status: 500 }
    )
  }
}

export async function GET(request: NextRequest) {
  try {
    // Заказы теперь хранятся в backend БД, не в локальной
    // Для получения заказов нужно использовать backend API с авторизацией
    // Этот endpoint можно использовать для получения заказов через backend API
    const { getOrdersFromAdmin } = await import('@/lib/admin-api')
    
    const searchParams = request.nextUrl.searchParams
    const status = searchParams.get('status')
    const limit = searchParams.get('limit') ? parseInt(searchParams.get('limit')!) : undefined
    
    const orders = await getOrdersFromAdmin({ status: status || undefined, limit })
    
    return NextResponse.json(orders)
  } catch (error) {
    console.error('Error fetching orders:', error)
    return NextResponse.json(
      { error: 'Ошибка при получении заказов. Заказы хранятся в основной системе.' },
      { status: 500 }
    )
  }
}

