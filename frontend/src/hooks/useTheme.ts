import { useContext } from 'react';
import { ThemeContext, ThemeContextType, Theme, ResolvedTheme } from '@/context/ThemeContext';

export type { Theme, ResolvedTheme, ThemeContextType };

export const useTheme = (): ThemeContextType => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};
