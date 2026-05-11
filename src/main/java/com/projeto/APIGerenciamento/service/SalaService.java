package com.projeto.APIGerenciamento.service;

import com.projeto.APIGerenciamento.domain.Sala;
import com.projeto.APIGerenciamento.dto.request.CriarSalaRequest;
import com.projeto.APIGerenciamento.exception.RecursoNaoEncontradoException;
import com.projeto.APIGerenciamento.exception.RegraDeNegocioException;
import com.projeto.APIGerenciamento.repository.SalaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalaService {

    private final SalaRepository salaRepository;

    public List<Sala> listarAtivas() {
        return salaRepository.findByAtivaTrue();
    }

    public List<Sala> listarTodas() {
        return salaRepository.findAll();
    }

    public Sala buscarPorId(Long id) {
        return salaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Sala não encontrada com id: " + id));
    }

    @Transactional
    public Sala criar(CriarSalaRequest request) {
        if (salaRepository.existsByNome(request.nome())) {
            throw new RegraDeNegocioException(
                    "Já existe uma sala com o nome: " + request.nome());
        }
        Sala sala = new Sala(request.nome(), request.capacidade(),
                request.localizacao());
        return salaRepository.save(sala);
    }

    @Transactional
    public Sala atualizar(Long id, CriarSalaRequest request) {
        Sala sala = buscarPorId(id);
        sala.atualizar(request.nome(), request.capacidade(),
                request.localizacao());
        return salaRepository.save(sala);
    }

    @Transactional
    public void desativar(Long id) {
        Sala sala = buscarPorId(id);
        sala.desativar();
        salaRepository.save(sala);
    }
}