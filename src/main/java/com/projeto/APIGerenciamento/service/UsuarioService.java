package com.projeto.APIGerenciamento.service;

import com.projeto.APIGerenciamento.domain.Usuario;
import com.projeto.APIGerenciamento.dto.request.CriarUsuarioRequest;
import com.projeto.APIGerenciamento.exception.RecursoNaoEncontradoException;
import com.projeto.APIGerenciamento.exception.RegraDeNegocioException;
import com.projeto.APIGerenciamento.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Page<Usuario> listar(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário não encontrado: " + id));
    }

    @Transactional
    public Usuario criar(CriarUsuarioRequest request) {
        if (usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            throw new RegraDeNegocioException(
                    "Já existe um usuário com o email: " + request.email());
        }
        return usuarioRepository.save(
                new Usuario(request.nome(), request.email(), request.departamento()));
    }

    @Transactional
    public Usuario atualizar(Long id, CriarUsuarioRequest request) {
        Usuario usuario = buscarPorId(id);
        usuario.atualizar(request.nome(), request.departamento());
        return usuarioRepository.save(usuario);
    }
}