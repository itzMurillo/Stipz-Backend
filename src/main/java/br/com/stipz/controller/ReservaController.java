package br.com.stipz.controller;

import br.com.stipz.DTO.ReservaRequestDTO;
import br.com.stipz.DTO.ReservaResponseDTO;
import br.com.stipz.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ReservaResponseDTO criar(
            @Valid @RequestBody ReservaRequestDTO dto,
            Authentication authentication
    ) {
        return reservaService.criarReservaCompleta(dto, authentication.getName());
    }

    @GetMapping
    public List<ReservaResponseDTO> listar() {
        return reservaService.listar();
    }

    @GetMapping("/minhas")
    public List<ReservaResponseDTO> listarMinhas(Authentication authentication) {
        return reservaService.listarMinhas(authentication.getName());
    }

    @PatchMapping("/{id}/aprovar")
    public ReservaResponseDTO aprovar(@PathVariable Long id) {
        return reservaService.aprovar(id);
    }

    @PatchMapping("/{id}/rejeitar")
    public ReservaResponseDTO rejeitar(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String motivo = body == null
                ? null
                : body.getOrDefault("motivoRejeicao", body.getOrDefault("motivo", body.get("justificativa")));

        return reservaService.rejeitar(id, motivo);
    }

    @PatchMapping("/{id}/cancelar")
    public ReservaResponseDTO cancelar(@PathVariable Long id, Authentication authentication) {
        return reservaService.cancelar(id, authentication.getName());
    }

    @PatchMapping("/{id}/substituir")
    public ReservaResponseDTO substituir(
            @PathVariable Long id,
            @Valid @RequestBody ReservaRequestDTO dto,
            Authentication authentication
    ) {
        return reservaService.substituirReservaRejeitada(id, dto, authentication.getName());
    }
}
