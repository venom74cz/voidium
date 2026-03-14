import { createContext, useContext } from 'react'

type Theme = 'dark' | 'light'

const ThemeContext = createContext<Theme>('dark')

export const ThemeProvider = ThemeContext.Provider

export function useTheme(): Theme {
  return useContext(ThemeContext)
}
