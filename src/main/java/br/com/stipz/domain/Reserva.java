package br.com.stipz.domain;

import br.com.stipz.enums.StatusReserva;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Entity
@Table(name = "reserva")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_sala", nullable = false)
    private Sala sala;

    @ManyToOne
    @JoinColumn(name = "id_evento")
    private Evento evento;

    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;

    @Enumerated(EnumType.STRING)
    private StatusReserva status;

    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
}
