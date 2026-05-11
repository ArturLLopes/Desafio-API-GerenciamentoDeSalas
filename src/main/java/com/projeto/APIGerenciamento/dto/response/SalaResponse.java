package com.projeto.APIGerenciamento.dto.response;

import com.projeto.APIGerenciamento.domain.Sala;

public record SalaResponse(
        Long id,
        String nome,
        int capacidade,
        String localizacao,
        boolean ativa
) {
    // Factory method — a conversão fica no DTO, não no controller nem no service
    public static SalaResponse de(Sala sala) {
        return new SalaResponse(
                sala.getId(),
                sala.getNome(),
                sala.getCapacidade(),
                sala.getLocalizacao(),
                sala.isAtiva()
        );
    }
}