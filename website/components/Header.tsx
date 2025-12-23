'use client'

import Link from 'next/link'
import { useState } from 'react'
import Logo from './Logo'

export default function Header() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  return (
    <header className="border-b border-gray-100 bg-white sticky top-0 z-50">
      <nav className="container mx-auto px-4 py-4 max-w-7xl">
        <div className="flex items-center justify-between">
          {/* Professional Logo */}
          <Logo />

          {/* Desktop Navigation */}
          <ul className="hidden lg:flex gap-6 items-center">
            <li>
              <Link href="/" className="text-gray-700 hover:text-black transition-colors font-medium">
                Главная
              </Link>
            </li>
            <li>
              <Link href="/order" className="text-gray-700 hover:text-black transition-colors font-medium">
                Создать заказ
              </Link>
            </li>
            <li>
              <Link href="/price" className="text-gray-700 hover:text-black transition-colors font-medium">
                Прайс
              </Link>
            </li>
            <li>
              <Link href="/forum" className="text-gray-700 hover:text-black transition-colors font-medium">
                Форум
              </Link>
            </li>
            <li>
              <Link href="/map" className="text-gray-700 hover:text-black transition-colors font-medium">
                Карта
              </Link>
            </li>
            <li>
              <Link href="/apps" className="text-gray-700 hover:text-black transition-colors font-medium">
                Приложения
              </Link>
            </li>
            <li>
              <Link href="/contacts" className="text-gray-700 hover:text-black transition-colors font-medium">
                Контакты
              </Link>
            </li>
          </ul>

          {/* CTA Button */}
          <Link
            href="/order"
            className="hidden lg:block bg-black text-white px-6 py-2.5 rounded-lg font-medium hover:bg-gray-800 transition"
          >
            Вызвать мастера
          </Link>

          {/* Mobile Menu Button */}
          <button
            className="lg:hidden"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-label="Toggle menu"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              {mobileMenuOpen ? (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              ) : (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              )}
            </svg>
          </button>
        </div>

        {/* Mobile Menu */}
        {mobileMenuOpen && (
          <div className="lg:hidden pt-4 pb-3 border-t border-gray-100 mt-4">
            <ul className="flex flex-col gap-4">
              <li>
                <Link 
                  href="/" 
                  className="block text-gray-700 hover:text-black transition-colors font-medium"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Главная
                </Link>
              </li>
              <li>
                <Link 
                  href="/order" 
                  className="block text-gray-700 hover:text-black transition-colors font-medium"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Создать заказ
                </Link>
              </li>
              <li>
                <Link 
                  href="/price" 
                  className="block text-gray-700 hover:text-black transition-colors font-medium"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Прайс
                </Link>
              </li>
              <li>
                <Link 
                  href="/forum" 
                  className="block text-gray-700 hover:text-black transition-colors font-medium"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Форум
                </Link>
              </li>
              <li>
                <Link 
                  href="/map" 
                  className="block text-gray-700 hover:text-black transition-colors font-medium"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Карта
                </Link>
              </li>
              <li>
                <Link 
                  href="/apps" 
                  className="block text-gray-700 hover:text-black transition-colors font-medium"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Приложения
                </Link>
              </li>
              <li>
                <Link 
                  href="/contacts" 
                  className="block text-gray-700 hover:text-black transition-colors font-medium"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Контакты
                </Link>
              </li>
              <li className="pt-2">
                <Link
                  href="/order"
                  className="block bg-black text-white px-6 py-2.5 rounded-lg font-medium hover:bg-gray-800 transition text-center"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  Вызвать мастера
                </Link>
              </li>
            </ul>
          </div>
        )}
      </nav>
    </header>
  )
}
