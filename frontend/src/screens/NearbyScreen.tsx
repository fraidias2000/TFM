import { Icon } from "../components/Icon"
import { useAhora } from "../hooks/useAhora"
import { minutesRemaining } from "../services/proximity"
import type { Chat, EstadoUbicacion } from "../types"

interface Props {
  chats: Chat[]
  alias: string
  ubicacion: EstadoUbicacion
  loading: boolean
  notice?: string
  onOpen: (chat: Chat) => void
  onCreate: () => void
  onRefresh: () => void
}

export function NearbyScreen({
  chats,
  alias,
  ubicacion,
  loading,
  notice,
  onOpen,
  onCreate,
  onRefresh,
}: Props) {
  const ahora = useAhora()
  const sinUbicacion = ubicacion.estado === "error"
  const cercanos = chats.filter((chat) => !chat.joined).length

  return (
    <div className="page nearby-page">
      <section className="welcome-row">
        <div>
          <span className="eyebrow">Tu zona ahora</span>
          <h1>Hola, {alias}</h1>
          <p>Descubre conversaciones a menos de 100 m.</p>
        </div>
        <div className="profile-avatar">{alias.slice(0, 1).toUpperCase()}</div>
      </section>

      <section className={`location-card ${sinUbicacion ? "has-error" : ""}`}>
        <span className="location-icon">
          <Icon name={sinUbicacion ? "signal" : "map"} />
        </span>
        <div className="location-copy">
          <strong>
            {ubicacion.estado === "lista"
              ? "Ubicación lista"
              : ubicacion.estado === "buscando"
                ? "Obteniendo ubicación…"
                : "Ubicación no disponible"}
          </strong>
          <span>
            {ubicacion.estado === "lista"
              ? `Actualizada · Precisión ±${ubicacion.precision} m`
              : ubicacion.estado === "buscando"
                ? "Se necesita una señal reciente y precisa."
                : "Actualízala para descubrir y crear chats."}
          </span>
        </div>
        <button onClick={onRefresh} aria-label="Actualizar ubicación">
          <Icon name="compass" size={20} />
        </button>
      </section>

      {(ubicacion.mensaje || notice) && (
        <div className="inline-alert" role="status">
          <Icon name="signal" size={19} />
          <span>{ubicacion.mensaje || notice}</span>
        </div>
      )}

      <div className="section-heading">
        <div>
          <h2>Chats cercanos</h2>
          <span>
            {loading
              ? "Buscando…"
              : sinUbicacion
                ? "Actualiza la ubicación para descubrir"
                : `${cercanos} disponibles en tu zona`}
          </span>
        </div>
        <button className="text-button" onClick={onRefresh} disabled={loading}>
          Actualizar
        </button>
      </div>

      <div className="chat-list">
        {chats.map((chat, index) => {
          const remaining = minutesRemaining(chat.expiresAt, ahora)
          return (
            <button
              className="chat-card"
              key={chat.id}
              onClick={() => onOpen(chat)}
            >
              <div className={`chat-accent accent-${(index % 3) + 1}`}>
                <Icon
                  name={index % 3 === 1 ? "compass" : index % 3 === 2 ? "spark" : "message"}
                  size={24}
                />
              </div>
              <div className="chat-card-main">
                <div className="chat-title-line">
                  <div>
                    <span className="topic">
                      {chat.joined ? "Participando" : chat.topic ?? "Chat cercano"}
                    </span>
                    <h3>{chat.title}</h3>
                  </div>
                  <Icon name="chevron" size={19} />
                </div>
                <div className="chat-meta">
                  {chat.distanceBand !== undefined && (
                    <span>
                      <Icon name="map" size={15} /> &lt; {chat.distanceBand} m
                    </span>
                  )}
                  <span>
                    <Icon name="users" size={15} /> {chat.participantCount}/
                    {chat.capacity}
                  </span>
                  <span className={remaining === 0 ? "danger" : ""}>
                    <Icon name="clock" size={15} />{" "}
                    {remaining === 0 ? "Caducado" : `${remaining} min`}
                  </span>
                </div>
              </div>
            </button>
          )
        })}

        {!loading && chats.length === 0 && (
          <div className="empty-state">
            <span>
              <Icon name="compass" size={27} />
            </span>
            {sinUbicacion ? (
              <>
                <h3>Necesitamos una ubicación válida</h3>
                <p>
                  Actualízala para mostrar chats cercanos sin revelar tus
                  coordenadas.
                </p>
                <button className="secondary-button" onClick={onRefresh}>
                  Actualizar ubicación
                </button>
              </>
            ) : (
              <>
                <h3>No hay chats cerca</h3>
                <p>Crea el primero y avisaremos a quien esté a menos de 100 m.</p>
              </>
            )}
          </div>
        )}
      </div>

      <button className="create-cta" onClick={onCreate}>
        <span>
          <Icon name="plus" />
        </span>
        <span>
          <strong>Crear un chat</strong>
          <small>Durará 1 hora desde su creación</small>
        </span>
        <Icon name="chevron" />
      </button>
    </div>
  )
}