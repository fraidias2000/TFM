import { OPCIONES_UBICACION } from "../services/ubicacion"
import type { FuenteUbicacion } from "../types"
import { Icon } from "./Icon"

interface Props {
  value: FuenteUbicacion
  onChange: (value: FuenteUbicacion) => void
  open: boolean
  onToggle: () => void
}

/**
 * Selector de ubicación para pruebas: permite usar el GPS real o simular
 * posiciones a distancias conocidas del punto A. Solo aparece en desarrollo.
 */
export function LocationSelector({ value, onChange, open, onToggle }: Props) {
  const active = OPCIONES_UBICACION.find((item) => item.id === value)!

  return (
    <div className="scenario-wrap">
      <button
        className="scenario-trigger"
        type="button"
        onClick={onToggle}
        aria-expanded={open}
      >
        <span className="pulse-dot" />
        <span>Ubicación: {active.short}</span>
        <Icon name="chevron" size={16} />
      </button>
      {open && (
        <>
          <button
            className="scenario-backdrop"
            aria-label="Cerrar selector de ubicación"
            onClick={onToggle}
          />
          <section className="scenario-sheet" aria-label="Ubicación de prueba">
            <div className="sheet-handle" />
            <div className="sheet-heading">
              <div>
                <span className="eyebrow">Modo pruebas</span>
                <h2>Elegir ubicación</h2>
              </div>
              <button
                className="icon-button"
                onClick={onToggle}
                aria-label="Cerrar"
              >
                <Icon name="close" />
              </button>
            </div>
            <p className="muted">
              Usa el GPS real o simula una posición. Las distancias se miden
              desde el punto A; el servidor aplica las reglas de acceso.
            </p>
            <div className="scenario-grid">
              {OPCIONES_UBICACION.map((option) => (
                <button
                  className={`scenario-option ${
                    option.id === value ? "is-active" : ""
                  }`}
                  key={option.id}
                  onClick={() => {
                    onChange(option.id)
                    onToggle()
                  }}
                >
                  <span>{option.label}</span>
                  {option.id === value && <Icon name="check" size={18} />}
                </button>
              ))}
            </div>
          </section>
        </>
      )}
    </div>
  )
}