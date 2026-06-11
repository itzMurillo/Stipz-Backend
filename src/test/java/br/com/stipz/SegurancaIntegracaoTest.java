package br.com.stipz;

import br.com.stipz.domain.Sala;
import br.com.stipz.domain.Usuario;
import br.com.stipz.enums.PerfilUsuario;
import br.com.stipz.repository.SalaRepository;
import br.com.stipz.repository.UsuarioRepository;
import br.com.stipz.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SegurancaIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private SalaRepository salaRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private Usuario admin;
    private Usuario comum;
    private String tokenAdmin;
    private String tokenComum;

    @BeforeEach
    void prepararUsuarios() {
        admin = usuarioRepository.findAll().stream()
                .filter(usuario -> usuario.getPerfil() == PerfilUsuario.ADMIN)
                .findFirst()
                .orElseThrow();

        comum = new Usuario();
        comum.setNome("Usuario Teste");
        comum.setEmail("usuario-teste-integracao@stipz.com");
        comum.setSenha(passwordEncoder.encode("senha123"));
        comum.setPerfil(PerfilUsuario.COMUM);
        comum = usuarioRepository.save(comum);

        tokenAdmin = jwtService.gerarToken(admin);
        tokenComum = jwtService.gerarToken(comum);
    }

    @Test
    void deveExigirTokenValido() throws Exception {
        mockMvc.perform(get("/salas"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/salas")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveSepararPermissoesDeAdminEComum() throws Exception {
        mockMvc.perform(get("/usuarios").header("Authorization", bearer(tokenComum)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/salas")
                        .header("Authorization", bearer(tokenComum))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Sala proibida","capacidade":20}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/reservas").header("Authorization", bearer(tokenComum)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/reservas/1/aprovar")
                        .header("Authorization", bearer(tokenComum)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/backups/auditoria/gerar")
                        .header("Authorization", bearer(tokenComum)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/reservas/minhas").header("Authorization", bearer(tokenComum)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/usuarios").header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("\"senha\""))));
    }

    @Test
    void naoDevePermitirExcluirAdministradorInicial() throws Exception {
        mockMvc.perform(delete("/usuarios/{id}", admin.getId())
                        .header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O administrador inicial não pode ser excluído"));
    }

    @Test
    void loginNaoDeveRevelarSeEmailExiste() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"inexistente@stipz.com","senha":"errada"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Email ou senha inválidos"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@stipz.com","senha":"errada"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Email ou senha inválidos"));
    }

    @Test
    void deveRetornarErrosParaJsonECamposObrigatoriosInvalidos() throws Exception {
        validarPostComErro("/auth/login", "{}", null, "email", "senha");
        validarPostComErro("/usuarios", "{}", tokenAdmin, "nome", "email", "senha", "perfil");
        validarPostComErro("/salas", "{}", tokenAdmin, "nome", "capacidade");
        validarPostComErro("/recursos", "{}", tokenAdmin,
                "nome", "descricao", "categoria", "quantidade", "salaId", "fixo");
        validarPostComErro("/reservas", "{}", tokenComum, "salaId", "inicio", "fim");
        validarPostComErro("/eventos", "{}", tokenComum, "nome", "inicio", "fim", "salas");

        mockMvc.perform(post("/reservas")
                        .header("Authorization", bearer(tokenComum))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("JSON inválido ou campo com tipo incorreto"));

        mockMvc.perform(post("/reservas")
                        .header("Authorization", bearer(tokenComum))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Corpo da requisição é obrigatório"));

        mockMvc.perform(get("/salas/disponiveis")
                        .header("Authorization", bearer(tokenComum)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("inicio")));

        mockMvc.perform(get("/rota-inexistente")
                        .header("Authorization", bearer(tokenAdmin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("Rota não encontrada"));
    }

    @Test
    void deveAceitarOpcionaisNulosERetornarPadroes() throws Exception {
        Sala sala = novaSala("Sala opcionais");
        LocalDateTime inicio = LocalDateTime.now().plusYears(2);

        mockMvc.perform(post("/reservas")
                        .header("Authorization", bearer(tokenComum))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "salaId": %d,
                                  "inicio": "%s",
                                  "fim": "%s",
                                  "recursos": null,
                                  "responsavel": null,
                                  "cadeirasExtras": null,
                                  "quantidadeCadeiras": null,
                                  "quantidadeParticipantes": null
                                }
                                """.formatted(sala.getId(), inicio, inicio.plusHours(2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.id").value(comum.getId()))
                .andExpect(jsonPath("$.cadeirasExtras").value(false))
                .andExpect(jsonPath("$.quantidadeCadeiras").value(0));
    }

    @Test
    void deveValidarSalaDuplicadaEventoEParticipantes() throws Exception {
        Sala sala1 = novaSala("Sala evento 1");
        Sala sala2 = novaSala("Sala evento 2");
        LocalDateTime inicio = LocalDateTime.now().plusYears(3);

        mockMvc.perform(post("/salas")
                        .header("Authorization", bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"sala EVENTO 1","capacidade":40}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Sala já cadastrada"));

        mockMvc.perform(post("/eventos")
                        .header("Authorization", bearer(tokenComum))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome":"Evento sem justificativa",
                                  "inicio":"%s",
                                  "fim":"%s",
                                  "salas":[{"salaId":%d},{"salaId":%d}]
                                }
                                """.formatted(inicio, inicio.plusHours(2), sala1.getId(), sala2.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro")
                        .value("Justificativa é obrigatória para eventos com múltiplas salas"));

        mockMvc.perform(post("/reservas")
                        .header("Authorization", bearer(tokenComum))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "salaId":%d,
                                  "inicio":"%s",
                                  "fim":"%s",
                                  "quantidadeParticipantes":0
                                }
                                """.formatted(sala1.getId(), inicio, inicio.plusHours(2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("quantidadeParticipantes")));
    }

    @Test
    void deveValidarCapacidadeECadeirasAntesDeCriarReserva() throws Exception {
        Sala sala = novaSala("Sala capacidade");
        LocalDateTime inicio = LocalDateTime.now().plusYears(4);

        mockMvc.perform(post("/reservas")
                        .header("Authorization", bearer(tokenComum))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "salaId":%d,
                                  "inicio":"%s",
                                  "fim":"%s",
                                  "quantidadeParticipantes":41
                                }
                                """.formatted(sala.getId(), inicio, inicio.plusHours(2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(
                        "Capacidade da sala excedida. Sala Sala capacidade comporta 40 pessoas, solicitado 41."
                ));

        mockMvc.perform(post("/reservas")
                        .header("Authorization", bearer(tokenComum))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "salaId":%d,
                                  "inicio":"%s",
                                  "fim":"%s",
                                  "cadeirasExtras":true,
                                  "quantidadeCadeiras":null
                                }
                                """.formatted(sala.getId(), inicio, inicio.plusHours(2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(
                        "Quantidade de cadeiras deve ser maior que zero quando cadeiras extras forem solicitadas"
                ));
    }

    private Sala novaSala(String nome) {
        Sala sala = new Sala();
        sala.setNome(nome);
        sala.setCapacidade(40);
        return salaRepository.save(sala);
    }

    private void validarPostComErro(
            String rota,
            String json,
            String token,
            String... campos
    ) throws Exception {
        var request = post(rota)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);

        if (token != null) {
            request.header("Authorization", bearer(token));
        }

        var resultado = mockMvc.perform(request)
                .andExpect(status().isBadRequest());

        for (String campo : campos) {
            resultado.andExpect(jsonPath("$.erro", containsString(campo)));
        }
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
