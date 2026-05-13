package com.projeto.APIGerenciamento.domain;

import com.projeto.APIGerenciamento.dto.request.CriarSalaRequest;
import com.projeto.APIGerenciamento.exception.RecursoNaoEncontradoException;
import com.projeto.APIGerenciamento.exception.RegraDeNegocioException;
import com.projeto.APIGerenciamento.repository.SalaRepository;
import com.projeto.APIGerenciamento.service.SalaService;
import com.projeto.APIGerenciamento.util.TestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalaService — regras de negócio")
class SalaServiceTest {

    @Mock
    private SalaRepository salaRepository;
    @InjectMocks
    private SalaService salaService;

    @Nested
    @DisplayName("criarSala")
    class CriarSala {

        @Test
        @DisplayName("cria sala quando nome ainda não existe")
        void criaComSucesso() {
            CriarSalaRequest request = new CriarSalaRequest(
                    "Nova Sala", 8, "Bloco C");
            Sala salva = new Sala("Nova Sala", 8, "Bloco C");

            given(salaRepository.existsByNomeIgnoreCase("Nova Sala"))
                    .willReturn(false);
            given(salaRepository.save(any(Sala.class))).willReturn(salva);

            Sala resultado = salaService.criar(request);

            assertThat(resultado.getNome()).isEqualTo("Nova Sala");
            assertThat(resultado.getCapacidade()).isEqualTo(8);
            assertThat(resultado.isAtiva()).isTrue();
        }

        @Test
        @DisplayName("lança RegraDeNegocioException quando nome já existe")
        void rejeitaNomeDuplicado() {
            CriarSalaRequest request = new CriarSalaRequest(
                    "Sala Criativa", 10, "Bloco A");

            given(salaRepository.existsByNomeIgnoreCase("Sala Criativa"))
                    .willReturn(true);

            assertThatThrownBy(() -> salaService.criar(request))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("Sala Criativa");

            then(salaRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("desativarSala")
    class DesativarSala {

        @Test
        @DisplayName("desativa sala ativa com sucesso")
        void desativaComSucesso() {
            Sala sala = TestFactory.salaCriativa();
            TestFactory.setId(sala, 1L);

            given(salaRepository.findById(1L)).willReturn(Optional.of(sala));
            given(salaRepository.save(sala)).willReturn(sala);

            salaService.desativar(1L);

            assertThat(sala.isAtiva()).isFalse();
            then(salaRepository).should(times(1)).save(sala);
        }

        @Test
        @DisplayName("lança RecursoNaoEncontradoException para id inexistente")
        void lancaExcecaoParaIdInexistente() {
            given(salaRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> salaService.desativar(99L))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }
}