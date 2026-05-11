package com.projeto.APIGerenciamento.repository;


import com.projeto.APIGerenciamento.domain.Sala;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaRepository extends JpaRepository<Sala, Long> {
    List<Sala> findByAtivaTrue();
    boolean existsByNome(String nome);
}