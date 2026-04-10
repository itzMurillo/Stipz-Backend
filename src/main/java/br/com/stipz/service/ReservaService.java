package br.com.stipz.service;

import br.com.stipz.DTO.*;
import br.com.stipz.domain.*;
import br.com.stipz.enums.StatusReserva;
import br.com.stipz.exception.RegraNegocioException;
import br.com.stipz.exception.RecursoNaoEncontradoException;
import br.com.stipz.repository.*;
import org.springframework.stereotype.Service;

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

    public ReservaService(
            ReservaRepository reservaRepository,
            SalaRepository salaRepository,
            UsuarioRepository usuarioRepository,
            RecursoRepository recursoRepository,
            ReservaRecursoRepository reservaRecursoRepository
    ) {
        this.reservaRepository = reservaRepository;
        this.salaRepository = salaRepository;
        this.usuarioRepository = usuarioRepository;
        this.recursoRepository = recursoRepository;
        this.reservaRecursoRepository = reservaRecursoRepository;
    }

    public List<ReservaResponseDTO> listar() {
        return reservaRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public ReservaResponseDTO criarReservaCompleta(ReservaRequestDTO dto) {

        Usuario usuario = usuarioRepository.findById(dto.usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        Sala sala = salaRepository.findById(dto.salaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sala não encontrada"));

        if (dto.fim.isBefore(dto.inicio)) {
            throw new RegraNegocioException("Data fim não pode ser antes do início");
        }

        boolean conflito = reservaRepository
                .existsBySalaAndDataInicioLessThanAndDataFimGreaterThanAndStatusNot(
                        sala, dto.fim, dto.inicio, StatusReserva.CANCELADA
                );

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
            throw new RegraNegocioException("Limite de 5 reservas por semana atingido");
        }

        Reserva reserva = new Reserva();
        reserva.setUsuario(usuario);
        reserva.setSala(sala);
        reserva.setDataInicio(dto.inicio);
        reserva.setDataFim(dto.fim);
        reserva.setDataCriacao(LocalDateTime.now());
        reserva.setDataAtualizacao(LocalDateTime.now());
        reserva.setStatus(StatusReserva.PENDENTE);

        reserva = reservaRepository.save(reserva);

        if (reserva.getRecursos() == null) {
            reserva.setRecursos(new java.util.ArrayList<>());
        }

        if (dto.recursos != null && !dto.recursos.isEmpty()) {

            for (RecursoDTO r : dto.recursos) {

                if (r.recursoId == null) {
                    throw new RegraNegocioException("ID do recurso não informado");
                }

                if (r.quantidade == null || r.quantidade <= 0) {
                    throw new RegraNegocioException("Quantidade inválida");
                }

                Recurso recurso = recursoRepository.findById(r.recursoId)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Recurso não encontrado"));

                TipoRecurso tipo = recurso.getTipoRecurso();

                if (!tipo.getPermitido() && !tipo.getExigeAprovacao()) {
                    throw new RegraNegocioException("Recurso não permitido: " + recurso.getNome());
                }

                if (tipo.getExigeAprovacao()) {
                    reserva.setStatus(StatusReserva.PENDENTE);
                }

                if (recurso.getFixo() && !recurso.getSala().getId().equals(sala.getId())) {
                    throw new RegraNegocioException("Recurso fixo não pertence a essa sala");
                }

                List<ReservaRecurso> reservas = reservaRecursoRepository.findByRecurso(recurso);

                int totalReservado = reservas.stream()
                        .filter(rr ->
                                rr.getReserva().getStatus() != StatusReserva.CANCELADA &&
                                        rr.getReserva().getDataInicio().isBefore(dto.fim) &&
                                        rr.getReserva().getDataFim().isAfter(dto.inicio)
                        )
                        .mapToInt(ReservaRecurso::getQuantidade)
                        .sum();

                if (totalReservado + r.quantidade > recurso.getQuantidade()) {
                    throw new RegraNegocioException("Recurso indisponível: " + recurso.getNome());
                }

                ReservaRecurso rr = new ReservaRecurso();
                rr.setReserva(reserva);
                rr.setRecurso(recurso);
                rr.setQuantidade(r.quantidade);

                rr = reservaRecursoRepository.save(rr);

                reserva.getRecursos().add(rr);
            }
        }

        return toDTO(reserva);
    }

    public Reserva aprovar(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada"));

        if (reserva.getStatus() != StatusReserva.PENDENTE) {
            throw new RegraNegocioException("Reserva já foi processada");
        }

        reserva.setStatus(StatusReserva.APROVADA);
        reserva.setDataAtualizacao(LocalDateTime.now());

        return reservaRepository.save(reserva);
    }

    public Reserva rejeitar(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada"));

        if (reserva.getStatus() != StatusReserva.PENDENTE) {
            throw new RegraNegocioException("Reserva já foi processada");
        }

        reserva.setStatus(StatusReserva.REJEITADA);
        reserva.setDataAtualizacao(LocalDateTime.now());

        return reservaRepository.save(reserva);
    }

    public Reserva cancelar(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada"));

        if (reserva.getStatus() == StatusReserva.CANCELADA) {
            throw new RegraNegocioException("Reserva já está cancelada");
        }

        LocalDateTime agora = LocalDateTime.now();

        if (reserva.getDataInicio().minusHours(3).isBefore(agora)) {
            throw new RegraNegocioException("Não é possível cancelar com menos de 3 horas");
        }

        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setDataAtualizacao(LocalDateTime.now());

        return reservaRepository.save(reserva);
    }

    private ReservaResponseDTO toDTO(Reserva reserva) {
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

        dto.recursos = (reserva.getRecursos() == null ? List.of() :
                reserva.getRecursos().stream().map(rr -> {
                    RecursoResumoDTO r = new RecursoResumoDTO();
                    r.nome = rr.getRecurso().getNome();
                    r.quantidade = rr.getQuantidade();
                    return r;
                }).toList());

        return dto;
    }
}