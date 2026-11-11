import { useEffect, useState } from 'react'

export function useDebounce(value, delay = 400) {
  const [v, setV] = useState(value)
  useEffect(() => {
    const t = setTimeout(() => setV(value), delay)
    return () => clearTimeout(t)
  }, [value, delay])
  return v
}

export const fmt = {
  num: (n, d = 2) => (n == null ? '–' : Number(n).toLocaleString(undefined, { minimumFractionDigits: d, maximumFractionDigits: d })),
  int: (n) => (n == null ? '–' : Number(n).toLocaleString()),
  compact: (n) => (n == null ? '–' : Intl.NumberFormat(undefined, { notation: 'compact', maximumFractionDigits: 2 }).format(n)),
}
