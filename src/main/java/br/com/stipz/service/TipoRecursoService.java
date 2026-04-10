package br.com.stipz.service;

import br.com.stipz.domain.TipoRecurso;
import br.com.stipz.enums.CategoriaRecurso;
import br.com.stipz.repository.TipoRecursoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TipoRecursoService {

    private final TipoRecursoRepository tipoRecursoRepository;

    public TipoRecursoService(TipoRecursoRepository tipoRecursoRepository) {
        this.tipoRecursoRepository = tipoRecursoRepository;
    }

    public TipoRecurso buscarOuCriar(String nome, CategoriaRecurso categoria) {

        return tipoRecursoRepository.findByNomeIgnoreCase(nome)
                .orElseGet(() -> {

                    TipoRecurso novo = new TipoRecurso();
                    novo.setNome(nome);
                    novo.setCategoria(categoria);

                    boolean exigeAprovacao =
                            categoria == CategoriaRecurso.ALIMENTICIO
                                    || categoria == CategoriaRecurso.OUTRO;

                    novo.setExigeAprovacao(exigeAprovacao);

                    if (exigeAprovacao) {
                        novo.setPermitido(false);
                    } else {
                        novo.setPermitido(true);
                    }

                    return tipoRecursoRepository.save(novo);
                });
    }

    public List<TipoRecurso> buscarPorNome(String nome) {
        return tipoRecursoRepository.findByNomeContainingIgnoreCase(nome);
    }
}
