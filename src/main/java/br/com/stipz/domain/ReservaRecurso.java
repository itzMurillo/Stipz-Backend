package br.com.stipz.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "reserva_recurso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class ReservaRecurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_reserva", nullable = false)
    private Reserva reserva;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_recurso", nullable = false)
    private Recurso recurso;

    private Integer quantidade;
}