package com.projeto.APIGerenciamento.domain;

import com.projeto.APIGerenciamento.dto.request.CriarReservaRequest;
import com.projeto.APIGerenciamento.exception.ConflitoDeReservaException;
import com.projeto.APIGerenciamento.exception.RecursoNaoEncontradoException;
import com.projeto.APIGerenciamento.exception.RegraDeNegocioException;
import com.projeto.APIGerenciamento.repository.ReservaRepository;
import com.projeto.APIGerenciamento.repository.SalaRepository;
import com.projeto.APIGerenciamento.repository.UsuarioRepository;
import com.projeto.APIGerenciamento.service.ReservaService;
import com.projeto.APIGerenciamento.util.TestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.projeto.APIGerenciamento.util.TestFactory.BASE;
import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservaService — regras de negócio")
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock private SalaRepository salaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ReservaService reservaService;

    private Sala sala;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        sala    = TestFactory.salaCriativa();
        usuario = TestFactory.usuarioAna();
        TestFactory.setId(sala,    1L);
        TestFactory.setId(usuario, 1L);
    }

    // ────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("criarReserva — caminho feliz")
    class CriarReservaFeliz {

        @Test
        @DisplayName("cria reserva quando sala livre no horário solicitado")
        void criaQuandoSalaDisponivel() {
            // GIVEN
            CriarReservaRequest request = requestValido(BASE, BASE.plusHours(2));

            given(salaRepository.findById(1L)).willReturn(Optional.of(sala));
            given(usuarioRepository.findById(1L)).willReturn(Optional.of(usuario));
            given(reservaRepository.findConflitantes(
                    eq(1L),
                    eq(BASE),
                    eq(BASE.plusHours(2)),
                    eq(StatusReserva.ATIVA)))
                    .willReturn(List.of());

            Reserva reservaSalva = new Reserva(sala, usuario,
                    BASE, BASE.plusHours(2), "Reunião");
            given(reservaRepository.save(any(Reserva.class)))
                    .willReturn(reservaSalva);

            // WHEN
            Reserva resultado = reservaService.criar(request);

            // THEN
            assertThat(resultado.getInicio()).isEqualTo(BASE);
            assertThat(resultado.getFim()).isEqualTo(BASE.plusHours(2));
            assertThat(resultado.getStatus()).isEqualTo(StatusReserva.ATIVA);

            then(reservaRepository).should(times(1)).save(any(Reserva.class));
        }

        @Test
        @DisplayName("reserva pode começar exatamente quando outra termina")
        void criaQuandoInicioIgualAoFimDeOutra() {
            // GIVEN — reserva existente termina às 11:00; nova começa às 11:00
            CriarReservaRequest request = requestValido(
                    BASE.plusHours(2), BASE.plusHours(4));

            given(salaRepository.findById(1L)).willReturn(Optional.of(sala));
            given(usuarioRepository.findById(1L)).willReturn(Optional.of(usuario));
            given(reservaRepository.findConflitantes(any(), any(), any(), any()))
                    .willReturn(List.of()); // query retorna vazio — borda livre

            Reserva reservaSalva = new Reserva(sala, usuario,
                    BASE.plusHours(2), BASE.plusHours(4), null);
            given(reservaRepository.save(any())).willReturn(reservaSalva);

            // WHEN / THEN — não lança exceção
            assertThatNoException().isThrownBy(() -> reservaService.criar(request));
        }
    }

    // ────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("criarReserva — conflito de horário")
    class CriarReservaConflito {

        @Test
        @DisplayName("lança ConflitoDeReservaException quando há sobreposição")
        void lancaExcecaoComConflito() {
            // GIVEN — o repository retorna uma reserva conflitante
            CriarReservaRequest request = requestValido(
                    BASE.plusHours(1), BASE.plusHours(3));

            Reserva conflitante = new Reserva(sala, usuario,
                    BASE, BASE.plusHours(2), "Existente");

            given(salaRepository.findById(1L)).willReturn(Optional.of(sala));
            given(usuarioRepository.findById(1L)).willReturn(Optional.of(usuario));
            given(reservaRepository.findConflitantes(any(), any(), any(), any()))
                    .willReturn(List.of(conflitante));

            // WHEN / THEN
            assertThatThrownBy(() -> reservaService.criar(request))
                    .isInstanceOf(ConflitoDeReservaException.class)
                    .hasMessageContaining("Sala Criativa");

            // O save nunca deve ser chamado quando há conflito
            then(reservaRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("não verifica conflito se sala não existe — falha antes")
        void naoChecaConflitoSeSalaInexistente() {
            CriarReservaRequest request = requestValido(BASE, BASE.plusHours(2));

            given(salaRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reservaService.criar(request))
                    .isInstanceOf(RecursoNaoEncontradoException.class);

            then(reservaRepository).should(never()).findConflitantes(
                    any(), any(), any(), any());
        }
    }

    // ────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("criarReserva — validações de entrada")
    class CriarReservaValidacoes {

        @Test
        @DisplayName("lança RegraDeNegocioException quando início >= fim")
        void rejeitaInicioMaiorOuIgualAoFim() {
            CriarReservaRequest request = requestValido(
                    BASE.plusHours(2), BASE); // inicio > fim

            given(salaRepository.findById(1L)).willReturn(Optional.of(sala));
            given(usuarioRepository.findById(1L)).willReturn(Optional.of(usuario));

            assertThatThrownBy(() -> reservaService.criar(request))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining(("anterior ao horário de fim"));
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException quando usuário não existe")
        void rejeitaUsuarioInexistente() {
            CriarReservaRequest request = requestValido(BASE, BASE.plusHours(2));

            given(salaRepository.findById(1L)).willReturn(Optional.of(sala));
            given(usuarioRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reservaService.criar(request))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("Usuário não encontrado");
        }
    }

    // ────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("cancelarReserva")
    class CancelarReserva {

        @Test
        @DisplayName("cancela reserva ativa com sucesso")
        void cancelaReservaAtiva() {
            // GIVEN
            Reserva reserva = new Reserva(sala, usuario,
                    BASE, BASE.plusHours(2), "Reunião");
            TestFactory.setId(reserva, 42L);

            given(reservaRepository.findById(42L)).willReturn(Optional.of(reserva));
            given(reservaRepository.save(reserva)).willReturn(reserva);

            // WHEN
            Reserva cancelada = reservaService.cancelar(42L);

            // THEN
            assertThat(cancelada.getStatus()).isEqualTo(StatusReserva.CANCELADA);
            then(reservaRepository).should(times(1)).save(reserva);
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException quando reserva não existe")
        void lancaExcecaoSeReservaNaoExiste() {
            given(reservaRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reservaService.cancelar(99L))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("lança IllegalStateException ao cancelar reserva já cancelada")
        void naoPermiteCancelarReservaCancelada() {
            Reserva reserva = new Reserva(sala, usuario,
                    BASE, BASE.plusHours(2), null);
            reserva.cancelar(); // já cancelada
            TestFactory.setId(reserva, 5L);

            given(reservaRepository.findById(5L)).willReturn(Optional.of(reserva));

            assertThatThrownBy(() -> reservaService.cancelar(5L))
                    .isInstanceOf(IllegalStateException.class);

            // O save não deve ser chamado se o domínio rejeitou
            then(reservaRepository).should(never()).save(any());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private CriarReservaRequest requestValido(LocalDateTime inicio,
                                              LocalDateTime fim) {
        return new CriarReservaRequest(1L, 1L, inicio, fim, "Reunião");
    }
}