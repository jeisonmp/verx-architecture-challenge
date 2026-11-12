// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.lancamentos.contract;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;

/**
 * Canal em memória (test-only) que substitui o broker Kafka no teste CDC do
 * produtor (A).
 *
 * <p>No Spring Cloud Contract 4.x o {@code StubMessages} resolve o destino do
 * contrato pelo <em>bean name</em> ({@code context.getBean("lancamento.criado",
 * PollableChannel.class)}). Este helper implementa {@link PollableChannel} (que
 * estende {@code MessageChannel}) sobre uma {@link BlockingQueue}, permitindo o
 * {@code send}/{@code receive} síncrono exigido pelo contrato
 * {@code lancamento-criado.groovy} sem broker real e sem adicionar
 * {@code spring-integration-core} (YAGNI — ADR-0015).
 */
public final class LancamentoCriadoChannel implements PollableChannel {

    private final BlockingQueue<Message<?>> queue = new LinkedBlockingQueue<>();

    @Override
    public boolean send(Message<?> message) {
        return this.queue.offer(message);
    }

    @Override
    public boolean send(Message<?> message, long timeout) {
        return this.queue.offer(message);
    }

    @Override
    public Message<?> receive() {
        return this.queue.poll();
    }

    @Override
    public Message<?> receive(long timeout) {
        try {
            return this.queue.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
