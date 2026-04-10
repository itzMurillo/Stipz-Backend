package br.com.stipz.DTO;

import br.com.stipz.enums.CategoriaRecurso;
import jakarta.validation.constraints.*;

public class RecursoRequestDTO {

    @NotBlank
    public String nome;

    @NotBlank
    public String descricao;

    @NotNull
    public CategoriaRecurso categoria;

    @NotNull
    @Min(1)
    public Integer quantidade;

    public Long salaId;

    @NotNull
    public Boolean fixo;
}
