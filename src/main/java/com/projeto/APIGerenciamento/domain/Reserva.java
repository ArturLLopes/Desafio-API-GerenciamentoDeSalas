package com.projeto.APIGerenciamento.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservas")
@Getter
@NoArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sala reservada — obrigatório, nunca nulo
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    // Usuário responsável pela reserva
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Intervalo semiaberto: [inicio, fim)
    // Início incluído, fim excluído — fim igual ao início de outra reserva é PERMITIDO
    @NotNull
    @Column(nullable = false)
    private LocalDateTime inicio;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime fim;

    // Motivo opcional — melhora rastreabilidade e relatórios
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusReserva status = StatusReserva.ATIVA;

    // Momento exato da criação — auditoria básica
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();

    // ─── Construtor com validações de domínio ───────────────────────────

    public Reserva(Sala sala, Usuario usuario,
                   LocalDateTime inicio, LocalDateTime fim,
                   String motivo) {

        validarSalaAtiva(sala);
        validarIntervalo(inicio, fim);

        this.sala = sala;
        this.usuario = usuario;
        this.inicio = inicio;
        this.fim = fim;
        this.motivo = motivo;
    }

    // ─── Regras de domínio ───────────────────────────────────────────────

    /**
     * Detecta sobreposição com outra reserva usando intervalo semiaberto [inicio, fim).
     *
     * Exemplos-limite:
     *   Reserva A: 09:00 → 10:00
     *   Reserva B: 10:00 → 11:00  → NÃO conflita (fim de A == início de B)
     *   Reserva C: 09:30 → 10:30  → CONFLITA
     *   Reserva D: 08:00 → 09:00  → NÃO conflita (fim de D == início de A)
     */
    public boolean conflitaCom(Reserva outra) {
        // Duas reservas se sobrepõem se cada uma começa antes do fim da outra
        return this.inicio.isBefore(outra.fim)
                && outra.inicio.isBefore(this.fim);
    }

    /**
     * Cancela a reserva. Lança exceção se já estiver cancelada.
     */
    public void cancelar() {
        if (!status.podeSerCancelada()) {
            throw new IllegalStateException(
                    "Reserva já está cancelada e não pode ser cancelada novamente."
            );
        }
        this.status = StatusReserva.CANCELADA;
    }

    /**
     * Indica se esta reserva deve participar de verificações de conflito.
     * Reservas canceladas são invisíveis para a checagem.
     */
    public boolean estaAtiva() {
        return this.status == StatusReserva.ATIVA;
    }

    // ─── Validações internas ─────────────────────────────────────────────

    private void validarSalaAtiva(Sala sala) {
        if (!sala.isAtiva()) {
            throw new IllegalArgumentException(
                    "Não é possível reservar a sala '" + sala.getNome() +
                            "' pois ela está inativa."
            );
        }
    }

    private void validarIntervalo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Início e fim são obrigatórios.");
        }
        if (!inicio.isBefore(fim)) {
            throw new IllegalArgumentException(
                    "O horário de início deve ser anterior ao horário de fim. " +
                            "Recebido: início=" + inicio + ", fim=" + fim
            );
        }
    }
}