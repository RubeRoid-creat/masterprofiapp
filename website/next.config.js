/** @type {import('next').NextConfig} */
const nextConfig = {
  images: {
    domains: ['localhost'],
  },
  experimental: {
    serverActions: true,
  },
  output: 'standalone',
}

module.exports = nextConfig

