import { useState } from "react"
import { Icon } from "../components/Icon"
import { pedirPermisoNotificaciones } from "../services/avisos"
import type { User } from "../types"

interface Props {
  user: User
  /** Guarda los cambios en el servidor; lanza un error si no se aceptan. */
  onProfileChange: (cambios: { alias?: string, avisosActivados?: boolean }) => Promise<void>
  onLogout: () => void
}

function Toggle({
  checked,
  onChange,
  label,
  disabled,
}: {
  checked: boolean
  onChange: () => void
  label: string
  disabled?: boolean
}) {
  return (
    <button
      className={`toggle ${checked ? "is-on" : ""}`}
      role="switch"
      aria-checked={checked}
      aria-label={label}
      onClick={onChange}
      disabled={disabled}
    >
      <span />
    </button>
  )
}

function estadoPermiso(): NotificationPermission | "no-disponible" {
  return typeof Notification === "undefined" ? "no-disponible" : Notification.permission
}

const TEXTO_PERMISO: Record<NotificationPermission | "no-disponible", string> = {
  granted: "Permitidas: recibirás el aviso aunque la pestaña esté en segundo plano",
  denied: "Bloqueadas en el navegador. Puedes permitirlas en la configuración del sitio",
  default: "Aún no se han permitido",
  "no-disponible": "Este navegador no las admite",
}

export function SettingsScreen({ user, onProfileChange, onLogout }: Props) {
  const [editing, setEditing] = useState(false)
  const [alias, setAlias] = useState(user.alias)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState("")
  const [permiso, setPermiso] = useState(estadoPermiso)

  async function guardar(cambios: { alias?: string, avisosActivados?: boolean }) {
    setSaving(true)
    setError("")
    try {
      await onProfileChange(cambios)
      return true
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se han podido guardar los cambios.")
      return false
    } finally {
      setSaving(false)
    }
  }

  async function guardarAlias() {
    const nuevo = alias.trim()
    if (!nuevo || nuevo === user.alias) {
      setAlias(user.alias)
      setEditing(false)
      return
    }
    if (await guardar({ alias: nuevo })) setEditing(false)
  }

  async function cambiarAvisos() {
    const activar = !user.avisosActivados
    // Al activar los avisos se pide también el permiso del navegador, que es
    // independiente de la preferencia guardada en el servidor (RF-15).
    if (activar) setPermiso(await pedirPermisoNotificaciones())
    await guardar({ avisosActivados: activar })
  }

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
                maxLength={50}
                onChange={(event) => setAlias(event.target.value)}
                autoFocus
              />
              <button
                onClick={() => void guardarAlias()}
                disabled={saving}
                aria-label="Guardar alias"
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

      {error && (
        <div className="inline-alert" role="alert">
          <Icon name="signal" size={19} />
          <span>{error}</span>
        </div>
      )}

      <section className="settings-section">
        <div className="settings-heading">
          <span>
            <Icon name="bell" />
          </span>
          <div>
            <h2>Notificaciones</h2>
            <p>Llegan mientras la aplicación está abierta</p>
          </div>
        </div>
        <div className="settings-list">
          <div>
            <span>
              <strong>Nuevos chats cercanos</strong>
              <small>Cuando alguien crea uno a menos de 100 m</small>
            </span>
            <Toggle
              checked={user.avisosActivados}
              onChange={() => void cambiarAvisos()}
              label="Nuevos chats cercanos"
              disabled={saving}
            />
          </div>
          <div>
            <span>
              <strong>Notificaciones del navegador</strong>
              <small>{TEXTO_PERMISO[permiso]}</small>
            </span>
            {permiso === "default" && (
              <button
                className="text-button"
                onClick={() => void pedirPermisoNotificaciones().then(setPermiso)}
              >
                Permitir
              </button>
            )}
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
        Prototipo · Tu ubicación solo se conserva 30 segundos en el servidor
      </p>
    </div>
  )
}
