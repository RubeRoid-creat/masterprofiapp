import Link from 'next/link'
import Logo from './Logo'

export default function Footer() {
  return (
    <footer className="bg-gray-50 border-t border-gray-100">
      <div className="container mx-auto px-4 py-12 max-w-6xl">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-8">
          {/* Company Info */}
          <div>
            <div className="mb-4">
              <Logo variant="footer" />
            </div>
            <p className="text-gray-600 max-w-sm leading-relaxed mt-4">
              Профессиональный ремонт бытовой техники. Гарантия результата — в договоре.
            </p>
          </div>

          {/* Quick Links */}
          <div>
            <h4 className="font-bold mb-4 text-black">Навигация</h4>
            <ul className="space-y-3">
              <li>
                <Link href="/" className="text-gray-600 hover:text-black transition-colors">
                  Главная
                </Link>
              </li>
              <li>
                <Link href="/order" className="text-gray-600 hover:text-black transition-colors">
                  Создать заказ
                </Link>
              </li>
              <li>
                <Link href="/price" className="text-gray-600 hover:text-black transition-colors">
                  Прайс
                </Link>
              </li>
              <li>
                <Link href="/forum" className="text-gray-600 hover:text-black transition-colors">
                  Форум
                </Link>
              </li>
            </ul>
          </div>

          {/* Additional Links */}
          <div>
            <h4 className="font-bold mb-4 text-black">Информация</h4>
            <ul className="space-y-3">
              <li>
                <Link href="/map" className="text-gray-600 hover:text-black transition-colors">
                  Карта услуг
                </Link>
              </li>
              <li>
                <Link href="/apps" className="text-gray-600 hover:text-black transition-colors">
                  Приложения
                </Link>
              </li>
              <li>
                <Link href="/contacts" className="text-gray-600 hover:text-black transition-colors">
                  Контакты
                </Link>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="border-t border-gray-200 pt-8 flex flex-col md:flex-row justify-between items-center gap-4">
          <p className="text-gray-500 text-sm">
            © {new Date().getFullYear()} Исправлено. Все права защищены.
          </p>
          <div className="flex gap-6 text-sm text-gray-500">
            <Link href="/privacy" className="hover:text-black transition-colors">
              Политика конфиденциальности
            </Link>
            <Link href="/terms" className="hover:text-black transition-colors">
              Условия использования
            </Link>
          </div>
        </div>
      </div>
    </footer>
  )
}
