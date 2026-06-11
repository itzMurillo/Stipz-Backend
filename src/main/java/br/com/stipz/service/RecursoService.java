package br.com.stipz.service;

import br.com.stipz.DTO.RecursoDisponibilidadeDTO;
import br.com.stipz.DTO.SalaResumoDTO;
import br.com.stipz.domain.*;
import br.com.stipz.enums.CategoriaRecurso;
import br.com.stipz.enums.StatusReserva;
import br.com.stipz.exception.RegraNegocioException;
import br.com.stipz.exception.RecursoNaoEncontradoException;
import br.com.stipz.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecursoService {

    private final RecursoRepository recursoRepository;
    private final ReservaRecursoRepository reservaRecursoRepository;
    private final TipoRecursoService tipoRecursoService;
    private final SalaRepository salaRepository;
    private final NotificacaoService notificacaoService;

    public RecursoService(
            RecursoRepository recursoRepository,
            ReservaRecursoRepository reservaRecursoRepository,
            TipoRecursoService tipoRecursoService,
            SalaRepository salaRepository,
            NotificacaoService notificacaoService
    ) {
        this.recursoRepository = recursoRepository;
        this.reservaRecursoRepository = reservaRecursoRepository;
        this.tipoRecursoService = tipoRecursoService;
        this.salaRepository = salaRepository;
        this.notificacaoService = notificacaoService;
    }

    public Recurso criar(
            String nome,
            String descricao,
            CategoriaRecurso categoria,
            Integer quantidade,
            Long salaId,
            Boolean fixo
    ) {

        if (fixo == null) {
            throw new RegraNegocioException("Campo 'fixo' é obrigatório");
        }

        if (salaId == null) {
            throw new RegraNegocioException("Recurso deve estar vinculado a uma sala");
        }

        Sala sala = salaRepository.findById(salaId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Sala não encontrada"));

        TipoRecurso tipoRecurso = tipoRecursoService.buscarOuCriar(descricao, categoria);

        if (Boolean.TRUE.equals(fixo)) {
            boolean exists = recursoRepository.existsByNomeAndSalaId(nome, salaId);

            if (exists) {
                throw new RegraNegocioException("Recurso já cadastrado nessa sala");
            }
        }

        Recurso recurso = new Recurso();
        recurso.setNome(nome);
        recurso.setQuantidade(quantidade);
        recurso.setTipoRecurso(tipoRecurso);
        recurso.setFixo(fixo);
        recurso.setSala(sala);

        Recurso recursoSalvo = recursoRepository.save(recurso);
        notificacaoService.avisarAlteracao("RECURSO_CRIADO");
        return recursoSalvo;
    }

    public List<Recurso> listar() {
        return recursoRepository.findAll();
    }

    public List<RecursoDisponibilidadeDTO> listarDisponibilidade(Long salaId, LocalDateTime inicio, LocalDateTime fim) {
        if ((inicio == null) != (fim == null)) {
            throw new RegraNegocioException("Informe início e fim para consultar o período");
        }

        if (inicio != null && !fim.isAfter(inicio)) {
            throw new RegraNegocioException("Data fim deve ser depois do início");
        }

        return recursoRepository.findAll()
                .stream()
                .filter(recurso -> pertenceAoContextoDaSala(recurso, salaId))
                .map(recurso -> toDisponibilidadeDTO(recurso, inicio, fim))
                .toList();
    }

    private boolean pertenceAoContextoDaSala(Recurso recurso, Long salaId) {
        if (salaId == null || !Boolean.TRUE.equals(recurso.getFixo())) {
            return true;
        }

        return recurso.getSala() != null && recurso.getSala().getId().equals(salaId);
    }

    private RecursoDisponibilidadeDTO toDisponibilidadeDTO(Recurso recurso, LocalDateTime inicio, LocalDateTime fim) {
        int quantidade = recurso.getQuantidade() == null ? 0 : recurso.getQuantidade();
        int reservado = 0;

        if (inicio != null && fim != null) {
            reservado = reservaRecursoRepository.somarQuantidadeReservadaNoPeriodo(
                    recurso,
                    inicio,
                    fim,
                    List.of(StatusReserva.PENDENTE, StatusReserva.APROVADA)
            );
        }

        RecursoDisponibilidadeDTO dto = new RecursoDisponibilidadeDTO();
        dto.id = recurso.getId();
        dto.nome = recurso.getNome();
        dto.quantidade = quantidade;
        dto.quantidadeReservada = reservado;
        dto.quantidadeDisponivel = Math.max(quantidade - reservado, 0);
        dto.fixo = recurso.getFixo();

        if (recurso.getSala() != null) {
            dto.sala = new SalaResumoDTO();
            dto.sala.id = recurso.getSala().getId();
            dto.sala.nome = recurso.getSala().getNome();
        }

        return dto;
    }
}
