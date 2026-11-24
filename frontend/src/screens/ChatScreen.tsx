import { useEffect, useRef, useState, type FormEvent } from "react"
import { Icon } from "../components/Icon"
import { useAhora } from "../hooks/useAhora"
import { formatTime, minutesRemaining } from "../services/proximity"
import type { Chat, Message, Participant } from "../types"

interface Props {
  chat: Chat
  participants: Participant[]
  messages: Message[]
  alias: string
  busy: boolean
  /** Si la conexión de mensajes en tiempo real está abierta. */
  connected: boolean
  error?: string
  onBack: () => void
  onJoin: () => void
  onLeave: () => void
  /** Envía el mensaje; devuelve true si el servidor lo ha aceptado. */
  onSend: (text: string) => Promise<boolean>
}

export function ChatScreen({
  chat,
  participants,
  messages,
  alias,
  busy,
  connected,
  error,
  onBack,
  onJoin,
  onLeave,
  onSend,
}: Props) {
  const [draft, setDraft] = useState("")
  const [sending, setSending] = useState(false)
  const finalLista = useRef<HTMLDivElement>(null)
  const ahora = useAhora(10_000)
  // El tiempo restante se calcula con la caducidad fijada por el servidor (RF-10).
  const expired = ahora >= chat.expiresAt

  // Muestra siempre el último mensaje.
  useEffect(() => {
    finalLista.current?.scrollIntoView({ block: "end" })
  }, [messages.length])

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!draft.trim() || expired || sending) return
    setSending(true)
    // El texto se envía tal como se ha escrito; el borrador solo se vacía si el
    // servidor confirma el mensaje, para poder reintentarlo si falla.
    const ok = await onSend(draft)
    setSending(false)
    if (ok) setDraft("")
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
            <i className={expired ? "status-expired" : ""} />{" "}
            {expired ? "Caducado" : `${chat.participantCount} participantes`}
          </span>
        </div>
        {chat.joined && !expired ? (
          <button className="leave-link" onClick={onLeave} disabled={busy}>
            Salir
          </button>
        ) : (
          <span />
        )}
      </header>

      <div className="chat-status-bar">
        <span>
          <Icon name="clock" size={16} />{" "}
          {expired
            ? "El chat ha terminado"
            : `Caduca en ${minutesRemaining(chat.expiresAt, ahora)} min`}
        </span>
        <span>
          <Icon name="users" size={16} /> {chat.participantCount}/{chat.capacity}
        </span>
      </div>

      {!chat.joined && !expired && (
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
          <div className={`distance-check ${error ? "has-error" : ""}`}>
            <Icon name={error ? "close" : "check"} />
            <div>
              <strong>
                {error ? "No puedes entrar ahora" : "Comprobaremos tu distancia"}
              </strong>
              <span>
                {error ??
                  (chat.distanceBand !== undefined
                    ? `Estás a menos de ${chat.distanceBand} m del chat`
                    : "Debes estar a menos de 100 m del punto de creación")}
              </span>
            </div>
          </div>
          <button className="primary-button" onClick={onJoin} disabled={busy}>
            {busy ? "Entrando…" : "Entrar al chat"}
          </button>
          <small>
            Salir no te expulsará por distancia, pero tendrás que volver a
            cumplirla para regresar.
          </small>
        </section>
      )}

      {expired ? (
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
            {!connected && (
              <div className="system-message">
                Sin conexión en tiempo real. Reconectando… Los mensajes se
                recuperarán automáticamente.
              </div>
            )}
            {participants.length > 0 && (
              <div className="system-message">
                Participantes:{" "}
                {participants
                  .map((p) => (p.isCreator ? `${p.alias} (creador)` : p.alias))
                  .join(", ")}
              </div>
            )}
            {messages.map((message) => (
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
            {error && <div className="system-message">{error}</div>}
            <div ref={finalLista} />
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
            <button
              disabled={!draft.trim() || sending}
              aria-label="Enviar mensaje"
            >
              <Icon name="send" size={20} />
            </button>
          </form>
        </>
      ) : null}
    </div>
  )
}
