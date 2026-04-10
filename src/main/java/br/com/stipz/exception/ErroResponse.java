package br.com.stipz.exception;

import java.time.LocalDateTime;

public class ErroResponse {

    private LocalDateTime timestamp;
    private Integer status;
    private String erro;
    private String caminho;

    public ErroResponse(LocalDateTime timestamp, Integer status, String erro, String caminho) {
        this.timestamp = timestamp;
        this.status = status;
        this.erro = erro;
        this.caminho = caminho;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public Integer getStatus() { return status; }
    public String getErro() { return erro; }
    public String getCaminho() { return caminho; }
}
