import { useAhora } from "../hooks/useAhora"
import { minutesRemaining } from "../services/proximity"
import type { Aviso } from "../types"
import { Icon } from "./Icon"

interface Props {
  aviso: Aviso
  /** Avisos pendientes además del visible. */
  pendientes: number
  onVer: () => void
  onCerrar: () => void
}

/** Aviso emergente de un nuevo chat cercano (RF-06). */
export function AvisoToast({ aviso, pendientes, onVer, onCerrar }: Props) {
  const ahora = useAhora(10_000)
  const minutos = minutesRemaining(aviso.expiresAt, ahora)

  return (
    <aside className="aviso-toast" role="status" aria-live="polite">
      <span className="aviso-icon">
        <Icon name="bell" size={20} />
      </span>
      <div className="aviso-copy">
        <span>
          Nuevo chat cerca{pendientes > 0 ? ` · ${pendientes} más` : ""}
        </span>
        <strong>{aviso.title}</strong>
        <small>
          A menos de {aviso.distanceBand} m · Caduca en {minutos} min
        </small>
        <div className="aviso-acciones">
          <button className="aviso-ver" onClick={onVer}>
            Ver chat
          </button>
          <button className="aviso-cerrar" onClick={onCerrar}>
            Ahora no
          </button>
        </div>
      </div>
    </aside>
  )
}
