package com.projeto.APIGerenciamento.controller;


import com.projeto.APIGerenciamento.dto.request.CriarUsuarioRequest;
import com.projeto.APIGerenciamento.dto.response.UsuarioResponse;
import com.projeto.APIGerenciamento.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public Page<UsuarioResponse> listar(
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {

        return usuarioService.listar(PageRequest.of(pagina, tamanho))
                .map(UsuarioResponse::de);
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscarPorId(@PathVariable Long id) {
        return UsuarioResponse.de(usuarioService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(
            @RequestBody @Valid CriarUsuarioRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(UsuarioResponse.de(usuarioService.criar(request)));
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(
            @PathVariable Long id,
            @RequestBody @Valid CriarUsuarioRequest request) {

        return UsuarioResponse.de(usuarioService.atualizar(id, request));
    }
}