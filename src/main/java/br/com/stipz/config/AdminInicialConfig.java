package br.com.stipz.config;

import br.com.stipz.domain.Usuario;
import br.com.stipz.enums.PerfilUsuario;
import br.com.stipz.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInicialConfig {

    @Bean
    public CommandLineRunner criarAdministradorInicial(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${stipz.security.admin.nome}") String adminNome,
            @Value("${stipz.security.admin.email}") String adminEmail,
            @Value("${stipz.security.admin.senha}") String adminSenha
    ) {
        return args -> {
            if (usuarioRepository.existsByEmailIgnoreCase(adminEmail)) {
                return;
            }

            if (adminSenha == null || adminSenha.isBlank()) {
                throw new IllegalStateException("Configure a variavel de ambiente ADMIN_PASSWORD antes do primeiro acesso.");
            }

            if (usuarioRepository.existsByPerfil(PerfilUsuario.ADMIN)) {
                throw new IllegalStateException("Já existe um administrador cadastrado. O sistema permite apenas um ADMIN.");
            }

            Usuario usuario = new Usuario();
            usuario.setNome(adminNome);
            usuario.setEmail(adminEmail);
            usuario.setSenha(passwordEncoder.encode(adminSenha));
            usuario.setPerfil(PerfilUsuario.ADMIN);

            usuarioRepository.save(usuario);
        };
    }
}
