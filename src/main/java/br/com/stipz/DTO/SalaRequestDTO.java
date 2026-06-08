package br.com.stipz.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SalaRequestDTO {

    @NotBlank(message = "Nome da sala é obrigatório")
    public String nome;

    @NotNull(message = "Capacidade da sala é obrigatória")
    @Min(value = 1, message = "Capacidade da sala deve ser maior que zero")
    public Integer capacidade;
}
