package com.projeto.APIGerenciamento.domain;

import com.projeto.APIGerenciamento.util.TestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.projeto.APIGerenciamento.util.TestFactory.BASE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DisplayName("Reserva — regras de domínio")
class ReservaTest {

    private Sala sala;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        sala = TestFactory.salaCriativa();
        usuario = TestFactory.usuarioAna();
    }

    // ────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Construção e validações de entrada")
    class Construcao {

        @Test
        @DisplayName("cria reserva válida com campos corretos")
        void criaReservaValida() {
            Reserva reserva = new Reserva(sala, usuario,
                    BASE, BASE.plusHours(2), "Reunião");

            assertThat(reserva.getInicio()).isEqualTo(BASE);
            assertThat(reserva.getFim()).isEqualTo(BASE.plusHours(2));
            assertThat(reserva.getStatus()).isEqualTo(StatusReserva.ATIVA);
            assertThat(reserva.getMotivo()).isEqualTo("Reunião");
        }

        @Test
        @DisplayName("rejeita reserva em sala inativa")
        void rejeitaSalaInativa() {
            Sala inativa = TestFactory.salaInativa();

            assertThatThrownBy(() ->
                    new Reserva(inativa, usuario, BASE, BASE.plusHours(1), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inativa");
        }

        @Test
        @DisplayName("rejeita início igual ao fim")
        void rejeitaInicioIgualAoFim() {
            assertThatThrownBy(() ->
                    new Reserva(sala, usuario, BASE, BASE, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("anterior ao horário de fim");
        }

        @Test
        @DisplayName("rejeita início posterior ao fim")
        void rejeitaInicioDepoisDoFim() {
            assertThatThrownBy(() ->
                    new Reserva(sala, usuario, BASE.plusHours(2), BASE, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("anterior ao horário de fim");
        }

        @Test
        @DisplayName("rejeita início ou fim nulos")
        void rejeitaDataNula() {
            assertThatThrownBy(() ->
                    new Reserva(sala, usuario, null, BASE.plusHours(1), null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() ->
                    new Reserva(sala, usuario, BASE, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Regra de conflito — intervalo semiaberto [início, fim)")
    class Conflito {

        private Reserva referencia; // 09:00 – 11:00

        @BeforeEach
        void criaReferencia() {
            referencia = TestFactory.reservaDas9Ate11(sala, usuario);
        }

        @Test
        @DisplayName("detecta sobreposição total")
        void sobrepoeTotal() {
            // 08:00 – 12:00 engloba a referência inteiramente
            Reserva outra = new Reserva(sala, usuario,
                    BASE.minusHours(1), BASE.plusHours(3), null);

            assertThat(referencia.conflitaCom(outra)).isTrue();
        }

        @Test
        @DisplayName("detecta sobreposição parcial no início")
        void sobrepoeInicioInvadindo() {
            // 08:00 – 10:00 invade o início da referência
            Reserva outra = new Reserva(sala, usuario,
                    BASE.minusHours(1), BASE.plusHours(1), null);

            assertThat(referencia.conflitaCom(outra)).isTrue();
        }

        @Test
        @DisplayName("detecta sobreposição parcial no fim")
        void sorepoesFimInvadindo() {
            // 10:00 – 12:00 invade o fim da referência
            Reserva outra = new Reserva(sala, usuario,
                    BASE.plusHours(1), BASE.plusHours(3), null);

            assertThat(referencia.conflitaCom(outra)).isTrue();
        }

        @Test
        @DisplayName("detecta sobreposição de reserva interna")
        void sobrepoeInternaContida() {
            // 09:30 – 10:30 está contida na referência
            Reserva outra = new Reserva(sala, usuario,
                    BASE.plusMinutes(30), BASE.plusMinutes(90), null);

            assertThat(referencia.conflitaCom(outra)).isTrue();
        }

        @Test
        @DisplayName("permite reserva cujo início coincide com o fim da referência — borda livre")
        void bordaFimIgualInicioNaoConflita() {
            // 11:00 – 13:00: início exatamente no fim da referência → LIVRE
            Reserva outra = TestFactory.reservaDas11Ate13(sala, usuario);

            assertThat(referencia.conflitaCom(outra)).isFalse();
        }

        @Test
        @DisplayName("permite reserva cujo fim coincide com o início da referência — borda livre")
        void bordaInicioIgualFimNaoConflita() {
            // 07:00 – 09:00: fim exatamente no início da referência → LIVRE
            Reserva outra = new Reserva(sala, usuario,
                    BASE.minusHours(2), BASE, null);

            assertThat(referencia.conflitaCom(outra)).isFalse();
        }

        @Test
        @DisplayName("permite reserva completamente anterior")
        void anteriorNaoConflita() {
            // 07:00 – 08:30: termina antes da referência começar
            Reserva outra = new Reserva(sala, usuario,
                    BASE.minusHours(2), BASE.minusMinutes(30), null);

            assertThat(referencia.conflitaCom(outra)).isFalse();
        }

        @Test
        @DisplayName("permite reserva completamente posterior")
        void posteriorNaoConflita() {
            // 11:30 – 13:00: começa depois da referência terminar
            Reserva outra = new Reserva(sala, usuario,
                    BASE.plusMinutes(150), BASE.plusHours(4), null);

            assertThat(referencia.conflitaCom(outra)).isFalse();
        }

        @Test
        @DisplayName("conflito é simétrico — A conflita com B implica B conflita com A")
        void conflitoEhSimetrico() {
            Reserva outra = new Reserva(sala, usuario,
                    BASE.plusMinutes(30), BASE.plusHours(3), null);

            assertThat(referencia.conflitaCom(outra))
                    .isEqualTo(outra.conflitaCom(referencia));
        }
    }

    // ────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Cancelamento e transições de estado")
    class Cancelamento {

        @Test
        @DisplayName("cancela reserva ativa com sucesso")
        void cancelaReservaAtiva() {
            Reserva reserva = TestFactory.reservaDas9Ate11(sala, usuario);

            reserva.cancelar();

            assertThat(reserva.getStatus()).isEqualTo(StatusReserva.CANCELADA);
            assertThat(reserva.estaAtiva()).isFalse();
        }

        @Test
        @DisplayName("lança exceção ao cancelar reserva já cancelada")
        void naoPermiteCancelarDuasVezes() {
            Reserva reserva = TestFactory.reservaDas9Ate11(sala, usuario);
            reserva.cancelar();

            assertThatThrownBy(reserva::cancelar)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("já está cancelada");
        }

        @Test
        @DisplayName("reserva cancelada não é considerada ativa")
        void reservaCanceladaNaoEstaAtiva() {
            Reserva reserva = TestFactory.reservaDas9Ate11(sala, usuario);
            reserva.cancelar();

            assertThat(reserva.estaAtiva()).isFalse();
        }
    }
}