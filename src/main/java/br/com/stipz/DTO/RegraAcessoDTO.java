package br.com.stipz.DTO;

import java.util.List;

public class RegraAcessoDTO {
    public String perfil;
    public String descricao;
    public List<String> funcoes;

    public RegraAcessoDTO(String perfil, String descricao, List<String> funcoes) {
        this.perfil = perfil;
        this.descricao = descricao;
        this.funcoes = funcoes;
    }
}
