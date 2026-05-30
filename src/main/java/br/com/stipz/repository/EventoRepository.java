package br.com.stipz.repository;

import br.com.stipz.domain.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    List<Evento> findByUsuarioId(Long usuarioId);

    @Query("""
        SELECT e FROM Evento e
        JOIN FETCH e.usuario
    """)
    List<Evento> findAllComUsuario();
}
