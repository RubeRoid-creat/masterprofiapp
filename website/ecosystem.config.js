// PM2 ecosystem config для Next.js website
// Использование: pm2 start ecosystem.config.js
// Путь к директории будет автоматически определен при запуске из этой директории
module.exports = {
  apps: [
    {
      name: 'ispravleno-website',
      script: 'node',
      args: '.next/standalone/server.js',
      // cwd будет автоматически установлен в директорию, где запускается PM2
      instances: 1,
      exec_mode: 'fork',
      env: {
        NODE_ENV: 'production',
        PORT: 3002,
        HOSTNAME: '0.0.0.0',
      },
      env_file: '.env',
      error_file: './logs/err.log',
      out_file: './logs/out.log',
      log_date_format: 'YYYY-MM-DD HH:mm:ss Z',
      merge_logs: true,
      autorestart: true,
      watch: false,
      max_memory_restart: '1G',
      min_uptime: '10s',
      max_restarts: 10,
    },
  ],
};
