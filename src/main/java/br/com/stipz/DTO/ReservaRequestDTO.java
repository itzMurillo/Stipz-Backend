package br.com.stipz.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class ReservaRequestDTO {

    @NotNull(message = "Usuário é obrigatório")
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
}
