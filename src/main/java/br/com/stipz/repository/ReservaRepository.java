package br.com.stipz.repository;

import br.com.stipz.domain.Reserva;
import br.com.stipz.domain.Evento;
import br.com.stipz.domain.Sala;
import br.com.stipz.domain.Usuario;
import br.com.stipz.enums.StatusReserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    boolean existsBySalaAndDataInicioLessThanAndDataFimGreaterThanAndStatusNot(
            Sala sala,
            LocalDateTime fim,
            LocalDateTime inicio,
            StatusReserva status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.sala = :sala
          AND r.dataInicio < :fim
          AND r.dataFim > :inicio
          AND r.status <> :statusIgnorado
    """)
    List<Reserva> findConflitosComBloqueio(
            @Param("sala") Sala sala,
            @Param("fim") LocalDateTime fim,
            @Param("inicio") LocalDateTime inicio,
            @Param("statusIgnorado") StatusReserva statusIgnorado
    );

    long countByUsuarioAndDataInicioBetween(
            Usuario usuario,
            LocalDateTime inicio,
            LocalDateTime fim
    );

    //evitar N+1(usuario + sala)
    @Query("""
        SELECT r FROM Reserva r
        JOIN FETCH r.usuario
        JOIN FETCH r.sala
    """)
    List<Reserva> findAllComUsuarioESala();

    @Query("""
        SELECT r FROM Reserva r
        JOIN FETCH r.usuario
        JOIN FETCH r.sala
        WHERE r.evento = :evento
    """)
    List<Reserva> findByEventoComUsuarioESala(@Param("evento") Evento evento);
}
