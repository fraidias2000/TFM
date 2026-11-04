import type { Chat, LocationSnapshot, Scenario } from "../types"

const now = Date.now()

export const initialChats: Chat[] = [
  {
    id: "cafeteria",
    title: "Café de la esquina",
    topic: "Plan espontáneo",
    createdAt: now - 12 * 60_000,
    expiresAt: now + 48 * 60_000,
    distance: 50,
    participantCount: 8,
    participants: ["Luna", "Nico", "Vera", "Dani", "Sol", "Leo", "Mar", "Álex"],
    messages: [
      {
        id: "m1",
        author: "Luna",
        text: "¿Alguien se apunta a un café rápido?",
        sentAt: now - 7 * 60_000,
      },
      {
        id: "m2",
        author: "Nico",
        text: "Estoy en la mesa grande junto a la ventana.",
        sentAt: now - 5 * 60_000,
      },
      {
        id: "m3",
        author: "Vera",
        text: "Voy para allá, llego en cinco.",
        sentAt: now - 3 * 60_000,
      },
    ],
  },
  {
    id: "parque",
    title: "Paseo con perros",
    topic: "Barrio",
    createdAt: now - 27 * 60_000,
    expiresAt: now + 33 * 60_000,
    distance: 76,
    participantCount: 4,
    participants: ["Mara", "Teo", "Iris", "Noa"],
    messages: [
      {
        id: "p1",
        author: "Mara",
        text: "Estamos cerca de la fuente norte.",
        sentAt: now - 4 * 60_000,
      },
    ],
  },
  {
    id: "musica",
    title: "Músicos en la plaza",
    topic: "Cultura",
    createdAt: now - 41 * 60_000,
    expiresAt: now + 19 * 60_000,
    distance: 92,
    participantCount: 12,
    participants: ["Gael", "Rocío", "Sam", "Paz", "Elia", "Max"],
    messages: [],
  },
]

export const scenarioOptions: { id: Scenario, label: string, short: string }[] = [
  { id: "50m", label: "A 50 metros", short: "50 m" },
  { id: "99m", label: "A 99 metros", short: "99 m" },
  { id: "100m", label: "A 100 metros", short: "100 m" },
  { id: "120m", label: "A 120 metros", short: "120 m" },
  { id: "invalid", label: "Ubicación inválida", short: "Sin señal" },
  { id: "full", label: "Chat completo", short: "Lleno" },
  { id: "expired", label: "Chat caducado", short: "Caducado" },
]

export function locationForScenario(scenario: Scenario): LocationSnapshot {
  const distances: Record<Scenario, number> = {
    "50m": 50,
    "99m": 99,
    "100m": 100,
    "120m": 120,
    invalid: 50,
    full: 50,
    expired: 50,
  }

  return {
    distance: distances[scenario],
    accuracy: scenario === "invalid" ? 35 : 12,
    updatedAt: scenario === "invalid" ? Date.now() - 46_000 : Date.now(),
  }
}
