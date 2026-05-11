package com.projeto.APIGerenciamento.exception;

public class ConflitoDeReservaException extends RuntimeException {
    public ConflitoDeReservaException(String mensagem) {
        super(mensagem);
    }
}