import { useState, type FormEvent } from "react"
import { Icon } from "../components/Icon"
import type { EstadoUbicacion } from "../types"

interface Props {
  ubicacion: EstadoUbicacion
  busy: boolean
  error?: string
  onCreate: (title: string, topic: string) => void
  onCancel: () => void
  onRefresh: () => void
}

export function CreateScreen({
  ubicacion,
  busy,
  error,
  onCreate,
  onCancel,
  onRefresh,
}: Props) {
  const [title, setTitle] = useState("")
  const [topic, setTopic] = useState("Plan espontáneo")
  const sinUbicacion = ubicacion.estado === "error"

  function submit(event: FormEvent) {
    event.preventDefault()
    if (!sinUbicacion && !busy) onCreate(title.trim(), topic)
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
          className={`creation-location ${sinUbicacion ? "has-error" : ""}`}
        >
          <span>
            <Icon name="map" />
          </span>
          <div>
            <strong>
              {sinUbicacion
                ? "Actualiza tu ubicación"
                : ubicacion.estado === "buscando"
                  ? "Obteniendo ubicación…"
                  : "Ubicación de creación lista"}
            </strong>
            <p>
              {sinUbicacion
                ? ubicacion.mensaje
                : ubicacion.estado === "lista"
                  ? `Reciente y con precisión de ±${ubicacion.precision} m. No mostraremos coordenadas.`
                  : "Se usará tu ubicación actual como punto fijo del chat."}
            </p>
          </div>
          {sinUbicacion && (
            <button type="button" onClick={onRefresh}>
              Actualizar
            </button>
          )}
        </section>

        {error && (
          <div className="inline-alert" role="alert">
            <Icon name="signal" size={19} />
            <span>{error}</span>
          </div>
        )}

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
          disabled={sinUbicacion || busy}
          type="submit"
        >
          {busy ? "Creando…" : "Crear chat"} <Icon name="plus" />
        </button>
      </form>
    </div>
  )
}