import Link from 'next/link'

interface LogoProps {
  variant?: 'default' | 'footer'
  className?: string
}

export default function Logo({ variant = 'default', className = '' }: LogoProps) {
  const textSize = variant === 'footer' ? 'text-xl' : 'text-xl'
  
  return (
    <Link href="/" className={`flex items-center gap-3 group ${className}`}>
      {/* Professional Logo Icon */}
      <div className="relative">
        {/* Main circle background */}
        <div className="w-10 h-10 bg-black rounded-xl flex items-center justify-center transform group-hover:scale-105 transition-transform duration-200">
          {/* Wrench + Checkmark combination */}
          <svg 
            className="w-6 h-6 text-white" 
            fill="none" 
            stroke="currentColor" 
            viewBox="0 0 24 24"
            strokeWidth="2"
          >
            {/* Wrench handle */}
            <path 
              strokeLinecap="round" 
              strokeLinejoin="round" 
              d="M11.42 15.17L17.25 21A2.652 2.652 0 0021 17.25l-5.877-5.877M11.42 15.17l2.496-3.03c.317-.384.74-.626 1.208-.766M11.42 15.17l-4.655 5.653a2.548 2.548 0 11-3.586-3.586l6.837-5.63m5.108-.233c.55-.164 1.163-.188 1.743-.14a4.5 4.5 0 004.486-6.336l-3.276 3.277a3.004 3.004 0 01-2.25-2.25l3.276-3.276a4.5 4.5 0 00-6.336 4.486c.091 1.076-.071 2.264-.904 2.95l-.102.085m-1.745 1.437L5.909 7.5H4.5L2.25 3.75l1.5-1.5L7.5 4.5v1.409l4.26 4.26m-1.745 1.437l1.745-1.437m6.615 8.206L15.75 15.75M4.867 19.125h.008v.008h-.008v-.008z" 
            />
          </svg>
        </div>
        
        {/* Checkmark badge overlay */}
        <div className="absolute -top-1 -right-1 w-4 h-4 bg-white rounded-full flex items-center justify-center border-2 border-black">
          <svg className="w-2.5 h-2.5 text-black" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={4} d="M5 13l4 4L19 7" />
          </svg>
        </div>
      </div>
      
      {/* Brand text */}
      <div className="flex flex-col -space-y-1">
        <span className={`${textSize} font-bold text-black leading-none group-hover:text-gray-800 transition-colors`}>
          Исправлено
        </span>
        <span className="text-[10px] text-gray-500 font-medium tracking-wider uppercase">
          Сервис ремонта
        </span>
      </div>
    </Link>
  )
}

// Alternative minimalist version
export function LogoMinimal({ className = '' }: { className?: string }) {
  return (
    <Link href="/" className={`flex items-center gap-2 group ${className}`}>
      <div className="w-8 h-8 bg-black rounded-lg flex items-center justify-center transform group-hover:rotate-12 transition-transform duration-300">
        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
        </svg>
      </div>
      <span className="text-xl font-bold text-black group-hover:text-gray-800 transition-colors">
        Исправлено
      </span>
    </Link>
  )
}

// Alternative tech version with circuit pattern
export function LogoTech({ className = '' }: { className?: string }) {
  return (
    <Link href="/" className={`flex items-center gap-3 group ${className}`}>
      <div className="relative w-10 h-10">
        {/* Hexagon shape */}
        <svg className="w-10 h-10" viewBox="0 0 40 40" fill="none">
          <path 
            d="M20 2L35 11V29L20 38L5 29V11L20 2Z" 
            fill="black"
            className="group-hover:fill-gray-800 transition-colors"
          />
          {/* Circuit lines */}
          <path 
            d="M20 12V20M20 20L26 23M20 20L14 23" 
            stroke="white" 
            strokeWidth="2" 
            strokeLinecap="round"
          />
          {/* Dots */}
          <circle cx="20" cy="12" r="1.5" fill="white" />
          <circle cx="26" cy="23" r="1.5" fill="white" />
          <circle cx="14" cy="23" r="1.5" fill="white" />
          {/* Checkmark */}
          <path 
            d="M13 20L17 24L27 16" 
            stroke="white" 
            strokeWidth="2.5" 
            strokeLinecap="round" 
            strokeLinejoin="round"
          />
        </svg>
      </div>
      <div className="flex flex-col -space-y-1">
        <span className="text-xl font-bold text-black group-hover:text-gray-800 transition-colors">
          Исправлено
        </span>
        <span className="text-[10px] text-gray-500 font-medium tracking-widest uppercase">
          Tech Service
        </span>
      </div>
    </Link>
  )
}

