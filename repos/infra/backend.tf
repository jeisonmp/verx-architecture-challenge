# Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
# All rights reserved.
# PROPRIETARY AND CONFIDENTIAL.
# No part of this software may be reproduced, distributed, or transmitted
# in any form or by any means without express written permission.

# ---------------------------------------------------------------------------
# State remoto S3 com locking nativo (Terraform 1.10+) — ADR-0012 dec2.
#
# `use_lockfile = true` usa S3 conditional writes (arquivo `.tflock` ao lado do
# estado) e elimina a tabela DynamoDB de lock. A partir do Terraform 1.11,
# `dynamodb_table` foi marcado como deprecated.
#
# Configuração PARCIAL: `bucket`/`region` são fornecidos no `terraform init`
# via `-backend-config` (o bloco backend NÃO aceita variáveis/locals):
#   terraform init \
#     -backend-config="bucket=<state-bucket>" \
#     -backend-config="region=us-east-1"
#
# `workspace_key_prefix = "env"` isola o estado por workspace (dev/qas/prd) —
# ADR-0012 dec5. Fluxo: `terraform workspace new dev` / `select dev`.
#
# Migração: `terraform init -reconfigure`.
# ---------------------------------------------------------------------------
terraform {
  backend "s3" {
    key                  = "fluxo-caixa/terraform.tfstate"
    encrypt              = true
    use_lockfile         = true
    workspace_key_prefix = "env"
  }
}
