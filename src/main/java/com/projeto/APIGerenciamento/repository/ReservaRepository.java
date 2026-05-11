package com.projeto.APIGerenciamento.repository;

import com.projeto.APIGerenciamento.domain.Reserva;
import com.projeto.APIGerenciamento.domain.StatusReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findBySalaIdOrderByInicioAsc(Long salaId);

    List<Reserva> findByUsuarioIdOrderByInicioAsc(Long usuarioId);

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
}