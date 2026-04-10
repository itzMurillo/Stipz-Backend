package br.com.stipz.controller;

import br.com.stipz.DTO.ReservaRequestDTO;
import br.com.stipz.DTO.ReservaResponseDTO;
import br.com.stipz.domain.Reserva;
import br.com.stipz.service.ReservaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ReservaResponseDTO criar(@RequestBody ReservaRequestDTO dto) {
        return reservaService.criarReservaCompleta(dto);
    }

    @GetMapping
    public List<ReservaResponseDTO> listar() {
        return reservaService.listar();
    }

    @PatchMapping("/{id}/aprovar")
    public Reserva aprovar(@PathVariable Long id) {
        return reservaService.aprovar(id);
    }

    @PatchMapping("/{id}/rejeitar")
    public Reserva rejeitar(@PathVariable Long id) {
        return reservaService.rejeitar(id);
    }

    @PatchMapping("/{id}/cancelar")
    public Reserva cancelar(@PathVariable Long id) {
        return reservaService.cancelar(id);
    }
}
