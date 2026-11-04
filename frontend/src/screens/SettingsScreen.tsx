import { useState } from "react"
import { Icon } from "../components/Icon"
import type { User } from "../types"

interface Props {
  user: User
  onAliasChange: (alias: string) => void
  onLogout: () => void
}

function Toggle({
  checked,
  onChange,
  label,
}: {
  checked: boolean
  onChange: () => void
  label: string
}) {
  return (
    <button
      className={`toggle ${checked ? "is-on" : ""}`}
      role="switch"
      aria-checked={checked}
      aria-label={label}
      onClick={onChange}
    >
      <span />
    </button>
  )
}

export function SettingsScreen({ user, onAliasChange, onLogout }: Props) {
  const [nearby, setNearby] = useState(true)
  const [messages, setMessages] = useState(true)
  const [expiring, setExpiring] = useState(false)
  const [editing, setEditing] = useState(false)
  const [alias, setAlias] = useState(user.alias)

  return (
    <div className="page settings-page">
      <div className="page-title">
        <span className="eyebrow">Tu espacio</span>
        <h1>Ajustes</h1>
        <p>Controla tu identidad y qué avisos quieres recibir.</p>
      </div>

      <section className="profile-card">
        <div className="large-avatar">
          {user.alias.slice(0, 1).toUpperCase()}
        </div>
        <div>
          <span>Alias visible</span>
          {editing ? (
            <div className="alias-editor">
              <input
                value={alias}
                maxLength={24}
                onChange={(event) => setAlias(event.target.value)}
                autoFocus
              />
              <button
                onClick={() => {
                  onAliasChange(alias.trim() || user.alias)
                  setEditing(false)
                }}
              >
                <Icon name="check" />
              </button>
            </div>
          ) : (
            <>
              <h2>{user.alias}</h2>
              <button className="text-button" onClick={() => setEditing(true)}>
                Editar alias
              </button>
            </>
          )}
        </div>
      </section>

      <section className="settings-section">
        <div className="settings-heading">
          <span>
            <Icon name="bell" />
          </span>
          <div>
            <h2>Notificaciones</h2>
            <p>Simuladas solo en este prototipo</p>
          </div>
        </div>
        <div className="settings-list">
          <div>
            <span>
              <strong>Nuevos chats cercanos</strong>
              <small>Cuando aparece uno en un radio de 100 m</small>
            </span>
            <Toggle
              checked={nearby}
              onChange={() => setNearby(!nearby)}
              label="Nuevos chats cercanos"
            />
          </div>
          <div>
            <span>
              <strong>Mensajes nuevos</strong>
              <small>De conversaciones en las que participas</small>
            </span>
            <Toggle
              checked={messages}
              onChange={() => setMessages(!messages)}
              label="Mensajes nuevos"
            />
          </div>
          <div>
            <span>
              <strong>Chat a punto de caducar</strong>
              <small>Un aviso 10 minutos antes</small>
            </span>
            <Toggle
              checked={expiring}
              onChange={() => setExpiring(!expiring)}
              label="Chat a punto de caducar"
            />
          </div>
        </div>
      </section>

      <section className="privacy-card">
        <span>
          <Icon name="lock" />
        </span>
        <div>
          <h2>Privacidad por diseño</h2>
          <p>
            Nunca mostramos coordenadas, correo ni datos privados. Los chats
            caducados dejan de ser consultables.
          </p>
        </div>
      </section>

      <button className="logout-button" onClick={onLogout}>
        <Icon name="logout" /> Cerrar sesión
      </button>
      <p className="version">
        Prototipo local · Los datos se guardan solo en memoria
      </p>
    </div>
  )
}
