// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.infra.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.verx.cashflow.lancamentos.application.CriarLancamentoService;
import com.verx.cashflow.lancamentos.domain.Lancamento;

import jakarta.validation.Valid;

/**
 * Adapter HTTP da escrita: {@code POST /lancamentos}.
 */
@RestController
@RequestMapping("/lancamentos")
public class LancamentoController {

    private final CriarLancamentoService service;

    public LancamentoController(CriarLancamentoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<LancamentoResponse> criar(@Valid @RequestBody LancamentoRequest request) {
        Lancamento lancamento = service.criar(
                request.tipo(), request.valor(), request.data(), request.descricao());
        URI location = URI.create("/lancamentos/" + lancamento.id());
        return ResponseEntity.created(location).body(LancamentoResponse.from(lancamento));
    }
}
