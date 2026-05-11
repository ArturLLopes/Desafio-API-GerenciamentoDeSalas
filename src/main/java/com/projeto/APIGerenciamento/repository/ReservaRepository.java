package com.projeto.APIGerenciamento.repository;

import com.projeto.APIGerenciamento.domain.Reserva;
import com.projeto.APIGerenciamento.domain.StatusReserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    Page<Reserva> findBySalaIdOrderByInicioAsc(Long salaId, Pageable pageable);

    Page<Reserva> findByUsuarioIdOrderByInicioAsc(Long usuarioId, Pageable pageable);

    List<Reserva> findBySalaIdAndStatusOrderByInicioAsc(Long salaId, StatusReserva status);


    // Detecta sobreposição: busca reservas ATIVAS na mesma sala cujo
    // intervalo [inicio, fim) colide com o intervalo solicitado.
    // A fórmula A.inicio < B.fim && B.inicio < A.fim é a implementação
    // direta do intervalo semiaberto definido no domínio.
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.sala.id = :salaId
          AND r.status = :status
          AND r.inicio < :fim
          AND r.fim > :inicio
    """)
    List<Reserva> findConflitantes(
            @Param("salaId")  Long salaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim")     LocalDateTime fim,
            @Param("status") StatusReserva status
    );

    // ── JOIN FETCH para evitar N+1 ─────────────────────────────────────
    // Sem JOIN FETCH: buscar 20 reservas + acessar sala/usuario em cada uma
    //   = 1 (reservas) + 20 (salas) + 20 (usuarios) = 41 queries
    // Com JOIN FETCH: 1 query com dois JOINs = 1 query total
    @Query("""
        SELECT r FROM Reserva r
        JOIN FETCH r.sala s
        JOIN FETCH r.usuario u
        WHERE r.id = :id
    """)
    Optional<Reserva> findByIdComDetalhes(@Param("id") Long id);

    @Query("""
        SELECT r FROM Reserva r
        JOIN FETCH r.sala
        JOIN FETCH r.usuario
        WHERE r.sala.id = :salaId
        ORDER BY r.inicio ASC
    """)
    List<Reserva> findBySalaComDetalhes(@Param("salaId") Long salaId);

    // ── Listagem por período — para relatórios e calendário ───────────
    @Query("""
        SELECT r FROM Reserva r
        JOIN FETCH r.sala
        JOIN FETCH r.usuario
        WHERE r.sala.id = :salaId
          AND r.status  = 'ATIVA'
          AND r.inicio >= :de
          AND r.inicio <  :ate
        ORDER BY r.inicio ASC
    """)
    List<Reserva> findBySalaEPeriodo(
            @Param("salaId") Long salaId,
            @Param("de")     LocalDateTime de,
            @Param("ate")    LocalDateTime ate
    );

    // Versão paginada — útil quando o intervalo é longo (meses, anos)
    @Query(
            value = """
            SELECT r FROM Reserva r
            JOIN FETCH r.sala
            JOIN FETCH r.usuario
            WHERE r.status = :status
              AND r.inicio >= :de
              AND r.inicio <  :ate
            ORDER BY r.inicio ASC
        """,
            // countQuery separado é obrigatório quando há JOIN FETCH + paginação
            countQuery = """
            SELECT COUNT(r) FROM Reserva r
            WHERE r.status = :status
              AND r.inicio >= :de
              AND r.inicio <  :ate
        """
    )
    Page<Reserva> findByPeriodo(
            @Param("status") StatusReserva status,
            @Param("de")     LocalDateTime de,
            @Param("ate")    LocalDateTime ate,
            Pageable pageable
    );


}