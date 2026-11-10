import { useCallback, useEffect, useMemo, useState } from "react"
import { AppShell } from "./components/AppShell"
import { initialChats, locationForScenario } from "./data/mockData"
import {
  canEnterChat,
  CHAT_LIFETIME,
  validateLocation,
} from "./services/proximity"
import { setUnauthorizedHandler } from "./services/api"
import {
  borrarSesion,
  guardarSesion,
  leerTokenGuardado,
  obtenerPerfil,
  type Session,
} from "./services/auth"
import { AuthScreen } from "./screens/AuthScreen"
import { ChatScreen } from "./screens/ChatScreen"
import { CreateScreen } from "./screens/CreateScreen"
import { NearbyScreen } from "./screens/NearbyScreen"
import { SettingsScreen } from "./screens/SettingsScreen"
import type { Chat, Scenario, Screen, User } from "./types"

export default function App() {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(null)
  // Mientras se comprueba si hay una sesión guardada no se muestra el login.
  const [checkingSession, setCheckingSession] = useState(
    () => leerTokenGuardado() !== null,
  )
  const [screen, setScreen] = useState<Screen>("nearby")
  const [scenario, setScenario] = useState<Scenario>("50m")
  const [scenarioOpen, setScenarioOpen] = useState(false)
  const [locationVersion, setLocationVersion] = useState(0)
  const [chats, setChats] = useState<Chat[]>(initialChats)
  const [activeChatId, setActiveChatId] = useState<string | null>(null)
  const [notice, setNotice] = useState("")

  const location = useMemo(
    () => locationForScenario(scenario),
    [scenario, locationVersion],
  )
  const activeChat = chats.find((chat) => chat.id === activeChatId)
  
  const logout = useCallback(() => {
    borrarSesion()
    setToken(null)
    setUser(null)
    setScreen("nearby")
    setActiveChatId(null)
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

  function navigate(next: Screen) {
    setScreen(next)
    setNotice("")
  }

  function openChat(chat: Chat) {
    setActiveChatId(chat.id)
    setScreen("chat")
  }

  function joinChat() {
    if (!activeChat || !user) return
    const error = canEnterChat(activeChat, location, scenario)
    if (error) {
      setNotice(error)
      return
    }
    setChats((current) =>
      current.map((chat) =>
        chat.id === activeChat.id
          ? {
              ...chat,
              joined: true,
              participantCount: chat.participantCount + 1,
              participants: [...chat.participants, user.alias],
            }
          : chat,
      ),
    )
  }

  function leaveChat() {
    if (!activeChat || !user) return
    setChats((current) =>
      current.map((chat) =>
        chat.id === activeChat.id
          ? {
              ...chat,
              joined: false,
              participantCount: Math.max(0, chat.participantCount - 1),
              participants: chat.participants.filter(
                (participant) => participant !== user.alias,
              ),
            }
          : chat,
      ),
    )
    setScreen("nearby")
  }

  function createChat(title: string, topic: string) {
    if (!user) return
    const error = validateLocation(location)
    if (error) {
      setNotice(error)
      return
    }
    const createdAt = Date.now()
    const chat: Chat = {
      id: `chat-${createdAt}`,
      title,
      topic,
      createdAt,
      expiresAt: createdAt + CHAT_LIFETIME,
      distance: 0,
      participantCount: 1,
      participants: [user.alias],
      messages: [],
      joined: true,
      createdByMe: true,
    }
    setChats((current) => [chat, ...current])
    setActiveChatId(chat.id)
    setScreen("chat")
  }

  function sendMessage(text: string) {
    if (!activeChat || !user) return
    setChats((current) =>
      current.map((chat) =>
        chat.id === activeChat.id
          ? {
              ...chat,
              messages: [
                ...chat.messages,
                {
                  id: `message-${Date.now()}`,
                  author: user.alias,
                  text: text.slice(0, 1000),
                  sentAt: Date.now(),
                  own: true,
                },
              ],
            }
          : chat,
      ),
    )
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
      scenario={scenario}
      onScenarioChange={(next) => {
        setScenario(next)
        setNotice("")
      }}
      scenarioOpen={scenarioOpen}
      onScenarioToggle={() => setScenarioOpen((open) => !open)}
    >
      {screen === "nearby" && (
        <NearbyScreen
          chats={chats}
          alias={user.alias}
          location={location}
          scenario={scenario}
          notice={notice}
          onOpen={openChat}
          onCreate={() => navigate("create")}
          onRefresh={() => setLocationVersion((value) => value + 1)}
        />
      )}
      {screen === "create" && (
        <CreateScreen
          location={location}
          onCreate={createChat}
          onCancel={() => navigate("nearby")}
          onRefresh={() => setLocationVersion((value) => value + 1)}
        />
      )}
      {screen === "chat" && activeChat && (
        <ChatScreen
          chat={activeChat}
          scenario={scenario}
          location={location}
          alias={user.alias}
          onBack={() => navigate("nearby")}
          onJoin={joinChat}
          onLeave={leaveChat}
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
