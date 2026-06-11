package br.com.stipz.repository;

import br.com.stipz.domain.Sala;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.stipz.enums.StatusReserva;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SalaRepository extends JpaRepository<Sala, Long> {

    Optional<Sala> findByNome(String nome);

    boolean existsByNomeIgnoreCase(String nome);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sala s WHERE s.id = :id")
    Optional<Sala> findByIdForUpdate(@Param("id") Long id);

    @Query("""
        SELECT s
        FROM Sala s
        WHERE NOT EXISTS (
            SELECT r.id
            FROM Reserva r
            WHERE r.sala = s
              AND r.status IN :statusesConsiderados
              AND r.dataInicio < :fim
              AND r.dataFim > :inicio
        )
        ORDER BY s.nome
    """)
    List<Sala> findDisponiveisNoPeriodo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("statusesConsiderados") Collection<StatusReserva> statusesConsiderados
    );
}
