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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final SalaRepository salaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Page<Reserva> listarPorSala(Long salaId, Pageable pageable) {
        return reservaRepository.findBySalaIdOrderByInicioAsc(salaId, pageable);
    }

    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Reserva não encontrada com id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Reserva> listarPorSalaEPeriodo(Long salaId,
                                               LocalDateTime de,
                                               LocalDateTime ate) {

        if (!de.isBefore(ate)) {
            throw new RegraDeNegocioException(
                    "'de' deve ser anterior a 'ate'.");
        }

        return reservaRepository.findBySalaEPeriodo(salaId, de, ate);
    }

    @Transactional
    public Reserva criar(CriarReservaRequest request) {

        // 1. Buscar sala
        log.info("Criando reserva: sala={}, usuario={}, inicio={}, fim={}",
                request.salaId(), request.usuarioId(), request.inicio(), request.fim());

        Sala sala = salaRepository.findById(request.salaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Sala não encontrada com id: " + request.salaId()));

        // 2. Buscar usuário
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário não encontrado com id: " + request.usuarioId()));

        // 3. Validar período
        if (!request.inicio().isBefore(request.fim())) {
            throw new RegraDeNegocioException(
                    "Horário de início deve ser anterior ao horário de fim.");
        }

        // 4. Verificar conflitos
        List<Reserva> conflitantes = reservaRepository.findConflitantes(
                sala.getId(),
                request.inicio(),
                request.fim(),
                StatusReserva.ATIVA
        );

        if (!conflitantes.isEmpty()) {
            Reserva conflito = conflitantes.get(0);

            throw new ConflitoDeReservaException(String.format(
                    "A sala '%s' já possui reserva das %s às %s.",
                    sala.getNome(),
                    conflito.getInicio(),
                    conflito.getFim()
            ));
        }

        // 5. Criar reserva
        Reserva reserva = new Reserva(
                sala,
                usuario,
                request.inicio(),
                request.fim(),
                request.motivo()
        );

        Reserva salva = reservaRepository.save(reserva);

        log.info("Reserva criada com sucesso: id={}", salva.getId());

        return salva;
    }

    // ── Cancelamento ───
    @Transactional
    public Reserva cancelar(Long id) {

        Reserva reserva = buscarPorId(id);

        reserva.cancelar();

        return reservaRepository.save(reserva);
    }

    // ── Listagem paginada geral ─────
    @Transactional(readOnly = true)
    public Page<Reserva> listarPorPeriodo(StatusReserva status,
                                          LocalDateTime de,
                                          LocalDateTime ate,
                                          Pageable pageable) {

        return reservaRepository.findByPeriodo(
                status,
                de,
                ate,
                pageable
        );
    }
}