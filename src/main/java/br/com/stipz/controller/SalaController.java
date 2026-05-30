package br.com.stipz.controller;

import br.com.stipz.DTO.SalaRequestDTO;
import br.com.stipz.domain.Sala;
import br.com.stipz.service.SalaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/salas")
public class SalaController {

    private final SalaService salaService;

    public SalaController(SalaService salaService) {
        this.salaService = salaService;
    }

    @PostMapping
    public Sala criar(@Valid @RequestBody SalaRequestDTO dto) {
        return salaService.criar(dto);
    }

    @GetMapping
    public List<Sala> listar() {
        return salaService.listar();
    }

    @GetMapping("/{id}")
    public Sala buscar(@PathVariable Long id) {
        return salaService.buscar(id);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        salaService.deletar(id);
    }
}
