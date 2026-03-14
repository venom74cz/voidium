import { createContext, useContext } from 'react'

type Locale = 'en' | 'cz'

const LocaleContext = createContext<Locale>('en')

export const LocaleProvider = LocaleContext.Provider

export function useLocale(): Locale {
  return useContext(LocaleContext)
}

export function useTr() {
  const locale = useLocale()
  return (en: string, cz: string) => locale === 'cz' ? cz : en
}
