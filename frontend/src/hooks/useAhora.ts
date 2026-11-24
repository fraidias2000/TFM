import { useEffect, useState } from "react"

/** Hora actual que se actualiza periódicamente, para recalcular el tiempo restante (RF-10). */
export function useAhora(intervalo = 15_000): number {
  const [ahora, setAhora] = useState(() => Date.now())
  useEffect(() => {
    const id = window.setInterval(() => setAhora(Date.now()), intervalo)
    return () => window.clearInterval(id)
  }, [intervalo])
  return ahora
}