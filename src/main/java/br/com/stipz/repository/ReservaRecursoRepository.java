package br.com.stipz.repository;

import br.com.stipz.domain.Reserva;
import br.com.stipz.domain.ReservaRecurso;
import br.com.stipz.domain.Recurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRecursoRepository extends JpaRepository<ReservaRecurso, Long> {

    List<ReservaRecurso> findByRecurso(Recurso recurso);

    @Query("""
SELECT r FROM Reserva r
WHERE r.sala.id = :salaId
AND (
    (:inicio BETWEEN r.dataInicio AND r.dataFim)
    OR (:fim BETWEEN r.dataInicio AND r.dataFim)
    OR (r.dataInicio BETWEEN :inicio AND :fim)
)
""")
    List<Reserva> verificarConflito(Long salaId, LocalDateTime inicio, LocalDateTime fim);
}
