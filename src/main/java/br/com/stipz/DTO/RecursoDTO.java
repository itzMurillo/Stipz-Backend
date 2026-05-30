package br.com.stipz.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class RecursoDTO {

    @NotNull(message = "Recurso é obrigatório")
    public Long recursoId;

    @NotNull(message = "Quantidade do recurso é obrigatória")
    @Positive(message = "Quantidade do recurso deve ser maior que zero")
    public Integer quantidade;
}
