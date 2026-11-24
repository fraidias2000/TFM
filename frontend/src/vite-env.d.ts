/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** URL base de la API del backend, por ejemplo http://localhost:8080/api/v1 */
  readonly VITE_API_URL?: string
  /** URL del WebSocket de mensajes. Si no se indica, se usa la del API con /ws. */
  readonly VITE_WS_URL?: string
  /** "true" para mostrar el selector de ubicación simulada fuera de desarrollo. */
  readonly VITE_UBICACION_SIMULADA?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
