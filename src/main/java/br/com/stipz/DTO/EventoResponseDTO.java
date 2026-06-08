package br.com.stipz.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class EventoResponseDTO {
    public Long id;
    public String nome;
    public String descricao;
    public String justificativa;
    public LocalDateTime inicio;
    public LocalDateTime fim;
    public LocalDateTime dataCriacao;
    public UsuarioResumoDTO usuario;
    public List<ReservaResponseDTO> reservas;
}
