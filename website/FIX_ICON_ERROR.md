# Исправление ошибки с иконкой

## Проблема

Ошибка при генерации иконки:
```
TypeError: Invalid URL
at new URL (node:internal/url:825:25)
input: '.\\file:\\Z:\\Seo%D0%A1%D0%B0%D0%B9%D1%82%D0%98%D1%81%D0%BF%D1%80%D0%B0%D0%B2%D0%BD%D0%BE\\...'
```

Причина: Next.js не может обработать путь с кириллицей при генерации иконки через `ImageResponse`.

## Решение

Удален файл `app/icon.tsx`, который использовал `ImageResponse` с кириллицей.

### Альтернативные решения:

#### Вариант 1: Использовать статический favicon.ico

Поместите файл `favicon.ico` в папку `public/`:
```
public/favicon.ico
```

Next.js автоматически будет использовать его.

#### Вариант 2: Создать простую SVG иконку

Создайте `app/icon.svg`:
```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32">
  <rect width="32" height="32" fill="#424242"/>
  <text x="16" y="22" font-size="20" fill="white" text-anchor="middle" font-weight="bold">I</text>
</svg>
```

#### Вариант 3: Использовать app/icon.tsx без кириллицы

Если нужна динамическая иконка, используйте только латиницу:
```typescript
import { ImageResponse } from 'next/og'

export default function Icon() {
  return new ImageResponse(
    <div style={{ background: '#424242', width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontSize: 20, fontWeight: 'bold' }}>
      I
    </div>,
    { width: 32, height: 32, fonts: [] }
  )
}
```

## Текущее решение

Файл `app/icon.tsx` удален. Добавьте статический `favicon.ico` в `public/` для отображения иконки.

