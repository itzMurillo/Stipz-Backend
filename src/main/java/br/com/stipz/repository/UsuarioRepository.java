package br.com.stipz.repository;

import br.com.stipz.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

        Optional<Usuario> findByEmail(String email);

        boolean existsByNomeIgnoreCaseAndEmailIgnoreCase(String nome, String email);
}
