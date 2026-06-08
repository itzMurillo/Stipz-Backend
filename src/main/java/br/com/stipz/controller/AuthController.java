package br.com.stipz.controller;

import br.com.stipz.DTO.LoginRequestDTO;
import br.com.stipz.DTO.LoginResponseDTO;
import br.com.stipz.DTO.RegraAcessoDTO;
import br.com.stipz.DTO.UsuarioAutenticadoDTO;
import br.com.stipz.domain.Usuario;
import br.com.stipz.exception.RegraNegocioException;
import br.com.stipz.service.JwtService;
import br.com.stipz.service.PermissaoService;
import br.com.stipz.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final PermissaoService permissaoService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            UsuarioService usuarioService,
            PermissaoService permissaoService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioService = usuarioService;
        this.permissaoService = permissaoService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        Usuario usuario = usuarioService.buscarPorEmail(request.email);

        if (!passwordEncoder.matches(request.senha, usuario.getSenha())) {
            throw new RegraNegocioException("Email ou senha inválidos");
        }

        LoginResponseDTO response = new LoginResponseDTO();
        response.token = jwtService.gerarToken(usuario);
        response.usuario = usuarioAutenticado(usuario);
        return response;
    }

    @GetMapping("/me")
    public UsuarioAutenticadoDTO me(Authentication authentication) {
        Usuario usuario = usuarioService.buscarPorEmail(authentication.getName());
        return usuarioAutenticado(usuario);
    }

    @GetMapping("/regras-acesso")
    public List<RegraAcessoDTO> regrasAcesso() {
        return permissaoService.listarRegras();
    }

    private UsuarioAutenticadoDTO usuarioAutenticado(Usuario usuario) {
        UsuarioAutenticadoDTO dto = new UsuarioAutenticadoDTO();
        dto.id = usuario.getId();
        dto.nome = usuario.getNome();
        dto.email = usuario.getEmail();
        dto.perfil = usuario.getPerfil().name();
        dto.permissoes = permissaoService.listarPermissoes(usuario.getPerfil());
        return dto;
    }
}
