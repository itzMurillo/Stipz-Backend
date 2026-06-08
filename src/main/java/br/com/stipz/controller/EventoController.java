package br.com.stipz.controller;

import br.com.stipz.DTO.EventoRequestDTO;
import br.com.stipz.DTO.EventoResponseDTO;
import br.com.stipz.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @PostMapping
    public EventoResponseDTO criar(@Valid @RequestBody EventoRequestDTO dto, Authentication authentication) {
        return eventoService.criar(dto, authentication.getName());
    }

    @GetMapping
    public List<EventoResponseDTO> listar() {
        return eventoService.listar();
    }
}
