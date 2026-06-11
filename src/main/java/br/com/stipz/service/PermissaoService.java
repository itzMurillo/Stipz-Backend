package br.com.stipz.service;

import br.com.stipz.DTO.RegraAcessoDTO;
import br.com.stipz.enums.PerfilUsuario;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissaoService {

    public List<String> listarPermissoes(PerfilUsuario perfil) {
        if (perfil == PerfilUsuario.ADMIN) {
            return List.of(
                    "Cadastrar, listar e remover usuários comuns",
                    "Cadastrar, listar e remover salas",
                    "Cadastrar e listar recursos",
                    "Criar, listar, aprovar, rejeitar e cancelar reservas",
                    "Consultar regras de acesso"
            );
        }

        return List.of(
                "Consultar salas, recursos, eventos e as próprias reservas",
                "Criar reservas",
                "Cancelar as próprias reservas conforme regra de antecedência",
                "Consultar regras de acesso"
        );
    }

    public List<RegraAcessoDTO> listarRegras() {
        return List.of(
                new RegraAcessoDTO(
                        PerfilUsuario.ADMIN.name(),
                        "Perfil único configurado na implantação, responsável pela manutenção do sistema e aprovação das reservas.",
                        listarPermissoes(PerfilUsuario.ADMIN)
                ),
                new RegraAcessoDTO(
                        PerfilUsuario.COMUM.name(),
                        "Perfil operacional para usuários que consultam recursos e solicitam reservas.",
                        listarPermissoes(PerfilUsuario.COMUM)
                )
        );
    }
}
