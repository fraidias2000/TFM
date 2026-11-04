import { useMemo, useState, type FormEvent } from "react"
import { Icon } from "../components/Icon"
import type { Chat, LocationSnapshot, Scenario } from "../types"
import {
  canEnterChat,
  formatTime,
  minutesRemaining,
} from "../services/proximity"

interface Props {
  chat: Chat
  scenario: Scenario
  location: LocationSnapshot
  alias: string
  onBack: () => void
  onJoin: () => void
  onLeave: () => void
  onSend: (text: string) => void
}

export function ChatScreen({
  chat,
  scenario,
  location,
  alias,
  onBack,
  onJoin,
  onLeave,
  onSend,
}: Props) {
  const [draft, setDraft] = useState("")
  const effectiveExpired =
    scenario === "expired" || Date.now() >= chat.expiresAt
  const entryError = useMemo(
    () => canEnterChat(chat, location, scenario),
    [chat, location, scenario],
  )
  const participantCount = scenario === "full" ? 20 : chat.participantCount

  function submit(event: FormEvent) {
    event.preventDefault()
    const value = draft.trim()
    if (!value || effectiveExpired) return
    onSend(value)
    setDraft("")
  }

  return (
    <div className="chat-screen">
      <header className="chat-header">
        <button className="icon-button" onClick={onBack} aria-label="Volver">
          <Icon name="arrow" />
        </button>
        <div>
          <h1>{chat.title}</h1>
          <span>
            <i className={effectiveExpired ? "status-expired" : ""} />{" "}
            {effectiveExpired
              ? "Caducado"
              : `${participantCount} participantes`}
          </span>
        </div>
        {chat.joined ? (
          <button className="leave-link" onClick={onLeave}>
            Salir
          </button>
        ) : (
          <span />
        )}
      </header>

      <div className="chat-status-bar">
        <span>
          <Icon name="clock" size={16} />{" "}
          {effectiveExpired
            ? "El chat ha terminado"
            : `Caduca en ${minutesRemaining(chat.expiresAt)} min`}
        </span>
        <span>
          <Icon name="users" size={16} /> {participantCount}/20
        </span>
      </div>

      {!chat.joined && !effectiveExpired && (
        <section className="join-gate">
          <span className="join-icon">
            <Icon name="message" size={30} />
          </span>
          <span className="eyebrow">Chat cercano</span>
          <h2>Únete a la conversación</h2>
          <p>
            Usarás el alias <strong>{alias}</strong>. Tu ubicación exacta no se
            compartirá.
          </p>
          <div className={`distance-check ${entryError ? "has-error" : ""}`}>
            <Icon name={entryError ? "close" : "check"} />
            <div>
              <strong>
                {entryError
                  ? "No puedes entrar ahora"
                  : "Estás dentro del radio"}
              </strong>
              <span>
                {entryError || `${location.distance} m · ubicación válida`}
              </span>
            </div>
          </div>
          <button
            className="primary-button"
            onClick={onJoin}
            disabled={Boolean(entryError)}
          >
            Entrar al chat
          </button>
          <small>
            Salir no te expulsará por distancia, pero tendrás que volver a
            cumplirla para regresar.
          </small>
        </section>
      )}

      {effectiveExpired ? (
        <section className="expired-state">
          <span>
            <Icon name="clock" size={29} />
          </span>
          <h2>Esta conversación ha caducado</h2>
          <p>
            Por privacidad, su historial ya no puede consultarse ni se pueden
            enviar mensajes.
          </p>
          <button className="secondary-button" onClick={onBack}>
            Ver chats activos
          </button>
        </section>
      ) : chat.joined ? (
        <>
          <div className="message-list">
            <div className="day-divider">
              <span>Ahora</span>
            </div>
            <div className="system-message">
              Te has unido como {alias}. Solo se muestra tu alias.
            </div>
            {chat.messages.map((message) => (
              <article
                className={`message-row ${message.own ? "own" : ""}`}
                key={message.id}
              >
                {!message.own && (
                  <span className="message-avatar">
                    {message.author.slice(0, 1)}
                  </span>
                )}
                <div>
                  {!message.own && <strong>{message.author}</strong>}
                  <p>{message.text}</p>
                  <time>{formatTime(message.sentAt)}</time>
                </div>
              </article>
            ))}
          </div>
          <form className="composer" onSubmit={submit}>
            <div>
              <textarea
                aria-label="Mensaje"
                placeholder="Escribe un mensaje…"
                value={draft}
                onChange={(event) =>
                  setDraft(event.target.value.slice(0, 1000))
                }
                maxLength={1000}
                rows={1}
              />
              {draft.length > 850 && <small>{draft.length}/1000</small>}
            </div>
            <button disabled={!draft.trim()} aria-label="Enviar mensaje">
              <Icon name="send" size={20} />
            </button>
          </form>
        </>
      ) : null}
    </div>
  )
}
