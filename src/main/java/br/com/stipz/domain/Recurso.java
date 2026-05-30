package br.com.stipz.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "recurso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class Recurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private Integer quantidade;

    private Boolean fixo;

    @ManyToOne
    @JoinColumn(name = "id_sala")
    private Sala sala;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_tipo_recurso", nullable = false)
    private TipoRecurso tipoRecurso;
}
