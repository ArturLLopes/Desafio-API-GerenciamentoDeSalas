package com.projeto.APIGerenciamento.domain;

public enum StatusReserva {
    ATIVA,
    CANCELADA;

    public boolean podeSerCancelada() {
        return this == ATIVA;
    }
}
