package com.projeto.APIGerenciamento.dto.response;

import com.projeto.APIGerenciamento.domain.Reserva;

import java.time.LocalDateTime;

public record ReservaResponse(
        Long id,
        Long salaId,
        String salaNome,
        Long usuarioId,
        String usuarioNome,
        LocalDateTime inicio,
        LocalDateTime fim,
        String status,
        String motivo,
        LocalDateTime criadaEm
) {
    public static ReservaResponse de(Reserva r) {
        return new ReservaResponse(
                r.getId(),
                r.getSala().getId(),
                r.getSala().getNome(),
                r.getUsuario().getId(),
                r.getUsuario().getNome(),
                r.getInicio(),
                r.getFim(),
                r.getStatus().name(),
                r.getMotivo(),
                r.getCriadaEm()
        );
    }
}