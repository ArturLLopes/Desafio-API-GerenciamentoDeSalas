package com.projeto.APIGerenciamento.dto.response;

import com.projeto.APIGerenciamento.domain.Usuario;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        String departamento
) {
    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(
                u.getId(), u.getNome(), u.getEmail(), u.getDepartamento());
    }
}