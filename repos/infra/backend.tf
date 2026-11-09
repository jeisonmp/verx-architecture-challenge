# Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
# All rights reserved.
# PROPRIETARY AND CONFIDENTIAL.
# No part of this software may be reproduced, distributed, or transmitted
# in any form or by any means without express written permission.

# ---------------------------------------------------------------------------
# State remoto com locking nativo (Terraform 1.10+).
#
# `use_lockfile = true` usa S3 conditional writes (arquivo `.tflock` ao lado do
# estado) e elimina a tabela DynamoDB de lock. A partir do Terraform 1.11,
# `dynamodb_table` foi marcado como deprecated.
#
# Migração: `terraform init -reconfigure`.
# ---------------------------------------------------------------------------
terraform {
  backend "s3" {
    bucket       = var.state_bucket
    key          = "fluxo-caixa/terraform.tfstate"
    region       = var.aws_region
    encrypt      = true
    use_lockfile = true
  }
}
