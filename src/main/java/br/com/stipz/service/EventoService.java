package br.com.stipz.service;

import br.com.stipz.DTO.EventoRequestDTO;
import br.com.stipz.DTO.EventoResponseDTO;
import br.com.stipz.DTO.ReservaRequestDTO;
import br.com.stipz.DTO.ReservaResponseDTO;
import br.com.stipz.DTO.UsuarioResumoDTO;
import br.com.stipz.domain.Evento;
import br.com.stipz.domain.Reserva;
import br.com.stipz.domain.Usuario;
import br.com.stipz.exception.RegraNegocioException;
import br.com.stipz.repository.EventoRepository;
import br.com.stipz.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final ReservaRepository reservaRepository;
    private final ReservaService reservaService;
    private final UsuarioService usuarioService;
    private final NotificacaoService notificacaoService;

    public EventoService(
            EventoRepository eventoRepository,
            ReservaRepository reservaRepository,
            ReservaService reservaService,
            UsuarioService usuarioService,
            NotificacaoService notificacaoService
    ) {
        this.eventoRepository = eventoRepository;
        this.reservaRepository = reservaRepository;
        this.reservaService = reservaService;
        this.usuarioService = usuarioService;
        this.notificacaoService = notificacaoService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public EventoResponseDTO criar(EventoRequestDTO dto, String emailUsuario) {
        Usuario usuario = usuarioService.buscarPorEmail(emailUsuario);

        if (!dto.fim.isAfter(dto.inicio)) {
            throw new RegraNegocioException("Data fim deve ser depois do início");
        }

        validarSalasDuplicadas(dto);
        validarJustificativa(dto);

        Evento evento = new Evento();
        evento.setUsuario(usuario);
        evento.setNome(dto.nome);
        evento.setDescricao(dto.descricao);
        evento.setJustificativa(dto.justificativa);
        evento.setDataInicio(dto.inicio);
        evento.setDataFim(dto.fim);
        evento.setDataCriacao(LocalDateTime.now());

        evento = eventoRepository.save(evento);

        for (var sala : dto.salas) {
            ReservaRequestDTO reserva = new ReservaRequestDTO();
            reserva.usuarioId = usuario.getId();
            reserva.salaId = sala.salaId;
            reserva.inicio = sala.inicio != null ? sala.inicio : dto.inicio;
            reserva.fim = sala.fim != null ? sala.fim : dto.fim;
            reserva.recursos = sala.recursos;
            reserva.quantidadeCadeiras = sala.quantidadeCadeiras;
            reserva.cadeirasExtras = sala.cadeirasExtras;
            reserva.quantidadeParticipantes = sala.quantidadeParticipantes;
            reserva.participantes = sala.participantes;
            reserva.capacidadeSolicitada = sala.capacidadeSolicitada;
            reserva.responsavel = sala.responsavel;
            reserva.nomeResponsavel = sala.nomeResponsavel;

            reservaService.criarReservaCompleta(reserva, evento);
        }

        EventoResponseDTO response = toDTO(evento);
        notificacaoService.avisarAlteracao("EVENTO_CRIADO");
        return response;
    }

    @Transactional(readOnly = true)
    public List<EventoResponseDTO> listar() {
        return eventoRepository.findAllComUsuario()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private void validarSalasDuplicadas(EventoRequestDTO dto) {
        Set<Long> salas = new HashSet<>();

        for (var sala : dto.salas) {
            if (!salas.add(sala.salaId)) {
                throw new RegraNegocioException("Sala duplicada no evento");
            }
        }
    }

    private void validarJustificativa(EventoRequestDTO dto) {
        if (dto.salas.size() > 1
                && (dto.justificativa == null || dto.justificativa.isBlank())) {
            throw new RegraNegocioException(
                    "Justificativa é obrigatória para eventos com múltiplas salas"
            );
        }
    }

    private EventoResponseDTO toDTO(Evento evento) {
        EventoResponseDTO dto = new EventoResponseDTO();
        dto.id = evento.getId();
        dto.nome = evento.getNome();
        dto.descricao = evento.getDescricao();
        dto.justificativa = evento.getJustificativa();
        dto.inicio = evento.getDataInicio();
        dto.fim = evento.getDataFim();
        dto.dataCriacao = evento.getDataCriacao();

        dto.usuario = new UsuarioResumoDTO();
        dto.usuario.id = evento.getUsuario().getId();
        dto.usuario.nome = evento.getUsuario().getNome();

        List<Reserva> reservas = reservaRepository.findByEventoComUsuarioESala(evento);
        dto.reservas = reservas.stream()
                .map(reservaService::toDTO)
                .toList();

        return dto;
    }
}
