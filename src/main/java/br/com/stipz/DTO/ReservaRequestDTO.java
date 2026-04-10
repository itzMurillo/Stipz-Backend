package br.com.stipz.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class ReservaRequestDTO {

    public Long usuarioId;
    public Long salaId;

    public LocalDateTime inicio;
    public LocalDateTime fim;

    public List<RecursoDTO> recursos;
}
