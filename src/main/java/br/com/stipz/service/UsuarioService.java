package br.com.stipz.service;

import br.com.stipz.DTO.UsuarioRequestDTO;
import br.com.stipz.domain.Usuario;
import br.com.stipz.enums.PerfilUsuario;
import br.com.stipz.exception.RecursoNaoEncontradoException;
import br.com.stipz.exception.RegraNegocioException;
import br.com.stipz.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario criar(UsuarioRequestDTO dto) {

        if (usuarioRepository.existsByNomeIgnoreCaseAndEmailIgnoreCase(dto.nome, dto.email)) {
            throw new RegraNegocioException("Já existe um usuário cadastrado com esse nome e email");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome);
        usuario.setEmail(dto.email);
        usuario.setSenha(dto.senha);
        usuario.setPerfil(PerfilUsuario.valueOf(dto.perfil));

        return usuarioRepository.save(usuario);
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    public void deletar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado");
        }
        usuarioRepository.deleteById(id);
    }
}
