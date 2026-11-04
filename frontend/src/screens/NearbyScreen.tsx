import { Icon } from "../components/Icon"
import type { Chat, LocationSnapshot, Scenario } from "../types"
import { minutesRemaining, validateLocation } from "../services/proximity"

interface Props {
  chats: Chat[]
  alias: string
  location: LocationSnapshot
  scenario: Scenario
  notice?: string
  onOpen: (chat: Chat) => void
  onCreate: () => void
  onRefresh: () => void
}

export function NearbyScreen({
  chats,
  alias,
  location,
  scenario,
  notice,
  onOpen,
  onCreate,
  onRefresh,
}: Props) {
  const locationError = validateLocation(location)

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

      <section className={`location-card ${locationError ? "has-error" : ""}`}>
        <span className="location-icon">
          <Icon name={locationError ? "signal" : "map"} />
        </span>
        <div className="location-copy">
          <strong>
            {locationError ? "Ubicación no disponible" : "Ubicación lista"}
          </strong>
          <span>
            {locationError
              ? "Se necesita una señal reciente y precisa."
              : `Actualizada ahora · Precisión ±${location.accuracy} m`}
          </span>
        </div>
        <button onClick={onRefresh} aria-label="Actualizar ubicación">
          <Icon name="compass" size={20} />
        </button>
      </section>

      {(locationError || notice) && (
        <div className="inline-alert" role="status">
          <Icon name="signal" size={19} />
          <span>{locationError || notice}</span>
        </div>
      )}

      <div className="section-heading">
        <div>
          <h2>Chats cercanos</h2>
          <span>
            {locationError
              ? "Actualiza para descubrir"
              : `${chats.length} activos en tu zona`}
          </span>
        </div>
        <button className="text-button" onClick={onRefresh}>
          Actualizar
        </button>
      </div>

      <div className="chat-list">
        {!locationError &&
          chats.map((chat, index) => {
            const distance = ["50m", "99m", "100m", "120m"].includes(scenario)
              ? location.distance
              : chat.distance
            const participants =
              scenario === "full" && index === 0 ? 20 : chat.participantCount
            const remaining =
              scenario === "expired" && index === 0
                ? 0
                : minutesRemaining(chat.expiresAt)
            return (
              <button
                className="chat-card"
                key={chat.id}
                onClick={() => onOpen(chat)}
              >
                <div className={`chat-accent accent-${index + 1}`}>
                  <Icon
                    name={
                      index === 1
                        ? "compass"
                        : index === 2
                          ? "spark"
                          : "message"
                    }
                    size={24}
                  />
                </div>
                <div className="chat-card-main">
                  <div className="chat-title-line">
                    <div>
                      <span className="topic">{chat.topic}</span>
                      <h3>{chat.title}</h3>
                    </div>
                    <Icon name="chevron" size={19} />
                  </div>
                  <div className="chat-meta">
                    <span className={distance >= 100 ? "danger" : ""}>
                      <Icon name="map" size={15} /> {distance} m
                    </span>
                    <span>
                      <Icon name="users" size={15} /> {participants}/20
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
        {locationError && (
          <div className="empty-state">
            <span>
              <Icon name="compass" size={27} />
            </span>
            <h3>Necesitamos una ubicación válida</h3>
            <p>
              Actualízala para mostrar chats cercanos sin revelar tus
              coordenadas.
            </p>
            <button className="secondary-button" onClick={onRefresh}>
              Actualizar ubicación
            </button>
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
