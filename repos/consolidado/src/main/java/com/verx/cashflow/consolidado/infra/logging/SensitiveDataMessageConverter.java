// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

package com.verx.cashflow.consolidado.infra.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Conversion word Logback {@code %maskedMsg}: aplica {@link SensitiveDataMasker}
 * à mensagem formatada antes de serializá-la em JSON (ADR-0011, LGPD §2.1).
 *
 * <p>Registrado em {@code logback-spring.xml} via {@code <conversionRule>}.
 */
public class SensitiveDataMessageConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        return SensitiveDataMasker.mask(super.convert(event));
    }
}
