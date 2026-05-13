package com.projeto.APIGerenciamento.util;

import com.projeto.APIGerenciamento.domain.Reserva;
import com.projeto.APIGerenciamento.domain.Sala;
import com.projeto.APIGerenciamento.domain.Usuario;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

//Uma classe central que cria objetos com estado válido.
// Evita repetir a construção em cada teste e torna os cenários mais legíveis:
public final class TestFactory {

    // Data de referência fixa — testes não dependem de LocalDateTime.now()
    public static final LocalDateTime BASE = LocalDateTime.of(2025, 9, 10, 9, 0);

    private TestFactory() {}

    public static Sala salaCriativa() {
        return new Sala("Sala Criativa", 10, "Bloco A");
    }

    public static Sala salaInativa() {
        Sala sala = new Sala("Sala Inativa", 5, "Bloco B");
        sala.desativar();
        return sala;
    }

    public static Usuario usuarioAna() {
        return new Usuario("Ana Lima", "ana@empresa.com", "TI");
    }

    public static Reserva reservaDas9Ate11(Sala sala, Usuario usuario) {
        return new Reserva(sala, usuario, BASE, BASE.plusHours(2), "Reunião");
    }

    public static Reserva reservaDas11Ate13(Sala sala, Usuario usuario) {
        return new Reserva(sala, usuario,
                BASE.plusHours(2), BASE.plusHours(4), "Treinamento");
    }

    // Helper para injetar IDs em entidades via reflection — simula o que o JPA faz
    public static void setId(Object objeto, Long id) {
        try {
            Field field = objeto.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(objeto, id);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao injetar id via reflection", e);
        }
    }
}