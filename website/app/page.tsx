import Link from 'next/link'
import NewsSection from '@/components/NewsSection'

export default function Home() {
  return (
    <main className="min-h-screen bg-white">
      {/* Hero Section */}
      <section className="bg-white py-20 md:py-32">
        <div className="container mx-auto px-4 max-w-6xl">
          <div className="max-w-3xl">
            <h1 className="text-5xl md:text-6xl font-bold mb-6 text-[#1a1a1a] leading-tight">
              Ваша техника исправлена.<br />
              Точно и в срок.
            </h1>
            <p className="text-lg md:text-xl mb-8 text-gray-600 leading-relaxed">
              Сервис премиум-ремонта бытовой техники. Гарантия результата — в договоре.
            </p>
            <div className="flex flex-col sm:flex-row gap-4">
              <Link
                href="/order"
                className="bg-black text-white px-8 py-4 rounded-lg font-medium hover:bg-gray-800 transition inline-block text-center"
              >
                Вызвать мастера
              </Link>
              <Link
                href="/price"
                className="bg-white text-black px-8 py-4 rounded-lg font-medium hover:bg-gray-50 transition inline-block text-center border-2 border-black"
              >
                Смотреть услуги
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* How We Work Section */}
      <section className="py-20 bg-gray-50">
        <div className="container mx-auto px-4 max-w-6xl">
          <h2 className="text-4xl md:text-5xl font-bold mb-16 text-[#1a1a1a]">Как мы работаем</h2>
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
            {/* Step 1 */}
            <div>
              <div className="w-14 h-14 border-2 border-black flex items-center justify-center mb-6 text-xl font-bold">
                01
              </div>
              <h3 className="text-xl font-bold mb-4 text-[#1a1a1a]">Точная диагностика</h3>
              <p className="text-gray-600 leading-relaxed">
                Определяем причину поломки за 15 минут на месте
              </p>
            </div>

            {/* Step 2 */}
            <div>
              <div className="w-14 h-14 border-2 border-black flex items-center justify-center mb-6 text-xl font-bold">
                02
              </div>
              <h3 className="text-xl font-bold mb-4 text-[#1a1a1a]">Честная смета</h3>
              <p className="text-gray-600 leading-relaxed">
                Фиксированная цена. Никаких доплат после начала работы
              </p>
            </div>

            {/* Step 3 */}
            <div>
              <div className="w-14 h-14 border-2 border-black flex items-center justify-center mb-6 text-xl font-bold">
                03
              </div>
              <h3 className="text-xl font-bold mb-4 text-[#1a1a1a]">Ремонт у вас дома</h3>
              <p className="text-gray-600 leading-relaxed">
                Приезжаем со всеми необходимыми инструментами и запчастями
              </p>
            </div>

            {/* Step 4 */}
            <div>
              <div className="w-14 h-14 border-2 border-black flex items-center justify-center mb-6 text-xl font-bold">
                04
              </div>
              <h3 className="text-xl font-bold mb-4 text-[#1a1a1a]">Гарантия до 2 лет</h3>
              <p className="text-gray-600 leading-relaxed">
                Письменная гарантия на все виды работ и замененные детали
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* News Section */}
      <section className="py-20 bg-white">
        <div className="container mx-auto px-4 max-w-6xl">
          <NewsSection />
        </div>
      </section>
    </main>
  )
}
