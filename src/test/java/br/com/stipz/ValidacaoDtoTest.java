package br.com.stipz;

import br.com.stipz.DTO.EventoRequestDTO;
import br.com.stipz.DTO.EventoSalaRequestDTO;
import br.com.stipz.DTO.LoginRequestDTO;
import br.com.stipz.DTO.RecursoRequestDTO;
import br.com.stipz.DTO.ReservaRequestDTO;
import br.com.stipz.DTO.SalaRequestDTO;
import br.com.stipz.DTO.UsuarioRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidacaoDtoTest {

    private static Validator validator;

    @BeforeAll
    static void prepararValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void deveRejeitarCamposObrigatoriosNulos() {
        assertCampos(new LoginRequestDTO(), "email", "senha");
        assertCampos(new UsuarioRequestDTO(), "nome", "email", "senha", "perfil");
        assertCampos(new SalaRequestDTO(), "nome", "capacidade");
        assertCampos(new RecursoRequestDTO(), "nome", "descricao", "categoria", "quantidade", "salaId", "fixo");
        assertCampos(new ReservaRequestDTO(), "salaId", "inicio", "fim");
        assertCampos(new EventoRequestDTO(), "nome", "inicio", "fim", "salas");
        assertCampos(new EventoSalaRequestDTO(), "salaId");
    }

    @Test
    void deveRejeitarNumerosInvalidos() {
        ReservaRequestDTO reserva = reservaValida();
        reserva.quantidadeParticipantes = 0;
        reserva.quantidadeCadeiras = -1;
        assertCampos(reserva, "quantidadeParticipantes", "quantidadeCadeiras");

        EventoSalaRequestDTO salaEvento = new EventoSalaRequestDTO();
        salaEvento.salaId = 1L;
        salaEvento.participantes = -1;
        salaEvento.capacidadeSolicitada = 0;
        assertCampos(salaEvento, "participantes", "capacidadeSolicitada");
    }

    @Test
    void deveRejeitarDatasPassadasInclusiveNasSalasDoEvento() {
        ReservaRequestDTO reserva = reservaValida();
        reserva.inicio = LocalDateTime.now().minusHours(2);
        reserva.fim = LocalDateTime.now().minusHours(1);
        assertCampos(reserva, "inicio", "fim");

        EventoSalaRequestDTO sala = new EventoSalaRequestDTO();
        sala.salaId = 1L;
        sala.inicio = LocalDateTime.now().minusHours(2);
        sala.fim = LocalDateTime.now().minusHours(1);

        EventoRequestDTO evento = new EventoRequestDTO();
        evento.nome = "Evento";
        evento.inicio = LocalDateTime.now().plusDays(1);
        evento.fim = evento.inicio.plusHours(2);
        evento.salas = List.of(sala);

        assertCampos(evento, "salas[0].inicio", "salas[0].fim");
    }

    @Test
    void deveAceitarCamposOpcionaisNulos() {
        ReservaRequestDTO reserva = reservaValida();
        assertTrue(validator.validate(reserva).isEmpty());
    }

    private ReservaRequestDTO reservaValida() {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.salaId = 1L;
        dto.inicio = LocalDateTime.now().plusDays(1);
        dto.fim = dto.inicio.plusHours(2);
        return dto;
    }

    private void assertCampos(Object dto, String... esperados) {
        Set<String> campos = validator.validate(dto).stream()
                .map(violacao -> violacao.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertEquals(Set.of(esperados), campos);
    }
}
