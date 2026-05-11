package com.projeto.APIGerenciamento.controller;

import com.projeto.APIGerenciamento.domain.StatusReserva;
import com.projeto.APIGerenciamento.dto.request.CriarReservaRequest;
import com.projeto.APIGerenciamento.dto.response.ReservaResponse;
import com.projeto.APIGerenciamento.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @GetMapping("/sala/{salaId}")
    public Page<ReservaResponse> listarPorSala(
            @PathVariable Long salaId,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "20") int tamanho,
            @RequestParam(defaultValue = "inicio") String ordenarPor) {

        Pageable pageable = PageRequest.of(pagina, tamanho,
                Sort.by(ordenarPor).ascending());

        return reservaService.listarPorSala(salaId, pageable)
                .map(ReservaResponse::de);
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
    public ResponseEntity<ReservaResponse> cancelar(
            @PathVariable Long id) {

        ReservaResponse response =
                ReservaResponse.de(reservaService.cancelar(id));

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public Page<ReservaResponse> listarPorPeriodo(

            @RequestParam(required = false) StatusReserva status,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime de,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime ate,

            @RequestParam(defaultValue = "0")
            int pagina,

            @RequestParam(defaultValue = "20")
            int tamanho,

            @RequestParam(defaultValue = "inicio")
            String ordenarPor
    ) {

        Pageable pageable = PageRequest.of(
                pagina,
                tamanho,
                Sort.by(ordenarPor).ascending()
        );

        return reservaService
                .listarPorPeriodo(status, de, ate, pageable)
                .map(ReservaResponse::de);
    }
}