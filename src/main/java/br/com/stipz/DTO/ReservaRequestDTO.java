package br.com.stipz.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;
import java.util.List;

public class ReservaRequestDTO {

    public Long usuarioId;

    @NotNull(message = "Sala é obrigatória")
    public Long salaId;

    @NotNull(message = "Data de início é obrigatória")
    @Future(message = "Data de início deve ser futura")
    public LocalDateTime inicio;

    @NotNull(message = "Data de fim é obrigatória")
    @Future(message = "Data de fim deve ser futura")
    public LocalDateTime fim;

    @Valid
    public List<RecursoDTO> recursos;

    public String responsavel;

    public String nomeResponsavel;

    @PositiveOrZero(message = "Quantidade de cadeiras não pode ser negativa")
    public Integer quantidadeCadeiras;

    public Boolean cadeirasExtras;

    @Positive(message = "Quantidade de participantes deve ser maior que zero")
    public Integer quantidadeParticipantes;

    @Positive(message = "Participantes deve ser maior que zero")
    public Integer participantes;

    @Positive(message = "Capacidade solicitada deve ser maior que zero")
    public Integer capacidadeSolicitada;
}
