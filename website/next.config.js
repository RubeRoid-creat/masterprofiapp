/** @type {import('next').NextConfig} */
const nextConfig = {
  images: {
    domains: ['localhost', '212.74.227.208', 'ispravleno.pro', 'www.ispravleno.pro'],
  },
  // Отключаем standalone режим для упрощения (можно включить позже при необходимости)
  // output: 'standalone',
  basePath: '',
  assetPrefix: '',
}

module.exports = nextConfig

