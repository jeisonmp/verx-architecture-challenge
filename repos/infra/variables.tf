# Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
# All rights reserved.
# PROPRIETARY AND CONFIDENTIAL.
# No part of this software may be reproduced, distributed, or transmitted
# in any form or by any means without express written permission.

variable "aws_region" {
  description = "Região AWS"
  type        = string
  default     = "us-east-1"
}

variable "state_bucket" {
  description = "Bucket S3 do estado remoto (locking nativo via use_lockfile)"
  type        = string
}

variable "environment" {
  description = "Ambiente (dev / qas / prd)"
  type        = string
  default     = "dev"
}

variable "db_password" {
  description = "Senha master do RDS — injetada via Secrets Manager na implantação (ADR-0007)"
  type        = string
  sensitive   = true
}

variable "rds_instance_class" {
  description = "Classe da instância RDS (dev = db.t4g.micro, FinOps)"
  type        = string
  default     = "db.t4g.micro"
}

variable "fargate_cpu" {
  description = "CPU das tasks Fargate"
  type        = string
  default     = "256"
}

variable "fargate_memory" {
  description = "Memória das tasks Fargate"
  type        = string
  default     = "512"
}

variable "ecs_desired_count" {
  description = "Tasks desejadas (dev = 0 fora do horário — FinOps)"
  type        = number
  default     = 1
}
