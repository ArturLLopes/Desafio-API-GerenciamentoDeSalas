package com.projeto.APIGerenciamento.controller;

import com.projeto.APIGerenciamento.dto.request.CriarReservaRequest;
import com.projeto.APIGerenciamento.dto.response.ReservaResponse;
import com.projeto.APIGerenciamento.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @GetMapping("/sala/{salaId}")
    public List<ReservaResponse> listarPorSala(@PathVariable Long salaId) {
        return reservaService.listarPorSala(salaId)
                .stream().map(ReservaResponse::de).toList();
    }

    @GetMapping("/{id}")
    public ReservaResponse buscarPorId(@PathVariable Long id) {
        return ReservaResponse.de(reservaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> criar(
            @RequestBody @Valid CriarReservaRequest request) {

        ReservaResponse response = ReservaResponse.de(
                reservaService.criar(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // PATCH semântico — muda apenas o status para CANCELADA
    @PatchMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@PathVariable Long id) {
        return ReservaResponse.de(reservaService.cancelar(id));
    }
}