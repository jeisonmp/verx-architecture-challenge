# Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
# All rights reserved.
# PROPRIETARY AND CONFIDENTIAL.
# No part of this software may be reproduced, distributed, or transmitted
# in any form or by any means without express written permission.

# ---------------------------------------------------------------------------
# Configuração por ambiente — lida de environments/<env>.yaml (ADR-0012 dec5).
#
# `file()` + `yamldecode()` são resolvidos em plan/apply (não em validate).
# Um arquivo por ambiente; o topo do YAML é a config daquele env.
# ---------------------------------------------------------------------------
locals {
  env = yamldecode(file("${path.module}/environments/${var.environment}.yaml"))

  # RDS (ADR-0004) — flags FinOps que variam por ambiente
  rds_multi_az             = local.env.rds.multi_az
  rds_performance_insights = local.env.rds.performance_insights
  rds_backup_retention     = local.env.rds.backup_retention_days
  rds_skip_final_snapshot  = local.env.rds.skip_final_snapshot
  rds_stop_start           = local.env.rds.stop_start # ADR-0014 — stop/start 10h/dia (dev)
  nat_stop_start           = local.env.nat.stop_start # ADR-0014 — stop/start 10h/dia (dev)

  # ECS (ADR-0010)
  ecs_container_insights = local.env.ecs.container_insights

  # Observabilidade (ADR-0011)
  log_retention_days = local.env.logs.retention_days
}
