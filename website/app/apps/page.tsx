import Link from 'next/link'

export default function AppsPage() {
  return (
    <div className="min-h-screen bg-white">
      {/* Hero Section */}
      <section className="bg-white py-16 md:py-24 border-b border-gray-100">
        <div className="container mx-auto px-4 max-w-6xl">
          <h1 className="text-5xl md:text-6xl font-bold text-[#1a1a1a] mb-6 leading-tight">
            Мобильные приложения
          </h1>
          <p className="text-lg md:text-xl text-gray-600 leading-relaxed max-w-2xl">
            Скачайте наши приложения для удобного управления заказами и общения с мастерами
          </p>
        </div>
      </section>

      <div className="container mx-auto px-4 py-16 max-w-6xl">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-20">
          {/* Приложение мастера */}
          <div className="border-2 border-gray-100 rounded-2xl p-10 hover:border-black transition">
            <div className="w-24 h-24 bg-black rounded-2xl mx-auto mb-8 flex items-center justify-center">
              <span className="text-white text-5xl">🔧</span>
            </div>
            <h2 className="text-3xl font-bold text-[#1a1a1a] mb-4 text-center">Для мастеров</h2>
            <p className="text-gray-600 mb-8 text-center leading-relaxed">
              Управляйте заказами, общайтесь с клиентами и отслеживайте свой график работы.
              Все инструменты для эффективной работы мастера в одном приложении.
            </p>
            <div className="space-y-3">
              <a
                href="/apps/master.apk"
                download
                className="block w-full bg-black text-white py-4 rounded-lg hover:bg-gray-800 transition font-medium text-center"
              >
                Скачать для Android
              </a>
              <a
                href="/apps/master.ipa"
                download
                className="block w-full bg-gray-50 text-black py-4 rounded-lg hover:bg-gray-100 transition font-medium text-center border-2 border-gray-100"
              >
                Скачать для iOS
              </a>
            </div>
          </div>

          {/* Приложение клиента */}
          <div className="border-2 border-gray-100 rounded-2xl p-10 hover:border-black transition">
            <div className="w-24 h-24 bg-black rounded-2xl mx-auto mb-8 flex items-center justify-center">
              <span className="text-white text-5xl">📱</span>
            </div>
            <h2 className="text-3xl font-bold text-[#1a1a1a] mb-4 text-center">Для клиентов</h2>
            <p className="text-gray-600 mb-8 text-center leading-relaxed">
              Создавайте заказы, отслеживайте статус ремонта, общайтесь с мастерами
              и получайте уведомления о готовности заказа.
            </p>
            <div className="space-y-3">
              <a
                href="/apps/client.apk"
                download
                className="block w-full bg-black text-white py-4 rounded-lg hover:bg-gray-800 transition font-medium text-center"
              >
                Скачать для Android
              </a>
              <a
                href="/apps/client.ipa"
                download
                className="block w-full bg-gray-50 text-black py-4 rounded-lg hover:bg-gray-100 transition font-medium text-center border-2 border-gray-100"
              >
                Скачать для iOS
              </a>
            </div>
          </div>
        </div>

        <div className="border-t-2 border-gray-100 pt-16">
          <h3 className="text-4xl font-bold text-[#1a1a1a] mb-12 text-center">Возможности приложений</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
            <div className="bg-gray-50 p-8 rounded-2xl">
              <h4 className="text-2xl font-bold mb-6 text-[#1a1a1a]">Для мастеров:</h4>
              <ul className="space-y-4">
                <li className="flex items-start">
                  <span className="text-black text-xl mr-3">✓</span>
                  <span className="text-gray-700">Просмотр новых заказов</span>
                </li>
                <li className="flex items-start">
                  <span className="text-black text-xl mr-3">✓</span>
                  <span className="text-gray-700">Принятие заказов в работу</span>
                </li>
                <li className="flex items-start">
                  <span className="text-black text-xl mr-3">✓</span>
                  <span className="text-gray-700">Обновление статуса заказа</span>
                </li>
                <li className="flex items-start">
                  <span className="text-black text-xl mr-3">✓</span>
                  <span className="text-gray-700">Чат с клиентами</span>
                </li>
                <li className="flex items-start">
                  <span className="text-black text-xl mr-3">✓</span>
                  <span className="text-gray-700">История выполненных заказов</span>
                </li>
              </ul>
            </div>
            <div className="bg-gray-50 p-8 rounded-2xl">
              <h4 className="text-2xl font-bold mb-6 text-[#1a1a1a]">Для клиентов:</h4>
              <ul className="space-y-4">
                <li className="flex items-start">
                  <span className="text-black text-xl mr-3">✓</span>
                  <span className="text-gray-700">Создание заказов</span>
                </li>
                <li className="flex items-start">
                  <span className="text-black text-xl mr-3">✓</span>
                  <span className="text-gray-700">Отслеживание статуса</span>
                </li>
                <li className="flex items-start">
                  <span className="text-black text-xl mr-3">✓</span>
                  <span className="text-gray-700">Общение с мастером</span>
                </li>
                <li className="flex items-start">
                  <span className="text-black text-xl mr-3">✓</span>
                  <span className="text-gray-700">История заказов</span>
                </li>
                <li className="flex items-start">
                  <span className="text-black text-xl mr-3">✓</span>
                  <span className="text-gray-700">Push-уведомления</span>
                </li>
              </ul>
            </div>
          </div>
        </div>

        {/* CTA */}
        <div className="mt-20 text-center bg-black text-white p-12 rounded-2xl">
          <h2 className="text-3xl md:text-4xl font-bold mb-4">Нужна помощь с приложением?</h2>
          <p className="text-xl mb-8 text-gray-300">
            Свяжитесь с нами, и мы поможем разобраться
          </p>
          <Link
            href="/contacts"
            className="inline-block bg-white text-black px-8 py-4 rounded-lg font-medium hover:bg-gray-100 transition"
          >
            Связаться с нами
          </Link>
        </div>
      </div>
    </div>
  )
}
