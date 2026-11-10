# Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
# All rights reserved.
# PROPRIETARY AND CONFIDENTIAL.
# No part of this software may be reproduced, distributed, or transmitted
# in any form or by any means without express written permission.

# ===========================================================================
# RDS stop/start — dev 10h/dia (ADR-0014)
#
# Para a instância RDS fora do horário de uso para cortar compute (FinOps,
# alvo ~US$ 30/mês). A janela de 10h (08:00–18:00 America/Sao_Paulo, UTC-3) é
# um superconjunto da janela do Fargate (8h/dia). O cron do EventBridge é UTC:
#   08:00 BRT = 11:00 UTC · 18:00 BRT = 21:00 UTC.
# Ativo somente quando environments/<env>.yaml define rds.stop_start: true
# (dev); qas/prd mantêm o RDS 24/7.
# ===========================================================================

data "archive_file" "rds_scheduler" {
  type        = "zip"
  source_file = "${path.module}/lambda/rds_scheduler/index.py"
  output_path = "${path.module}/.build/rds_scheduler.zip"
}

resource "aws_iam_role" "rds_scheduler" {
  count = local.rds_stop_start ? 1 : 0

  name = "fluxo-caixa-rds-scheduler-${var.environment}"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "rds_scheduler" {
  count = local.rds_stop_start ? 1 : 0

  name = "fluxo-caixa-rds-scheduler"
  role = aws_iam_role.rds_scheduler[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        # least-privilege — só a instância RDS da solução (ADR-0007)
        Effect   = "Allow"
        Action   = ["rds:DescribeDBInstances", "rds:StartDBInstance", "rds:StopDBInstance"]
        Resource = [aws_db_instance.cashflow.arn]
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

resource "aws_lambda_function" "rds_scheduler" {
  count = local.rds_stop_start ? 1 : 0

  function_name = "fluxo-caixa-rds-scheduler-${var.environment}"
  role          = aws_iam_role.rds_scheduler[0].arn
  handler       = "index.handler"
  runtime       = "python3.12"
  timeout       = 60

  filename         = data.archive_file.rds_scheduler.output_path
  source_code_hash = data.archive_file.rds_scheduler.output_base64sha256

  environment {
    variables = {
      DB_INSTANCE_IDENTIFIER = aws_db_instance.cashflow.identifier
    }
  }
}

# --- Start: 08:00 BRT (11:00 UTC) ---
resource "aws_cloudwatch_event_rule" "rds_start" {
  count = local.rds_stop_start ? 1 : 0

  name                = "fluxo-caixa-rds-start-${var.environment}"
  description         = "Inicia o RDS às 08:00 BRT (11:00 UTC) — janela dev 10h/dia (ADR-0014)"
  schedule_expression = "cron(0 11 * * ? *)"
}

resource "aws_cloudwatch_event_target" "rds_start" {
  count = local.rds_stop_start ? 1 : 0

  rule      = aws_cloudwatch_event_rule.rds_start[0].name
  target_id = "rds-start"
  arn       = aws_lambda_function.rds_scheduler[0].arn
  input     = jsonencode({ action = "start" })
}

resource "aws_lambda_permission" "rds_start" {
  count = local.rds_stop_start ? 1 : 0

  statement_id  = "AllowEventBridgeStart"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.rds_scheduler[0].function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.rds_start[0].arn
}

# --- Stop: 18:00 BRT (21:00 UTC) ---
resource "aws_cloudwatch_event_rule" "rds_stop" {
  count = local.rds_stop_start ? 1 : 0

  name                = "fluxo-caixa-rds-stop-${var.environment}"
  description         = "Para o RDS às 18:00 BRT (21:00 UTC) — janela dev 10h/dia (ADR-0014)"
  schedule_expression = "cron(0 21 * * ? *)"
}

resource "aws_cloudwatch_event_target" "rds_stop" {
  count = local.rds_stop_start ? 1 : 0

  rule      = aws_cloudwatch_event_rule.rds_stop[0].name
  target_id = "rds-stop"
  arn       = aws_lambda_function.rds_scheduler[0].arn
  input     = jsonencode({ action = "stop" })
}

resource "aws_lambda_permission" "rds_stop" {
  count = local.rds_stop_start ? 1 : 0

  statement_id  = "AllowEventBridgeStop"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.rds_scheduler[0].function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.rds_stop[0].arn
}
