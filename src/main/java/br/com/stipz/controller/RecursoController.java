package br.com.stipz.controller;

import br.com.stipz.DTO.RecursoRequestDTO;
import br.com.stipz.domain.Recurso;
import br.com.stipz.service.RecursoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recursos")
public class RecursoController {

    private final RecursoService recursoService;

    public RecursoController(RecursoService recursoService) {
        this.recursoService = recursoService;
    }

    @PostMapping
    public Recurso criar(@Valid @RequestBody RecursoRequestDTO dto) {
        return recursoService.criar(
                dto.nome,
                dto.descricao,
                dto.categoria,
                dto.quantidade,
                dto.salaId,
                dto.fixo
        );
    }

    @GetMapping
    public List<Recurso> listar() {
        return recursoService.listar();
    }
}
