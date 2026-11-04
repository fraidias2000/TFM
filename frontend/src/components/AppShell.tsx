import type { ReactNode } from "react"
import type { Scenario, Screen } from "../types"
import { Icon, type IconName } from "./Icon"
import { ScenarioSelector } from "./ScenarioSelector"

interface Props {
  children: ReactNode
  active: Screen
  onNavigate: (screen: Screen) => void
  scenario: Scenario
  onScenarioChange: (scenario: Scenario) => void
  scenarioOpen: boolean
  onScenarioToggle: () => void
}

const navItems: { id: Screen, label: string, icon: IconName }[] = [
  { id: "nearby", label: "HablaBar", icon: "compass" },
  { id: "create", label: "Crear", icon: "plus" },
  { id: "settings", label: "Ajustes", icon: "user" },
]

export function AppShell({
  children,
  active,
  onNavigate,
  scenario,
  onScenarioChange,
  scenarioOpen,
  onScenarioToggle,
}: Props) {
  return (
    <div className="app-frame">
      <aside className="desktop-rail">
        <div className="brand brand-light">
          <span className="brand-mark">
            <Icon name="signal" size={22} />
          </span>
          <span>HablaBar</span>
        </div>
        <p className="rail-copy">
          Conversaciones espontáneas con personas que están aquí, ahora.
        </p>
        <nav className="rail-nav" aria-label="Navegación principal">
          {navItems.map((item) => (
            <button
              key={item.id}
              className={active === item.id ? "is-active" : ""}
              onClick={() => onNavigate(item.id)}
            >
              <Icon name={item.icon} />
              {item.label}
            </button>
          ))}
        </nav>
        <div className="privacy-note">
          <Icon name="lock" size={18} />
          <span>Tu ubicación exacta nunca se comparte.</span>
        </div>
      </aside>

      <main className="mobile-app">
        <header className="topbar">
          <div className="brand">
            <span className="brand-mark">
              <Icon name="signal" size={19} />
            </span>
            <span>HablaBar</span>
          </div>
          <ScenarioSelector
            value={scenario}
            onChange={onScenarioChange}
            open={scenarioOpen}
            onToggle={onScenarioToggle}
          />
        </header>
        <div className="screen-content">{children}</div>
        {active !== "chat" && (
          <nav className="bottom-nav" aria-label="Navegación principal">
            {navItems.map((item) => (
              <button
                key={item.id}
                className={active === item.id ? "is-active" : ""}
                onClick={() => onNavigate(item.id)}
              >
                <span className="nav-icon">
                  <Icon name={item.icon} size={22} />
                </span>
                <span>{item.label}</span>
              </button>
            ))}
          </nav>
        )}
      </main>
    </div>
  )
}
