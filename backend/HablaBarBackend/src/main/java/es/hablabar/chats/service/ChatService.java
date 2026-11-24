package es.hablabar.chats.service;

import es.hablabar.chats.dto.ChatCercanoResponse;
import es.hablabar.chats.dto.ChatResponse;
import es.hablabar.chats.dto.CrearChatRequest;
import es.hablabar.chats.dto.ParticipanteResponse;
import es.hablabar.chats.entity.Chat;
import es.hablabar.chats.exception.ChatException;
import es.hablabar.chats.repository.ChatRepository;
import es.hablabar.location.model.UbicacionReciente;
import es.hablabar.location.service.DistanciaService;
import es.hablabar.location.service.UbicacionService;
import es.hablabar.participants.entity.Participacion;
import es.hablabar.participants.service.ParticipacionService;
import es.hablabar.users.service.UsuarioService;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de los chats: crear, descubrir, entrar, salir y consultar
 * (RF-04, RF-05, RF-07, RF-11 a RF-13). Aplica las reglas RN-01 a RN-06 y RN-09.
 */
@Service
public class ChatService {

    /** RN-02: la distancia al punto de origen debe ser estrictamente inferior a este valor. */
    public static final double RADIO_METROS = 100.0;

    /**
     * Margen del filtro previo por coordenadas, en grados de latitud (unos 165 m).
     * Es mayor que el radio para no descartar ningún chat válido antes del cálculo exacto.
     */
    private static final double MARGEN_GRADOS = 0.0015;

    private final ChatRepository chatRepository;
    private final ParticipacionService participacionService;
    private final UbicacionService ubicacionService;
    private final DistanciaService distanciaService;
    private final UsuarioService usuarioService;
    private final Clock clock;

    public ChatService(
            ChatRepository chatRepository,
            ParticipacionService participacionService,
            UbicacionService ubicacionService,
            DistanciaService distanciaService,
            UsuarioService usuarioService,
            Clock clock) {
        this.chatRepository = chatRepository;
        this.participacionService = participacionService;
        this.ubicacionService = ubicacionService;
        this.distanciaService = distanciaService;
        this.usuarioService = usuarioService;
        this.clock = clock;
    }

    /**
     * RF-04: crea un chat en la ubicación actual del usuario (RN-03, RN-04) con
     * caducidad a una hora (RN-06). El creador queda incorporado como participante.
     */
    @Transactional
    public ChatResponse crear(UUID usuarioId, CrearChatRequest request) {
        UbicacionReciente ubicacion = ubicacionService.exigirValida(usuarioId);
        Instant ahora = clock.instant();

        Chat chat = chatRepository.saveAndFlush(new Chat(
                usuarioId,
                request.nombre(),
                request.tema(),
                ubicacion.latitud(),
                ubicacion.longitud(),
                ahora));
        Participacion participacion =
                participacionService.registrarEntrada(usuarioId, chat.getId(), chat.getFechaCreacion());

        return respuesta(chat, usuarioId, 1, participacion);
    }

    /**
     * RF-05: chats activos a menos de 100 m (RN-02) con plazas libres (RN-01).
     * Los chats en los que el usuario ya participa se muestran aunque estén llenos.
     * Se ordenan del más cercano al más lejano.
     */
    @Transactional(readOnly = true)
    public List<ChatCercanoResponse> cercanos(UUID usuarioId) {
        UbicacionReciente ubicacion = ubicacionService.exigirValida(usuarioId);
        Instant ahora = clock.instant();

        double lat = ubicacion.latitud();
        double lon = ubicacion.longitud();
        double margenLongitud = MARGEN_GRADOS / Math.max(Math.cos(Math.toRadians(lat)), 0.01);
        List<Chat> candidatos = chatRepository.buscarActivosEnZona(
                ahora, lat - MARGEN_GRADOS, lat + MARGEN_GRADOS, lon - margenLongitud, lon + margenLongitud);

        record Cercano(Chat chat, double distancia, long participantes, boolean participando) {
        }

        return candidatos.stream()
                .map(chat -> {
                    double distancia = distanciaService.metros(lat, lon, chat.getLatitudOrigen(), chat.getLongitudOrigen());
                    if (distancia >= RADIO_METROS) {
                        return null;
                    }
                    long participantes = participacionService.contarActivas(chat.getId());
                    boolean participando = participacionService.participa(usuarioId, chat.getId());
                    if (participantes >= chat.getCapacidadMaxima() && !participando) {
                        return null;
                    }
                    return new Cercano(chat, distancia, participantes, participando);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(Cercano::distancia))
                .map(c -> new ChatCercanoResponse(
                        c.chat().getId(),
                        c.chat().getNombre(),
                        c.chat().getTema(),
                        (int) c.participantes(),
                        c.chat().getCapacidadMaxima(),
                        c.chat().getFechaCaducidad(),
                        distanciaAproximada(c.distancia()),
                        c.participando()))
                .toList();
    }

    /**
     * RF-07: entra en un chat. El servidor comprueba que el chat está activo, que la
     * ubicación es válida, que está a menos de 100 m y que hay plazas, aunque el
     * usuario haya llegado desde un aviso. Si ya participa, devuelve su participación
     * actual sin crear otra (RN-05).
     */
    @Transactional
    public ChatResponse entrar(UUID usuarioId, UUID chatId) {
        Chat chat = chatRepository.bloquearPorId(chatId).orElseThrow(ChatException::noEncontrado);
        Instant ahora = clock.instant();
        if (!chat.estaActivoEn(ahora)) {
            throw ChatException.finalizado();
        }

        Optional<Participacion> actual = participacionService.activa(usuarioId, chatId);
        if (actual.isPresent()) {
            return respuesta(chat, usuarioId, participacionService.contarActivas(chatId), actual.get());
        }

        UbicacionReciente ubicacion = ubicacionService.exigirValida(usuarioId);
        double distancia = distanciaService.metros(
                ubicacion.latitud(), ubicacion.longitud(), chat.getLatitudOrigen(), chat.getLongitudOrigen());
        if (distancia >= RADIO_METROS) {
            throw ChatException.fueraDeRadio();
        }

        long participantes = participacionService.contarActivas(chatId);
        if (participantes >= chat.getCapacidadMaxima()) {
            throw ChatException.completo();
        }

        Participacion participacion = participacionService.registrarEntrada(usuarioId, chatId, ahora);
        return respuesta(chat, usuarioId, participantes + 1, participacion);
    }

    /**
     * RF-11: sale del chat. La salida del creador no cierra el chat ni expulsa a
     * los demás (RN-09). Volver a entrar exige de nuevo proximidad (RF-12).
     */
    @Transactional
    public void salir(UUID usuarioId, UUID chatId) {
        if (!chatRepository.existsById(chatId)) {
            throw ChatException.noEncontrado();
        }
        participacionService.registrarSalida(usuarioId, chatId, clock.instant());
    }

    /**
     * Detalle de un chat para un participante. No exige proximidad: alejarse no
     * revoca una participación vigente (RN-05). Un chat caducado deja de ser
     * accesible (RF-13).
     */
    @Transactional(readOnly = true)
    public ChatResponse detalle(UUID usuarioId, UUID chatId) {
        Acceso acceso = exigirAcceso(usuarioId, chatId);
        return respuesta(acceso.chat(), usuarioId,
                participacionService.contarActivas(chatId), acceso.participacion());
    }

    /** RF-09: alias de los participantes activos, sin correos ni otros datos privados. */
    @Transactional(readOnly = true)
    public List<ParticipanteResponse> participantes(UUID usuarioId, UUID chatId) {
        Chat chat = exigirAcceso(usuarioId, chatId).chat();
        List<UUID> ids = participacionService.participantesActivos(chatId);
        Map<UUID, String> alias = usuarioService.aliasPorId(ids);
        return ids.stream()
                .map(id -> new ParticipanteResponse(id, alias.get(id), chat.esCreador(id)))
                .toList();
    }

    /**
     * RF-14: chats activos en los que el usuario sigue participando, para recuperar
     * el estado tras recargar la aplicación o perder la conexión.
     */
    @Transactional(readOnly = true)
    public List<ChatResponse> participando(UUID usuarioId) {
        Instant ahora = clock.instant();
        Map<UUID, Participacion> porChat = participacionService.activasDelUsuario(usuarioId).stream()
                .collect(Collectors.toMap(Participacion::getChatId, p -> p));
        return chatRepository.findAllById(porChat.keySet()).stream()
                .filter(chat -> chat.estaActivoEn(ahora))
                .sorted(Comparator.comparing(Chat::getFechaCaducidad))
                .map(chat -> respuesta(chat, usuarioId,
                        participacionService.contarActivas(chat.getId()), porChat.get(chat.getId())))
                .toList();
    }

    /**
     * Comprueba que el chat existe, sigue activo y el usuario tiene una participación
     * activa en él, y devuelve esa participación. Lo usa el módulo de mensajes para
     * aplicar las mismas reglas de acceso que el detalle del chat (RF-11, RF-13, RNF-01).
     */
    @Transactional(readOnly = true)
    public Participacion exigirParticipacionActiva(UUID usuarioId, UUID chatId) {
        return exigirAcceso(usuarioId, chatId).participacion();
    }

    /**
     * Convierte la distancia exacta en un tramo: menos de 25, de 50 o de 100 metros.
     * Así el cliente puede orientar al usuario sin recibir la distancia exacta (RNF-03).
     */
    public static int distanciaAproximada(double metros) {
        if (metros < 25) {
            return 25;
        }
        if (metros < 50) {
            return 50;
        }
        return 100;
    }

    private record Acceso(Chat chat, Participacion participacion) {
    }

    private Acceso exigirAcceso(UUID usuarioId, UUID chatId) {
        Chat chat = chatRepository.findById(chatId).orElseThrow(ChatException::noEncontrado);
        if (!chat.estaActivoEn(clock.instant())) {
            throw ChatException.finalizado();
        }
        Participacion participacion = participacionService.activa(usuarioId, chatId)
                .orElseThrow(ChatException::noParticipa);
        return new Acceso(chat, participacion);
    }

    private static ChatResponse respuesta(Chat chat, UUID usuarioId, long participantes, Participacion participacion) {
        return new ChatResponse(
                chat.getId(),
                chat.getNombre(),
                chat.getTema(),
                (int) participantes,
                chat.getCapacidadMaxima(),
                chat.getFechaCreacion(),
                chat.getFechaCaducidad(),
                chat.esCreador(usuarioId),
                true,
                participacion.getFechaEntrada());
    }
}