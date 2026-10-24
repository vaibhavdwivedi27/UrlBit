import React, { useState, useRef, useEffect } from 'react';
import { useTheme, Theme } from '@/hooks/useTheme';
import { Sun, Moon, Laptop, Check } from 'lucide-react';

export const ThemeToggle: React.FC = () => {
  const { theme, resolvedTheme, setTheme } = useTheme();
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const options: { label: string; value: Theme; icon: React.ReactNode }[] = [
    { label: 'Light', value: 'light', icon: <Sun className="w-4 h-4 text-amber-500" /> },
    { label: 'Dark', value: 'dark', icon: <Moon className="w-4 h-4 text-cyan-400" /> },
    { label: 'System', value: 'system', icon: <Laptop className="w-4 h-4 text-slate-400" /> },
  ];

  return (
    <div className="relative" ref={menuRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        aria-label="Toggle theme"
        className="w-9 h-9 rounded-xl flex items-center justify-center transition-colors border border-slate-200 dark:border-slate-800 bg-slate-100 hover:bg-slate-200 dark:bg-slate-900/80 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200"
      >
        {resolvedTheme === 'light' ? (
          <Sun className="w-4 h-4 text-amber-500" />
        ) : (
          <Moon className="w-4 h-4 text-cyan-400" />
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-36 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-xl py-1.5 z-50 animate-in fade-in zoom-in-95 duration-100">
          {options.map((opt) => (
            <button
              key={opt.value}
              onClick={() => {
                setTheme(opt.value);
                setIsOpen(false);
              }}
              className={`w-full flex items-center justify-between px-3 py-2 text-xs font-medium transition-colors ${
                theme === opt.value
                  ? 'text-blue-600 dark:text-cyan-400 bg-blue-50 dark:bg-slate-800/60'
                  : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800/40'
              }`}
            >
              <span className="flex items-center gap-2">
                {opt.icon}
                {opt.label}
              </span>
              {theme === opt.value && <Check className="w-3.5 h-3.5" />}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};
