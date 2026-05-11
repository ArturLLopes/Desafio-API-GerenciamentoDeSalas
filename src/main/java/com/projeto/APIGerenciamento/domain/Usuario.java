package com.projeto.APIGerenciamento.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuarios")
@Getter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Column(nullable = false)
    private String nome;

    // Email único — será a chave de identificação natural
    @Email(message = "Email inválido")
    @NotBlank(message = "Email é obrigatório")
    @Column(nullable = false, unique = true)
    private String email;

    // Departamento opcional — útil para relatórios e filtros futuros
    private String departamento;

    public Usuario(String nome, String email, String departamento) {
        this.nome = nome;
        this.email = email;
        this.departamento = departamento;
    }

    public void atualizar(String nome, String departamento) {
        this.nome = nome;
        this.departamento = departamento;
    }
}