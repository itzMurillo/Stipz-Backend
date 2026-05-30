package br.com.stipz.controller;

import br.com.stipz.DTO.UsuarioRequestDTO;
import br.com.stipz.DTO.UsuarioResponseDTO;
import br.com.stipz.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public UsuarioResponseDTO criar(@Valid @RequestBody UsuarioRequestDTO dto) {
        return usuarioService.criarResposta(dto);
    }

    @GetMapping
    public List<UsuarioResponseDTO> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    public UsuarioResponseDTO buscar(@PathVariable Long id) {
        return usuarioService.buscarPorId(id);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        usuarioService.deletar(id);
    }
}
