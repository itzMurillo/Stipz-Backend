package br.com.stipz.service;

import br.com.stipz.DTO.SalaRequestDTO;
import br.com.stipz.domain.Sala;
import br.com.stipz.exception.RecursoNaoEncontradoException;
import br.com.stipz.exception.RegraNegocioException;
import br.com.stipz.enums.StatusReserva;
import br.com.stipz.repository.SalaRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SalaService {

    private final SalaRepository salaRepository;
    private final NotificacaoService notificacaoService;

    public SalaService(SalaRepository salaRepository, NotificacaoService notificacaoService) {
        this.salaRepository = salaRepository;
        this.notificacaoService = notificacaoService;
    }

    public Sala criar(SalaRequestDTO dto) {
        if (salaRepository.existsByNomeIgnoreCase(dto.nome.trim())) {
            throw new RegraNegocioException("Sala já cadastrada");
        }

        Sala sala = new Sala();
        sala.setNome(dto.nome.trim());
        sala.setCapacidade(dto.capacidade);
        Sala salaSalva = salaRepository.save(sala);
        notificacaoService.avisarAlteracao("SALA_CRIADA");
        return salaSalva;
    }

    public List<Sala> listar() {
        return salaRepository.findAll();
    }

    public Sala buscar(Long id) {
        return salaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sala não encontrada"));
    }

    public List<Sala> listarDisponiveis(LocalDateTime inicio, LocalDateTime fim) {
        if (!fim.isAfter(inicio)) {
            throw new RegraNegocioException("Data fim deve ser depois do início");
        }

        return salaRepository.findDisponiveisNoPeriodo(
                inicio,
                fim,
                List.of(StatusReserva.PENDENTE, StatusReserva.APROVADA)
        );
    }

    public void deletar(Long id) {
        salaRepository.deleteById(id);
        notificacaoService.avisarAlteracao("SALA_EXCLUIDA");
    }
}
