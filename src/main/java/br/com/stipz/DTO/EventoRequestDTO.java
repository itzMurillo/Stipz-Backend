package br.com.stipz.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class EventoRequestDTO {

    @NotBlank(message = "Nome do evento é obrigatório")
    public String nome;

    public String descricao;

    public String justificativa;

    @NotNull(message = "Data de início é obrigatória")
    @Future(message = "Data de início deve ser futura")
    public LocalDateTime inicio;

    @NotNull(message = "Data de fim é obrigatória")
    @Future(message = "Data de fim deve ser futura")
    public LocalDateTime fim;

    @Valid
    @NotEmpty(message = "Informe ao menos uma sala para o evento")
    public List<EventoSalaRequestDTO> salas;
}
