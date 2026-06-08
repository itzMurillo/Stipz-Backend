package br.com.stipz.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class EventoSalaRequestDTO {

    @NotNull(message = "Sala é obrigatória")
    public Long salaId;

    @Valid
    public List<RecursoDTO> recursos;
}
