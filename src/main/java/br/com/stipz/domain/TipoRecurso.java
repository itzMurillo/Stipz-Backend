package br.com.stipz.domain;

import br.com.stipz.enums.CategoriaRecurso;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "tipo_recurso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class TipoRecurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Enumerated(EnumType.STRING)
    private CategoriaRecurso categoria;

    private Boolean exigeAprovacao;

    private Boolean permitido;
}