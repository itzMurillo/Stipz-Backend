package br.com.stipz.service;

import br.com.stipz.DTO.*;
import br.com.stipz.domain.*;
import br.com.stipz.enums.StatusReserva;
import br.com.stipz.exception.RegraNegocioException;
import br.com.stipz.exception.RecursoNaoEncontradoException;
import br.com.stipz.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final SalaRepository salaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RecursoRepository recursoRepository;
    private final ReservaRecursoRepository reservaRecursoRepository;
    private final NotificacaoService notificacaoService;

    public ReservaService(
            ReservaRepository reservaRepository,
            SalaRepository salaRepository,
            UsuarioRepository usuarioRepository,
            RecursoRepository recursoRepository,
            ReservaRecursoRepository reservaRecursoRepository,
            NotificacaoService notificacaoService
    ) {
        this.reservaRepository = reservaRepository;
        this.salaRepository = salaRepository;
        this.usuarioRepository = usuarioRepository;
        this.recursoRepository = recursoRepository;
        this.reservaRecursoRepository = reservaRecursoRepository;
        this.notificacaoService = notificacaoService;
    }

    @Transactional(readOnly = true)
    public List<ReservaResponseDTO> listar() {
        return reservaRepository.findAllComUsuarioESala()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReservaResponseDTO criarReservaCompleta(ReservaRequestDTO dto) {
        return criarReservaCompleta(dto, null);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReservaResponseDTO criarReservaCompleta(ReservaRequestDTO dto, Evento evento) {

        Usuario usuario = usuarioRepository.findById(dto.usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        Sala sala = salaRepository.findByIdForUpdate(dto.salaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sala não encontrada"));

        if (!dto.fim.isAfter(dto.inicio)) {
            throw new RegraNegocioException("Data fim deve ser depois do início");
        }

        boolean conflito = !reservaRepository
                .findConflitosComBloqueio(sala, dto.fim, dto.inicio, StatusReserva.CANCELADA)
                .isEmpty();

        if (conflito) {
            throw new RegraNegocioException("Conflito de horário");
        }

        LocalDateTime inicioSemana = dto.inicio
                .with(DayOfWeek.MONDAY)
                .withHour(0).withMinute(0).withSecond(0);

        LocalDateTime fimSemana = inicioSemana
                .plusDays(6)
                .withHour(23).withMinute(59).withSecond(59);

        long total = reservaRepository.countByUsuarioAndDataInicioBetween(
                usuario, inicioSemana, fimSemana
        );

        if (total >= 5) {
            throw new RegraNegocioException("Limite semanal atingido");
        }

        Reserva reserva = new Reserva();
        reserva.setUsuario(usuario);
        reserva.setSala(sala);
        reserva.setEvento(evento);
        reserva.setDataInicio(dto.inicio);
        reserva.setDataFim(dto.fim);
        reserva.setDataCriacao(LocalDateTime.now());
        reserva.setDataAtualizacao(LocalDateTime.now());
        reserva.setStatus(StatusReserva.PENDENTE);

        reserva = reservaRepository.save(reserva);

        if (dto.recursos != null) {

            for (RecursoDTO r : dto.recursos) {

                Recurso recurso = recursoRepository.findByIdForUpdate(r.recursoId)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Recurso não encontrado"));

                if (recurso.getFixo() && !recurso.getSala().getId().equals(sala.getId())) {
                    throw new RegraNegocioException(
                            "Recurso fixo '" + recurso.getNome() + "' pertence à sala "
                                    + recurso.getSala().getId() + " e não pode ser reservado na sala " + sala.getId()
                    );
                }

                int totalReservado = reservaRecursoRepository.somarQuantidadeReservadaNoPeriodo(
                        recurso,
                        dto.inicio,
                        dto.fim,
                        StatusReserva.CANCELADA
                );

                if (totalReservado + r.quantidade > recurso.getQuantidade()) {
                    int disponivel = recurso.getQuantidade() - totalReservado;
                    throw new RegraNegocioException(
                            "Recurso '" + recurso.getNome() + "' indisponível para a sala "
                                    + sala.getId() + ". Solicitado: " + r.quantidade
                                    + ", disponível: " + Math.max(disponivel, 0)
                    );
                }

                ReservaRecurso rr = new ReservaRecurso();
                rr.setReserva(reserva);
                rr.setRecurso(recurso);
                rr.setQuantidade(r.quantidade);

                reservaRecursoRepository.save(rr);
            }
        }

        ReservaResponseDTO response = toDTO(reserva);
        notificacaoService.avisarAlteracao("RESERVA_CRIADA");
        return response;
    }

    public ReservaResponseDTO toDTO(Reserva reserva) {

        ReservaResponseDTO dto = new ReservaResponseDTO();

        dto.id = reserva.getId();
        dto.dataInicio = reserva.getDataInicio();
        dto.dataFim = reserva.getDataFim();
        dto.status = reserva.getStatus().name();

        dto.usuario = new UsuarioResumoDTO();
        dto.usuario.id = reserva.getUsuario().getId();
        dto.usuario.nome = reserva.getUsuario().getNome();

        dto.sala = new SalaResumoDTO();
        dto.sala.id = reserva.getSala().getId();
        dto.sala.nome = reserva.getSala().getNome();

        List<ReservaRecurso> lista = reservaRecursoRepository.findByReservaComRecurso(reserva);

        dto.recursos = lista.stream().map(rr -> {
            RecursoResumoDTO r = new RecursoResumoDTO();
            r.nome = rr.getRecurso().getNome();
            r.quantidade = rr.getQuantidade();
            return r;
        }).toList();

        return dto;
    }

    @Transactional
    public Reserva aprovar(Long id) {

        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada"));

        if (reserva.getStatus() != StatusReserva.PENDENTE) {
            throw new RegraNegocioException("Reserva já foi processada");
        }

        reserva.setStatus(StatusReserva.APROVADA);
        reserva.setDataAtualizacao(LocalDateTime.now());

        Reserva reservaSalva = reservaRepository.save(reserva);
        notificacaoService.avisarAlteracao("RESERVA_APROVADA");
        return reservaSalva;
    }
    @Transactional
    public Reserva rejeitar(Long id) {

        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada"));

        if (reserva.getStatus() != StatusReserva.PENDENTE) {
            throw new RegraNegocioException("Reserva já foi processada");
        }

        reserva.setStatus(StatusReserva.REJEITADA);
        reserva.setDataAtualizacao(LocalDateTime.now());

        Reserva reservaSalva = reservaRepository.save(reserva);
        notificacaoService.avisarAlteracao("RESERVA_REJEITADA");
        return reservaSalva;
    }

    @Transactional
    public Reserva cancelar(Long id) {

        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada"));

        if (reserva.getStatus() == StatusReserva.CANCELADA) {
            throw new RegraNegocioException("Reserva já está cancelada");
        }

        if (reserva.getDataInicio().minusHours(3).isBefore(LocalDateTime.now())) {
            throw new RegraNegocioException("Não é possível cancelar com menos de 3 horas");
        }

        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setDataAtualizacao(LocalDateTime.now());

        Reserva reservaSalva = reservaRepository.save(reserva);
        notificacaoService.avisarAlteracao("RESERVA_CANCELADA");
        return reservaSalva;
    }
}
