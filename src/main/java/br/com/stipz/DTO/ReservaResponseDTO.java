package br.com.stipz.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class ReservaResponseDTO {

    public Long id;
    public LocalDateTime dataInicio;
    public LocalDateTime dataFim;
    public String status;

    public UsuarioResumoDTO usuario;
    public SalaResumoDTO sala;

    public List<RecursoResumoDTO> recursos;
}
