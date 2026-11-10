# Copyright (C) 2026 MAX JEISON PRASS TECNOLOGIA LTDA (CNPJ 26.360.770/0001-39)
# All rights reserved.
# PROPRIETARY AND CONFIDENTIAL.
# No part of this software may be reproduced, distributed, or transmitted
# in any form or by any means without express written permission.

"""NAT Instance stop/start — dev 10h/dia (ADR-0014).

Acionado por EventBridge (cron). O evento informa `action` = "start" | "stop".
A instância alvo vem da variável de ambiente NAT_INSTANCE_ID.
"""

import os

import boto3
from botocore.exceptions import ClientError

INSTANCE_ID = os.environ["NAT_INSTANCE_ID"]
ec2 = boto3.client("ec2")


def handler(event, context):
    action = event.get("action")

    try:
        if action == "start":
            ec2.start_instances(InstanceIds=[INSTANCE_ID])
        elif action == "stop":
            ec2.stop_instances(InstanceIds=[INSTANCE_ID])
        else:
            raise ValueError(f"action inválida: {action!r}")
    except ClientError as exc:
        if exc.response["Error"]["Code"] == "IncorrectInstanceState":
            # já no estado desejado (ou em transição) — no-op idempotente
            return {"action": action, "instance": INSTANCE_ID, "status": "noop"}
        raise

    return {"action": action, "instance": INSTANCE_ID, "status": "ok"}
