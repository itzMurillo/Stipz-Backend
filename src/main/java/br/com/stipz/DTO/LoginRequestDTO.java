package br.com.stipz.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequestDTO {

    @Email(message = "Email inválido")
    @NotBlank(message = "Email é obrigatório")
    public String email;

    @NotBlank(message = "Senha é obrigatória")
    public String senha;
}
