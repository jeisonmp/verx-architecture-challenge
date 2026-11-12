-- Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
-- All rights reserved.
-- PROPRIETARY AND CONFIDENTIAL.
-- No part of this software may be reproduced, distributed, or transmitted
-- in any form or by any means without express written permission.

-- Instância única com schemas lógicos por serviço (ADR-0013).
-- Executado apenas na primeira inicialização do volume do PostgreSQL.
CREATE SCHEMA IF NOT EXISTS lancamentos;
CREATE SCHEMA IF NOT EXISTS consolidado;
