/**
 * Registro, inicio de sesión y conservación de la sesión (RF-01, RF-02).
 * El token se guarda en el almacenamiento local del navegador para que la sesión
 * se mantenga al recargar; al cerrar sesión se elimina.
 */
import { apiFetch } from "./api"
import type { User } from "../types"

/** Perfil tal como lo devuelve el backend (UsuarioResponse). */
interface UsuarioResponse {
  id: string
  alias: string
  correo: string
  avisosActivados: boolean
  fechaAlta: string
}

/** Respuesta de registro e inicio de sesión (AuthResponse). */
interface AuthResponse {
  token: string
  tipo: string
  expiraEn: string
  usuario: UsuarioResponse
}

export interface Session {
  token: string
  expiraEn: string
  user: User
}

const STORAGE_KEY = "hablabar.sesion"

function toUser(usuario: UsuarioResponse): User {
  return {
    id: usuario.id,
    alias: usuario.alias,
    email: usuario.correo,
    avisosActivados: usuario.avisosActivados,
  }
}

function toSession(response: AuthResponse): Session {
  return {
    token: response.token,
    expiraEn: response.expiraEn,
    user: toUser(response.usuario),
  }
}

export async function registrar(
  alias: string,
  correo: string,
  password: string,
): Promise<Session> {
  const response = await apiFetch<AuthResponse>("/auth/registro", {
    method: "POST",
    body: { alias, correo, password },
  })
  return toSession(response)
}

export async function iniciarSesion(
  correo: string,
  password: string,
): Promise<Session> {
  const response = await apiFetch<AuthResponse>("/auth/login", {
    method: "POST",
    body: { correo, password },
  })
  return toSession(response)
}

/** Comprueba que el token sigue siendo válido y devuelve el perfil actualizado. */
export async function obtenerPerfil(token: string): Promise<User> {
  const usuario = await apiFetch<UsuarioResponse>("/usuarios/me", { token })
  return toUser(usuario)
}

// ------------------------------------------------------------ Almacenamiento

export function guardarSesion(session: Session) {
  try {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({ token: session.token, expiraEn: session.expiraEn }),
    )
  } catch {
    // Sin almacenamiento disponible: la sesión dura hasta recargar la página.
  }
}

/** Devuelve el token guardado si existe y no ha caducado. */
export function leerTokenGuardado(): { token: string; expiraEn: string } | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const stored = JSON.parse(raw) as { token?: string; expiraEn?: string }
    if (!stored.token || !stored.expiraEn) return null
    if (new Date(stored.expiraEn).getTime() <= Date.now()) {
      borrarSesion()
      return null
    }
    return { token: stored.token, expiraEn: stored.expiraEn }
  } catch {
    return null
  }
}

export function borrarSesion() {
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    // Nada que borrar.
  }
}