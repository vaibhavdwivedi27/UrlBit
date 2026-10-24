import React from 'react';
import { Link } from 'react-router-dom';
import { useTheme } from '@/hooks/useTheme';

interface LogoProps {
  className?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  showTagline?: boolean;
  to?: string;
  forceVariant?: 'light' | 'dark';
}

export const Logo: React.FC<LogoProps> = ({
  className = '',
  size = 'md',
  showTagline = false,
  to = '/',
  forceVariant,
}) => {
  const { resolvedTheme } = useTheme();
  const effectiveVariant = forceVariant || resolvedTheme;

  const heightClasses = {
    sm: 'h-8',
    md: 'h-10',
    lg: 'h-14',
    xl: 'h-20',
  };

  const logoSrc =
    effectiveVariant === 'light' ? '/brand/logo-light.png' : '/brand/logo-dark.png';

  const content = (
    <div className={`flex items-center gap-3 select-none ${className}`}>
      <img
        key={logoSrc}
        src={logoSrc}
        alt="Urlbit Logo"
        className={`${heightClasses[size]} w-auto object-contain transition-transform duration-200 hover:scale-105`}
        onError={(e) => {
          const target = e.currentTarget;
          target.style.display = 'none';
          const fallback = target.nextElementSibling;
          if (fallback) fallback.classList.remove('hidden');
        }}
      />
      {/* Fallback stylized logo */}
      <div className="hidden items-center gap-2">
        <div className="w-8 h-8 rounded-lg bg-urlbit-gradient flex items-center justify-center font-bold text-white shadow-lg shadow-cyan-500/20">
          U
        </div>
        <div className="flex flex-col">
          <span className="font-bold tracking-tight text-xl leading-none text-slate-900 dark:text-white">
            Url<span className="text-urlbit-gradient">bit</span>
          </span>
          {showTagline && (
            <span className="text-[10px] text-slate-500 dark:text-slate-400 font-medium tracking-wide">
              Shorten. Track. Grow.
            </span>
          )}
        </div>
      </div>
    </div>
  );

  if (to) {
    return (
      <Link to={to} className="inline-flex items-center focus:outline-none">
        {content}
      </Link>
    );
  }

  return content;
};
