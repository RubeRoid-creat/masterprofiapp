import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import './globals.css'
import Header from '@/components/Header'
import Footer from '@/components/Footer'

const inter = Inter({ subsets: ['cyrillic', 'latin'] })

export const metadata: Metadata = {
  title: 'Исправлено - Ремонт бытовой техники',
  description: 'Профессиональный ремонт бытовой техники. Качественный сервис, доступные цены, гарантия на все виды работ.',
  keywords: 'ремонт бытовой техники, ремонт холодильников, ремонт стиральных машин, ремонт микроволновок, сервисный центр',
  authors: [{ name: 'Исправлено' }],
  openGraph: {
    title: 'Исправлено - Ремонт бытовой техники',
    description: 'Профессиональный ремонт бытовой техники. Качественный сервис, доступные цены, гарантия на все виды работ.',
    type: 'website',
    locale: 'ru_RU',
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
    },
  },
  alternates: {
    canonical: 'https://ispravleno.ru',
  },
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="ru">
      <head>
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{
            __html: JSON.stringify({
              '@context': 'https://schema.org',
              '@type': 'LocalBusiness',
              name: 'Исправлено',
              description: 'Профессиональный ремонт бытовой техники',
              '@id': 'https://ispravleno.ru',
              url: 'https://ispravleno.ru',
              telephone: '+7-XXX-XXX-XX-XX',
              priceRange: '$$',
              address: {
                '@type': 'PostalAddress',
                addressCountry: 'RU',
              },
            }),
          }}
        />
      </head>
      <body className={inter.className}>
        <Header />
        <main className="min-h-screen">{children}</main>
        <Footer />
      </body>
    </html>
  )
}

