package br.com.stipz.DTO;

import br.com.stipz.enums.CategoriaRecurso;
import jakarta.validation.constraints.*;

public class RecursoRequestDTO {

    @NotBlank(message = "Nome do recurso é obrigatório")
    public String nome;

    @NotBlank(message = "Descrição do recurso é obrigatória")
    public String descricao;

    @NotNull(message = "Categoria do recurso é obrigatória")
    public CategoriaRecurso categoria;

    @NotNull(message = "Quantidade do recurso é obrigatória")
    @Min(value = 1, message = "Quantidade do recurso deve ser maior que zero")
    public Integer quantidade;

    @NotNull(message = "Sala do recurso é obrigatória")
    public Long salaId;

    @NotNull(message = "Campo fixo é obrigatório")
    public Boolean fixo;
}
