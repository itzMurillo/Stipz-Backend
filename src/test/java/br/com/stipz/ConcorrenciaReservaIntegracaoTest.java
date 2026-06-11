package br.com.stipz;

import br.com.stipz.DTO.ReservaRequestDTO;
import br.com.stipz.domain.Sala;
import br.com.stipz.domain.Usuario;
import br.com.stipz.enums.PerfilUsuario;
import br.com.stipz.repository.ReservaRepository;
import br.com.stipz.repository.SalaRepository;
import br.com.stipz.repository.UsuarioRepository;
import br.com.stipz.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ConcorrenciaReservaIntegracaoTest {

    @Autowired
    private ReservaService reservaService;
    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private SalaRepository salaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void apenasUmaReservaSimultaneaDeveSerCriadaParaOMesmoHorario() throws Exception {
        String identificador = String.valueOf(System.nanoTime());

        Usuario usuario = new Usuario();
        usuario.setNome("Concorrencia");
        usuario.setEmail("concorrencia-" + identificador + "@stipz.com");
        usuario.setSenha("nao-utilizada");
        usuario.setPerfil(PerfilUsuario.COMUM);
        usuario = usuarioRepository.save(usuario);

        Sala sala = new Sala();
        sala.setNome("Sala concorrencia " + identificador);
        sala.setCapacidade(20);
        sala = salaRepository.save(sala);

        Long usuarioId = usuario.getId();
        Long salaId = sala.getId();
        String email = usuario.getEmail();
        LocalDateTime inicio = LocalDateTime.now().plusYears(5);
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<Boolean>> resultados = new ArrayList<>();

            for (int i = 0; i < 2; i++) {
                resultados.add(executor.submit(() -> {
                    ReservaRequestDTO dto = new ReservaRequestDTO();
                    dto.usuarioId = usuarioId;
                    dto.salaId = salaId;
                    dto.inicio = inicio;
                    dto.fim = inicio.plusHours(2);

                    largada.await();

                    try {
                        reservaService.criarReservaCompleta(dto, email);
                        return true;
                    } catch (RuntimeException ex) {
                        return false;
                    }
                }));
            }

            largada.countDown();

            long sucessos = 0;
            for (Future<Boolean> resultado : resultados) {
                if (resultado.get()) {
                    sucessos++;
                }
            }

            assertEquals(1, sucessos);
        } finally {
            executor.shutdownNow();
            reservaRepository.findAll().stream()
                    .filter(reserva -> reserva.getSala().getId().equals(salaId))
                    .forEach(reservaRepository::delete);
            usuarioRepository.deleteById(usuarioId);
            salaRepository.deleteById(salaId);
        }
    }
}
