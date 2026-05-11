package com.projeto.APIGerenciamento.repository;


import com.projeto.APIGerenciamento.domain.Sala;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SalaRepository extends JpaRepository<Sala, Long> {

    List<Sala> findByAtivaTrue();

    Page<Sala> findByAtivaTrue(Pageable pageable);

    Optional<Sala> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);

    // Busca salas com capacidade mínima — útil para filtrar por número de participantes
    @Query("SELECT s FROM Sala s WHERE s.ativa = true AND s.capacidade >= :minCapacidade")
    List<Sala> findAtivasComCapacidadeMinima(int minCapacidade);
}