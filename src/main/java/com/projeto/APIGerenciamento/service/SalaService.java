package com.projeto.APIGerenciamento.service;

import com.projeto.APIGerenciamento.domain.Sala;
import com.projeto.APIGerenciamento.dto.request.CriarSalaRequest;
import com.projeto.APIGerenciamento.exception.RecursoNaoEncontradoException;
import com.projeto.APIGerenciamento.exception.RegraDeNegocioException;
import com.projeto.APIGerenciamento.repository.SalaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalaService {

    private final SalaRepository salaRepository;

    @Transactional(readOnly = true)
    public List<Sala> listarAtivas() {
        return salaRepository.findByAtivaTrue();
    }

    @Transactional(readOnly = true)
    public List<Sala> listarTodas() {
        return salaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Sala buscarPorId(Long id) {
        return salaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Sala não encontrada com id: " + id));
    }

    @Transactional
    public Sala criar(CriarSalaRequest request) {

        if (salaRepository.existsByNomeIgnoreCase(request.nome())) {
            throw new RegraDeNegocioException(
                    "Já existe uma sala com o nome: "
                            + request.nome()
            );
        }

        Sala sala = new Sala(
                request.nome(),
                request.capacidade(),
                request.localizacao()
        );

        Sala salva = salaRepository.save(sala);

        log.info("Sala criada com sucesso: id={}", salva.getId());

        return salva;
    }

    @Transactional
    public Sala atualizar(Long id,
                          CriarSalaRequest request) {

        Sala sala = buscarPorId(id);

        boolean nomeJaExiste =
                salaRepository
                        .existsByNomeIgnoreCaseAndIdNot(
                                request.nome(),
                                id
                        );

        if (nomeJaExiste) {
            throw new RegraDeNegocioException(
                    "Já existe outra sala com o nome: "
                            + request.nome()
            );
        }

        sala.atualizar(
                request.nome(),
                request.capacidade(),
                request.localizacao()
        );

        Sala atualizada = salaRepository.save(sala);

        log.info("Sala atualizada: id={}",
                atualizada.getId());

        return atualizada;
    }

    @Transactional
    public void desativar(Long id) {

        Sala sala = buscarPorId(id);

        sala.desativar();

        salaRepository.save(sala);

        log.info("Sala desativada: id={}", id);
    }
}