/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** URL base de la API del backend, por ejemplo http://localhost:8080/api/v1 */
  readonly VITE_API_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
