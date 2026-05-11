package com.projeto.APIGerenciamento.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "salas")
@Getter
@NoArgsConstructor
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nome visível para o usuário — não pode ser vazio
    @NotBlank(message = "Nome da sala é obrigatório")
    @Column(nullable = false, unique = true)
    private String nome;

    // Capacidade máxima de pessoas — deve ser ao menos 1
    @Min(value = 1, message = "Capacidade deve ser no mínimo 1")
    @Column(nullable = false)
    private int capacidade;

    // Sala inativa não aceita novas reservas — começa ativa por padrão
    @Column(nullable = false)
    private boolean ativa = true;

    // Localização física opcional — útil para buscas futuras
    private String localizacao;

    public Sala(String nome, int capacidade, String localizacao) {
        this.nome = nome;
        this.capacidade = capacidade;
        this.localizacao = localizacao;
    }

    // Métodos de domínio — comportamento junto dos dados
    public void desativar() {
        this.ativa = false;
    }

    public void ativar() {
        this.ativa = true;
    }

    public void atualizar(String nome, int capacidade, String localizacao) {
        this.nome = nome;
        this.capacidade = capacidade;
        this.localizacao = localizacao;
    }

}
