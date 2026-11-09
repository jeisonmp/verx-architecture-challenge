# Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
# All rights reserved.
# PROPRIETARY AND CONFIDENTIAL.
# No part of this software may be reproduced, distributed, or transmitted
# in any form or by any means without express written permission.

# ===========================================================================
# Rede — VPC + sub-redes privadas + security groups
# ===========================================================================
data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = { Name = "fluxo-caixa-${var.environment}" }
}

resource "aws_subnet" "private" {
  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(aws_vpc.main.cidr_block, 8, count.index + 1)
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = { Name = "fluxo-caixa-private-${count.index + 1}" }
}

resource "aws_security_group" "app" {
  name   = "fluxo-caixa-app-${var.environment}"
  vpc_id = aws_vpc.main.id

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "fluxo-caixa-app-${var.environment}" }
}

resource "aws_security_group" "rds" {
  name   = "fluxo-caixa-rds-${var.environment}"
  vpc_id = aws_vpc.main.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  tags = { Name = "fluxo-caixa-rds-${var.environment}" }
}

resource "aws_security_group" "msk" {
  name   = "fluxo-caixa-msk-${var.environment}"
  vpc_id = aws_vpc.main.id

  ingress {
    from_port       = 9098
    to_port         = 9098
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  tags = { Name = "fluxo-caixa-msk-${var.environment}" }
}

# ===========================================================================
# Criptografia em repouso — KMS (ADR-0007)
# ===========================================================================
resource "aws_kms_key" "main" {
  description             = "Chave KMS — criptografia em repouso (ADR-0007)"
  deletion_window_in_days = 7
  enable_key_rotation     = true
}

# ===========================================================================
# Banco de dados — RDS PostgreSQL (ADR-0004): escrita (A) + projeção (B)
# ===========================================================================
resource "aws_db_subnet_group" "main" {
  name       = "fluxo-caixa-${var.environment}"
  subnet_ids = aws_subnet.private[*].id

  tags = { Name = "fluxo-caixa-${var.environment}" }
}

resource "aws_db_instance" "lancamentos" {
  identifier     = "fluxo-caixa-lancamentos-${var.environment}"
  engine         = "postgres"
  engine_version = "16.3"
  instance_class = var.rds_instance_class

  db_name  = "lancamentos"
  username = "cashflow"
  password = var.db_password # injetado via Secrets Manager (ADR-0007)

  # RDS IAM auth (ADR-0007) — token de vida curta, sem segredo estático no serviço
  iam_database_authentication_enabled = true

  allocated_storage       = 20
  storage_encrypted       = true
  kms_key_id              = aws_kms_key.main.arn
  vpc_security_group_ids  = [aws_security_group.rds.id]
  db_subnet_group_name    = aws_db_subnet_group.main.name

  multi_az                    = false  # FinOps: dev single-AZ
  backup_retention_period     = 7
  skip_final_snapshot         = true
  performance_insights_enabled = false # FinOps: desligado em dev

  tags = { Name = "fluxo-caixa-lancamentos-${var.environment}" }
}

resource "aws_db_instance" "consolidado" {
  identifier     = "fluxo-caixa-consolidado-${var.environment}"
  engine         = "postgres"
  engine_version = "16.3"
  instance_class = var.rds_instance_class

  db_name  = "consolidado"
  username = "cashflow"
  password = var.db_password

  iam_database_authentication_enabled = true

  allocated_storage       = 20
  storage_encrypted       = true
  kms_key_id              = aws_kms_key.main.arn
  vpc_security_group_ids  = [aws_security_group.rds.id]
  db_subnet_group_name    = aws_db_subnet_group.main.name

  multi_az                    = false
  backup_retention_period     = 7
  skip_final_snapshot         = true
  performance_insights_enabled = false

  tags = { Name = "fluxo-caixa-consolidado-${var.environment}" }
}

# ===========================================================================
# Mensageria — MSK Serverless (ADR-0003) com IAM auth (ADR-0007)
# ===========================================================================
resource "aws_msk_serverlesscluster" "kafka" {
  cluster_name = "fluxo-caixa-${var.environment}"

  vpc_configs {
    subnet_ids         = aws_subnet.private[*].id
    security_group_ids = [aws_security_group.msk.id]
  }

  client_authentication {
    sasl {
      iam {
        enabled = true # MSK IAM auth — passwordless (ADR-0007)
      }
    }
  }

  # Em repouso: MSK Serverless usa KMS por padrão (ADR-0007).

  tags = { Name = "fluxo-caixa-${var.environment}" }
}

# ===========================================================================
# ECS Fargate (ADR-0010) — cluster + IAM + tasks/services (A e B)
# ===========================================================================
resource "aws_ecs_cluster" "main" {
  name = "fluxo-caixa-${var.environment}"

  setting {
    name  = "containerInsights"
    value = "disabled" # FinOps: desligado em dev
  }
}

resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/fluxo-caixa-${var.environment}"
  retention_in_days = 7
}

resource "aws_iam_role" "ecs_execution" {
  name = "fluxo-caixa-execution-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_execution" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "ecs_task" {
  name = "fluxo-caixa-task-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

# Task role — RDS IAM + MSK IAM (ADR-0007), acesso a dados sem segredo estático
resource "aws_iam_role_policy" "ecs_task" {
  name = "fluxo-caixa-data-access"
  role = aws_iam_role.ecs_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["rds-db:connect"]
        Resource = ["arn:aws:rds-db:${var.aws_region}:*:dbuser:*/cashflow"]
      },
      {
        Effect = "Allow"
        Action = [
          "kafka-cluster:Connect",
          "kafka-cluster:DescribeTopic",
          "kafka-cluster:ReadData",
          "kafka-cluster:WriteData"
        ]
        Resource = ["*"]
      }
    ]
  })
}

resource "aws_ecs_task_definition" "lancamentos" {
  family                   = "fluxo-caixa-lancamentos"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.fargate_cpu
  memory                   = var.fargate_memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name  = "lancamentos"
    image = "fluxo-caixa/lancamentos:latest"
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.app.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "lancamentos"
      }
    }
    portMappings = [{ containerPort = 8080 }]
  }])
}

resource "aws_ecs_task_definition" "consolidado" {
  family                   = "fluxo-caixa-consolidado"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.fargate_cpu
  memory                   = var.fargate_memory
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name  = "consolidado"
    image = "fluxo-caixa/consolidado:latest"
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.app.name
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "consolidado"
      }
    }
    portMappings = [{ containerPort = 8080 }]
  }])
}

resource "aws_ecs_service" "lancamentos" {
  name            = "lancamentos"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.lancamentos.arn
  desired_count   = var.ecs_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.app.id]
    assign_public_ip = false
  }

  tags = { Name = "fluxo-caixa-lancamentos-${var.environment}" }
}

resource "aws_ecs_service" "consolidado" {
  name            = "consolidado"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.consolidado.arn
  desired_count   = var.ecs_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.app.id]
    assign_public_ip = false
  }

  tags = { Name = "fluxo-caixa-consolidado-${var.environment}" }
}
