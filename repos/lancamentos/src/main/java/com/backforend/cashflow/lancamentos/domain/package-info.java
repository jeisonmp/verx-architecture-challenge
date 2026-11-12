// Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
// All rights reserved.
// PROPRIETARY AND CONFIDENTIAL.
// No part of this software may be reproduced, distributed, or transmitted
// in any form or by any means without express written permission.

/**
 * Domínio — modelo de negócio puro do microsserviço de escrita.
 *
 * <p>As regras de lançamento e o Transactional Outbox vivem aqui, sem
 * dependência de framework (Spring/JDBC/Kafka). A comunicação com o exterior é
 * declarada como contratos (portas) em {@code application} e implementada em
 * {@code infra}.
 */
package com.backforend.cashflow.lancamentos.domain;
