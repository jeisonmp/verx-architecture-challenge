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
# Egress — NAT Instance t3.micro (ADR-0012 Q5)
#
# As sub-redes privadas (Fargate) precisam de saída à internet para puxar a
# imagem do ECR e enviar CloudWatch Logs/X-Ray. NAT Instance (free tier) em vez
# de NAT Gateway (~US$ 32/mês) — regra FinOps do motor (§6.3). Single point of
# failure aceitável em dev.
#
# Nota FinOps: on-demand 10h/dia (300h/mês < 750h free tier = US$ 0 por 12
# meses; após ≈ US$ 3,16/mês). Spot descartado (reclaim de ~2min derrubaria o
# egress). Janela alinhada ao RDS 10h/dia (rds_scheduler.tf, ADR-0014).
# ===========================================================================

data "aws_ssm_parameter" "nat_ami" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-minimal-kernel-x86_64"
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = { Name = "fluxo-caixa-igw-${var.environment}" }
}

resource "aws_subnet" "public" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(aws_vpc.main.cidr_block, 8, 0)
  availability_zone = data.aws_availability_zones.available.names[0]

  tags = { Name = "fluxo-caixa-public-1" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = { Name = "fluxo-caixa-public-${var.environment}" }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block           = "0.0.0.0/0"
    network_interface_id = aws_network_interface.nat.id
  }

  tags = { Name = "fluxo-caixa-private-${var.environment}" }
}

resource "aws_route_table_association" "private" {
  count          = 2
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}

resource "aws_security_group" "nat" {
  name   = "fluxo-caixa-nat-${var.environment}"
  vpc_id = aws_vpc.main.id

  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["10.0.0.0/16"] # tráfego das sub-redes privadas
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "fluxo-caixa-nat-${var.environment}" }
}

resource "aws_network_interface" "nat" {
  subnet_id         = aws_subnet.public.id
  private_ips       = ["10.0.0.10"] # IP privado fixo — sobrevive ao stop/start da NAT
  security_groups   = [aws_security_group.nat.id]
  source_dest_check = false         # habilita o forwarding (SNAT) — essencial para NAT

  tags = { Name = "fluxo-caixa-nat-eni-${var.environment}" }
}

resource "aws_eip" "nat" {
  domain = "vpc"

  tags = { Name = "fluxo-caixa-nat-eip-${var.environment}" }
}

resource "aws_instance" "nat" {
  ami           = data.aws_ssm_parameter.nat_ami.value
  instance_type = "t3.micro"

  network_interface {
    network_interface_id = aws_network_interface.nat.id
    device_index         = 0 # ENI primária fixa — sobrevive ao stop/start
  }

  root_block_device {
    encrypted  = true
    kms_key_id = aws_kms_key.main.arn
  }

  # cloud-config: `bootcmd` roda a CADA boot (não só no 1º) — essencial para o
  # stop/start da NAT reaplicar forwarding + masquerade após cada reboot.
  user_data = <<-EOF
    #cloud-config
    bootcmd:
      - echo 'net.ipv4.ip_forward = 1' > /etc/sysctl.d/99-nat.conf
      - sysctl -w net.ipv4.ip_forward=1
      - iptables -t nat -C POSTROUTING -s 10.0.0.0/16 -o eth0 -j MASQUERADE || iptables -t nat -A POSTROUTING -s 10.0.0.0/16 -o eth0 -j MASQUERADE
  EOF

  tags = { Name = "fluxo-caixa-nat-${var.environment}" }
}

resource "aws_eip_association" "nat" {
  allocation_id        = aws_eip.nat.id
  network_interface_id = aws_network_interface.nat.id
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
# Banco de dados — RDS PostgreSQL (ADR-0004 + ADR-0013)
# Instância ÚNICA em dev com 2 schemas lógicos (lancamentos + consolidado).
# O isolamento lógico do database-per-service é preservado no nível de schema
# (cada role RDS IAM só enxerga o próprio schema); a separação física era
# desnecessária a 50 req/s e o read side é reconstruível via replay do Kafka
# (ADR-0003). Separar de volta em produção é uma opção YAGNI se a carga ou o
# isolamento de falha real exigirem. Schemas/roles criados via Flyway (ADR-0004).
# ===========================================================================
resource "aws_db_subnet_group" "main" {
  name       = "fluxo-caixa-${var.environment}"
  subnet_ids = aws_subnet.private[*].id

  tags = { Name = "fluxo-caixa-${var.environment}" }
}

resource "aws_db_instance" "cashflow" {
  identifier     = "fluxo-caixa-${var.environment}"
  engine         = "postgres"
  engine_version = "16.3"
  instance_class = var.rds_instance_class

  db_name  = "cashflow"      # banco único — 2 schemas lógicos (ADR-0013)
  username = "cashflow"      # master (admin/migrations); apps usam roles RDS IAM próprias
  password = var.db_password # injetado via Secrets Manager (ADR-0007)

  # RDS IAM auth (ADR-0007) — token de vida curta, sem segredo estático no serviço
  iam_database_authentication_enabled = true

  allocated_storage       = 20
  storage_encrypted       = true
  kms_key_id              = aws_kms_key.main.arn
  vpc_security_group_ids  = [aws_security_group.rds.id]
  db_subnet_group_name    = aws_db_subnet_group.main.name

  multi_az                    = local.rds_multi_az
  backup_retention_period     = local.rds_backup_retention
  skip_final_snapshot         = local.rds_skip_final_snapshot
  performance_insights_enabled = local.rds_performance_insights

  tags = { Name = "fluxo-caixa-${var.environment}" }
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
    value = local.ecs_container_insights ? "enabled" : "disabled"
  }
}

resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/fluxo-caixa-${var.environment}"
  retention_in_days = local.log_retention_days
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
        # Duas roles RDS IAM — uma por schema (ADR-0013); passwordless (ADR-0007)
        Resource = [
          "arn:aws:rds-db:${var.aws_region}:*:dbuser:*/lancamentos",
          "arn:aws:rds-db:${var.aws_region}:*:dbuser:*/consolidado"
        ]
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
