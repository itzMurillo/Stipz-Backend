package br.com.stipz.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;
import java.util.List;

public class EventoSalaRequestDTO {

    @NotNull(message = "Sala é obrigatória")
    public Long salaId;

    @Future(message = "Data de início da sala deve ser futura")
    public LocalDateTime inicio;

    @Future(message = "Data de fim da sala deve ser futura")
    public LocalDateTime fim;

    @Valid
    public List<RecursoDTO> recursos;

    @PositiveOrZero(message = "Quantidade de cadeiras não pode ser negativa")
    public Integer quantidadeCadeiras;

    public Boolean cadeirasExtras;

    @Positive(message = "Quantidade de participantes deve ser maior que zero")
    public Integer quantidadeParticipantes;

    @Positive(message = "Participantes deve ser maior que zero")
    public Integer participantes;

    @Positive(message = "Capacidade solicitada deve ser maior que zero")
    public Integer capacidadeSolicitada;

    public String responsavel;

    public String nomeResponsavel;
}
