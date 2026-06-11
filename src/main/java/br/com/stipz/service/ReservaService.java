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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Transactional(readOnly = true)
    public List<ReservaResponseDTO> listarMinhas(String emailUsuario) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        return reservaRepository.findByUsuarioComUsuarioESala(usuario)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReservaResponseDTO criarReservaCompleta(ReservaRequestDTO dto) {
        return criarReservaCompleta(dto, (Evento) null);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReservaResponseDTO criarReservaCompleta(ReservaRequestDTO dto, String emailUsuario) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        dto.usuarioId = usuario.getId();
        return criarReservaCompleta(dto, (Evento) null);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReservaResponseDTO criarReservaCompleta(ReservaRequestDTO dto, Evento evento) {

        Usuario usuario = usuarioRepository.findById(dto.usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        Sala sala = salaRepository.findByIdForUpdate(dto.salaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sala não encontrada"));

        validarCapacidadeSala(sala, dto);
        validarRecursosDuplicados(dto);
        validarCadeirasExtras(dto);

        if (!dto.fim.isAfter(dto.inicio)) {
            throw new RegraNegocioException("Data fim deve ser depois do início");
        }

        boolean conflito = !reservaRepository
                .findConflitosComBloqueio(
                        sala,
                        dto.fim,
                        dto.inicio,
                        List.of(StatusReserva.PENDENTE, StatusReserva.APROVADA)
                )
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
        reserva.setResponsavel(resolverResponsavel(dto, usuario));
        aplicarCadeirasExtras(reserva, dto);

        reserva = reservaRepository.save(reserva);

        if (dto.recursos != null) {

            for (RecursoDTO r : dto.recursos) {

                Recurso recurso = recursoRepository.findByIdForUpdate(r.recursoId)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Recurso não encontrado"));

                if (r.quantidade == null || r.quantidade <= 0) {
                    throw new RegraNegocioException("Quantidade do recurso deve ser maior que zero");
                }

                if (recurso.getQuantidade() == null || recurso.getQuantidade() <= 0) {
                    throw new RegraNegocioException("Recurso '" + recurso.getNome() + "' não possui quantidade disponível cadastrada");
                }

                if (r.quantidade > recurso.getQuantidade()) {
                    throw new RegraNegocioException(
                            "Recurso '" + recurso.getNome() + "' indisponível. Solicitado: "
                                    + r.quantidade + ", total cadastrado: " + recurso.getQuantidade()
                    );
                }

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
                        List.of(StatusReserva.PENDENTE, StatusReserva.APROVADA)
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
        dto.responsavel = reserva.getResponsavel();
        dto.motivoRejeicao = reserva.getMotivoRejeicao();
        dto.cadeirasExtras = Boolean.TRUE.equals(reserva.getCadeirasExtras());
        dto.quantidadeCadeiras = dto.cadeirasExtras
                ? valorOuZero(reserva.getQuantidadeCadeiras())
                : 0;

        if (reserva.getEvento() != null) {
            dto.eventoId = reserva.getEvento().getId();
            dto.evento = reserva.getEvento().getNome();
            dto.eventoDescricao = reserva.getEvento().getDescricao();
            dto.justificativa = reserva.getEvento().getJustificativa();
        }

        dto.usuario = new UsuarioResumoDTO();
        dto.usuario.id = reserva.getUsuario().getId();
        dto.usuario.nome = reserva.getUsuario().getNome();

        dto.sala = new SalaResumoDTO();
        dto.sala.id = reserva.getSala().getId();
        dto.sala.nome = reserva.getSala().getNome();

        List<ReservaRecurso> lista = reservaRecursoRepository.findByReservaComRecurso(reserva);

        dto.recursos = lista.stream().map(rr -> {
            RecursoResumoDTO r = new RecursoResumoDTO();
            r.id = rr.getRecurso().getId();
            r.nome = rr.getRecurso().getNome();
            r.quantidade = rr.getQuantidade();
            return r;
        }).toList();

        return dto;
    }

    @Transactional
    public ReservaResponseDTO aprovar(Long id) {

        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada"));

        if (reserva.getStatus() != StatusReserva.PENDENTE) {
            throw new RegraNegocioException("Reserva já foi processada");
        }

        reserva.setStatus(StatusReserva.APROVADA);
        reserva.setDataAtualizacao(LocalDateTime.now());

        Reserva reservaSalva = reservaRepository.save(reserva);
        notificacaoService.avisarAlteracao("RESERVA_APROVADA");
        return toDTO(reservaSalva);
    }
    @Transactional
    public ReservaResponseDTO rejeitar(Long id) {
        return rejeitar(id, null);
    }

    @Transactional
    public ReservaResponseDTO rejeitar(Long id, String motivo) {

        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada"));

        if (reserva.getStatus() != StatusReserva.PENDENTE) {
            throw new RegraNegocioException("Reserva já foi processada");
        }

        reserva.setStatus(StatusReserva.REJEITADA);
        reserva.setMotivoRejeicao(motivo);
        reserva.setDataAtualizacao(LocalDateTime.now());

        Reserva reservaSalva = reservaRepository.save(reserva);
        notificacaoService.avisarAlteracao("RESERVA_REJEITADA");
        return toDTO(reservaSalva);
    }

    @Transactional
    public ReservaResponseDTO cancelar(Long id, String emailUsuario) {

        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada"));

        if (!reserva.getUsuario().getEmail().equalsIgnoreCase(emailUsuario)) {
            throw new RegraNegocioException("Você só pode cancelar suas próprias reservas");
        }

        if (reserva.getStatus() != StatusReserva.PENDENTE) {
            throw new RegraNegocioException("Somente reservas pendentes podem ser canceladas");
        }

        if (reserva.getDataInicio().minusHours(3).isBefore(LocalDateTime.now())) {
            throw new RegraNegocioException("Não é possível cancelar com menos de 3 horas");
        }

        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setDataAtualizacao(LocalDateTime.now());

        Reserva reservaSalva = reservaRepository.save(reserva);
        notificacaoService.avisarAlteracao("RESERVA_CANCELADA");
        return toDTO(reservaSalva);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReservaResponseDTO substituirReservaRejeitada(
            Long id,
            ReservaRequestDTO dto,
            String emailUsuario
    ) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada"));

        if (!reserva.getUsuario().getEmail().equalsIgnoreCase(emailUsuario)) {
            throw new RegraNegocioException("Você só pode editar suas próprias reservas");
        }

        if (reserva.getEvento() == null) {
            throw new RegraNegocioException("Somente reservas de evento podem ter a sala substituída");
        }

        if (reserva.getStatus() != StatusReserva.REJEITADA) {
            throw new RegraNegocioException("Somente uma sala rejeitada do evento pode ser substituída");
        }

        Sala sala = salaRepository.findByIdForUpdate(dto.salaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sala não encontrada"));

        validarCapacidadeSala(sala, dto);
        validarRecursosDuplicados(dto);
        validarCadeirasExtras(dto);

        if (!dto.fim.isAfter(dto.inicio)) {
            throw new RegraNegocioException("Data fim deve ser depois do início");
        }

        boolean conflito = !reservaRepository
                .findConflitosComBloqueio(
                        sala,
                        dto.fim,
                        dto.inicio,
                        List.of(StatusReserva.PENDENTE, StatusReserva.APROVADA)
                )
                .isEmpty();

        if (conflito) {
            throw new RegraNegocioException("Conflito de horário");
        }

        reservaRecursoRepository.deleteByReserva(reserva);
        reservaRecursoRepository.flush();

        reserva.setSala(sala);
        reserva.setDataInicio(dto.inicio);
        reserva.setDataFim(dto.fim);
        reserva.setResponsavel(resolverResponsavel(dto, reserva.getUsuario()));
        aplicarCadeirasExtras(reserva, dto);
        reserva.setMotivoRejeicao(null);
        reserva.setStatus(StatusReserva.PENDENTE);
        reserva.setDataAtualizacao(LocalDateTime.now());
        reserva = reservaRepository.save(reserva);

        salvarRecursosDaReserva(reserva, sala, dto);

        ReservaResponseDTO response = toDTO(reserva);
        notificacaoService.avisarAlteracao("RESERVA_EVENTO_SUBSTITUIDA");
        return response;
    }

    private void salvarRecursosDaReserva(Reserva reserva, Sala sala, ReservaRequestDTO dto) {
        if (dto.recursos == null) {
            return;
        }

        for (RecursoDTO recursoSolicitado : dto.recursos) {
            Recurso recurso = recursoRepository.findByIdForUpdate(recursoSolicitado.recursoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Recurso não encontrado"));

            if (recursoSolicitado.quantidade == null || recursoSolicitado.quantidade <= 0) {
                throw new RegraNegocioException("Quantidade do recurso deve ser maior que zero");
            }

            if (recurso.getQuantidade() == null || recurso.getQuantidade() <= 0) {
                throw new RegraNegocioException(
                        "Recurso '" + recurso.getNome() + "' não possui quantidade disponível cadastrada"
                );
            }

            if (recurso.getFixo() && !recurso.getSala().getId().equals(sala.getId())) {
                throw new RegraNegocioException(
                        "Recurso fixo '" + recurso.getNome() + "' pertence à sala "
                                + recurso.getSala().getNome()
                );
            }

            int totalReservado = reservaRecursoRepository.somarQuantidadeReservadaNoPeriodo(
                    recurso,
                    dto.inicio,
                    dto.fim,
                    List.of(StatusReserva.PENDENTE, StatusReserva.APROVADA)
            );

            if (totalReservado + recursoSolicitado.quantidade > recurso.getQuantidade()) {
                int disponivel = recurso.getQuantidade() - totalReservado;
                throw new RegraNegocioException(
                        "Recurso '" + recurso.getNome() + "' indisponível. Disponível: "
                                + Math.max(disponivel, 0) + ", solicitado: " + recursoSolicitado.quantidade
                );
            }

            ReservaRecurso reservaRecurso = new ReservaRecurso();
            reservaRecurso.setReserva(reserva);
            reservaRecurso.setRecurso(recurso);
            reservaRecurso.setQuantidade(recursoSolicitado.quantidade);
            reservaRecursoRepository.save(reservaRecurso);
        }
    }

    private String resolverResponsavel(ReservaRequestDTO dto, Usuario usuario) {
        if (dto.responsavel != null && !dto.responsavel.isBlank()) {
            return dto.responsavel.trim();
        }

        if (dto.nomeResponsavel != null && !dto.nomeResponsavel.isBlank()) {
            return dto.nomeResponsavel.trim();
        }

        return usuario.getNome();
    }

    private void validarCapacidadeSala(Sala sala, ReservaRequestDTO dto) {
        Integer quantidade = resolverQuantidadeSolicitada(dto);

        if (quantidade == null || sala.getCapacidade() == null) {
            return;
        }

        if (quantidade > sala.getCapacidade()) {
            throw new RegraNegocioException(
                    "Capacidade da sala excedida. Sala " + sala.getNome()
                            + " comporta " + sala.getCapacidade()
                            + " pessoas, solicitado " + quantidade + "."
            );
        }
    }

    private Integer resolverQuantidadeSolicitada(ReservaRequestDTO dto) {
        if (dto.quantidadeParticipantes != null) {
            return dto.quantidadeParticipantes;
        }

        if (dto.participantes != null) {
            return dto.participantes;
        }

        return dto.capacidadeSolicitada;
    }

    private void aplicarCadeirasExtras(Reserva reserva, ReservaRequestDTO dto) {
        boolean possuiCadeirasExtras = Boolean.TRUE.equals(dto.cadeirasExtras);
        reserva.setCadeirasExtras(possuiCadeirasExtras);
        reserva.setQuantidadeCadeiras(
                possuiCadeirasExtras ? valorOuZero(dto.quantidadeCadeiras) : 0
        );
    }

    private void validarCadeirasExtras(ReservaRequestDTO dto) {
        if (Boolean.TRUE.equals(dto.cadeirasExtras)
                && (dto.quantidadeCadeiras == null || dto.quantidadeCadeiras <= 0)) {
            throw new RegraNegocioException(
                    "Quantidade de cadeiras deve ser maior que zero quando cadeiras extras forem solicitadas"
            );
        }
    }

    private int valorOuZero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private void validarRecursosDuplicados(ReservaRequestDTO dto) {
        if (dto.recursos == null) {
            return;
        }

        Set<Long> recursos = new HashSet<>();

        for (RecursoDTO recurso : dto.recursos) {
            if (recurso.recursoId != null && !recursos.add(recurso.recursoId)) {
                throw new RegraNegocioException(
                        "O recurso #" + recurso.recursoId + " foi informado mais de uma vez"
                );
            }
        }
    }
}
