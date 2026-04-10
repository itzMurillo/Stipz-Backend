package br.com.stipz.repository;

import br.com.stipz.domain.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    List<Evento> findByUsuarioId(Long usuarioId);
}
