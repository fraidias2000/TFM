import { scenarioOptions } from "../data/mockData"
import type { Scenario } from "../types"
import { Icon } from "./Icon"

interface Props {
  value: Scenario
  onChange: (value: Scenario) => void
  open: boolean
  onToggle: () => void
}

export function ScenarioSelector({ value, onChange, open, onToggle }: Props) {
  const active = scenarioOptions.find((item) => item.id === value)!

  return (
    <div className="scenario-wrap">
      <button
        className="scenario-trigger"
        type="button"
        onClick={onToggle}
        aria-expanded={open}
      >
        <span className="pulse-dot" />
        <span>Prueba: {active.short}</span>
        <Icon name="chevron" size={16} />
      </button>
      {open && (
        <>
          <button
            className="scenario-backdrop"
            aria-label="Cerrar escenarios"
            onClick={onToggle}
          />
          <section className="scenario-sheet" aria-label="Escenarios de prueba">
            <div className="sheet-handle" />
            <div className="sheet-heading">
              <div>
                <span className="eyebrow">Modo prototipo</span>
                <h2>Simular un escenario</h2>
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
              Cambia la distancia y el estado para comprobar cada regla de
              acceso.
            </p>
            <div className="scenario-grid">
              {scenarioOptions.map((option) => (
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
