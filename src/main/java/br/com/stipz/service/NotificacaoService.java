package br.com.stipz.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificacaoService {

    private final List<SseEmitter> clientes = new CopyOnWriteArrayList<>();

    public SseEmitter conectar() {
        SseEmitter emitter = new SseEmitter(0L);
        clientes.add(emitter);

        emitter.onCompletion(() -> clientes.remove(emitter));
        emitter.onTimeout(() -> clientes.remove(emitter));
        emitter.onError(erro -> clientes.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("conectado")
                    .data("Conectado em " + LocalDateTime.now()));
        } catch (IOException ex) {
            clientes.remove(emitter);
        }

        return emitter;
    }

    public int totalConectados() {
        return clientes.size();
    }

    public void avisarAlteracao(String tipo) {
        for (SseEmitter cliente : clientes) {
            try {
                cliente.send(SseEmitter.event()
                        .name("alteracao")
                        .data(tipo));
            } catch (IOException ex) {
                clientes.remove(cliente);
            }
        }
    }
}
