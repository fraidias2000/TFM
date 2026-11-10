/**
 * Cliente HTTP común para hablar con el backend de HablaBar.
 * Añade el token de acceso, convierte las respuestas de error (ErrorResponse)
 * en ApiError y avisa a la aplicación cuando el token deja de ser válido.
 */

export const API_URL: string =
  import.meta.env.VITE_API_URL ?? "http://localhost:8080/api/v1"

/** Error devuelto por la API con el formato ErrorResponse del backend. */
export class ApiError extends Error {
  readonly status: number
  readonly codigo: string
  readonly detalles: Record<string, string>

  constructor(
    status: number,
    codigo: string,
    mensaje: string,
    detalles: Record<string, string> = {},
  ) {
    super(mensaje)
    this.name = "ApiError"
    this.status = status
    this.codigo = codigo
    this.detalles = detalles
  }
}

let onUnauthorized: (() => void) | null = null

/** Registra qué hacer cuando una petición con token recibe 401 (token caducado o no válido). */
export function setUnauthorizedHandler(handler: (() => void) | null) {
  onUnauthorized = handler
}

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE"
  body?: unknown
  token?: string | null
}

export async function apiFetch<T>(
  path: string,
  { method = "GET", body, token }: RequestOptions = {},
): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" }
  if (body !== undefined) headers["Content-Type"] = "application/json"
  if (token) headers.Authorization = `Bearer ${token}`

  let response: Response
  try {
    response = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(
      0,
      "SIN_CONEXION",
      "No se puede conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.",
    )
  }

  if (response.status === 204) return undefined as T

  const data: unknown = await response.json().catch(() => null)

  if (!response.ok) {
    if (response.status === 401 && token) onUnauthorized?.()
    const error = (data ?? {}) as {
      codigo?: string
      mensaje?: string
      detalles?: Record<string, string>
    }
    throw new ApiError(
      response.status,
      error.codigo ?? "ERROR",
      error.mensaje ?? mensajePorEstado(response.status),
      error.detalles ?? {},
    )
  }

  return data as T
}

function mensajePorEstado(status: number): string {
  if (status === 401) return "Tu sesión ha caducado. Vuelve a iniciar sesión."
  if (status === 403) return "No tienes permiso para realizar esta acción."
  if (status >= 500) return "El servidor no está disponible. Inténtalo más tarde."
  return "No se ha podido completar la operación."
}