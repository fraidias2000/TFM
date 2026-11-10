import { useState, type FormEvent } from "react"
import { Icon } from "../components/Icon"
import { ApiError } from "../services/api"
import { iniciarSesion, registrar, type Session } from "../services/auth"

interface Props {
  onAuthenticate: (session: Session) => void
}

type Field = "alias" | "correo" | "password"

export function AuthScreen({ onAuthenticate }: Props) {
  const [mode, setMode] = useState<"login" | "register">("register")
  const [alias, setAlias] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<Field, string>>>({})

  function changeMode(next: "login" | "register") {
    setMode(next)
    setError("")
    setFieldErrors({})
  }

  function clearFieldError(field: Field) {
    setError("")
    setFieldErrors((current) => ({ ...current, [field]: undefined }))
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (loading) return
    setLoading(true)
    setError("")
    setFieldErrors({})
    try {
      const session =
        mode === "register"
          ? await registrar(alias.trim(), email.trim(), password)
          : await iniciarSesion(email.trim(), password)
      onAuthenticate(session)
    } catch (err) {
      if (err instanceof ApiError) {
        const detalles = err.detalles as Partial<Record<Field, string>>
        if (Object.keys(detalles).length > 0) {
          setFieldErrors(detalles)
        } else if (err.codigo === "CORREO_REGISTRADO") {
          setFieldErrors({ correo: err.message })
        } else {
          setError(err.message)
        }
      } else {
        setError("Ha ocurrido un error inesperado. Inténtalo de nuevo.")
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-visual">
        <div className="brand brand-light auth-brand">
          <span className="brand-mark">
            <Icon name="signal" size={22} />
          </span>
          <span>HablaBar</span>
        </div>
        <div className="orbit" aria-hidden="true">
          <span className="orbit-ring ring-one" />
          <span className="orbit-ring ring-two" />
          <span className="avatar-dot avatar-one">L</span>
          <span className="avatar-dot avatar-two">N</span>
          <span className="avatar-dot avatar-three">V</span>
          <span className="center-pin">
            <Icon name="message" size={25} />
          </span>
        </div>
        <div className="visual-copy">
          <span className="eyebrow light">Aquí y ahora</span>
          <h1>Habla con quienes tienes cerca.</h1>
          <p>Chats temporales, locales y sin compartir tu ubicación exacta.</p>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-mobile-brand brand">
          <span className="brand-mark">
            <Icon name="signal" size={20} />
          </span>
          <span>HablaBar</span>
        </div>
        <div className="auth-heading">
          <span className="eyebrow">Empieza a conectar</span>
          <h2>{mode === "register" ? "Crea tu perfil" : "Qué bueno verte"}</h2>
          <p>
            {mode === "register"
              ? "Elige cómo te verán las personas cercanas."
              : "Entra para ver qué está pasando cerca."}
          </p>
        </div>

        <div className="auth-tabs" role="tablist">
          <button
            className={mode === "register" ? "is-active" : ""}
            type="button"
            onClick={() => changeMode("register")}
          >
            Registrarme
          </button>
          <button
            className={mode === "login" ? "is-active" : ""}
            type="button"
            onClick={() => changeMode("login")}
          >
            Iniciar sesión
          </button>
        </div>

        <form onSubmit={submit} className="auth-form">
          {mode === "register" && (
            <label>
              <span>Alias visible</span>
              <div className={`input-wrap ${fieldErrors.alias ? "has-error" : ""}`}>
                <Icon name="user" size={19} />
                <input
                  value={alias}
                  onChange={(event) => {
                    setAlias(event.target.value)
                    clearFieldError("alias")
                  }}
                  placeholder="Por ejemplo, Ari"
                  autoComplete="nickname"
                  maxLength={24}
                  required
                />
              </div>
              {fieldErrors.alias ? (
                <small className="field-error">{fieldErrors.alias}</small>
              ) : (
                <small>Este nombre será visible para otros participantes.</small>
              )}
            </label>
          )}
          <label>
            <span>Correo electrónico</span>
            <div className={`input-wrap ${fieldErrors.correo ? "has-error" : ""}`}>
              <Icon name="message" size={19} />
              <input
                type="email"
                value={email}
                onChange={(event) => {
                  setEmail(event.target.value)
                  clearFieldError("correo")
                }}
                placeholder="tu@correo.com"
                autoComplete="email"
                maxLength={254}
                required
              />
            </div>
            {fieldErrors.correo && (
              <small className="field-error">{fieldErrors.correo}</small>
            )}
          </label>
          <label>
            <span>Contraseña</span>
            <div className={`input-wrap ${fieldErrors.password ? "has-error" : ""}`}>
              <Icon name="lock" size={19} />
              <input
                type="password"
                value={password}
                onChange={(event) => {
                  setPassword(event.target.value)
                  clearFieldError("password")
                }}
                placeholder="8 caracteres como mínimo"
                autoComplete={mode === "register" ? "new-password" : "current-password"}
                minLength={mode === "register" ? 8 : undefined}
                maxLength={64}
                required
              />
              <Icon name="eye" size={19} />
            </div>
            {fieldErrors.password && (
              <small className="field-error">{fieldErrors.password}</small>
            )}
          </label>
          {error && (
            <div className="inline-alert" role="alert">
              <Icon name="lock" size={19} />
              <span>{error}</span>
            </div>
          )}
          <button className="primary-button" type="submit" disabled={loading}>
            {loading
              ? mode === "register"
                ? "Creando cuenta…"
                : "Entrando…"
              : mode === "register"
                ? "Crear cuenta"
                : "Entrar"}
            {!loading && <Icon name="arrow" className="arrow-forward" />}
          </button>
        </form>

        <div className="security-line">
          <Icon name="lock" size={16} />
          <span>Tu correo y tus datos privados nunca se muestran a otros.</span>
        </div>
      </section>
    </main>
  )
}