package br.com.stipz.service;

import br.com.stipz.DTO.UsuarioRequestDTO;
import br.com.stipz.DTO.UsuarioResponseDTO;
import br.com.stipz.domain.Usuario;
import br.com.stipz.enums.PerfilUsuario;
import br.com.stipz.exception.RecursoNaoEncontradoException;
import br.com.stipz.exception.RegraNegocioException;
import br.com.stipz.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificacaoService notificacaoService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            NotificacaoService notificacaoService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificacaoService = notificacaoService;
    }

    public Usuario criar(UsuarioRequestDTO dto) {

        if (usuarioRepository.existsByEmailIgnoreCase(dto.email)) {
            throw new RegraNegocioException("Já existe um usuário cadastrado com esse email");
        }

        PerfilUsuario perfil = PerfilUsuario.valueOf(dto.perfil);

        if (perfil == PerfilUsuario.ADMIN) {
            throw new RegraNegocioException("O sistema permite apenas um administrador configurado na implantação");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome);
        usuario.setEmail(dto.email);
        usuario.setSenha(passwordEncoder.encode(dto.senha));
        usuario.setPerfil(perfil);

        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        notificacaoService.avisarAlteracao("USUARIO_CRIADO");
        return usuarioSalvo;
    }

    public UsuarioResponseDTO criarResposta(UsuarioRequestDTO dto) {
        return toResponseDTO(criar(dto));
    }

    public List<UsuarioResponseDTO> listar() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public UsuarioResponseDTO buscarPorId(Long id) {
        return toResponseDTO(buscarEntidadePorId(id));
    }

    public Usuario buscarEntidadePorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    public void deletar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        if (usuario.getPerfil() == PerfilUsuario.ADMIN) {
            throw new RegraNegocioException("O administrador inicial não pode ser excluído");
        }

        usuarioRepository.delete(usuario);
        notificacaoService.avisarAlteracao("USUARIO_EXCLUIDO");
    }

    public UsuarioResponseDTO toResponseDTO(Usuario usuario) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.id = usuario.getId();
        dto.nome = usuario.getNome();
        dto.email = usuario.getEmail();
        dto.perfil = usuario.getPerfil().name();
        return dto;
    }
}
