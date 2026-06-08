package br.com.stipz.controller;

import br.com.stipz.service.JwtService;
import br.com.stipz.service.NotificacaoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;
    private final JwtService jwtService;

    public NotificacaoController(NotificacaoService notificacaoService, JwtService jwtService) {
        this.notificacaoService = notificacaoService;
        this.jwtService = jwtService;
    }

    @GetMapping("/stream")
    @ResponseStatus(HttpStatus.OK)
    public SseEmitter stream(@RequestParam String token) {
        String email = jwtService.validarToken(token);

        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }

        return notificacaoService.conectar();
    }

    @PostMapping("/teste")
    public Map<String, Object> teste() {
        notificacaoService.avisarAlteracao("TESTE");

        return Map.of(
                "mensagem", "Evento de teste enviado",
                "clientesConectados", notificacaoService.totalConectados()
        );
    }
}
