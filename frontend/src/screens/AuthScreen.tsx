import { useState, type FormEvent } from "react"
import { Icon } from "../components/Icon"
import type { User } from "../types"

interface Props {
  onAuthenticate: (user: User) => void
}

export function AuthScreen({ onAuthenticate }: Props) {
  const [mode, setMode] = useState<"login" | "register">("register")
  const [alias, setAlias] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")

  function submit(event: FormEvent) {
    event.preventDefault()
    onAuthenticate({
      alias: alias.trim() || "Ari",
      email: email.trim() || "demo@hablabar.app",
    })
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
            onClick={() => setMode("register")}
          >
            Registrarme
          </button>
          <button
            className={mode === "login" ? "is-active" : ""}
            onClick={() => setMode("login")}
          >
            Iniciar sesión
          </button>
        </div>

        <form onSubmit={submit} className="auth-form">
          <label>
            <span>Alias visible</span>
            <div className="input-wrap">
              <Icon name="user" size={19} />
              <input
                value={alias}
                onChange={(event) => setAlias(event.target.value)}
                placeholder="Por ejemplo, Ari"
                maxLength={24}
                required
              />
            </div>
            <small>Este nombre será visible para otros participantes.</small>
          </label>
          <label>
            <span>Correo electrónico</span>
            <div className="input-wrap">
              <Icon name="message" size={19} />
              <input
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="tu@correo.com"
                required
              />
            </div>
          </label>
          <label>
            <span>Contraseña</span>
            <div className="input-wrap">
              <Icon name="lock" size={19} />
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="8 caracteres como mínimo"
                minLength={8}
                required
              />
              <Icon name="eye" size={19} />
            </div>
          </label>
          <button className="primary-button" type="submit">
            {mode === "register" ? "Crear cuenta" : "Entrar"}
            <Icon name="arrow" className="arrow-forward" />
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
