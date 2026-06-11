package br.com.stipz.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class ReservaResponseDTO {

    public Long id;
    public LocalDateTime dataInicio;
    public LocalDateTime dataFim;
    public String status;
    public String responsavel;
    public String motivoRejeicao;
    public Long eventoId;
    public String evento;
    public String eventoDescricao;
    public String justificativa;
    public Boolean cadeirasExtras;
    public Integer quantidadeCadeiras;

    public UsuarioResumoDTO usuario;
    public SalaResumoDTO sala;

    public List<RecursoResumoDTO> recursos;
}
