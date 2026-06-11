package br.com.stipz.service;

import br.com.stipz.DTO.RecursoDTO;
import br.com.stipz.DTO.ReservaRequestDTO;
import br.com.stipz.domain.Recurso;
import br.com.stipz.domain.Reserva;
import br.com.stipz.domain.Sala;
import br.com.stipz.domain.Usuario;
import br.com.stipz.enums.StatusReserva;
import br.com.stipz.exception.RegraNegocioException;
import br.com.stipz.repository.RecursoRepository;
import br.com.stipz.repository.ReservaRecursoRepository;
import br.com.stipz.repository.ReservaRepository;
import br.com.stipz.repository.SalaRepository;
import br.com.stipz.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private SalaRepository salaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RecursoRepository recursoRepository;
    @Mock
    private ReservaRecursoRepository reservaRecursoRepository;
    @Mock
    private NotificacaoService notificacaoService;

    @InjectMocks
    private ReservaService reservaService;

    @Test
    void naoDeveSalvarReservaComRecursoDuplicado() {
        Usuario usuario = new Usuario();
        Sala sala = new Sala();
        sala.setCapacidade(60);

        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.usuarioId = 1L;
        dto.salaId = 1L;
        dto.recursos = List.of(recurso(1L), recurso(1L));

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(salaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sala));

        RegraNegocioException erro = assertThrows(
                RegraNegocioException.class,
                () -> reservaService.criarReservaCompleta(dto)
        );

        assertEquals("O recurso #1 foi informado mais de uma vez", erro.getMessage());
        verify(reservaRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void devePersistirEDevolverCadeirasExtras() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Murillo");

        Sala sala = new Sala();
        sala.setId(1L);
        sala.setNome("LAB 1");
        sala.setCapacidade(60);

        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.usuarioId = 1L;
        dto.salaId = 1L;
        dto.inicio = LocalDateTime.now().plusDays(1);
        dto.fim = dto.inicio.plusHours(2);
        dto.cadeirasExtras = true;
        dto.quantidadeCadeiras = 100;
        dto.quantidadeParticipantes = 50;

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(salaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sala));
        when(reservaRepository.findConflitosComBloqueio(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(reservaRepository.save(any())).thenAnswer(invocacao -> {
            br.com.stipz.domain.Reserva reserva = invocacao.getArgument(0);
            reserva.setId(1L);
            return reserva;
        });
        when(reservaRecursoRepository.findByReservaComRecurso(any())).thenReturn(List.of());

        var resposta = reservaService.criarReservaCompleta(dto);

        assertTrue(resposta.cadeirasExtras);
        assertEquals(100, resposta.quantidadeCadeiras);
    }

    @Test
    void naoDeveSalvarQuandoCapacidadeDaSalaForExcedida() {
        Usuario usuario = usuario(1L, "Murillo", "murillo@stipz.com");
        Sala sala = sala(1L, "LAB 1", 30);
        ReservaRequestDTO dto = reservaValida(1L, 1L);
        dto.quantidadeParticipantes = 31;

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(salaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sala));

        RegraNegocioException erro = assertThrows(
                RegraNegocioException.class,
                () -> reservaService.criarReservaCompleta(dto)
        );

        assertEquals(
                "Capacidade da sala excedida. Sala LAB 1 comporta 30 pessoas, solicitado 31.",
                erro.getMessage()
        );
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void quantidadeDeCadeirasExtrasNaoDeveSerUsadaComoParticipantes() {
        Usuario usuario = usuario(1L, "Murillo", "murillo@stipz.com");
        Sala sala = sala(1L, "LAB 1", 30);
        ReservaRequestDTO dto = reservaValida(1L, 1L);
        dto.cadeirasExtras = true;
        dto.quantidadeCadeiras = 100;

        prepararCriacaoSemConflito(usuario, sala);
        when(reservaRecursoRepository.findByReservaComRecurso(any())).thenReturn(List.of());

        var resposta = reservaService.criarReservaCompleta(dto);

        assertTrue(resposta.cadeirasExtras);
        assertEquals(100, resposta.quantidadeCadeiras);
    }

    @Test
    void deveBloquearConflitoELimiteSemanalAntesDeSalvar() {
        Usuario usuario = usuario(1L, "Murillo", "murillo@stipz.com");
        Sala sala = sala(1L, "LAB 1", 30);
        ReservaRequestDTO dto = reservaValida(1L, 1L);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(salaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sala));
        when(reservaRepository.findConflitosComBloqueio(any(), any(), any(), any()))
                .thenReturn(List.of(new Reserva()));

        assertEquals(
                "Conflito de horário",
                assertThrows(
                        RegraNegocioException.class,
                        () -> reservaService.criarReservaCompleta(dto)
                ).getMessage()
        );
        verify(reservaRepository, never()).save(any());

        when(reservaRepository.findConflitosComBloqueio(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(reservaRepository.countByUsuarioAndDataInicioBetween(eq(usuario), any(), any()))
                .thenReturn(5L);

        assertEquals(
                "Limite semanal atingido",
                assertThrows(
                        RegraNegocioException.class,
                        () -> reservaService.criarReservaCompleta(dto)
                ).getMessage()
        );
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void deveValidarCadeirasExtrasERecursosIndisponiveis() {
        Usuario usuario = usuario(1L, "Murillo", "murillo@stipz.com");
        Sala sala = sala(1L, "LAB 1", 30);
        ReservaRequestDTO dto = reservaValida(1L, 1L);
        dto.cadeirasExtras = true;

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(salaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sala));

        assertEquals(
                "Quantidade de cadeiras deve ser maior que zero quando cadeiras extras forem solicitadas",
                assertThrows(
                        RegraNegocioException.class,
                        () -> reservaService.criarReservaCompleta(dto)
                ).getMessage()
        );
        verify(reservaRepository, never()).save(any());

        dto.cadeirasExtras = false;
        dto.recursos = List.of(recurso(9L, 3));
        Recurso recurso = new Recurso();
        recurso.setId(9L);
        recurso.setNome("Projetor");
        recurso.setQuantidade(2);
        recurso.setFixo(false);
        recurso.setSala(sala);

        prepararCriacaoSemConflito(usuario, sala);
        when(recursoRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(recurso));

        assertEquals(
                "Recurso 'Projetor' indisponível. Solicitado: 3, total cadastrado: 2",
                assertThrows(
                        RegraNegocioException.class,
                        () -> reservaService.criarReservaCompleta(dto)
                ).getMessage()
        );
    }

    @Test
    void deveValidarProprietarioStatusEAntecedenciaAoCancelar() {
        Usuario usuario = usuario(1L, "Murillo", "murillo@stipz.com");
        Sala sala = sala(1L, "LAB 1", 30);
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setUsuario(usuario);
        reserva.setSala(sala);
        reserva.setStatus(StatusReserva.PENDENTE);
        reserva.setDataInicio(LocalDateTime.now().plusHours(5));

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        assertEquals(
                "Você só pode cancelar suas próprias reservas",
                assertThrows(
                        RegraNegocioException.class,
                        () -> reservaService.cancelar(1L, "outro@stipz.com")
                ).getMessage()
        );

        reserva.setStatus(StatusReserva.APROVADA);
        assertEquals(
                "Somente reservas pendentes podem ser canceladas",
                assertThrows(
                        RegraNegocioException.class,
                        () -> reservaService.cancelar(1L, usuario.getEmail())
                ).getMessage()
        );

        reserva.setStatus(StatusReserva.PENDENTE);
        reserva.setDataInicio(LocalDateTime.now().plusHours(2));
        assertEquals(
                "Não é possível cancelar com menos de 3 horas",
                assertThrows(
                        RegraNegocioException.class,
                        () -> reservaService.cancelar(1L, usuario.getEmail())
                ).getMessage()
        );
        verify(reservaRepository, never()).save(any());
    }

    private void prepararCriacaoSemConflito(Usuario usuario, Sala sala) {
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));
        when(salaRepository.findByIdForUpdate(sala.getId())).thenReturn(Optional.of(sala));
        when(reservaRepository.findConflitosComBloqueio(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(reservaRepository.save(any())).thenAnswer(invocacao -> {
            Reserva reserva = invocacao.getArgument(0);
            reserva.setId(1L);
            return reserva;
        });
    }

    private ReservaRequestDTO reservaValida(Long usuarioId, Long salaId) {
        ReservaRequestDTO dto = new ReservaRequestDTO();
        dto.usuarioId = usuarioId;
        dto.salaId = salaId;
        dto.inicio = LocalDateTime.now().plusDays(2);
        dto.fim = dto.inicio.plusHours(2);
        return dto;
    }

    private Usuario usuario(Long id, String nome, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setEmail(email);
        return usuario;
    }

    private Sala sala(Long id, String nome, Integer capacidade) {
        Sala sala = new Sala();
        sala.setId(id);
        sala.setNome(nome);
        sala.setCapacidade(capacidade);
        return sala;
    }

    private RecursoDTO recurso(Long id) {
        return recurso(id, 1);
    }

    private RecursoDTO recurso(Long id, Integer quantidade) {
        RecursoDTO recurso = new RecursoDTO();
        recurso.recursoId = id;
        recurso.quantidade = quantidade;
        return recurso;
    }
}
