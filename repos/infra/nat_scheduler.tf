# Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
# All rights reserved.
# PROPRIETARY AND CONFIDENTIAL.
# No part of this software may be reproduced, distributed, or transmitted
# in any form or by any means without express written permission.

# ===========================================================================
# NAT Instance stop/start — dev 10h/dia (ADR-0014)
#
# Para a NAT Instance fora do horário de uso (FinOps). Janela idêntica ao RDS
# (08:00–18:00 America/Sao_Paulo, UTC-3), superconjunto do Fargate (8h/dia).
# Cron UTC: 08:00 BRT = 11:00 UTC · 18:00 BRT = 21:00 UTC.
# Ativo somente quando environments/<env>.yaml define nat.stop_start: true
# (dev); qas/prd mantêm a NAT 24/7.
#
# Nota FinOps (EIP idle): com a ENI fixa (necessária para a rota privada
# sobreviver ao stop/start), o IP público vem de um EIP. Quando a instância
# está parada (14h/dia), o EIP ocioso é cobrado (~US$ 0,005/h ≈ US$ 2,10/mês).
# Alternativa sem EIP: auto-assign de IP público + rota por instance_id (legado).
# ===========================================================================

data "archive_file" "nat_scheduler" {
  type        = "zip"
  source_file = "${path.module}/lambda/nat_scheduler/index.py"
  output_path = "${path.module}/.build/nat_scheduler.zip"
}

resource "aws_iam_role" "nat_scheduler" {
  count = local.nat_stop_start ? 1 : 0

  name = "fluxo-caixa-nat-scheduler-${local.environment}"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "nat_scheduler" {
  count = local.nat_stop_start ? 1 : 0

  name = "fluxo-caixa-nat-scheduler"
  role = aws_iam_role.nat_scheduler[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        # least-privilege — só a NAT Instance da solução (ADR-0007)
        Effect   = "Allow"
        Action   = ["ec2:StartInstances", "ec2:StopInstances"]
        Resource = [aws_instance.nat.arn]
      },
      {
        # CloudWatch Logs do próprio Lambda (padrão)
        Effect   = "Allow"
        Action   = ["logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"]
        Resource = ["*"]
      }
    ]
  })
}

resource "aws_lambda_function" "nat_scheduler" {
  count = local.nat_stop_start ? 1 : 0

  function_name = "fluxo-caixa-nat-scheduler-${local.environment}"
  role          = aws_iam_role.nat_scheduler[0].arn
  handler       = "index.handler"
  runtime       = "python3.12"
  timeout       = 60

  filename         = data.archive_file.nat_scheduler.output_path
  source_code_hash = data.archive_file.nat_scheduler.output_base64sha256

  environment {
    variables = {
      NAT_INSTANCE_ID = aws_instance.nat.id
    }
  }
}

# --- Start: 08:00 BRT (11:00 UTC) ---
resource "aws_cloudwatch_event_rule" "nat_start" {
  count = local.nat_stop_start ? 1 : 0

  name                = "fluxo-caixa-nat-start-${local.environment}"
  description         = "Inicia a NAT Instance às 08:00 BRT (11:00 UTC) — janela dev 10h/dia (ADR-0014)"
  schedule_expression = "cron(0 11 * * ? *)"
}

resource "aws_cloudwatch_event_target" "nat_start" {
  count = local.nat_stop_start ? 1 : 0

  rule      = aws_cloudwatch_event_rule.nat_start[0].name
  target_id = "nat-start"
  arn       = aws_lambda_function.nat_scheduler[0].arn
  input     = jsonencode({ action = "start" })
}

resource "aws_lambda_permission" "nat_start" {
  count = local.nat_stop_start ? 1 : 0

  statement_id  = "AllowEventBridgeStart"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.nat_scheduler[0].function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.nat_start[0].arn
}

# --- Stop: 18:00 BRT (21:00 UTC) ---
resource "aws_cloudwatch_event_rule" "nat_stop" {
  count = local.nat_stop_start ? 1 : 0

  name                = "fluxo-caixa-nat-stop-${local.environment}"
  description         = "Para a NAT Instance às 18:00 BRT (21:00 UTC) — janela dev 10h/dia (ADR-0014)"
  schedule_expression = "cron(0 21 * * ? *)"
}

resource "aws_cloudwatch_event_target" "nat_stop" {
  count = local.nat_stop_start ? 1 : 0

  rule      = aws_cloudwatch_event_rule.nat_stop[0].name
  target_id = "nat-stop"
  arn       = aws_lambda_function.nat_scheduler[0].arn
  input     = jsonencode({ action = "stop" })
}

resource "aws_lambda_permission" "nat_stop" {
  count = local.nat_stop_start ? 1 : 0

  statement_id  = "AllowEventBridgeStop"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.nat_scheduler[0].function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.nat_stop[0].arn
}
