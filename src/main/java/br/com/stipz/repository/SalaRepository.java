package br.com.stipz.repository;

import br.com.stipz.domain.Sala;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SalaRepository extends JpaRepository<Sala, Long> {

    Optional<Sala> findByNome(String nome);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sala s WHERE s.id = :id")
    Optional<Sala> findByIdForUpdate(@Param("id") Long id);
}
