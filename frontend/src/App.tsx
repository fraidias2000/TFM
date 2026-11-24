import { useCallback, useEffect, useRef, useState } from "react"
import { AppShell } from "./components/AppShell"
import { useUbicacion } from "./hooks/useUbicacion"
import { ApiError, setUnauthorizedHandler } from "./services/api"
import {
  borrarSesion,
  guardarSesion,
  leerTokenGuardado,
  obtenerPerfil,
  type Session,
} from "./services/auth"
import {
  chatsCercanos,
  chatsParticipando,
  crearChat,
  detalleChat,
  entrarChat,
  participantesChat,
  salirChat,
} from "./services/chats"
import {
  aMensaje,
  enviarMensaje,
  fusionar,
  historialMensajes,
  nuevoIdCliente,
  type MensajeApi,
} from "./services/mensajes"
import { conectarTiempoReal } from "./services/tiempoReal"
import {
  borrarUbicacion,
  guardarFuente,
  leerFuenteGuardada,
} from "./services/ubicacion"
import { AuthScreen } from "./screens/AuthScreen"
import { ChatScreen } from "./screens/ChatScreen"
import { CreateScreen } from "./screens/CreateScreen"
import { NearbyScreen } from "./screens/NearbyScreen"
import { SettingsScreen } from "./screens/SettingsScreen"
import type {
  Chat,
  FuenteUbicacion,
  Message,
  Participant,
  Screen,
  User,
} from "./types"

/** Cada cuánto se refrescan la lista de chats y el chat abierto. */
const INTERVALO_REFRESCO = 20_000

function mensajeDe(error: unknown): string {
  return error instanceof Error ? error.message : "No se ha podido completar la operación."
}

export default function App() {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(null)
  // Mientras se comprueba si hay una sesión guardada no se muestra el login.
  const [checkingSession, setCheckingSession] = useState(
    () => leerTokenGuardado() !== null,
  )
  const [screen, setScreen] = useState<Screen>("nearby")
  const [fuente, setFuente] = useState<FuenteUbicacion>(leerFuenteGuardada)
  const [selectorOpen, setSelectorOpen] = useState(false)

  const [chats, setChats] = useState<Chat[]>([])
  const [loadingChats, setLoadingChats] = useState(false)
  const [activeChat, setActiveChat] = useState<Chat | null>(null)
  const [participants, setParticipants] = useState<Participant[]>([])
  // Mensajes de cada chat, indexados por el identificador del chat.
  const [messages, setMessages] = useState<Record<string, Message[]>>({})
  const [tiempoRealConectado, setTiempoRealConectado] = useState(false)
  // Último envío fallido: si se repite el mismo texto, se reutiliza su
  // identificador para que el servidor no lo duplique (RNF-06).
  const pendiente = useRef<{ chatId: string, texto: string, idCliente: string } | null>(null)
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState("")
  const [chatError, setChatError] = useState("")

  const { ubicacion, actualizarUbicacion, asegurarUbicacion } = useUbicacion(
    token,
    fuente,
  )
  const tokenRef = useRef(token)
  tokenRef.current = token
  const userIdRef = useRef(user?.id ?? "")
  userIdRef.current = user?.id ?? ""

  /** Añade mensajes del servidor a su chat, sin duplicarlos. */
  const incorporarMensajes = useCallback((chatId: string, nuevos: MensajeApi[]) => {
    if (nuevos.length === 0) return
    setMessages((actuales) => ({
      ...actuales,
      [chatId]: fusionar(
        actuales[chatId] ?? [],
        nuevos.map((mensaje) => aMensaje(mensaje, userIdRef.current)),
      ),
    }))
  }, [])

  const logout = useCallback(() => {
    const actual = tokenRef.current
    // Se elimina la ubicación del servidor antes de olvidar el token.
    if (actual) borrarUbicacion(actual).catch(() => undefined)
    borrarSesion()
    setToken(null)
    setUser(null)
    setScreen("nearby")
    setActiveChat(null)
    setChats([])
    setParticipants([])
    setMessages({})
    setNotice("")
  }, [])

  function startSession(session: Session) {
    guardarSesion(session)
    setToken(session.token)
    setUser(session.user)
  }

  // Si una petición autenticada recibe 401 (token caducado), se cierra la sesión.
  useEffect(() => {
    setUnauthorizedHandler(logout)
    return () => setUnauthorizedHandler(null)
  }, [logout])

  // Al abrir la app, recupera la sesión guardada y comprueba que el token sigue siendo válido.
  useEffect(() => {
    const stored = leerTokenGuardado()
    if (!stored) return
    let cancelled = false
    obtenerPerfil(stored.token)
      .then((perfil) => {
        if (cancelled) return
        setToken(stored.token)
        setUser(perfil)
      })
      .catch(() => {
        if (!cancelled) borrarSesion()
      })
      .finally(() => {
        if (!cancelled) setCheckingSession(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  // Mientras hay sesión, se mantiene abierta la conexión de mensajes en tiempo real.
  useEffect(() => {
    if (!token) return
    return conectarTiempoReal(
      token,
      (mensaje) => incorporarMensajes(mensaje.chatId, [mensaje]),
      setTiempoRealConectado,
    )
  }, [token, incorporarMensajes])

  // ------------------------------------------------------------ Lista de chats

  /**
   * Carga los chats en los que el usuario participa (RF-14), que no exigen
   * proximidad, y los chats cercanos disponibles (RF-05), que sí la exigen.
   */
  const cargarChats = useCallback(async () => {
    const actual = tokenRef.current
    if (!actual) return
    setLoadingChats(true)
    try {
      const participando = await chatsParticipando(actual)
      let cercanos: Chat[] = []
      if (await asegurarUbicacion()) {
        try {
          cercanos = await chatsCercanos(actual)
          setNotice("")
        } catch (error) {
          setNotice(mensajeDe(error))
        }
      }
      const tramos = new Map(cercanos.map((chat) => [chat.id, chat.distanceBand]))
      const ids = new Set(participando.map((chat) => chat.id))
      setChats([
        ...participando.map((chat) => ({ ...chat, distanceBand: tramos.get(chat.id) })),
        ...cercanos.filter((chat) => !ids.has(chat.id)),
      ])
    } catch (error) {
      setNotice(mensajeDe(error))
    } finally {
      setLoadingChats(false)
    }
  }, [asegurarUbicacion])

  // Mientras se ve la lista, se carga al entrar, al cambiar de ubicación y cada 20 s.
  useEffect(() => {
    if (!token || screen !== "nearby") return
    void cargarChats()
    const id = window.setInterval(() => void cargarChats(), INTERVALO_REFRESCO)
    return () => window.clearInterval(id)
  }, [token, screen, fuente, cargarChats])

  // ------------------------------------------------------------- Chat abierto

  const refrescarChatAbierto = useCallback(async (chatId: string) => {
    const actual = tokenRef.current
    if (!actual) return
    try {
      // El historial recupera los mensajes que no hayan llegado por WebSocket (RF-14).
      const [detalle, lista, historial] = await Promise.all([
        detalleChat(actual, chatId),
        participantesChat(actual, chatId),
        historialMensajes(actual, chatId),
      ])
      setActiveChat(detalle)
      setParticipants(lista)
      incorporarMensajes(chatId, historial)
    } catch (error) {
      if (error instanceof ApiError && error.codigo === "CHAT_FINALIZADO") {
        // El servidor manda sobre la caducidad aunque el reloj local no haya llegado (RF-13).
        setActiveChat((chat) => (chat ? { ...chat, expiresAt: Date.now() } : chat))
      } else if (error instanceof ApiError && error.codigo === "NO_PARTICIPA") {
        setActiveChat((chat) => (chat ? { ...chat, joined: false } : chat))
      } else {
        setChatError(mensajeDe(error))
      }
    }
  }, [incorporarMensajes])

  // Mientras se participa en el chat abierto, se refrescan sus datos cada 20 s
  // y también al recuperar la conexión en tiempo real.
  const activeId = activeChat?.id
  const activeJoined = activeChat?.joined
  useEffect(() => {
    if (screen !== "chat" || !activeId || !activeJoined) return
    void refrescarChatAbierto(activeId)
    const id = window.setInterval(
      () => void refrescarChatAbierto(activeId),
      INTERVALO_REFRESCO,
    )
    return () => window.clearInterval(id)
  }, [screen, activeId, activeJoined, refrescarChatAbierto, tiempoRealConectado])

  function navigate(next: Screen) {
    setScreen(next)
    setNotice("")
    setChatError("")
  }

  function openChat(chat: Chat) {
    setActiveChat(chat)
    setParticipants([])
    setChatError("")
    setScreen("chat")
  }

  async function joinChat() {
    if (!activeChat || !token) return
    setBusy(true)
    setChatError("")
    try {
      if (!(await asegurarUbicacion())) {
        setChatError("Necesitas una ubicación válida para entrar. Actualízala e inténtalo de nuevo.")
        return
      }
      setActiveChat(await entrarChat(token, activeChat.id))
    } catch (error) {
      if (error instanceof ApiError && error.codigo === "CHAT_FINALIZADO") {
        setActiveChat({ ...activeChat, expiresAt: Date.now() })
      }
      setChatError(mensajeDe(error))
    } finally {
      setBusy(false)
    }
  }

  async function leaveChat() {
    if (!activeChat || !token) return
    setBusy(true)
    try {
      await salirChat(token, activeChat.id)
      setActiveChat(null)
      setParticipants([])
      navigate("nearby")
    } catch (error) {
      setChatError(mensajeDe(error))
    } finally {
      setBusy(false)
    }
  }

  async function createChat(title: string, topic: string) {
    if (!token) return
    setBusy(true)
    setNotice("")
    try {
      if (!(await asegurarUbicacion())) {
        setNotice("Necesitas una ubicación válida para crear un chat.")
        return
      }
      const chat = await crearChat(token, title, topic || null)
      openChat(chat)
    } catch (error) {
      setNotice(mensajeDe(error))
    } finally {
      setBusy(false)
    }
  }

  /** Envía un mensaje. Devuelve true si el servidor lo ha aceptado. */
  async function sendMessage(text: string): Promise<boolean> {
    if (!activeChat || !token) return false
    const chatId = activeChat.id
    const anterior = pendiente.current
    const idCliente =
      anterior && anterior.chatId === chatId && anterior.texto === text
        ? anterior.idCliente
        : nuevoIdCliente()
    pendiente.current = { chatId, texto: text, idCliente }
    setChatError("")
    try {
      const mensaje = await enviarMensaje(token, chatId, idCliente, text)
      pendiente.current = null
      incorporarMensajes(chatId, [mensaje])
      return true
    } catch (error) {
      if (error instanceof ApiError && error.codigo === "CHAT_FINALIZADO") {
        setActiveChat((chat) => (chat ? { ...chat, expiresAt: Date.now() } : chat))
      } else if (error instanceof ApiError && error.codigo === "NO_PARTICIPA") {
        setActiveChat((chat) => (chat ? { ...chat, joined: false } : chat))
      }
      setChatError(mensajeDe(error))
      return false
    }
  }

  function cambiarFuente(next: FuenteUbicacion) {
    guardarFuente(next)
    setFuente(next)
    setNotice("")
  }

  if (checkingSession) {
    return (
      <main className="session-loading" aria-busy="true">
        <span>Comprobando sesión…</span>
      </main>
    )
  }

  if (!user || !token) return <AuthScreen onAuthenticate={startSession} />

  return (
    <AppShell
      active={screen}
      onNavigate={navigate}
      fuente={fuente}
      onFuenteChange={cambiarFuente}
      selectorOpen={selectorOpen}
      onSelectorToggle={() => setSelectorOpen((open) => !open)}
    >
      {screen === "nearby" && (
        <NearbyScreen
          chats={chats}
          alias={user.alias}
          ubicacion={ubicacion}
          loading={loadingChats}
          notice={notice}
          onOpen={openChat}
          onCreate={() => navigate("create")}
          onRefresh={() => {
            void actualizarUbicacion().then(() => cargarChats())
          }}
        />
      )}
      {screen === "create" && (
        <CreateScreen
          ubicacion={ubicacion}
          busy={busy}
          error={notice}
          onCreate={(title, topic) => void createChat(title, topic)}
          onCancel={() => navigate("nearby")}
          onRefresh={() => void actualizarUbicacion()}
        />
      )}
      {screen === "chat" && activeChat && (
        <ChatScreen
          chat={activeChat}
          participants={participants}
          messages={messages[activeChat.id] ?? []}
          alias={user.alias}
          busy={busy}
          connected={tiempoRealConectado}
          error={chatError || undefined}
          onBack={() => navigate("nearby")}
          onJoin={() => void joinChat()}
          onLeave={() => void leaveChat()}
          onSend={sendMessage}
        />
      )}
      {screen === "settings" && (
        <SettingsScreen
          user={user}
          onAliasChange={(alias) => setUser({ ...user, alias })}
          onLogout={logout}
        />
      )}
    </AppShell>
  )
}
