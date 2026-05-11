package com.projeto.APIGerenciamento.service;

import com.projeto.APIGerenciamento.domain.Reserva;
import com.projeto.APIGerenciamento.domain.Sala;
import com.projeto.APIGerenciamento.domain.StatusReserva;
import com.projeto.APIGerenciamento.domain.Usuario;
import com.projeto.APIGerenciamento.dto.request.CriarReservaRequest;
import com.projeto.APIGerenciamento.exception.ConflitoDeReservaException;
import com.projeto.APIGerenciamento.exception.RecursoNaoEncontradoException;
import com.projeto.APIGerenciamento.exception.RegraDeNegocioException;
import com.projeto.APIGerenciamento.repository.ReservaRepository;
import com.projeto.APIGerenciamento.repository.SalaRepository;
import com.projeto.APIGerenciamento.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final SalaRepository salaRepository;
    private final UsuarioRepository usuarioRepository;

    public List<Reserva> listarPorSala(Long salaId) {
        return reservaRepository.findBySalaIdOrderByInicioAsc(salaId);
    }

    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Reserva não encontrada com id: " + id));
    }

    @Transactional
    public Reserva criar(CriarReservaRequest request) {

        // 1. Carrega e valida os recursos referenciados
        Sala sala = salaRepository.findById(request.salaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Sala não encontrada com id: " + request.salaId()));

        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário não encontrado com id: " + request.usuarioId()));

        // 2. Valida que início < fim
        if (!request.inicio().isBefore(request.fim())) {
            throw new RegraDeNegocioException(
                    "Horário de início deve ser anterior ao horário de fim.");
        }

        // 3. Verifica conflito de horário (apenas reservas ATIVAS)
        List<Reserva> conflitantes = reservaRepository.findConflitantes(
                sala.getId(), request.inicio(), request.fim(), StatusReserva.ATIVA);

        if (!conflitantes.isEmpty()) {
            Reserva conflito = conflitantes.get(0);
            throw new ConflitoDeReservaException(String.format(
                    "A sala '%s' já possui reserva das %s às %s nesse horário.",
                    sala.getNome(), conflito.getInicio(), conflito.getFim()));
        }

        // 4. Cria — validações de domínio (sala ativa, intervalo) disparam no construtor
        Reserva reserva = new Reserva(sala, usuario,
                request.inicio(), request.fim(),
                request.motivo());
        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva cancelar(Long id) {
        Reserva reserva = buscarPorId(id);
        reserva.cancelar(); // lança IllegalStateException se já cancelada
        return reservaRepository.save(reserva);
    }
}