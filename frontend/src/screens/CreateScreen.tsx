import { useState, type FormEvent } from "react"
import { Icon } from "../components/Icon"
import type { LocationSnapshot } from "../types"
import { validateLocation } from "../services/proximity"

interface Props {
  location: LocationSnapshot
  onCreate: (title: string, topic: string) => void
  onCancel: () => void
  onRefresh: () => void
}

export function CreateScreen({
  location,
  onCreate,
  onCancel,
  onRefresh,
}: Props) {
  const [title, setTitle] = useState("")
  const [topic, setTopic] = useState("Plan espontáneo")
  const locationError = validateLocation(location)

  function submit(event: FormEvent) {
    event.preventDefault()
    if (!locationError) onCreate(title.trim(), topic)
  }

  return (
    <div className="page create-page">
      <button className="back-button" onClick={onCancel}>
        <Icon name="arrow" /> Volver
      </button>
      <div className="page-title">
        <span className="eyebrow">Nuevo punto de encuentro</span>
        <h1>Crea un chat cercano</h1>
        <p>Las personas a menos de 100 m podrán verlo y unirse.</p>
      </div>

      <form className="create-form" onSubmit={submit}>
        <label>
          <span>Nombre del chat</span>
          <input
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="¿Qué está pasando?"
            maxLength={50}
            required
          />
          <small>{title.length}/50</small>
        </label>
        <fieldset>
          <legend>Tema</legend>
          <div className="topic-picker">
            {["Plan espontáneo", "Barrio", "Cultura", "Ayuda"].map((item) => (
              <button
                type="button"
                className={topic === item ? "is-active" : ""}
                key={item}
                onClick={() => setTopic(item)}
              >
                {item}
              </button>
            ))}
          </div>
        </fieldset>

        <section
          className={`creation-location ${locationError ? "has-error" : ""}`}
        >
          <span>
            <Icon name="map" />
          </span>
          <div>
            <strong>
              {locationError
                ? "Actualiza tu ubicación"
                : "Ubicación de creación lista"}
            </strong>
            <p>
              {locationError ||
                `Reciente y con precisión de ±${location.accuracy} m. No mostraremos coordenadas.`}
            </p>
          </div>
          {locationError && (
            <button type="button" onClick={onRefresh}>
              Actualizar
            </button>
          )}
        </section>

        <div className="rules-card">
          <h2>Antes de crear</h2>
          <ul>
            <li>
              <Icon name="clock" /> Caduca 1 hora después de crearse.
            </li>
            <li>
              <Icon name="users" /> Admite hasta 20 participantes.
            </li>
            <li>
              <Icon name="check" /> Entrarás automáticamente como participante.
            </li>
          </ul>
        </div>

        <button
          className="primary-button"
          disabled={Boolean(locationError)}
          type="submit"
        >
          Crear chat <Icon name="plus" />
        </button>
      </form>
    </div>
  )
}
