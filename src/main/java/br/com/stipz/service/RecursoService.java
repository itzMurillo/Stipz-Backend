package br.com.stipz.service;

import br.com.stipz.domain.*;
import br.com.stipz.enums.CategoriaRecurso;
import br.com.stipz.exception.RegraNegocioException;
import br.com.stipz.exception.RecursoNaoEncontradoException;
import br.com.stipz.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecursoService {

    private final RecursoRepository recursoRepository;
    private final TipoRecursoService tipoRecursoService;
    private final SalaRepository salaRepository;
    private final NotificacaoService notificacaoService;

    public RecursoService(
            RecursoRepository recursoRepository,
            TipoRecursoService tipoRecursoService,
            SalaRepository salaRepository,
            NotificacaoService notificacaoService
    ) {
        this.recursoRepository = recursoRepository;
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
}
