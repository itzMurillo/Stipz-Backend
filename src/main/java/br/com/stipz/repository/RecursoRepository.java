package br.com.stipz.repository;

import br.com.stipz.domain.Recurso;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecursoRepository extends JpaRepository<Recurso, Long> {
    boolean existsByNomeAndSalaId(String nome, Long salaId);
    List<Recurso> findByNomeContainingIgnoreCase(String nome);
    List<Recurso> findByTipoRecurso_NomeContainingIgnoreCase(String tipo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Recurso r WHERE r.id = :id")
    Optional<Recurso> findByIdForUpdate(@Param("id") Long id);
}
