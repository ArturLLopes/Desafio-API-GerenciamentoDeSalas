package com.projeto.APIGerenciamento.controller;


import com.projeto.APIGerenciamento.dto.request.CriarSalaRequest;
import com.projeto.APIGerenciamento.dto.response.SalaResponse;
import com.projeto.APIGerenciamento.service.SalaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/salas")
@RequiredArgsConstructor
public class SalaController {

    private final SalaService salaService;

    @GetMapping
    public List<SalaResponse> listar(
            @RequestParam(defaultValue = "true") boolean apenasAtivas) {

        var salas = apenasAtivas
                ? salaService.listarAtivas()
                : salaService.listarTodas();

        return salas.stream().map(SalaResponse::de).toList();
    }

    @GetMapping("/{id}")
    public SalaResponse buscarPorId(@PathVariable Long id) {
        return SalaResponse.de(salaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<SalaResponse> criar(
            @RequestBody @Valid CriarSalaRequest request) {

        SalaResponse response = SalaResponse.de(salaService.criar(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public SalaResponse atualizar(
            @PathVariable Long id,
            @RequestBody @Valid CriarSalaRequest request) {

        return SalaResponse.de(salaService.atualizar(id, request));
    }

    // DELETE lógico — desativa em vez de apagar para preservar histórico
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        salaService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}