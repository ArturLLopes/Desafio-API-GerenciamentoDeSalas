package com.projeto.APIGerenciamento.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CriarReservaRequest(

        @NotNull(message = "ID da sala é obrigatório")
        Long salaId,

        @NotNull(message = "ID do usuário é obrigatório")
        Long usuarioId,

        @NotNull(message = "Horário de início é obrigatório")
        @Future(message = "Início deve ser no futuro")
        LocalDateTime inicio,

        @NotNull(message = "Horário de fim é obrigatório")
        LocalDateTime fim,

        String motivo   // opcional
) {}