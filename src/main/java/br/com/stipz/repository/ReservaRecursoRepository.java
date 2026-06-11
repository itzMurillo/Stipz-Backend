package br.com.stipz.repository;

import br.com.stipz.domain.Reserva;
import br.com.stipz.domain.ReservaRecurso;
import br.com.stipz.domain.Recurso;
import br.com.stipz.enums.StatusReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservaRecursoRepository extends JpaRepository<ReservaRecurso, Long> {

    List<ReservaRecurso> findByRecurso(Recurso recurso);

    void deleteByReserva(Reserva reserva);

    @Query("""
        SELECT COALESCE(SUM(rr.quantidade), 0)
        FROM ReservaRecurso rr
        WHERE rr.recurso = :recurso
          AND rr.reserva.status IN :statusesConsiderados
          AND rr.reserva.dataInicio < :fim
          AND rr.reserva.dataFim > :inicio
    """)
    Integer somarQuantidadeReservadaNoPeriodo(
            @Param("recurso") Recurso recurso,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("statusesConsiderados") Collection<StatusReserva> statusesConsiderados
    );

    //evitar N+1(recurso)
    @Query("""
        SELECT rr FROM ReservaRecurso rr
        JOIN FETCH rr.recurso
        WHERE rr.reserva = :reserva
    """)
    List<ReservaRecurso> findByReservaComRecurso(@Param("reserva") Reserva reserva);
}
