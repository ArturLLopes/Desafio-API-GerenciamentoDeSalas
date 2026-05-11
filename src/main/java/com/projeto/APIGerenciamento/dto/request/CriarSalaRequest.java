package com.projeto.APIGerenciamento.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CriarSalaRequest(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @Min(value = 1, message = "Capacidade mínima é 1")
        int capacidade,
        String localizacao   // opcional
        ) {}
