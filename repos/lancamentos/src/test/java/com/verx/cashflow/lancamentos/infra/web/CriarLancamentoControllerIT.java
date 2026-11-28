// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.infra.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.verx.cashflow.lancamentos.application.CriarLancamentoService;
import com.verx.cashflow.lancamentos.infra.persistence.LancamentoRepository;

/**
 * Integração do {@code POST /lancamentos} contra PostgreSQL real (Testcontainers):
 * validação, 201 + Location e o commit atômico de {@code lancamento} +
 * {@code outbox_event} (Transactional Outbox) — US-002.
 *
 * <p>{@code @Testcontainers(disabledWithoutDocker = true)} pula a classe quando o
 * daemon Docker não está disponível (runner BYOI sem socket); os unit tests seguem
 * verdes e esta integração roda no host/CI com Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
class CriarLancamentoControllerIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private Jdbi jdbi;
    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());

        Flyway.configure().dataSource(dataSource).load().migrate();
        jdbi = Jdbi.create(dataSource).installPlugin(new SqlObjectPlugin());
        LancamentoRepository repository = jdbi.onDemand(LancamentoRepository.class);

        objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        CriarLancamentoService service = new CriarLancamentoService(repository, objectMapper);
        LancamentoController controller = new LancamentoController(service);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void postLancamentoValidoRetorna201ComLocationECommitAtomico() throws Exception {
        String body = """
                {"tipo":"ENTRADA","valor":150.00,"data":"2026-11-21T10:00:00Z","descricao":"Venda — pagamento recebido"}
                """;

        MvcResult result = mockMvc.perform(post("/lancamentos")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.descricao").value("Venda — pagamento recebido"))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        String id = responseJson.get("id").asText();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/lancamentos/" + id);

        // AC-6: lancamento e outbox_event commitados juntos (mesma transação).
        List<Map<String, Object>> lancamentos = jdbi.withHandle(h -> h.createQuery(
                "SELECT id, tipo, valor, descricao FROM lancamento").mapToMap().list());
        assertThat(lancamentos).hasSize(1);
        assertThat(lancamentos.get(0).get("tipo")).isEqualTo("ENTRADA");

        List<Map<String, Object>> outbox = jdbi.withHandle(h -> h.createQuery(
                "SELECT aggregate_id, topic, schema_version, status FROM outbox_event").mapToMap().list());
        assertThat(outbox).hasSize(1);
        assertThat(outbox.get(0).get("topic")).isEqualTo("lancamento.criado");
        assertThat(outbox.get(0).get("schema_version")).isEqualTo(1);
        assertThat(outbox.get(0).get("status")).isEqualTo("PENDING");
        assertThat(outbox.get(0).get("aggregate_id").toString())
                .isEqualTo(lancamentos.get(0).get("id").toString());
    }

    @Test
    void postLancamentoComValorZeradoOuDescricaoVaziaRetorna400() throws Exception {
        String body = """
                {"tipo":"ENTRADA","valor":0,"data":"2026-11-21T10:00:00Z","descricao":""}
                """;

        mockMvc.perform(post("/lancamentos")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postLancamentoComTipoDesconhecidoRetorna400() throws Exception {
        String body = """
                {"tipo":"INVALIDO","valor":10.00,"data":"2026-11-21T10:00:00Z","descricao":"x"}
                """;

        mockMvc.perform(post("/lancamentos")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
