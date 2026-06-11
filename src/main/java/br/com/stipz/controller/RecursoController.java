package br.com.stipz.controller;

import br.com.stipz.DTO.RecursoDisponibilidadeDTO;
import br.com.stipz.DTO.RecursoRequestDTO;
import br.com.stipz.domain.Recurso;
import br.com.stipz.service.RecursoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    @GetMapping("/disponibilidade")
    public List<RecursoDisponibilidadeDTO> listarDisponibilidade(
            @RequestParam(required = false) Long salaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim
    ) {
        return recursoService.listarDisponibilidade(salaId, inicio, fim);
    }
}
